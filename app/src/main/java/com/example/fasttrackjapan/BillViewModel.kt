package com.example.fasttrackjapan

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

class BillViewModel : ViewModel() {
    private val _bills = mutableStateListOf<Bill>()
    val bills: List<Bill> get() = _bills

    init {
        fetchBills()
    }

    fun fetchBills() {
        viewModelScope.launch {
            try {
                val user = Supabase.client.auth.currentUserOrNull()
                if (user != null) {
                    val billsFromDb = Supabase.client.postgrest["bills"]
                        .select {
                            filter {
                                eq("userId", user.id)
                            }
                        }
                        .decodeList<Bill>()
                    
                    _bills.clear()
                    _bills.addAll(billsFromDb)
                } else {
                    _bills.clear()
                }
            } catch (e: Exception) {
                Log.e("BillViewModel", "Error fetching bills: ${e.message}")
            }
        }
    }

    fun clearBills() {
        _bills.clear()
    }

    fun addBill(context: Context, label: String, date: String, imageUri: Uri) {
        Log.d("BillViewModel", "Starting addBill: label=$label, date=$date, uri=$imageUri")
        viewModelScope.launch {
            try {
                val user = Supabase.client.auth.currentUserOrNull()
                if (user == null) {
                    Log.e("BillViewModel", "Error: No user logged in")
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Error: You must be logged in", android.widget.Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }
                Log.d("BillViewModel", "Logged in user ID: ${user.id}")

                val fileName = "${user.id}/${System.currentTimeMillis()}.jpg"
                val bucket = Supabase.client.storage["Bills"]

                // 1. Upload to Storage
                Log.d("BillViewModel", "Reading bytes from URI...")
                val bytes = withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.openInputStream(imageUri)?.use { it.readBytes() }
                    } catch (e: Exception) {
                        Log.e("BillViewModel", "Failed to read image file", e)
                        null
                    }
                }
                
                if (bytes == null) {
                    Log.e("BillViewModel", "Error: Could not read bytes from URI")
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Error: Failed to read image", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                Log.d("BillViewModel", "Bytes read: ${bytes.size}. Starting upload to storage as $fileName...")

                try {
                    bucket.upload(fileName, bytes) {
                        upsert = true
                    }
                    Log.d("BillViewModel", "Upload successful")
                } catch (e: Exception) {
                    Log.e("BillViewModel", "Storage upload failed: ${e.message}", e)
                    withContext(Dispatchers.Main) {
                        val errorMsg = if (e.message?.contains("policy") == true) 
                            "Storage Permission Error: Check your Supabase Policies" 
                            else "Upload failed: ${e.message}"
                        android.widget.Toast.makeText(context, errorMsg, android.widget.Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }

                // 2. Get Public URL
                val imageUrl = bucket.publicUrl(fileName)
                Log.d("BillViewModel", "Public URL obtained: $imageUrl")

                // 3. Save to Database
                val newBill = Bill(
                    label = label,
                    date = date,
                    imageUrl = imageUrl,
                    userId = user.id
                )
                Log.d("BillViewModel", "Inserting bill into database: $newBill")

                try {
                    Supabase.client.postgrest["bills"].insert(newBill)
                    Log.d("BillViewModel", "Database insert successful")
                } catch (e: Exception) {
                    Log.e("BillViewModel", "Database insert failed: ${e.message}", e)
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Database error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }

                // 4. Update UI
                _bills.add(newBill)
                Log.d("BillViewModel", "Bill added to local list. Total bills: ${_bills.size}")
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "Bill saved successfully!", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("BillViewModel", "Unexpected error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "An unexpected error occurred", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun deleteBill(bill: Bill) {
        viewModelScope.launch {
            try {
                // Delete from DB
                Supabase.client.postgrest["bills"].delete {
                    filter {
                        eq("id", bill.id)
                    }
                }

                // Delete the associated image from Storage so we don't leave orphaned files.
                storagePathFromPublicUrl(bill.imageUrl)?.let { path ->
                    try {
                        Supabase.client.storage["Bills"].delete(path)
                    } catch (e: Exception) {
                        Log.e("BillViewModel", "Failed to delete bill image: ${e.message}")
                    }
                }

                _bills.removeIf { it.id == bill.id }
            } catch (e: Exception) {
                Log.e("BillViewModel", "Error deleting bill: ${e.message}")
            }
        }
    }

    private fun storagePathFromPublicUrl(url: String): String? {
        val marker = "/object/public/Bills/"
        val idx = url.indexOf(marker)
        return if (idx == -1) null else url.substring(idx + marker.length).substringBefore('?')
    }

    fun updateBill(updatedBill: Bill) {
        viewModelScope.launch {
            try {
                Supabase.client.postgrest["bills"].update(updatedBill) {
                    filter {
                        eq("id", updatedBill.id)
                    }
                }
                val index = _bills.indexOfFirst { it.id == updatedBill.id }
                if (index != -1) {
                    _bills[index] = updatedBill
                }
            } catch (e: Exception) {
                Log.e("BillViewModel", "Error updating bill: ${e.message}")
            }
        }
    }

    fun sortBillsByLabel() {
        val sorted = _bills.sortedBy { it.label.lowercase() }
        _bills.clear()
        _bills.addAll(sorted)
    }

    fun sortBillsByDate() {
        // Sort chronologically by parsed date; unparseable dates sort last.
        val sorted = _bills.sortedBy { parseDateOrNull(it.date) ?: LocalDate.MAX }
        _bills.clear()
        _bills.addAll(sorted)
    }

    private fun parseDateOrNull(value: String): LocalDate? = try {
        LocalDate.parse(value)
    } catch (e: Exception) {
        null
    }
}

package com.example.fasttrackjapan

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

open class BillViewModel(app: Application) : AndroidViewModel(app) {

    /** Minimal repo surface for testability. `BillRepository` implements it in production. */
    interface Repo {
        suspend fun fetchBills(): List<Bill>
        suspend fun addBill(context: Context, label: String, date: String, imageUri: Uri): AddBillResult
        suspend fun updateBill(bill: Bill)
        suspend fun deleteBill(bill: Bill)
    }

    private val repo: Repo = DefaultRepo(BillRepository())
    private var testRepo: Repo? = null
    protected fun setRepoForTest(r: Repo) { testRepo = r }
    private val effectiveRepo: Repo get() = testRepo ?: repo

    private val _bills = mutableStateListOf<Bill>()
    val bills: List<Bill> get() = _bills

    fun fetchBills() {
        viewModelScope.launch {
            try {
                val list = effectiveRepo.fetchBills()
                _bills.clear()
                _bills.addAll(list)
            } catch (e: Exception) {
                Log.e("BillViewModel", "fetch failed: ${e.message}")
            }
        }
    }

    fun clearBills() {
        _bills.clear()
    }

    fun addBill(context: Context, label: String, date: String, imageUri: Uri) {
        Log.d("BillViewModel", "Starting addBill")
        viewModelScope.launch {
            try {
                when (val result = effectiveRepo.addBill(context, label, date, imageUri)) {
                    is AddBillResult.Success -> {
                        _bills.add(result.bill)
                        toast(context, "Bill saved successfully!")
                    }
                    is AddBillResult.NotLoggedIn -> toast(context, "Error: You must be logged in")
                    is AddBillResult.ReadFailed -> toast(context, "Error: Failed to read image")
                    is AddBillResult.UploadFailed -> {
                        val msg = if (result.message.contains("policy"))
                            "Storage Permission Error: Check your Supabase Policies"
                        else "Upload failed: ${result.message}"
                        toast(context, msg)
                    }
                    is AddBillResult.InsertFailed -> toast(context, "Database error: ${result.message}")
                }
            } catch (e: Exception) {
                Log.e("BillViewModel", "Unexpected error: ${e.message}", e)
                toast(context, "An unexpected error occurred")
            }
        }
    }

    private suspend fun toast(context: Context, message: String) {
        withContext(Dispatchers.Main) {
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    fun deleteBill(bill: Bill) {
        viewModelScope.launch {
            try {
                effectiveRepo.deleteBill(bill)
                _bills.removeIf { it.id == bill.id }
            } catch (e: Exception) {
                Log.e("BillViewModel", "delete failed: ${e.message}")
            }
        }
    }

    fun updateBill(updatedBill: Bill) {
        viewModelScope.launch {
            try {
                effectiveRepo.updateBill(updatedBill)
                val index = _bills.indexOfFirst { it.id == updatedBill.id }
                if (index != -1) _bills[index] = updatedBill
            } catch (e: Exception) {
                Log.e("BillViewModel", "update failed: ${e.message}")
            }
        }
    }

    fun sortBillsByLabel() {
        val sorted = _bills.sortedBy { it.label.lowercase() }
        _bills.clear()
        _bills.addAll(sorted)
    }

    fun sortBillsByDate() {
        val sorted = _bills.sortedBy { parseDateOrNull(it.date) ?: LocalDate.MAX }
        _bills.clear()
        _bills.addAll(sorted)
    }

    private fun parseDateOrNull(value: String): LocalDate? = try {
        LocalDate.parse(value)
    } catch (e: Exception) {
        null
    }

    /** Adapts the real repository to the ViewModel's Repo surface. */
    private class DefaultRepo(private val real: BillRepository) : Repo {
        override suspend fun fetchBills() = real.fetchBills()
        override suspend fun addBill(context: Context, label: String, date: String, imageUri: Uri) =
            real.addBill(context, label, date, imageUri)
        override suspend fun updateBill(bill: Bill) = real.updateBill(bill)
        override suspend fun deleteBill(bill: Bill) = real.deleteBill(bill)
    }

    companion object {
        /** Testing seam: build a VM against a fake Repo. */
        fun newForTest(fake: Repo): BillViewModel {
            return object : BillViewModel(Application()) {
                init { this.setRepoForTest(fake) }
            }
        }
    }
}

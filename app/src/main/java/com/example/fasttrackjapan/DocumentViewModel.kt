package com.example.fasttrackjapan

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

class DocumentViewModel(application: Application) : AndroidViewModel(application) {
    private val _documents = mutableStateListOf<ExpirationDocument>()
    val documents: List<ExpirationDocument> get() = _documents

    init {
        fetchDocuments()
    }

    fun fetchDocuments() {
        viewModelScope.launch {
            try {
                val user = Supabase.client.auth.currentUserOrNull()
                if (user != null) {
                    val docsFromDb = Supabase.client.postgrest["documents"]
                        .select {
                            filter {
                                eq("userId", user.id)
                            }
                        }
                        .decodeList<ExpirationDocument>()
                    
                    _documents.clear()
                    _documents.addAll(docsFromDb)
                    
                    DocumentReminderScheduler.schedule(getApplication())
                }
            } catch (e: Exception) {
                Log.e("DocumentViewModel", "Error fetching documents: ${e.message}")
            }
        }
    }

    fun addDocument(type: String, expirationDate: String, notificationLeadTime: Int) {
        viewModelScope.launch {
            try {
                val user = Supabase.client.auth.currentUserOrNull() ?: return@launch
                val newDoc = ExpirationDocument(
                    type = type,
                    expirationDate = expirationDate,
                    notificationLeadTime = notificationLeadTime,
                    userId = user.id
                )
                
                Supabase.client.postgrest["documents"].insert(newDoc)
                _documents.add(newDoc)
                
                DocumentReminderScheduler.schedule(getApplication())
            } catch (e: Exception) {
                Log.e("DocumentViewModel", "Error adding document: ${e.message}")
            }
        }
    }

    fun deleteDocument(doc: ExpirationDocument) {
        viewModelScope.launch {
            try {
                Supabase.client.postgrest["documents"].delete {
                    filter {
                        eq("id", doc.id)
                    }
                }
                _documents.removeIf { it.id == doc.id }
            } catch (e: Exception) {
                Log.e("DocumentViewModel", "Error deleting document: ${e.message}")
            }
        }
    }

    fun updateDocument(doc: ExpirationDocument) {
        viewModelScope.launch {
            try {
                Supabase.client.postgrest["documents"].update(doc) {
                    filter {
                        eq("id", doc.id)
                    }
                }
                val index = _documents.indexOfFirst { it.id == doc.id }
                if (index != -1) {
                    _documents[index] = doc
                }
                
                // Reschedule to update reminder timings
                DocumentReminderScheduler.schedule(getApplication())
            } catch (e: Exception) {
                Log.e("DocumentViewModel", "Error updating document: ${e.message}")
            }
        }
    }
    
    fun clearDocuments() {
        _documents.clear()
        DocumentReminderScheduler.cancel(getApplication())
    }
}

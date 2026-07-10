package com.example.fasttrackjapan

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

open class DocumentViewModel(application: Application) : AndroidViewModel(application) {

    /** Minimal repo surface for testability. `DocumentRepository` implements it in production. */
    interface Repo {
        suspend fun fetchDocuments(): List<ExpirationDocument>
        suspend fun addDocument(type: String, expirationDate: String, notificationLeadTime: Int): ExpirationDocument?
        suspend fun deleteDocument(id: String)
        suspend fun updateDocument(doc: ExpirationDocument)
    }

    private val repo: Repo = DefaultRepo(DocumentRepository())
    private var testRepo: Repo? = null
    protected fun setRepoForTest(r: Repo) { testRepo = r }
    private val effectiveRepo: Repo get() = testRepo ?: repo

    private val _documents = mutableStateListOf<ExpirationDocument>()
    val documents: List<ExpirationDocument> get() = _documents

    fun fetchDocuments() {
        viewModelScope.launch {
            try {
                val docs = effectiveRepo.fetchDocuments()
                _documents.clear()
                _documents.addAll(docs)
                DocumentReminderScheduler.schedule(getApplication())
            } catch (e: Exception) {
                Log.e("DocumentViewModel", "fetch failed: ${e.message}")
            }
        }
    }

    fun addDocument(type: String, expirationDate: String, notificationLeadTime: Int) {
        viewModelScope.launch {
            try {
                val newDoc = effectiveRepo.addDocument(type, expirationDate, notificationLeadTime) ?: return@launch
                _documents.add(newDoc)
                DocumentReminderScheduler.schedule(getApplication())
            } catch (e: Exception) {
                Log.e("DocumentViewModel", "add failed: ${e.message}")
            }
        }
    }

    fun deleteDocument(doc: ExpirationDocument) {
        viewModelScope.launch {
            try {
                effectiveRepo.deleteDocument(doc.id)
                _documents.removeIf { it.id == doc.id }
            } catch (e: Exception) {
                Log.e("DocumentViewModel", "delete failed: ${e.message}")
            }
        }
    }

    fun updateDocument(doc: ExpirationDocument) {
        viewModelScope.launch {
            try {
                effectiveRepo.updateDocument(doc)
                val index = _documents.indexOfFirst { it.id == doc.id }
                if (index != -1) _documents[index] = doc
                DocumentReminderScheduler.schedule(getApplication())
            } catch (e: Exception) {
                Log.e("DocumentViewModel", "update failed: ${e.message}")
            }
        }
    }

    fun clearDocuments() {
        _documents.clear()
        DocumentReminderScheduler.cancel(getApplication())
    }

    /** Adapts the real repository to the ViewModel's Repo surface. */
    private class DefaultRepo(private val real: DocumentRepository) : Repo {
        override suspend fun fetchDocuments() = real.fetchDocuments()
        override suspend fun addDocument(type: String, expirationDate: String, notificationLeadTime: Int) =
            real.addDocument(type, expirationDate, notificationLeadTime)
        override suspend fun deleteDocument(id: String) = real.deleteDocument(id)
        override suspend fun updateDocument(doc: ExpirationDocument) = real.updateDocument(doc)
    }

    companion object {
        /** Testing seam: build a VM against a fake Repo. */
        fun newForTest(fake: Repo): DocumentViewModel {
            return object : DocumentViewModel(Application()) {
                init { this.setRepoForTest(fake) }
            }
        }
    }
}

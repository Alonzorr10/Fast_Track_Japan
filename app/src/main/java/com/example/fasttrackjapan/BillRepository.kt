package com.example.fasttrackjapan

import android.content.Context
import android.net.Uri
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Owns Supabase reads/writes and Storage upload/delete for bills. */
class BillRepository {

    /** Bills owned by the current user, or empty if not signed in. */
    suspend fun fetchBills(): List<Bill> {
        val user = Supabase.client.auth.currentUserOrNull() ?: return emptyList()
        return Supabase.client.postgrest["bills"]
            .select { filter { eq("userId", user.id) } }
            .decodeList()
    }

    /**
     * Insert a bill with its uploaded image URL. Returns the persisted row.
     * The multi-step upload flow (read bytes → upload → publicUrl → insert) lives here
     * so the ViewModel is not responsible for Storage IO.
     */
    suspend fun addBill(context: Context, label: String, date: String, imageUri: Uri): AddBillResult {
        val user = Supabase.client.auth.currentUserOrNull() ?: return AddBillResult.NotLoggedIn
        val fileName = "${user.id}/${System.currentTimeMillis()}.jpg"
        val bucket = Supabase.client.storage["Bills"]

        val bytes = withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(imageUri)?.use { it.readBytes() }
            } catch (e: Exception) {
                null
            }
        } ?: return AddBillResult.ReadFailed

        try {
            bucket.upload(fileName, bytes) { upsert = true }
        } catch (e: Exception) {
            return AddBillResult.UploadFailed(e.message ?: "Upload failed")
        }

        val imageUrl = bucket.publicUrl(fileName)
        val newBill = Bill(label = label, date = date, imageUrl = imageUrl, userId = user.id)
        try {
            Supabase.client.postgrest["bills"].insert(newBill)
        } catch (e: Exception) {
            return AddBillResult.InsertFailed(e.message ?: "Insert failed")
        }
        return AddBillResult.Success(newBill)
    }

    suspend fun updateBill(bill: Bill) {
        Supabase.client.postgrest["bills"].update(bill) { filter { eq("id", bill.id) } }
    }

    /** Deletes the row and (best-effort) the associated Storage image. */
    suspend fun deleteBill(bill: Bill) {
        Supabase.client.postgrest["bills"].delete { filter { eq("id", bill.id) } }
        storagePathFromPublicUrl(bill.imageUrl)?.let { path ->
            try {
                Supabase.client.storage["Bills"].delete(path)
            } catch (_: Exception) {
                // Best-effort cleanup; the row is gone either way.
            }
        }
    }

    /** Extracts "<userId>/<file>.jpg" from a Supabase public URL. */
    internal fun storagePathFromPublicUrl(url: String): String? {
        val marker = "/object/public/Bills/"
        val idx = url.indexOf(marker)
        return if (idx == -1) null else url.substring(idx + marker.length).substringBefore('?')
    }
}

/** Outcome of the multi-step add-bill flow, so the ViewModel can surface the right toast. */
sealed class AddBillResult {
    data class Success(val bill: Bill) : AddBillResult()
    object NotLoggedIn : AddBillResult()
    object ReadFailed : AddBillResult()
    data class UploadFailed(val message: String) : AddBillResult()
    data class InsertFailed(val message: String) : AddBillResult()
}

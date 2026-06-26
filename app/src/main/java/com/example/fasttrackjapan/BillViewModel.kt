package com.example.fasttrackjapan

import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel

class BillViewModel : ViewModel() {
    private val _bills = mutableStateListOf<Bill>()
    val bills: List<Bill> get() = _bills

    fun addBill(label: String, date: String, imageUri: Uri) {
        _bills.add(Bill(label = label, date = date, imageUri = imageUri))
    }

    fun updateBill(updatedBill: Bill) {
        val index = _bills.indexOfFirst { it.id == updatedBill.id }
        if (index != -1) {
            _bills[index] = updatedBill
        }
    }

    fun sortBillsByLabel() {
        val sorted = _bills.sortedBy { it.label }
        _bills.clear()
        _bills.addAll(sorted)
    }

    fun sortBillsByDate() {
        val sorted = _bills.sortedBy { it.date }
        _bills.clear()
        _bills.addAll(sorted)
    }
}

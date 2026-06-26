package com.example.fasttrackjapan

import android.net.Uri
import java.util.Date

data class Bill(
    val id: String = java.util.UUID.randomUUID().toString(),
    val label: String,
    val date: String,
    val imageUri: Uri
)

package com.example.catcom.domain.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Message(
    val id: String = "",
    val senderId: String = "",
    val text: String = "",
    @ServerTimestamp
    val timestamp: Date? = null
)

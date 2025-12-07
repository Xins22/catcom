package com.example.catcom.domain.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Conversation(
    val id: String = "",
    val participants: List<String> = emptyList(),
    val lastMessage: String = "",
    @ServerTimestamp
    val lastTimestamp: Date? = null,
    
    // Field bantuan untuk UI (tidak disimpan di DB Firestore, diisi manual di Repository/ViewModel)
    var otherUserId: String = "",
    var otherUserName: String = ""
)

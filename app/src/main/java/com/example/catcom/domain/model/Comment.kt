package com.example.catcom.domain.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Comment(
    val id: String = "",
    val postId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorPhoto: String = "",
    val text: String = "",
    @ServerTimestamp
    val timestamp: Date? = null
)

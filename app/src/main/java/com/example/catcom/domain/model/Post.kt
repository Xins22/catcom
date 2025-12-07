package com.example.catcom.domain.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Post(
    val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorPhoto: String = "", // URL Foto Profil User
    val content: String = "",
    val mediaUrl: String = "", // URL Foto Postingan (Opsional)
    @ServerTimestamp
    val timestamp: Date? = null, // null saat dikirim, diisi otomatis oleh server
    val likeCount: Int = 0
)

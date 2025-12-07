package com.example.catcom.domain.model

import androidx.annotation.Keep

@Keep
data class User(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val petType: String = "Cat",
    val bio: String = "",
    val role: String = "member",
    val photoUrl: String = ""
)

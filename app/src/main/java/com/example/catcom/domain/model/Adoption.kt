package com.example.catcom.domain.model

import androidx.annotation.Keep
import com.google.firebase.firestore.DocumentId

@Keep
data class Adoption(
    @DocumentId
    val id: String = "",
    val ownerId: String = "",
    val petName: String = "",
    val petBreed: String = "",
    val description: String = "",
    val status: String = "available", // Perbaikan: lowercase agar konsisten dengan query
    val images: List<String> = emptyList(),
    val ownerName: String = "",
    val age: String = "",
    val contactInfo: String = "",
    val timestamp: Long = 0L
)

package com.example.catcom.domain.model

import androidx.annotation.Keep
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

@Keep
data class Adoption(
    @DocumentId
    val id: String = "",
    val ownerId: String = "",
    val petName: String = "",
    val petBreed: String = "",
    val description: String = "",
    val status: String = "available",
    val images: List<String> = emptyList(),
    val ownerName: String = "",
    val age: Int = 0,
    @ServerTimestamp
    val timestamp: Date? = null
)

package com.example.catcom.domain.model

data class Event(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val date: Long = 0L,
    val location: String = "",
    val organizerId: String = "",
    val imageUrl: String = ""
)

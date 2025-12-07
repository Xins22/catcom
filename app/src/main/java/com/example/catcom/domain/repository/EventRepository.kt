package com.example.catcom.domain.repository

import android.net.Uri
import com.example.catcom.common.Result
import com.example.catcom.domain.model.Event
import kotlinx.coroutines.flow.Flow

interface EventRepository {
    fun getEvents(): Flow<Result<List<Event>>>
    fun createEvent(event: Event, imageUri: Uri?): Flow<Result<Boolean>>
    fun deleteEvent(eventId: String): Flow<Result<Boolean>>
}

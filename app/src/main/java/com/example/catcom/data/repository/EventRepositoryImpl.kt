package com.example.catcom.data.repository

import android.content.Context
import android.net.Uri
import com.example.catcom.common.Result
import com.example.catcom.domain.model.Event
import com.example.catcom.domain.repository.EventRepository
import com.example.catcom.util.ImageUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    @ApplicationContext private val context: Context
) : EventRepository {

    override fun getEvents(): Flow<Result<List<Event>>> = callbackFlow {
        trySend(Result.Loading)
        
        val listenerRegistration = firestore.collection("events")
            .orderBy("date", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.Error(error))
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val events = snapshot.toObjects(Event::class.java)
                    trySend(Result.Success(events))
                }
            }

        awaitClose { listenerRegistration.remove() }
    }

    override fun createEvent(event: Event, imageUri: Uri?): Flow<Result<Boolean>> = flow {
        emit(Result.Loading)
        try {
            var imageUrl = ""

            // 1. Upload Image
            if (imageUri != null) {
                val compressedBytes = ImageUtils.compressImage(context, imageUri)
                if (compressedBytes != null) {
                    val filename = UUID.randomUUID().toString()
                    val ref = storage.reference.child("events/$filename.jpg")
                    ref.putBytes(compressedBytes).await()
                    imageUrl = ref.downloadUrl.await().toString()
                }
            }

            // 2. Save Event
            val eventId = UUID.randomUUID().toString()
            val newEvent = event.copy(id = eventId, imageUrl = imageUrl)
            
            firestore.collection("events").document(eventId).set(newEvent).await()
            
            emit(Result.Success(true))
        } catch (e: Exception) {
            emit(Result.Error(e))
        }
    }

    override fun deleteEvent(eventId: String): Flow<Result<Boolean>> = flow {
        emit(Result.Loading)
        try {
            // Optional: Hapus gambar dari Storage jika perlu (memerlukan logic ekstra untuk parsing URL)
            firestore.collection("events").document(eventId).delete().await()
            emit(Result.Success(true))
        } catch (e: Exception) {
            emit(Result.Error(e))
        }
    }
}

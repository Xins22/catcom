package com.example.catcom.data.repository

import com.example.catcom.common.Result
import com.example.catcom.domain.model.Conversation
import com.example.catcom.domain.model.Message
import com.example.catcom.domain.repository.ChatRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.launch

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ChatRepository {

    override fun startChat(targetUserId: String): Flow<Result<String>> = flow {
        emit(Result.Loading)
        try {
            val currentUserId = auth.currentUser?.uid ?: throw Exception("Not logged in")
            
            // Query untuk mencari conversation yang sudah ada
            // Note: Firestore tidak mendukung array-contains untuk multiple value sekaligus dengan urutan spesifik
            // dan 'participants' bisa saja [A, B] atau [B, A].
            // Solusi sederhana: query whereArrayContains 'participants' currentUserId, 
            // lalu filter di client side untuk targetUserId.
            // Solusi ideal: Buat ID conversation deterministik (misal: sort(uid1, uid2).join("_"))
            
            // Kita coba pakai ID deterministik untuk simplifikasi dan efisiensi
            val participants = listOf(currentUserId, targetUserId).sorted()
            val conversationId = "${participants[0]}_${participants[1]}"
            
            val docRef = firestore.collection("conversations").document(conversationId)
            val snapshot = docRef.get().await()
            
            if (!snapshot.exists()) {
                val newConversation = Conversation(
                    id = conversationId,
                    participants = participants,
                    lastMessage = "",
                    lastTimestamp = null
                )
                docRef.set(newConversation).await()
            }
            
            emit(Result.Success(conversationId))
            
        } catch (e: Exception) {
            emit(Result.Error(e))
        }
    }

    override fun getMessages(chatId: String): Flow<List<Message>> = callbackFlow {
        val listenerRegistration = firestore.collection("conversations")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val messages = snapshot.toObjects(Message::class.java)
                    trySend(messages)
                }
            }
        
        awaitClose { listenerRegistration.remove() }
    }

    override fun sendMessage(chatId: String, text: String): Flow<Result<Boolean>> = flow {
        try {
            val currentUserId = auth.currentUser?.uid ?: throw Exception("Not logged in")
            
            val messageId = UUID.randomUUID().toString()
            val message = Message(
                id = messageId,
                senderId = currentUserId,
                text = text,
                timestamp = null // ServerTimestamp
            )
            
            val conversationRef = firestore.collection("conversations").document(chatId)
            val messageRef = conversationRef.collection("messages").document(messageId)
            
            firestore.runTransaction { transaction ->
                transaction.set(messageRef, message)
                
                val updateData = mapOf(
                    "lastMessage" to text,
                    "lastTimestamp" to FieldValue.serverTimestamp()
                )
                // Gunakan SetOptions.merge() jika dokumen mungkin belum ada (walaupun seharusnya sudah ada)
                transaction.set(conversationRef, updateData, SetOptions.merge())
            }.await()
            
            emit(Result.Success(true))
        } catch (e: Exception) {
            emit(Result.Error(e))
        }
    }
    override fun getConversations(): Flow<List<Conversation>> = callbackFlow {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            close() // Tutup flow jika tidak login
            return@callbackFlow
        }

        // Listener untuk perubahan realtime di daftar chat
        val listenerRegistration = firestore.collection("conversations")
            .whereArrayContains("participants", currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    // Gunakan 'launch' (dari ProducerScope) untuk fetch data user secara async
                    // tanpa memblokir UI thread
                    launch {
                        val conversationList = snapshot.documents.mapNotNull { doc ->
                            val conv = doc.toObject(Conversation::class.java) ?: return@mapNotNull null

                            // 1. Cari ID lawan bicara
                            val otherId = conv.participants.find { it != currentUserId } ?: ""

                            // 2. Ambil Nama & Foto dari koleksi 'users'
                            var displayName = "Unknown"
                            var photoUrl = ""

                            if (otherId.isNotEmpty()) {
                                try {
                                    // Fetch dokumen user lawan bicara
                                    val userSnapshot = firestore.collection("users").document(otherId).get().await()
                                    displayName = userSnapshot.getString("displayName") ?: "No Name"
                                    photoUrl = userSnapshot.getString("photoUrl") ?: ""
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }

                            // 3. Update objek conversation dengan data asli
                            conv.copy(
                                id = doc.id,
                                otherUserId = otherId,
                                otherUserName = displayName,
                                // Jika di model Conversation ada field photo, set juga disini
                                // otherUserPhoto = photoUrl
                            )
                        }

                        // Sortir manual berdasarkan waktu terbaru
                        val sortedList = conversationList.sortedByDescending { it.lastTimestamp }

                        // Kirim data yang sudah lengkap ke UI
                        trySend(sortedList)
                    }
                }
            }

        awaitClose { listenerRegistration.remove() }
    }
}

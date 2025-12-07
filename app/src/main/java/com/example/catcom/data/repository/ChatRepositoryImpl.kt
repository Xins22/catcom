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
            close(Exception("Not logged in"))
            return@callbackFlow
        }

        val listenerRegistration = firestore.collection("conversations")
            .whereArrayContains("participants", currentUserId)
            //.orderBy("lastTimestamp", Query.Direction.DESCENDING) // Memerlukan index composite
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // close(error) // Jangan close flow agar UI bisa retry atau handle error state
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val conversations = snapshot.toObjects(Conversation::class.java)
                    
                    // Logic tambahan: Isi otherUserId dan otherUserName
                    // Note: otherUserName memerlukan fetch user data tambahan,
                    // di sini kita hanya set ID dulu atau nama dummy.
                    // Idealnya participants menyimpan object {uid, name, photo}
                    // atau kita fetch user detail terpisah.
                    
                    conversations.forEach { conversation ->
                        val otherId = conversation.participants.find { it != currentUserId } ?: ""
                        conversation.otherUserId = otherId
                        conversation.otherUserName = "User $otherId" // Placeholder, perlu fetch user profile
                    }
                    
                    // Sort manual karena query composite index mungkin belum ada
                    val sortedConversations = conversations.sortedByDescending { it.lastTimestamp }
                    
                    trySend(sortedConversations)
                }
            }

        awaitClose { listenerRegistration.remove() }
    }
}

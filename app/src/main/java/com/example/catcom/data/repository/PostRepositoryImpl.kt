package com.example.catcom.data.repository

import android.content.Context
import android.net.Uri
import com.example.catcom.common.Result
import com.example.catcom.domain.model.Comment
import com.example.catcom.domain.model.Post
import com.example.catcom.domain.repository.PostRepository
import com.example.catcom.util.ImageUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
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
class PostRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val storage: FirebaseStorage,
    @ApplicationContext private val context: Context
) : PostRepository {

    override fun getFeed(): Flow<Result<List<Post>>> = callbackFlow {
        trySend(Result.Loading)
        
        val listenerRegistration = firestore.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.Error(error))
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val posts = snapshot.toObjects(Post::class.java)
                    trySend(Result.Success(posts))
                }
            }

        awaitClose { listenerRegistration.remove() }
    }

    override fun createPost(content: String, imageUri: Uri?): Flow<Result<Boolean>> = flow {
        emit(Result.Loading)
        try {
            val user = auth.currentUser ?: throw Exception("User not logged in")
            val userId = user.uid
            // Gunakan displayName dari Auth, atau fallback ke "User" jika null
            val userName = user.displayName ?: "Catcom User"
            val userPhoto = user.photoUrl?.toString() ?: ""

            var downloadUrl = ""

            // 1. Upload Image if exists
            if (imageUri != null) {
                val compressedBytes = ImageUtils.compressImage(context, imageUri)
                if (compressedBytes != null) {
                    val filename = UUID.randomUUID().toString()
                    val ref = storage.reference.child("posts/$filename.jpg")
                    
                    // Upload byte array
                    ref.putBytes(compressedBytes).await()
                    
                    // Get Download URL
                    downloadUrl = ref.downloadUrl.await().toString()
                }
            }

            // 2. Create Post Object
            val postId = UUID.randomUUID().toString() // Generate ID manual atau biarkan firestore generate
            val newPost = Post(
                id = postId,
                authorId = userId,
                authorName = userName,
                authorPhoto = userPhoto,
                content = content,
                mediaUrl = downloadUrl,
                timestamp = null, // ServerTimestamp will fill this
                likeCount = 0
            )

            // 3. Save to Firestore
            // Gunakan document(postId) agar ID dokumen sama dengan ID di model
            firestore.collection("posts").document(postId).set(newPost).await()
            
            emit(Result.Success(true))

        } catch (e: Exception) {
            emit(Result.Error(e))
        }
    }

    override fun toggleLike(postId: String): Flow<Result<Boolean>> = flow {
        emit(Result.Loading)
        try {
            val user = auth.currentUser ?: throw Exception("User not logged in")
            val userId = user.uid
            val postRef = firestore.collection("posts").document(postId)
            val likeRef = postRef.collection("likes").document(userId)

            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(likeRef)
                
                if (snapshot.exists()) {
                    // Unlike
                    transaction.delete(likeRef)
                    transaction.update(postRef, "likeCount", FieldValue.increment(-1))
                } else {
                    // Like
                    val likeData = hashMapOf(
                        "userId" to userId,
                        "timestamp" to FieldValue.serverTimestamp()
                    )
                    transaction.set(likeRef, likeData)
                    transaction.update(postRef, "likeCount", FieldValue.increment(1))
                }
            }.await()

            emit(Result.Success(true))
        } catch (e: Exception) {
            emit(Result.Error(e))
        }
    }

    override fun getComments(postId: String): Flow<Result<List<Comment>>> = callbackFlow {
        trySend(Result.Loading)

        val listenerRegistration = firestore.collection("posts")
            .document(postId)
            .collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.Error(error))
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val comments = snapshot.toObjects(Comment::class.java)
                    trySend(Result.Success(comments))
                }
            }
        
        awaitClose { listenerRegistration.remove() }
    }

    override fun sendComment(postId: String, text: String): Flow<Result<Boolean>> = flow {
        emit(Result.Loading)
        try {
            val user = auth.currentUser ?: throw Exception("User not logged in")
            val userId = user.uid
            val userName = user.displayName ?: "Catcom User"
            val userPhoto = user.photoUrl?.toString() ?: ""
            
            val commentId = UUID.randomUUID().toString()
            val comment = Comment(
                id = commentId,
                postId = postId,
                authorId = userId,
                authorName = userName,
                authorPhoto = userPhoto,
                text = text,
                timestamp = null
            )

            val postRef = firestore.collection("posts").document(postId)
            val commentRef = postRef.collection("comments").document(commentId)

            firestore.runTransaction { transaction ->
                transaction.set(commentRef, comment)
                // Optional: transaction.update(postRef, "commentCount", FieldValue.increment(1))
            }.await()
            
            emit(Result.Success(true))
        } catch (e: Exception) {
            emit(Result.Error(e))
        }
    }

    override fun getUserPosts(userId: String): Flow<Result<List<Post>>> = callbackFlow {
        trySend(Result.Loading)

        val listenerRegistration = firestore.collection("posts")
            .whereEqualTo("authorId", userId)
            // .orderBy("timestamp", Query.Direction.DESCENDING) // Requires index
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.Error(error))
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val posts = snapshot.toObjects(Post::class.java)
                    // Manual sort if index is missing or query is simple
                    val sortedPosts = posts.sortedByDescending { it.timestamp }
                    trySend(Result.Success(sortedPosts))
                }
            }

        awaitClose { listenerRegistration.remove() }
    }

    override fun searchPosts(query: String): Flow<Result<List<Post>>> = callbackFlow {
        trySend(Result.Loading)
        if (query.isEmpty()) {
            trySend(Result.Success(emptyList()))
            awaitClose { }
            return@callbackFlow
        }

        val listenerRegistration = firestore.collection("posts")
            .whereGreaterThanOrEqualTo("content", query)
            .whereLessThanOrEqualTo("content", query + "\uf8ff")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.Error(error))
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val posts = snapshot.toObjects(Post::class.java)
                    trySend(Result.Success(posts))
                }
            }

        awaitClose { listenerRegistration.remove() }
    }
}

package com.example.catcom.data.repository

import android.content.Context
import android.net.Uri
import com.example.catcom.common.Result
import com.example.catcom.domain.model.User
import com.example.catcom.domain.repository.AuthRepository
import com.example.catcom.util.ImageUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    @ApplicationContext private val context: Context
) : AuthRepository {

    override fun login(email: String, pass: String): Flow<Result<String>> = flow {
        emit(Result.Loading)
        try {
            val authResult = auth.signInWithEmailAndPassword(email, pass).await()
            val uid = authResult.user?.uid
            if (uid != null) {
                emit(Result.Success(uid))
            } else {
                emit(Result.Error(Exception("Login failed: User ID is null")))
            }
        } catch (e: Exception) {
            emit(Result.Error(e))
        }
    }

    override fun register(email: String, pass: String, user: User): Flow<Result<String>> = flow {
        emit(Result.Loading)
        try {
            // 1. Create User in Auth
            val authResult = auth.createUserWithEmailAndPassword(email, pass).await()
            val uid = authResult.user?.uid ?: throw Exception("Registration failed: User ID is null")

            // 2. Save User Data to Firestore
            // Ensure the user object has the correct ID
            val userWithId = user.copy(uid = uid)
            firestore.collection("users").document(uid).set(userWithId).await()

            // 3. Update Profile Display Name in Auth (optional but good for consistency)
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(user.displayName)
                .build()
            auth.currentUser?.updateProfile(profileUpdates)?.await()

            emit(Result.Success(uid))
        } catch (e: Exception) {
            emit(Result.Error(e))
        }
    }

    override fun logout() {
        auth.signOut()
    }

    override fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    override fun updateProfile(name: String, bio: String, photoUri: Uri?): Flow<Result<Boolean>> = flow {
        emit(Result.Loading)
        try {
            val currentUser = auth.currentUser ?: throw Exception("User not logged in")
            val uid = currentUser.uid
            
            var photoUrl = currentUser.photoUrl?.toString() ?: ""

            // 1. Upload Photo if exists
            if (photoUri != null) {
                val compressedBytes = ImageUtils.compressImage(context, photoUri)
                if (compressedBytes != null) {
                    val filename = "profile_$uid.jpg"
                    val ref = storage.reference.child("users/$filename")
                    
                    ref.putBytes(compressedBytes).await()
                    photoUrl = ref.downloadUrl.await().toString()
                }
            }

            // 2. Update Firestore
            val updateData = hashMapOf<String, Any>(
                "displayName" to name,
                "bio" to bio,
                "photoUrl" to photoUrl
            )
            firestore.collection("users").document(uid).set(updateData, SetOptions.merge()).await()

            // 3. Update Auth Profile
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .setPhotoUri(if (photoUrl.isNotEmpty()) Uri.parse(photoUrl) else null)
                .build()
            
            currentUser.updateProfile(profileUpdates).await()

            emit(Result.Success(true))
        } catch (e: Exception) {
            emit(Result.Error(e))
        }
    }

    override fun searchUsers(query: String): Flow<Result<List<User>>> = callbackFlow {
        trySend(Result.Loading)
        if (query.isEmpty()) {
            trySend(Result.Success(emptyList()))
            awaitClose { }
            return@callbackFlow
        }

        val listenerRegistration = firestore.collection("users")
            .whereGreaterThanOrEqualTo("displayName", query)
            .whereLessThanOrEqualTo("displayName", query + "\uf8ff")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.Error(error))
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val users = snapshot.toObjects(User::class.java)
                    trySend(Result.Success(users))
                }
            }

        awaitClose { listenerRegistration.remove() }
    }
}

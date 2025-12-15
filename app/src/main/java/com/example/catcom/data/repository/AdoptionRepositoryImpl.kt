package com.example.catcom.data.repository

import com.example.catcom.common.Result
import com.example.catcom.domain.model.Adoption
import com.example.catcom.domain.repository.AdoptionRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdoptionRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : AdoptionRepository {

    override fun submitAdoption(adoption: Adoption): Flow<Result<Boolean>> = flow {
        emit(Result.Loading)
        try {
            val user = auth.currentUser ?: throw Exception("User not logged in")
            val userId = user.uid
            
            // Assign ID jika belum ada
            val adoptionId = if (adoption.id.isEmpty()) UUID.randomUUID().toString() else adoption.id
            
            val finalAdoption = adoption.copy(
                id = adoptionId,
                ownerId = userId,
                // Pastikan status default
                status = if (adoption.status.isEmpty()) "Available" else adoption.status,
                timestamp = System.currentTimeMillis() // Atau gunakan FieldValue.serverTimestamp() di map
            )
            
            firestore.collection("adoptions").document(adoptionId).set(finalAdoption).await()
            emit(Result.Success(true))
        } catch (e: Exception) {
            emit(Result.Error(e))
        }
    }

    override fun getAvailableAdoptions(): Flow<Result<List<Adoption>>> = callbackFlow {
        trySend(Result.Loading) // Kirim status Loading dulu

        // Gunakan addSnapshotListener (Real-time), BUKAN .get().await()
        val listener = firestore.collection("adoptions")
            .whereEqualTo("status", "available") // Filter hanya yang available
            .orderBy("timestamp", Query.Direction.DESCENDING) // Urutkan dari yang terbaru
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.Error(error))
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    // Ubah dokumen menjadi list object Adoption
                    val adoptions = snapshot.toObjects(Adoption::class.java)
                    trySend(Result.Success(adoptions))
                }
            }

        // Membersihkan listener saat layar ditutup agar tidak memori bocor
        awaitClose { listener.remove() }
    }
    override fun getUserAdoptions(userId: String): Flow<Result<List<Adoption>>> = callbackFlow {
        trySend(Result.Loading)
        
        val listenerRegistration = firestore.collection("adoptions")
            .whereEqualTo("ownerId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.Error(error))
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val adoptions = snapshot.toObjects(Adoption::class.java)
                    val sortedAdoptions = adoptions.sortedByDescending { it.timestamp }
                    trySend(Result.Success(sortedAdoptions))
                }
            }
            
        awaitClose { listenerRegistration.remove() }
    }

    override fun deleteAdoption(adoptionId: String): Flow<Result<Boolean>> = flow {
        emit(Result.Loading)
        try {
            firestore.collection("adoptions").document(adoptionId).delete().await()
            emit(Result.Success(true))
        } catch (e: Exception) {
            emit(Result.Error(e))
        }
    }

    override fun getAdoptionById(adoptionId: String): Flow<Result<Adoption>> = callbackFlow {
        trySend(Result.Loading)
        
        val listenerRegistration = firestore.collection("adoptions").document(adoptionId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.Error(error))
                    return@addSnapshotListener
                }
                
                if (snapshot != null && snapshot.exists()) {
                    val adoption = snapshot.toObject(Adoption::class.java)
                    if (adoption != null) {
                        trySend(Result.Success(adoption))
                    } else {
                        trySend(Result.Error(Exception("Adoption data is null")))
                    }
                } else {
                    trySend(Result.Error(Exception("Adoption not found")))
                }
            }
            
        awaitClose { listenerRegistration.remove() }
    }
}

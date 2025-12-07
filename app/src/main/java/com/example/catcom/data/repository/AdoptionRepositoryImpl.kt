package com.example.catcom.data.repository

import com.example.catcom.common.Result
import com.example.catcom.domain.model.Adoption
import com.example.catcom.domain.repository.AdoptionRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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
            // Pastikan data owner sesuai dengan user yang login
            val adoptionWithUser = adoption.copy(
                id = UUID.randomUUID().toString(),
                ownerId = user.uid,
                ownerName = user.displayName ?: "Owner"
                // timestamp will be set by server or keep as is if client sets it
            )
            
            firestore.collection("adoptions")
                .document(adoptionWithUser.id)
                .set(adoptionWithUser)
                .await()
            
            emit(Result.Success(true))
        } catch (e: Exception) {
            emit(Result.Error(e))
        }
    }

    override fun getAvailableAdoptions(): Flow<Result<List<Adoption>>> = callbackFlow {
        trySend(Result.Loading)
        
        // Asumsi: Ambil semua adopsi (bisa difilter status = 'available' jika ada field status)
        val listenerRegistration = firestore.collection("adoptions")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.Error(error))
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val adoptions = snapshot.toObjects(Adoption::class.java)
                    trySend(Result.Success(adoptions))
                }
            }
            
        awaitClose { listenerRegistration.remove() }
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
                    trySend(Result.Success(adoptions))
                }
            }

        awaitClose { listenerRegistration.remove() }
    }
}

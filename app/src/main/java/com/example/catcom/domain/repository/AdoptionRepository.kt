package com.example.catcom.domain.repository

import com.example.catcom.common.Result
import com.example.catcom.domain.model.Adoption
import kotlinx.coroutines.flow.Flow

interface AdoptionRepository {
    fun submitAdoption(adoption: Adoption): Flow<Result<Boolean>>
    fun getAvailableAdoptions(): Flow<Result<List<Adoption>>>
    fun getUserAdoptions(userId: String): Flow<Result<List<Adoption>>>
    fun deleteAdoption(adoptionId: String): Flow<Result<Boolean>>
    fun getAdoptionById(adoptionId: String): Flow<Result<Adoption>>
}

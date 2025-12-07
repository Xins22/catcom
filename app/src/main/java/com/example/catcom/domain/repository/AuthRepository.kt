package com.example.catcom.domain.repository

import android.net.Uri
import com.example.catcom.common.Result
import com.example.catcom.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun login(email: String, pass: String): Flow<Result<String>>
    fun register(email: String, pass: String, user: User): Flow<Result<String>>
    fun logout()
    fun isUserLoggedIn(): Boolean
    fun updateProfile(name: String, bio: String, photoUri: Uri?): Flow<Result<Boolean>>
    fun searchUsers(query: String): Flow<Result<List<User>>>
}

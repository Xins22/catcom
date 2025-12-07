package com.example.catcom.ui.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.catcom.common.Result
import com.example.catcom.domain.model.Adoption
import com.example.catcom.domain.model.Post
import com.example.catcom.domain.model.User
import com.example.catcom.domain.repository.AdoptionRepository
import com.example.catcom.domain.repository.AuthRepository
import com.example.catcom.domain.repository.PostRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val postRepository: PostRepository,
    private val adoptionRepository: AdoptionRepository,
    private val firestore: FirebaseFirestore, // Untuk fetch user detail manual
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _userData = MutableStateFlow<User?>(null)
    val userData: StateFlow<User?> = _userData.asStateFlow()

    private val _userPosts = MutableStateFlow<List<Post>>(emptyList())
    val userPosts: StateFlow<List<Post>> = _userPosts.asStateFlow()

    private val _userAdoptions = MutableStateFlow<List<Adoption>>(emptyList())
    val userAdoptions: StateFlow<List<Adoption>> = _userAdoptions.asStateFlow()
    
    private val _profileState = MutableStateFlow<String?>(null) // null = idle, else = error/success message
    val profileState: StateFlow<String?> = _profileState.asStateFlow()

    fun loadProfile(userId: String?) {
        val targetId = if (userId.isNullOrEmpty()) auth.currentUser?.uid else userId
        
        if (targetId != null) {
            fetchUserData(targetId)
            fetchUserPosts(targetId)
            fetchUserAdoptions(targetId)
        }
    }

    private fun fetchUserData(uid: String) {
        viewModelScope.launch {
            try {
                // Fetch user doc directly since AuthRepository doesn't have getUser(uid) exposed currently
                // Ideally this should be in AuthRepository
                val snapshot = firestore.collection("users").document(uid).get().await()
                if (snapshot.exists()) {
                    val user = snapshot.toObject(User::class.java)
                    _userData.value = user
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun fetchUserPosts(uid: String) {
        viewModelScope.launch {
            postRepository.getUserPosts(uid).collect { result ->
                if (result is Result.Success) {
                    _userPosts.value = result.data
                }
            }
        }
    }

    private fun fetchUserAdoptions(uid: String) {
        viewModelScope.launch {
            adoptionRepository.getUserAdoptions(uid).collect { result ->
                if (result is Result.Success) {
                    _userAdoptions.value = result.data
                }
            }
        }
    }

    fun updateProfile(name: String, bio: String, photoUri: Uri?) {
        viewModelScope.launch {
            authRepository.updateProfile(name, bio, photoUri).collect { result ->
                when (result) {
                    is Result.Loading -> _profileState.value = "Updating..."
                    is Result.Success -> {
                        _profileState.value = "Profile Updated"
                        loadProfile(null) // Reload current user profile
                    }
                    is Result.Error -> _profileState.value = "Error: ${result.exception.message}"
                }
            }
        }
    }

    fun logout() {
        authRepository.logout()
    }
    
    fun clearProfileState() {
        _profileState.value = null
    }
}

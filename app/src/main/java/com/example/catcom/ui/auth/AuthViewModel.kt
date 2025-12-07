package com.example.catcom.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.catcom.common.Result
import com.example.catcom.domain.model.User
import com.example.catcom.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AuthState {
    data object Idle : AuthState
    data object Loading : AuthState
    data object Success : AuthState
    data class Error(val message: String) : AuthState
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            authRepository.login(email, pass).collect { result ->
                when (result) {
                    is Result.Loading -> _authState.value = AuthState.Loading
                    is Result.Success -> _authState.value = AuthState.Success
                    is Result.Error -> _authState.value = AuthState.Error(result.exception.message ?: "Login failed")
                }
            }
        }
    }

    fun register(email: String, pass: String, name: String, petType: String) {
        viewModelScope.launch {
            // Create User object
            // uid is empty here, it will be assigned by the repository/firebase
            val newUser = User(
                uid = "",
                email = email,
                displayName = name,
                petType = petType
            )

            authRepository.register(email, pass, newUser).collect { result ->
                when (result) {
                    is Result.Loading -> _authState.value = AuthState.Loading
                    is Result.Success -> _authState.value = AuthState.Success
                    is Result.Error -> _authState.value = AuthState.Error(result.exception.message ?: "Registration failed")
                }
            }
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }
}

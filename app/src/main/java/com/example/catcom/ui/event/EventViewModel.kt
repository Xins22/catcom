package com.example.catcom.ui.event

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.catcom.common.Result
import com.example.catcom.domain.model.Event
import com.example.catcom.domain.repository.AuthRepository
import com.example.catcom.domain.repository.EventRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class EventViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val authRepository: AuthRepository,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = _events.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        checkUserRole()
        loadEvents()
    }

    private fun checkUserRole() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            viewModelScope.launch {
                try {
                    val document = firestore.collection("users").document(currentUser.uid).get().await()
                    val role = document.getString("role")
                    _isAdmin.value = role == "admin"
                } catch (e: Exception) {
                    _isAdmin.value = false
                }
            }
        } else {
            _isAdmin.value = false
        }
    }

    fun loadEvents() {
        viewModelScope.launch {
            eventRepository.getEvents().collect { result ->
                when (result) {
                    is Result.Loading -> _isLoading.value = true
                    is Result.Success -> {
                        _isLoading.value = false
                        _events.value = result.data
                    }
                    is Result.Error -> {
                        _isLoading.value = false
                        _errorMessage.value = result.exception.message
                    }
                }
            }
        }
    }

    fun createEvent(title: String, description: String, location: String, dateLong: Long, onSuccess: () -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _errorMessage.value = "User not logged in"
            return
        }
        
        val newEvent = Event(
            id = UUID.randomUUID().toString(),
            title = title,
            description = description,
            location = location,
            date = dateLong,
            imageUrl = "", // Default empty or upload logic if needed
            organizerId = currentUser.uid
        )

        viewModelScope.launch {
            eventRepository.createEvent(newEvent, null).collect { result ->
                when (result) {
                    is Result.Loading -> _isLoading.value = true
                    is Result.Success -> {
                        _isLoading.value = false
                        onSuccess()
                    }
                    is Result.Error -> {
                        _isLoading.value = false
                        _errorMessage.value = result.exception.message
                    }
                }
            }
        }
    }

    fun deleteEvent(eventId: String) {
        viewModelScope.launch {
            eventRepository.deleteEvent(eventId).collect { result ->
                when (result) {
                    is Result.Loading -> _isLoading.value = true
                    is Result.Success -> {
                        _isLoading.value = false
                        // List usually updates automatically via Flow
                    }
                    is Result.Error -> {
                        _isLoading.value = false
                        _errorMessage.value = result.exception.message
                    }
                }
            }
        }
    }
}

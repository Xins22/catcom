package com.example.catcom.ui.event

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.catcom.common.Result
import com.example.catcom.domain.model.Event
import com.example.catcom.domain.model.User
import com.example.catcom.domain.repository.EventRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class EventViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = _events.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    init {
        loadEvents()
        checkUserRole()
    }

    private fun checkUserRole() {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid
            if (uid != null) {
                try {
                    val snapshot = firestore.collection("users").document(uid).get().await()
                    if (snapshot.exists()) {
                        val role = snapshot.getString("role")
                        _isAdmin.value = role == "admin"
                    }
                } catch (e: Exception) {
                    _isAdmin.value = false
                }
            }
        }
    }

    private fun loadEvents() {
        viewModelScope.launch {
            eventRepository.getEvents().collect { result ->
                when (result) {
                    is Result.Loading -> _loading.value = true
                    is Result.Success -> {
                        _loading.value = false
                        _events.value = result.data
                    }
                    is Result.Error -> {
                        _loading.value = false
                        // Handle error
                    }
                }
            }
        }
    }

    fun createEvent(title: String, description: String, date: Date, location: String, imageUri: Uri?) {
        viewModelScope.launch {
            _loading.value = true
            val userId = auth.currentUser?.uid ?: ""
            val event = Event(
                title = title,
                description = description,
                date = date.time,
                location = location,
                organizerId = userId
            )
            eventRepository.createEvent(event, imageUri).collect { result ->
                if (result !is Result.Loading) {
                    _loading.value = false
                }
            }
        }
    }
}

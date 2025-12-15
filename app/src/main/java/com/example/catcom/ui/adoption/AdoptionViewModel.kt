package com.example.catcom.ui.adoption

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.catcom.common.Result
import com.example.catcom.domain.model.Adoption
import com.example.catcom.domain.repository.AdoptionRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdoptionViewModel @Inject constructor(
    private val repository: AdoptionRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    // 1. STATE LIST ADOPSI (Real-time)
    val adoptions: StateFlow<Result<List<Adoption>>> = repository.getAvailableAdoptions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Result.Loading
        )

    // 2. STATE DETAIL & FORM
    private val _selectedAdoption = MutableStateFlow<Adoption?>(null)
    val selectedAdoption: StateFlow<Adoption?> = _selectedAdoption

    private val _submissionState = MutableStateFlow<Result<Boolean>?>(null)
    val submissionState: StateFlow<Result<Boolean>?> = _submissionState

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isUserAdmin = MutableStateFlow(false)
    val isUserAdmin: StateFlow<Boolean> = _isUserAdmin

    val currentUserId: String
        get() = auth.currentUser?.uid ?: ""

    init {
        checkUserRole()
    }

    private fun checkUserRole() {
        // Todo: Implementasi cek role admin
    }

    fun loadAdoptionDetail(id: String) {
        _isLoading.value = true
        viewModelScope.launch {
            repository.getAdoptionById(id).collect { result ->
                if (result is Result.Success) {
                    _selectedAdoption.value = result.data
                }
                _isLoading.value = false
            }
        }
    }

    // 3. FUNGSI SUBMIT DIPERBAIKI
    fun submitAdoption(petName: String, breed: String, description: String, imageUrl: String, age: String, contactInfo: String) {
        viewModelScope.launch {
            _submissionState.value = Result.Loading

            // Ubah imageUrl string menjadi List<String> agar sesuai dengan Model Adoption
            val imagesList = if (imageUrl.isNotBlank()) listOf(imageUrl) else emptyList()

            val newAdoption = Adoption(
                ownerId = currentUserId,
                petName = petName,
                petBreed = breed,
                description = description,
                status = "Available",
                images = imagesList, // Perbaikan: gunakan 'images' bukan 'imageUrl'
                age = age,
                contactInfo = contactInfo,
                timestamp = 0L
            )

            repository.submitAdoption(newAdoption).collect { result ->
                _submissionState.value = result
            }
        }
    }

    fun deleteAdoption(adoptionId: String) {
        viewModelScope.launch {
            repository.deleteAdoption(adoptionId).collect {
            }
        }
    }

    fun resetState() {
        _submissionState.value = null
    }
}

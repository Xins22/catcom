package com.example.catcom.ui.adoption

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.catcom.common.Result
import com.example.catcom.domain.model.Adoption
import com.example.catcom.domain.repository.AdoptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdoptionViewModel @Inject constructor(
    private val repository: AdoptionRepository
) : ViewModel() {

    private val _submissionState = MutableStateFlow<Result<Boolean>?>(null)
    val submissionState: StateFlow<Result<Boolean>?> = _submissionState.asStateFlow()

    private val _adoptions = MutableStateFlow<Result<List<Adoption>>>(Result.Loading)
    val adoptions: StateFlow<Result<List<Adoption>>> = _adoptions.asStateFlow()

    init {
        fetchAdoptions()
    }

    private fun fetchAdoptions() {
        viewModelScope.launch {
            repository.getAvailableAdoptions().collect { result ->
                _adoptions.value = result
            }
        }
    }

    fun submitAdoption(petName: String, petBreed: String, description: String) {
        viewModelScope.launch {
            val adoption = Adoption(
                petName = petName,
                petBreed = petBreed,
                description = description
            )
            repository.submitAdoption(adoption).collect { result ->
                _submissionState.value = result
            }
        }
    }

    fun resetState() {
        _submissionState.value = null
    }
}

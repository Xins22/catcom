package com.example.catcom.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.catcom.common.Result
import com.example.catcom.domain.model.Post
import com.example.catcom.domain.model.User
import com.example.catcom.domain.repository.AuthRepository
import com.example.catcom.domain.repository.PostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val postRepository: PostRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _userResults = MutableStateFlow<List<User>>(emptyList())
    val userResults: StateFlow<List<User>> = _userResults.asStateFlow()

    private val _postResults = MutableStateFlow<List<Post>>(emptyList())
    val postResults: StateFlow<List<Post>> = _postResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
        searchJob?.cancel()

        if (newQuery.length > 2) {
            searchJob = viewModelScope.launch {
                delay(500) // Debounce
                performSearch(newQuery)
            }
        } else {
            _userResults.value = emptyList()
            _postResults.value = emptyList()
            _isLoading.value = false
        }
    }

    private suspend fun performSearch(query: String) {
        _isLoading.value = true
        
        // Flags to track if initial load is done for each
        var usersLoaded = false
        var postsLoaded = false
        
        val job = viewModelScope.launch {
            launch {
                authRepository.searchUsers(query).collect { result ->
                    when (result) {
                        is Result.Success -> {
                            _userResults.value = result.data
                            usersLoaded = true
                            checkLoading(usersLoaded, postsLoaded)
                        }
                        is Result.Error -> {
                            usersLoaded = true // Treat as loaded even if error
                            checkLoading(usersLoaded, postsLoaded)
                        }
                        is Result.Loading -> {
                            // Already handled by initial _isLoading.value = true
                        }
                    }
                }
            }

            launch {
                postRepository.searchPosts(query).collect { result ->
                    when (result) {
                        is Result.Success -> {
                            _postResults.value = result.data
                            postsLoaded = true
                            checkLoading(usersLoaded, postsLoaded)
                        }
                        is Result.Error -> {
                            postsLoaded = true // Treat as loaded even if error
                            checkLoading(usersLoaded, postsLoaded)
                        }
                        is Result.Loading -> {
                             // Already handled
                        }
                    }
                }
            }
        }
        
        // Keep the job running to listen for updates
        // _isLoading will be set to false once both have emitted at least once
    }

    private fun checkLoading(usersLoaded: Boolean, postsLoaded: Boolean) {
        if (usersLoaded && postsLoaded) {
            _isLoading.value = false
        }
    }
}

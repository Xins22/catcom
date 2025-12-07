package com.example.catcom.ui.comment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.catcom.common.Result
import com.example.catcom.domain.model.Comment
import com.example.catcom.domain.repository.PostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface CommentState {
    data object Loading : CommentState
    data class Success(val comments: List<Comment>) : CommentState
    data class Error(val message: String) : CommentState
}

@HiltViewModel
class CommentViewModel @Inject constructor(
    private val postRepository: PostRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // postId bisa diambil dari argumen navigasi jika menggunakan Navigation Component
    private val postId: String = checkNotNull(savedStateHandle["postId"])

    private val _commentState = MutableStateFlow<CommentState>(CommentState.Loading)
    val commentState: StateFlow<CommentState> = _commentState.asStateFlow()

    private val _commentText = MutableStateFlow("")
    val commentText: StateFlow<String> = _commentText.asStateFlow()

    init {
        loadComments()
    }

    fun loadComments() {
        viewModelScope.launch {
            postRepository.getComments(postId).collect { result ->
                when (result) {
                    is Result.Loading -> _commentState.value = CommentState.Loading
                    is Result.Success -> _commentState.value = CommentState.Success(result.data)
                    is Result.Error -> _commentState.value = CommentState.Error(result.exception.message ?: "Gagal memuat komentar")
                }
            }
        }
    }

    fun onCommentTextChanged(text: String) {
        _commentText.value = text
    }

    fun sendComment() {
        val text = _commentText.value
        if (text.isBlank()) return

        viewModelScope.launch {
            // Optimistic update logic could go here, but for simplicity we rely on Firestore realtime updates
            postRepository.sendComment(postId, text).collect { result ->
                if (result is Result.Success) {
                    _commentText.value = ""
                }
                // Handle error if needed (e.g. show toast)
            }
        }
    }
}

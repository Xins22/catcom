package com.example.catcom.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.catcom.common.Result
import com.example.catcom.domain.model.Conversation
import com.example.catcom.domain.model.Message
import com.example.catcom.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ChatUiState {
    data object Loading : ChatUiState
    data class Success(val chatId: String) : ChatUiState
    data class Error(val message: String) : ChatUiState
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {

    // --- Chat Room State ---
    private val _chatUiState = MutableStateFlow<ChatUiState>(ChatUiState.Loading)
    val chatUiState: StateFlow<ChatUiState> = _chatUiState.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _messageText = MutableStateFlow("")
    val messageText: StateFlow<String> = _messageText.asStateFlow()

    // --- Inbox State ---
    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    init {
        loadConversations()
    }

    // Dipanggil saat masuk ke Inbox
    private fun loadConversations() {
        viewModelScope.launch {
            chatRepository.getConversations().collect { list ->
                _conversations.value = list
            }
        }
    }

    // Dipanggil saat masuk ke Chat Room
    fun initChat(targetUserId: String) {
        viewModelScope.launch {
            _chatUiState.value = ChatUiState.Loading
            chatRepository.startChat(targetUserId).collect { result ->
                when (result) {
                    is Result.Success -> {
                        val chatId = result.data
                        _chatUiState.value = ChatUiState.Success(chatId)
                        observeMessages(chatId)
                    }
                    is Result.Error -> {
                        _chatUiState.value = ChatUiState.Error(result.exception.message ?: "Gagal memulai chat")
                    }
                    is Result.Loading -> {
                        // Already handled by initial state
                    }
                }
            }
        }
    }

    private fun observeMessages(chatId: String) {
        viewModelScope.launch {
            chatRepository.getMessages(chatId).collectLatest { msgs ->
                _messages.value = msgs
            }
        }
    }

    fun onMessageTextChanged(text: String) {
        _messageText.value = text
    }

    fun sendMessage() {
        val currentState = _chatUiState.value
        val text = _messageText.value
        
        if (currentState is ChatUiState.Success && text.isNotBlank()) {
            val chatId = currentState.chatId
            viewModelScope.launch {
                _messageText.value = "" // Clear input immediately (Optimistic)
                chatRepository.sendMessage(chatId, text).collect { result ->
                     if (result is Result.Error) {
                         // Handle error (e.g. show snackbar, restore text)
                     }
                }
            }
        }
    }
}

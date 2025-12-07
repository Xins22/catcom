package com.example.catcom.domain.repository

import com.example.catcom.common.Result
import com.example.catcom.domain.model.Conversation
import com.example.catcom.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun startChat(targetUserId: String): Flow<Result<String>>
    fun getMessages(chatId: String): Flow<List<Message>>
    fun sendMessage(chatId: String, text: String): Flow<Result<Boolean>>
    fun getConversations(): Flow<List<Conversation>>
}

package com.example.catcom.ui.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.catcom.domain.model.Message
import com.google.firebase.auth.auth
import com.google.firebase.Firebase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    targetUserId: String,
    targetUserName: String,
    viewModel: ChatViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    // Inisialisasi chat dengan target user
    LaunchedEffect(targetUserId) {
        viewModel.initChat(targetUserId)
    }

    val chatUiState by viewModel.chatUiState.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val messageText by viewModel.messageText.collectAsState()
    
    // Auto-scroll logic
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(targetUserName) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        },
        bottomBar = {
            ChatInput(
                text = messageText,
                onTextChanged = viewModel::onMessageTextChanged,
                onSendClick = viewModel::sendMessage
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            when (chatUiState) {
                is ChatUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is ChatUiState.Error -> {
                    Text(
                        text = (chatUiState as ChatUiState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is ChatUiState.Success -> {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(messages) { message ->
                            MessageBubble(message = message)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: Message) {
    val isMe = message.senderId == Firebase.auth.currentUser?.uid
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMe) 16.dp else 4.dp,
                bottomEnd = if (isMe) 4.dp else 16.dp
            ),
            color = if (isMe) MaterialTheme.colorScheme.primary else Color.LightGray,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(12.dp),
                color = if (isMe) MaterialTheme.colorScheme.onPrimary else Color.Black
            )
        }
        
        // Timestamp (Opsional, kecil di bawah bubble)
        /*
        if (message.timestamp != null) {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            Text(
                text = sdf.format(message.timestamp),
                fontSize = 10.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        */
    }
}

@Composable
fun ChatInput(
    text: String,
    onTextChanged: (String) -> Unit,
    onSendClick: () -> Unit
) {
    Surface(
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChanged,
                placeholder = { Text("Tulis pesan...") },
                modifier = Modifier.weight(1f),
                maxLines = 3
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            IconButton(
                onClick = onSendClick,
                enabled = text.isNotBlank()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Kirim",
                    tint = if (text.isNotBlank()) MaterialTheme.colorScheme.primary else Color.Gray
                )
            }
        }
    }
}

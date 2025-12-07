package com.example.catcom.ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.catcom.domain.model.Conversation
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    viewModel: ChatViewModel = hiltViewModel(),
    onNavigateToChat: (String, String) -> Unit // targetUserId, targetUserName
) {
    val conversations by viewModel.conversations.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pesan") }
            )
        }
    ) { paddingValues ->
        if (conversations.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Belum ada percakapan", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(conversations) { conversation ->
                    ConversationItem(
                        conversation = conversation,
                        onClick = {
                            onNavigateToChat(conversation.otherUserId, conversation.otherUserName)
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}

@Composable
fun ConversationItem(
    conversation: Conversation,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar Placeholder
        Surface(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxSize(),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = conversation.otherUserName.ifEmpty { "User" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                if (conversation.lastTimestamp != null) {
                    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                    Text(
                        text = sdf.format(conversation.lastTimestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = conversation.lastMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

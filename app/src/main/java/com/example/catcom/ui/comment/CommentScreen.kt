package com.example.catcom.ui.comment

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.catcom.domain.model.Comment
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentScreen(
    viewModel: CommentViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val commentState by viewModel.commentState.collectAsState()
    val commentText by viewModel.commentText.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Komentar") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        },
        bottomBar = {
            CommentInput(
                text = commentText,
                onTextChanged = viewModel::onCommentTextChanged,
                onSendClick = viewModel::sendComment
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            when (val state = commentState) {
                is CommentState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is CommentState.Success -> {
                    if (state.comments.isEmpty()) {
                        Text(
                            text = "Belum ada komentar",
                            color = Color.Gray,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(state.comments) { comment ->
                                CommentItem(comment = comment)
                            }
                        }
                    }
                }
                is CommentState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@Composable
fun CommentItem(comment: Comment) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        if (comment.authorPhoto.isNotEmpty()) {
            AsyncImage(
                model = comment.authorPhoto,
                contentDescription = "Foto Profil",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray)
                    .padding(4.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = comment.authorName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                if (comment.timestamp != null) {
                    val sdf = SimpleDateFormat("dd MMM HH:mm", Locale.getDefault())
                    Text(
                        text = sdf.format(comment.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        fontSize = 10.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = comment.text,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun CommentInput(
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
                placeholder = { Text("Tulis komentar...") },
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

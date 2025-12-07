package com.example.catcom.ui.feed

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    viewModel: FeedViewModel = hiltViewModel(),
    onPostCreated: () -> Unit
) {
    var content by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val uploadState by viewModel.uploadState.collectAsState()
    val context = LocalContext.current

    // Image Picker Launcher
    val pickMedia = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            selectedImageUri = uri
        }
    )

    // Observe upload state for navigation/feedback
    LaunchedEffect(uploadState) {
        when (uploadState) {
            is PostUploadState.Success -> {
                Toast.makeText(context, "Berhasil diposting!", Toast.LENGTH_SHORT).show()
                viewModel.resetUploadState()
                onPostCreated()
            }
            is PostUploadState.Error -> {
                val message = (uploadState as PostUploadState.Error).message
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Buat Postingan Baru") })
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Input Teks
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Apa yang kamu pikirkan?") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Image Preview
                if (selectedImageUri != null) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "Preview Foto",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Tombol Pilih Foto
                Button(onClick = {
                    pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) {
                    Text("Pilih Foto")
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Tombol Posting
                Button(
                    onClick = {
                        if (content.isNotBlank() || selectedImageUri != null) {
                            viewModel.createPost(content, selectedImageUri)
                        } else {
                            Toast.makeText(context, "Tulis sesuatu atau pilih foto", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uploadState !is PostUploadState.Loading
                ) {
                    Text("Posting")
                }
            }

            // Loading overlay
            if (uploadState is PostUploadState.Loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

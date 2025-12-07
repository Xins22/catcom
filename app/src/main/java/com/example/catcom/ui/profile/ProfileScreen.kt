package com.example.catcom.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.catcom.domain.model.Adoption
import com.example.catcom.domain.model.Post
import com.example.catcom.domain.model.User
import com.google.firebase.auth.auth
import com.google.firebase.Firebase

@Composable
fun ProfileScreen(
    userId: String, // "me" or actual UID
    viewModel: ProfileViewModel = hiltViewModel(),
    onNavigateToChat: (String) -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val currentUid = Firebase.auth.currentUser?.uid
    
    // Tentukan apakah ini profil sendiri
    val isSelf = userId == "me" || userId == currentUid

    LaunchedEffect(userId) {
        // Jika "me", pass null ke viewModel agar load current user
        val targetId = if (userId == "me") null else userId
        viewModel.loadProfile(targetId)
    }

    val userData by viewModel.userData.collectAsState()
    val userPosts by viewModel.userPosts.collectAsState()
    val userAdoptions by viewModel.userAdoptions.collectAsState()
    val profileState by viewModel.profileState.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            // Optional: Bisa tambah TopAppBar jika perlu, 
            // tapi design profile biasanya header menyatu dengan body
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            // 1. Header Profil
            if (userData != null) {
                ProfileHeader(
                    user = userData!!,
                    isSelf = isSelf,
                    onEditClick = { showEditDialog = true },
                    onChatClick = { onNavigateToChat(userData!!.uid) },
                    onLogoutClick = {
                        viewModel.logout()
                        onLogout()
                    }
                )
            } else {
                Box(modifier = Modifier.height(200.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Tabs
            TabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Postingan") }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Adopsi") }
                )
            }

            // 3. Content
            Box(modifier = Modifier.fillMaxSize()) {
                if (selectedTabIndex == 0) {
                    PostGridContent(posts = userPosts)
                } else {
                    AdoptionListContent(adoptions = userAdoptions)
                }
            }
        }
    }

    // Dialog Edit Profil
    if (showEditDialog && userData != null) {
        EditProfileDialog(
            user = userData!!,
            onDismiss = { showEditDialog = false },
            onConfirm = { name, bio, uri ->
                viewModel.updateProfile(name, bio, uri)
                showEditDialog = false
            }
        )
    }
}

@Composable
fun ProfileHeader(
    user: User,
    isSelf: Boolean,
    onEditClick: () -> Unit,
    onChatClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Foto Profil
        if (user.photoUrl.isNotEmpty()) {
            AsyncImage(
                model = user.photoUrl,
                contentDescription = "Foto Profil",
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray)
                    .padding(16.dp),
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Nama & Bio
        Text(
            text = user.displayName.ifEmpty { "User Tanpa Nama" },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        if (user.bio.isNotEmpty()) {
            Text(
                text = user.bio,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tombol Aksi
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (isSelf) {
                Button(onClick = onEditClick) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit Profil")
                }
                
                Button(
                    onClick = onLogoutClick,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Logout")
                }
            } else {
                Button(onClick = onChatClick) {
                    Icon(Icons.Default.Chat, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Chat")
                }
            }
        }
    }
}

@Composable
fun PostGridContent(posts: List<Post>) {
    if (posts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Belum ada postingan", color = Color.Gray)
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(4.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(posts) { post ->
                AsyncImage(
                    model = post.mediaUrl.ifEmpty { post.authorPhoto }, // Fallback logic
                    contentDescription = null,
                    modifier = Modifier
                        .aspectRatio(1f)
                        .padding(2.dp)
                        .background(Color.LightGray),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@Composable
fun AdoptionListContent(adoptions: List<Adoption>) {
    if (adoptions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Belum ada adopsi yang ditawarkan", color = Color.Gray)
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(adoptions) { adoption ->
                Card(
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = adoption.images.firstOrNull() ?: "",
                            contentDescription = null,
                            modifier = Modifier
                                .size(60.dp)
                                .clip(MaterialTheme.shapes.small),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = adoption.petName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${adoption.petBreed} • ${adoption.age} bulan",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditProfileDialog(
    user: User,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Uri?) -> Unit
) {
    var name by remember { mutableStateOf(user.displayName) }
    var bio by remember { mutableStateOf(user.bio) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profil") },
        text = {
            Column {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier.clickable { 
                        launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) {
                        if (selectedImageUri != null) {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else if (user.photoUrl.isNotEmpty()) {
                            AsyncImage(
                                model = user.photoUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(Color.LightGray)
                                    .padding(8.dp)
                            )
                        }
                        
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Ganti Foto",
                            tint = Color.White,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                .padding(4.dp)
                                .size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Lengkap") },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Bio") },
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(name, bio, selectedImageUri) }) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

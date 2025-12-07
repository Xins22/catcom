package com.example.catcom.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.catcom.domain.model.Post
import com.example.catcom.domain.model.User
import com.example.catcom.ui.feed.PostItem

@Composable
fun SearchScreen(
    viewModel: SearchViewModel = hiltViewModel(),
    onNavigateToProfile: (String) -> Unit,
    onNavigateToPostDetail: (String) -> Unit = {} // Opsional, bisa digunakan nanti
) {
    val query by viewModel.query.collectAsState()
    val userResults by viewModel.userResults.collectAsState()
    val postResults by viewModel.postResults.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Pengguna", "Postingan")

    Column(modifier = Modifier.fillMaxSize()) {
        // Search Bar
        SearchTopBar(
            query = query,
            onQueryChange = viewModel::onQueryChange,
            onClearClick = { viewModel.onQueryChange("") }
        )

        // Tabs
        TabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title) }
                )
            }
        }

        // Content
        Box(modifier = Modifier.fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                if (query.isEmpty()) {
                    EmptyStateMessage(message = "Cari teman baru atau postingan menarik...")
                } else {
                    when (selectedTabIndex) {
                        0 -> UserList(users = userResults, onUserClick = { onNavigateToProfile(it.uid) })
                        1 -> PostList(posts = postResults, onPostClick = { onNavigateToPostDetail(it.id) })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearClick: () -> Unit
) {
    Surface(
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Cari...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = onClearClick) {
                            Icon(Icons.Default.Clear, contentDescription = "Hapus")
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )
        }
    }
}

@Composable
fun UserList(users: List<User>, onUserClick: (User) -> Unit) {
    if (users.isEmpty()) {
        EmptyStateMessage("Tidak ada pengguna ditemukan.")
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(users) { user ->
                UserItem(user = user, onClick = { onUserClick(user) })
            }
        }
    }
}

@Composable
fun UserItem(user: User, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (user.photoUrl.isNotEmpty()) {
            AsyncImage(
                model = user.photoUrl,
                contentDescription = "Foto Profil",
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Surface(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxSize()
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = user.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (user.bio.isNotEmpty()) {
                Text(
                    text = user.bio,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun PostList(posts: List<Post>, onPostClick: (Post) -> Unit) {
    if (posts.isEmpty()) {
        EmptyStateMessage("Tidak ada postingan ditemukan.")
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(posts) { post ->
                // Menggunakan PostItem yang sudah ada di FeedScreen
                // Kita perlu membuat wrapper agar bisa menangani klik jika PostItem tidak support klik keseluruhan
                // Namun, PostItem biasanya kompleks. Untuk search, mungkin cukup tampilkan saja.
                // Jika ingin navigasi ke detail, kita harus pastikan PostItem mendukungnya atau wrap.
                // Di sini kita gunakan PostItem standard, dengan dummy callback untuk like/comment karena ini search result
                // Idealnya SearchViewModel juga handle like, tapi untuk MVP view-only is okay.
                
                // Perlu diperhatikan PostItem memiliki callback. Kita bisa kosongkan atau hubungkan ke ViewModel jika perlu.
                // Untuk sekarang, kita biarkan callback kosong atau navigasi standar.
                
                PostItem(
                    post = post,
                    isLiked = false, // Search result mungkin belum punya info like status user saat ini
                    onLikeClick = { }, // Disable like di search result untuk kesederhanaan
                    onCommentClick = { },
                    onUserClick = { } 
                )
            }
        }
    }
}

@Composable
fun EmptyStateMessage(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp)
        )
    }
}

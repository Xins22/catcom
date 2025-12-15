package com.example.catcom.ui.adoption

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.catcom.domain.model.Adoption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdoptionDetailScreen(
    adoptionId: String,
    viewModel: AdoptionViewModel = hiltViewModel(),
    onContactOwner: (String) -> Unit,
    onBack: () -> Unit
) {
    val adoption by viewModel.selectedAdoption.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isUserAdmin by viewModel.isUserAdmin.collectAsState()
    val currentUserId = viewModel.currentUserId

    LaunchedEffect(adoptionId) {
        viewModel.loadAdoptionDetail(adoptionId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detail Adopsi") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        },
        bottomBar = {
            adoption?.let { detail ->
                val isOwner = detail.ownerId == currentUserId
                val canDelete = isOwner || isUserAdmin

                Surface(
                    shadowElevation = 8.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        if (canDelete) {
                            Button(
                                onClick = {
                                    viewModel.deleteAdoption(detail.id)
                                    onBack()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Hapus Postingan")
                            }
                        } else {
                            Button(
                                onClick = { onContactOwner(detail.ownerId) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Hubungi Pemilik")
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                adoption?.let { detail ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Header Image
                        // Menggunakan list 'images' dari model, mengambil yang pertama atau kosong
                        val imageUrl = detail.images.firstOrNull() ?: ""
                        
                        if (imageUrl.isNotEmpty()) {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = "Foto Kucing",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp)
                                    .background(Color.LightGray),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Tidak ada gambar", color = Color.Gray)
                            }
                        }

                        Column(modifier = Modifier.padding(16.dp)) {
                            // Title & Breed
                            Text(
                                text = detail.petName, // Menggunakan petName sesuai model
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            // Info umur jika ada
                            val ageText = if (detail.age.isNotEmpty()) " • ${detail.age}" else ""
                            
                            Text(
                                text = "${detail.petBreed}$ageText", // Menggunakan petBreed
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Location / Contact Info
                            if (detail.contactInfo.isNotEmpty()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = detail.contactInfo,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Owner Info
                            Text(
                                text = "Pemilik",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    val ownerDisplay = if (detail.ownerName.isNotEmpty()) detail.ownerName else "Pemilik Kucing"
                                    Text(
                                        text = ownerDisplay,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Member Catcom",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Description
                            Text(
                                text = "Tentang Kucing",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = detail.description,
                                style = MaterialTheme.typography.bodyMedium,
                                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
                            )
                        }
                    }
                } ?: run {
                    if (!isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Data adopsi tidak ditemukan.")
                        }
                    }
                }
            }
        }
    }
}

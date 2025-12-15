package com.example.catcom.ui.adoption

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
import com.example.catcom.common.Result

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdoptionFormScreen(
    viewModel: AdoptionViewModel = hiltViewModel(),
    onSuccess: () -> Unit
) {
    var petName by remember { mutableStateOf("") }
    var petBreed by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var contactInfo by remember { mutableStateOf("") }
    var ownerName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    // State untuk URI gambar yang dipilih dari galeri
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    
    val submissionState by viewModel.submissionState.collectAsState()
    val context = LocalContext.current

    // Image Picker Launcher
    val pickMedia = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            selectedImageUri = uri
        }
    )

    LaunchedEffect(submissionState) {
        when (val state = submissionState) {
            is Result.Success -> {
                Toast.makeText(context, "Adoption submitted successfully!", Toast.LENGTH_SHORT).show()
                viewModel.resetState()
                onSuccess()
            }
            is Result.Error -> {
                Toast.makeText(context, "Error: ${state.exception.message}", Toast.LENGTH_LONG).show()
                viewModel.resetState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Submit for Adoption") })
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = petName,
                    onValueChange = { petName = it },
                    label = { Text("Pet Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = petBreed,
                    onValueChange = { petBreed = it },
                    label = { Text("Breed") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = age,
                    onValueChange = { age = it },
                    label = { Text("Pet Age (e.g. 2 years)") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Owner Name might be fetched from Auth user profile, but keeping input if desired
                OutlinedTextField(
                    value = ownerName,
                    onValueChange = { ownerName = it },
                    label = { Text("Owner Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = contactInfo,
                    onValueChange = { contactInfo = it },
                    label = { Text("Contact Info (Phone/Location)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                Spacer(modifier = Modifier.height(8.dp))

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
                Button(
                    onClick = {
                        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Pilih Foto")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (petName.isNotBlank() && petBreed.isNotBlank() && description.isNotBlank()) {
                            viewModel.submitAdoption(
                                petName = petName,
                                breed = petBreed,
                                description = description,
                                imageUrl = selectedImageUri?.toString() ?: "",
                                age = age,
                                contactInfo = contactInfo
                            )
                        } else {
                            Toast.makeText(context, "Please fill all required fields", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = submissionState !is Result.Loading
                ) {
                    Text("Submit")
                }
            }

            if (submissionState is Result.Loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

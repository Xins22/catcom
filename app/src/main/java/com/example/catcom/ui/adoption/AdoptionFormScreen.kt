package com.example.catcom.ui.adoption

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.catcom.common.Result

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdoptionFormScreen(
    viewModel: AdoptionViewModel = hiltViewModel(),
    onSuccess: () -> Unit
) {
    var petName by remember { mutableStateOf("") }
    var petBreed by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    
    val submissionState by viewModel.submissionState.collectAsState()
    val context = LocalContext.current

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
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
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
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                Button(
                    onClick = {
                        if (petName.isNotBlank() && petBreed.isNotBlank() && description.isNotBlank()) {
                            viewModel.submitAdoption(petName, petBreed, description)
                        } else {
                            Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = submissionState !is Result.Loading
                ) {
                    Text("Submit")
                }
            }

            if (submissionState is Result.Loading) {
                CircularProgressIndicator()
            }
        }
    }
}

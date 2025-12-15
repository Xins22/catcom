package com.example.catcom.ui.adoption

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.catcom.common.Result 
import com.example.catcom.domain.model.Adoption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdoptionListScreen(
    viewModel: AdoptionViewModel = hiltViewModel(),
    onNavigateToForm: () -> Unit,
    onItemClick: (String) -> Unit = {}
) {
    val adoptionsState by viewModel.adoptions.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Available Adoptions") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToForm) {
                Icon(Icons.Default.Add, contentDescription = "Add Adoption")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = adoptionsState) {
                is Result.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is Result.Error -> {
                    Text(
                        text = "Error: ${state.exception.message}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }

                is Result.Success -> {
                    val adoptionList = state.data

                    if (adoptionList.isEmpty()) {
                        Text(
                            text = "No adoptions available yet.",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(adoptionList) { adoption ->
                                AdoptionItem(
                                    adoption = adoption,
                                    onClick = { onItemClick(adoption.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdoptionItem(
    adoption: Adoption,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {
            Text(
                text = adoption.petName, // Updated field name
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Breed: ${adoption.petBreed}", // Updated field name
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Status: ${adoption.status}",
                style = MaterialTheme.typography.labelSmall,
                color = if (adoption.status.lowercase() == "available")
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.secondary
            )
        }
    }
}

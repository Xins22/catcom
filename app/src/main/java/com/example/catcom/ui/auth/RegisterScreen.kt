package com.example.catcom.ui.auth

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onNavigateToHome: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var petType by remember { mutableStateOf("") }

    val authState by viewModel.authState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthState.Success -> {
                Toast.makeText(context, "Selamat Bergabung!", Toast.LENGTH_SHORT).show()
                onNavigateToHome()
                viewModel.resetState()
            }
            is AuthState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
            }
            else -> {}
        }
    }

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Header
                Icon(
                    imageVector = Icons.Default.Pets,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Bergabung dengan Catcom",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Input Nama
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Lengkap") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Input Email
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Input Password
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Input Pet
                OutlinedTextField(
                    value = petType,
                    onValueChange = { petType = it },
                    label = { Text("Jenis Kucing (Cth: Persia)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Tombol Daftar
                Button(
                    onClick = {
                        if (name.isNotBlank() && email.isNotBlank() && password.isNotBlank() && petType.isNotBlank()) {
                            viewModel.register(email, password, name, petType)
                        } else {
                            Toast.makeText(context, "Mohon lengkapi semua data", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = authState !is AuthState.Loading
                ) {
                    Text("Daftar")
                }
            }

            // Loading
            if (authState is AuthState.Loading) {
                CircularProgressIndicator()
            }
        }
    }
}

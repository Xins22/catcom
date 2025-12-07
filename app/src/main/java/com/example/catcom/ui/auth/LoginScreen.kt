package com.example.catcom.ui.auth

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
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
fun LoginScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onNavigateToHome: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val authState by viewModel.authState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthState.Success -> {
                Toast.makeText(context, "Login Berhasil!", Toast.LENGTH_SHORT).show()
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
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Logo & Judul
                Icon(
                    imageVector = Icons.Default.Pets,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Catcom",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(48.dp))

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
                        imeAction = ImeAction.Done
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Tombol Masuk
                Button(
                    onClick = { viewModel.login(email, password) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = authState !is AuthState.Loading
                ) {
                    Text("Masuk")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Link Daftar
                Text(
                    text = "Belum punya akun? Daftar",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onNavigateToRegister() }
                )
            }

            // Loading Indicator
            if (authState is AuthState.Loading) {
                CircularProgressIndicator()
            }
        }
    }
}

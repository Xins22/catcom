package com.example.catcom.ui.event

import android.app.DatePickerDialog
import android.net.Uri
import android.widget.DatePicker
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.catcom.domain.model.Event
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventListScreen(
    viewModel: EventViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val events by viewModel.events.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Event Komunitas") }
            )
        },
        floatingActionButton = {
            if (isAdmin) {
                FloatingActionButton(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah Event")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            if (loading && events.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (events.isEmpty()) {
                Text(
                    text = "Belum ada event",
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.Gray
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(events) { event ->
                        EventItem(event = event)
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateEventDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { title, desc, date, loc, uri ->
                viewModel.createEvent(title, desc, date, loc, uri)
                showCreateDialog = false
            }
        )
    }
}

@Composable
fun EventItem(event: Event) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            if (event.imageUrl.isNotEmpty()) {
                AsyncImage(
                    model = event.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentScale = ContentScale.Crop
                )
            }
            
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                if (event.date != 0L) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        val sdf = SimpleDateFormat("EEEE, dd MMM yyyy • HH:mm", Locale("id", "ID"))
                        Text(
                            text = sdf.format(Date(event.date)),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = event.location,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = event.description,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun CreateEventDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Date, String, Uri?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(Date()) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    
    // Simple Date Picker Logic
    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
            calendar.set(year, month, dayOfMonth)
            selectedDate = calendar.time
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Buat Event Baru") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Judul Event") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Deskripsi") },
                    maxLines = 3
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Lokasi") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                    Text("Tanggal: ${sdf.format(selectedDate)}")
                    Button(onClick = { datePickerDialog.show() }) {
                        Text("Pilih")
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = { 
                        launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) 
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (selectedImageUri == null) "Upload Gambar" else "Ganti Gambar")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, description, selectedDate, location, selectedImageUri) },
                enabled = title.isNotBlank() && location.isNotBlank()
            ) {
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

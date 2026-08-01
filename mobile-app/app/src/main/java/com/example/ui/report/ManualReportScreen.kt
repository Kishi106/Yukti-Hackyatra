package com.example.ui.report

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.ui.map.Severity

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.example.data.UserPrefs
import com.example.network.ApiResult
import com.example.network.PotholeRepository
import com.example.services.LocationService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualReportScreen(navController: NavController) {
    var roadName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedSeverity by remember { mutableStateOf(Severity.MEDIUM) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var photoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    val isPhotoAttached by remember { derivedStateOf { photoBitmap != null || photoUri != null } }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val locationService = remember { LocationService(context) }
    val potholeRepository = remember { PotholeRepository() }
    val userPrefs = remember { UserPrefs(context) }

    var isSubmitting by remember { mutableStateOf(false) }
    var submitError by remember { mutableStateOf<String?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            photoBitmap = bitmap
            photoUri = null
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch(null)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            photoUri = uri
            photoBitmap = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Report a Pothole") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = Color.Transparent,
                contentPadding = PaddingValues(16.dp)
            ) {
                Button(
                    onClick = {
                        submitError = null
                        val severity = selectedSeverity.name.lowercase()
                        coroutineScope.launch {
                            isSubmitting = true
                            try {
                                val bytes = when {
                                    photoUri != null -> context.contentResolver.openInputStream(photoUri!!)?.use { it.readBytes() }
                                    photoBitmap != null -> {
                                        val stream = java.io.ByteArrayOutputStream()
                                        photoBitmap!!.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                                        stream.toByteArray()
                                    }
                                    else -> null
                                }

                                var photoUrlValue: String? = null
                                if (bytes != null) {
                                    when (val uploadResult = potholeRepository.uploadPhoto(bytes, "pothole_${System.currentTimeMillis()}.jpg")) {
                                        is ApiResult.Success -> photoUrlValue = uploadResult.data
                                        is ApiResult.Error -> {
                                            submitError = "Photo upload failed: ${uploadResult.message}"
                                            isSubmitting = false
                                            return@launch
                                        }
                                    }
                                }

                                val location = locationService.requestFreshLocation()
                                val lat = location?.latitude ?: 17.7293
                                val lng = location?.longitude ?: 83.3152
                                val reporterId = userPrefs.getUserId()

                                when (val result = potholeRepository.submitReport(
                                    lat = lat,
                                    lng = lng,
                                    severity = severity,
                                    source = "citizen",
                                    ward = null,
                                    photoUrl = photoUrlValue,
                                    reporterId = reporterId
                                )) {
                                    is ApiResult.Success -> {
                                        showSuccessDialog = true
                                    }
                                    is ApiResult.Error -> {
                                        submitError = result.message
                                    }
                                }
                            } catch (e: Exception) {
                                submitError = e.message ?: "Something went wrong while submitting the report."
                            } finally {
                                isSubmitting = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = isPhotoAttached && !isSubmitting
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.CloudUpload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Submit Report", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // GPS Location Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Current GPS Location", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Lat: 17.7293, Lng: 83.3152", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Input Fields Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = roadName,
                        onValueChange = { roadName = it },
                        label = { Text("Road Name (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Signpost, contentDescription = null) }
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5,
                        leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) }
                    )
                    
                    Text("Severity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SeverityOption(Severity.LOW, selectedSeverity) { selectedSeverity = it }
                        SeverityOption(Severity.MEDIUM, selectedSeverity) { selectedSeverity = it }
                        SeverityOption(Severity.HIGH, selectedSeverity) { selectedSeverity = it }
                    }
                }
            }

            // Photo Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Photo (Mandatory)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    if (isPhotoAttached) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.LightGray),
                            contentAlignment = Alignment.Center
                        ) {
                            if (photoUri != null) {
                                AsyncImage(
                                    model = photoUri,
                                    contentDescription = "Selected Photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else if (photoBitmap != null) {
                                AsyncImage(
                                    model = photoBitmap,
                                    contentDescription = "Captured Photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(
                            onClick = { 
                                photoBitmap = null
                                photoUri = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Remove Photo", color = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA) },
                                modifier = Modifier.weight(1f).height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Take Photo")
                            }
                            OutlinedButton(
                                onClick = { galleryLauncher.launch("image/*") },
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Gallery")
                            }
                        }
                    }
                }
            }

            if (submitError != null) {
                Text(
                    text = submitError ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text("Report Submitted Successfully", textAlign = TextAlign.Center)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Status: Pending Verification",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        navController.navigate("home") { popUpTo(0) }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Return to Home")
                }
            }
        )
    }
}

@Composable
fun RowScope.SeverityOption(
    severity: Severity,
    selected: Severity,
    onSelect: (Severity) -> Unit
) {
    val isSelected = severity == selected
    Surface(
        modifier = Modifier.weight(1f).height(48.dp),
        color = if (isSelected) severity.color else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        onClick = { onSelect(severity) }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = severity.label,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

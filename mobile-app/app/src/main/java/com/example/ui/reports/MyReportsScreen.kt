package com.example.ui.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.network.ApiResult
import com.example.network.PotholeDto
import com.example.network.PotholeRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyReportsScreen(navController: NavController) {
    val repository = remember { PotholeRepository() }
    var reports by remember { mutableStateOf<List<PotholeDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        when (val result = repository.getPotholes()) {
            is ApiResult.Success -> {
                reports = result.data
                errorMessage = null
            }
            is ApiResult.Error -> {
                errorMessage = result.message
            }
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Reports") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when {
                isLoading -> Text("Loading reports...", style = MaterialTheme.typography.bodyLarge)
                errorMessage != null -> Text(
                    "Failed to load reports: $errorMessage",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
                reports.isEmpty() -> Text("No reports yet.", style = MaterialTheme.typography.bodyLarge)
                else -> reports.forEach { report -> ReportDetailCard(report) }
            }
        }
    }
}

private fun humanizeStatus(status: String): String = when (status) {
    "new" -> "Pending"
    "in_progress" -> "In Progress"
    "fixed" -> "Repaired"
    else -> status.replaceFirstChar { it.uppercase() }
}

private fun formatTimestamp(iso: String): String = iso.replace("T", " ").take(16)

@Composable
fun ReportDetailCard(report: PotholeDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            // Photo Placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color(0xFFE0E0E0)),
                contentAlignment = Alignment.Center
            ) {
                if (report.photoUrl != null) {
                    AsyncImage(
                        model = report.photoUrl,
                        contentDescription = "Report Photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                // Status Badge
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Status: ${humanizeStatus(report.status)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Details
                Text("Road Name", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(report.ward ?: "Unspecified area", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(8.dp))

                DetailRow(icon = Icons.Default.LocationOn, label = "Location", value = "${report.lat}, ${report.lng}")
                DetailRow(icon = Icons.Default.Map, label = "Ward", value = report.ward ?: "Unknown")

                Spacer(modifier = Modifier.height(16.dp))

                Text("Description", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "${report.severity.replaceFirstChar { it.uppercase() }} severity pothole reported via " +
                        if (report.source == "auto") "automatic sensor detection." else "citizen report.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Timeline
                Text("Timeline", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                TimelineItem("Report Submitted", formatTimestamp(report.createdAt), isCompleted = true)
                TimelineItem("Verified", if (report.status != "new") "Confirmed" else "Pending", isCompleted = report.status != "new")
                TimelineItem("Repair Scheduled", if (report.status == "fixed") "Completed" else "Pending", isCompleted = report.status == "fixed")

                Spacer(modifier = Modifier.height(16.dp))

                // Map Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(Color(0xFFE0E0E0), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Map, contentDescription = null, tint = Color.Gray)
                    Text("Map Preview", color = Color.DarkGray, modifier = Modifier.padding(top = 40.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Buttons
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { /* Track */ }, modifier = Modifier.weight(1f).height(48.dp)) {
                            Icon(Icons.Default.DirectionsCar, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Track Status")
                        }
                        OutlinedButton(onClick = { /* Share */ }, modifier = Modifier.weight(1f).height(48.dp)) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Share")
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { /* Add Photo */ }, modifier = Modifier.weight(1f).height(48.dp)) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add Photo")
                        }
                        OutlinedButton(onClick = { /* Support */ }, modifier = Modifier.weight(1f).height(48.dp)) {
                            Icon(Icons.Default.Help, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Support")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun TimelineItem(title: String, time: String, isCompleted: Boolean) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (isCompleted) MaterialTheme.colorScheme.primary else Color.Gray
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = if (isCompleted) FontWeight.Bold else FontWeight.Normal)
            Text(time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

package com.example.ui.detection

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.services.DrivingManager
import com.example.services.LocationService
import com.example.viewmodels.DrivingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetectionScreen(
    navController: NavController,
    viewModel: DrivingViewModel = viewModel()
) {
    val context = LocalContext.current
    val isDriving by viewModel.isDriving.collectAsStateWithLifecycle()
    
    val currentSpeed by DrivingManager.currentSpeedKmh.collectAsStateWithLifecycle()
    val speedString by DrivingManager.currentSpeedString.collectAsStateWithLifecycle()
    val cooldownActive by viewModel.debugCooldownActive.collectAsStateWithLifecycle()
    val cooldownRemaining by viewModel.debugCooldownRemaining.collectAsStateWithLifecycle()
    val detectionState by viewModel.debugDetectionState.collectAsStateWithLifecycle()

    val possibleDetections by DrivingManager.possibleDetections.collectAsStateWithLifecycle()
    val confirmedReports by DrivingManager.confirmedReports.collectAsStateWithLifecycle()
    val ignoredDetections by DrivingManager.ignoredDetections.collectAsStateWithLifecycle()

    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Auto-Detection Engine") },
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Pulse Animation
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = if (isDriving) 1.12f else 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )

            Box(
                modifier = Modifier
                    .size(160.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(
                        if (isDriving) MaterialTheme.colorScheme.primaryContainer 
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = { 
                        if (isDriving) viewModel.stopDriving() else viewModel.startDriving()
                    },
                    modifier = Modifier.size(100.dp)
                ) {
                    Icon(
                        imageVector = if (isDriving) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = if (isDriving) "Stop" else "Start",
                        modifier = Modifier.size(56.dp),
                        tint = if (isDriving) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Text(
                text = if (isDriving) "Detection Active\nMonitoring road conditions..." else "Detection Paused\nTap to Start Driving Mode",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isDriving) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            // Production Detection Engine Status Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Live Detection Status", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    
                    SensorDataRow("Vehicle Speed", speedString)
                    SensorDataRow(
                        "Speed Requirement",
                        if (currentSpeed >= 2.0f) "✓ Moving (≥ 2 km/h)"
                        else "Stationary (< 2 km/h)"
                    )
                    SensorDataRow("Detection Engine", detectionState)
                    SensorDataRow("Cooldown Window", if (cooldownActive) "Active (${cooldownRemaining}s)" else "Ready (2.0s cooldown)")
                }
            }

            // Detection Summary Counters Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Session Detection Summary", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        CounterStat("Candidates", possibleDetections.toString(), MaterialTheme.colorScheme.primary)
                        CounterStat("Confirmed", confirmedReports.toString(), MaterialTheme.colorScheme.secondary)
                        CounterStat("Ignored", ignoredDetections.toString(), MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            icon = { 
                Icon(
                    Icons.Outlined.WarningAmber, 
                    contentDescription = null, 
                    tint = MaterialTheme.colorScheme.error, 
                    modifier = Modifier.size(48.dp)
                ) 
            },
            title = { Text("Possible Pothole Detected") },
            text = { Text("A vertical shock impact candidate was detected. Did you encounter a pothole?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDialog = false
                        DrivingManager.userConfirmedDetection()
                        navController.navigate("confirm_pothole")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("YES")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { 
                        showDialog = false
                        DrivingManager.userIgnoredDetection()
                    }
                ) {
                    Text("NO")
                }
            }
        )
    }
}

@Composable
fun CounterStat(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SensorDataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

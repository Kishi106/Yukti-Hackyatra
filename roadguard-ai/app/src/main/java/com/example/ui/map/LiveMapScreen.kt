package com.example.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.network.ApiResult
import com.example.network.PotholeDto
import com.example.network.PotholeRepository

enum class Severity(val color: Color, val label: String) {
    HIGH(Color(0xFFD32F2F), "High"),
    MEDIUM(Color(0xFFF57C00), "Medium"),
    LOW(Color(0xFFFBC02D), "Low"),
    REPAIRED(Color(0xFF2E7D32), "Repaired")
}

data class PotholeMarker(
    val id: String,
    val ward: String,
    val roadName: String,
    val status: String,
    val severity: Severity,
    val date: String,
    val distance: String,
    val xOffset: Float,
    val yOffset: Float
)

// Approximate bounding box for the pilot ward (GVMC / Visakhapatnam area), used to
// convert real lat/lng into the stylized map's proportional x/y offsets.
private const val WARD_MIN_LAT = 17.65
private const val WARD_MAX_LAT = 17.80
private const val WARD_MIN_LNG = 83.15
private const val WARD_MAX_LNG = 83.35

private fun humanizeStatus(status: String): String = when (status) {
    "new" -> "Pending"
    "in_progress" -> "In Progress"
    "fixed" -> "Repaired"
    else -> status.replaceFirstChar { it.uppercase() }
}

private fun mapSeverity(dto: PotholeDto): Severity {
    if (dto.status == "fixed") return Severity.REPAIRED
    return when (dto.severity.lowercase()) {
        "low" -> Severity.LOW
        "high" -> Severity.HIGH
        else -> Severity.MEDIUM
    }
}

private fun formatDisplayDate(iso: String): String {
    return try {
        val parser = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
        val date = parser.parse(iso.take(19))
        java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.US).format(date!!)
    } catch (e: Exception) {
        iso.take(10)
    }
}

private fun PotholeDto.toMarker(): PotholeMarker {
    val xOffset = ((lng - WARD_MIN_LNG) / (WARD_MAX_LNG - WARD_MIN_LNG)).toFloat().coerceIn(0.05f, 0.95f)
    val yOffset = (1f - ((lat - WARD_MIN_LAT) / (WARD_MAX_LAT - WARD_MIN_LAT)).toFloat()).coerceIn(0.05f, 0.95f)
    return PotholeMarker(
        id = id,
        ward = ward ?: "Unknown ward",
        roadName = ward ?: "Unspecified location",
        status = humanizeStatus(status),
        severity = mapSeverity(this),
        date = formatDisplayDate(createdAt),
        distance = "—",
        xOffset = xOffset,
        yOffset = yOffset
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveMapScreen(navController: androidx.navigation.NavController) {
    var selectedMarker by remember { mutableStateOf<PotholeMarker?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    val repository = remember { PotholeRepository() }
    var markers by remember { mutableStateOf<List<PotholeMarker>>(emptyList()) }

    LaunchedEffect(Unit) {
        when (val result = repository.getPotholes()) {
            is ApiResult.Success -> markers = result.data.map { it.toMarker() }
            is ApiResult.Error -> { /* keep the map empty on failure */ }
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("manual_report") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.AddLocation, contentDescription = "Report Pothole")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Fake Map Background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFE0E5EC))
            ) {
                // Grid Lines to simulate map
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceEvenly) {
                    repeat(10) { Divider(color = Color.White.copy(alpha = 0.5f)) }
                }
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    repeat(6) { Divider(color = Color.White.copy(alpha = 0.5f), modifier = Modifier.fillMaxHeight().width(1.dp)) }
                }
            }

            // Map Markers Overlay
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val width = maxWidth
                val height = maxHeight

                markers.forEach { marker ->
                    Box(
                        modifier = Modifier
                            .offset(x = width * marker.xOffset, y = height * marker.yOffset)
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable { selectedMarker = marker },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Marker",
                            tint = marker.severity.color,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }

            // Floating Controls
            MapControls()
        }
    }

    // Bottom Sheet for Marker Details
    if (selectedMarker != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedMarker = null },
            sheetState = sheetState
        ) {
            MarkerDetailsContent(marker = selectedMarker!!)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapControls() {
    var searchQuery by remember { mutableStateOf("") }

    // Top Search Bar
    SearchBar(
        query = searchQuery,
        onQueryChange = { searchQuery = it },
        onSearch = { },
        active = false,
        onActiveChange = { },
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        placeholder = { Text("Search location or road...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
        trailingIcon = { Icon(Icons.Default.Mic, contentDescription = "Voice Search") }
    ) {}

    // Right Side Controls (Filter, Refresh)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 100.dp, end = 16.dp),
        horizontalAlignment = Alignment.End
    ) {
        FloatingMapButton(icon = Icons.Default.FilterList, onClick = { /* Filter */ })
        Spacer(modifier = Modifier.height(16.dp))
        FloatingMapButton(icon = Icons.Default.Refresh, onClick = { /* Refresh */ })
    }

    // Bottom Right Control (Current Location - just above FAB)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 100.dp, end = 16.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.End
    ) {
        FloatingMapButton(icon = Icons.Default.MyLocation, onClick = { /* Current Location */ })
    }
}

@Composable
fun FloatingMapButton(icon: ImageVector, onClick: () -> Unit) {
    SmallFloatingActionButton(
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Icon(icon, contentDescription = null)
    }
}

@Composable
fun MarkerDetailsContent(marker: PotholeMarker) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        // Photo Placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = "Photo Placeholder",
                modifier = Modifier.size(48.dp),
                tint = Color.Gray
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Title & Status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = marker.roadName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Surface(
                color = marker.severity.color.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = marker.status,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = marker.severity.color,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Divider(color = MaterialTheme.colorScheme.surfaceVariant)
        Spacer(modifier = Modifier.height(16.dp))
        
        // Details Grid
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            DetailRow(icon = Icons.Default.LocationCity, label = "Ward", value = marker.ward)
            DetailRow(
                icon = Icons.Default.Warning,
                label = "Severity",
                value = marker.severity.label,
                valueColor = marker.severity.color
            )
            DetailRow(icon = Icons.Default.DateRange, label = "Reported On", value = marker.date)
            DetailRow(icon = Icons.Default.Route, label = "Distance", value = marker.distance)
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun DetailRow(icon: ImageVector, label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}

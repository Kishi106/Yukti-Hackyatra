package com.example.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.location.Location
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.models.PotholeReport
import com.example.viewmodels.ReportUiState
import com.example.viewmodels.ReportViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt


@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun LiveMapScreen(
    navController: androidx.navigation.NavController,
    reportViewModel: ReportViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Initialize OSMDroid Configuration
    DisposableEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        onDispose { }
    }

    // Permission state
    val locationPermissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    // Request permissions on screen enter
    LaunchedEffect(Unit) {
        if (!locationPermissionsState.allPermissionsGranted) {
            locationPermissionsState.launchMultiplePermissionRequest()
        }
    }

    // States
    var userLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var followUserLocation by remember { mutableStateOf(false) }
    var selectedPothole by remember { mutableStateOf<PotholeReport?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showFilterDialog by remember { mutableStateOf(false) }
    var currentZoom by remember { mutableDoubleStateOf(16.0) }
    var mapViewInstance by remember { mutableStateOf<MapView?>(null) }

    // Filters state
    var severityFilter by remember { mutableStateOf("ALL") } // ALL, HIGH, MEDIUM, LOW
    var statusFilter by remember { mutableStateOf("ALL") }   // ALL, NEW, IN_PROGRESS, FIXED
    var wardFilter by remember { mutableStateOf("ALL") }     // ALL, Ward Name
    var sourceFilter by remember { mutableStateOf("ALL") }   // ALL, CITIZEN, AUTO

    // Data from Supabase
    val allPotholesState by reportViewModel.allPotholesState.collectAsStateWithLifecycle()

    // Automatic refresh every 30s
    LaunchedEffect(Unit) {
        while (isActive) {
            reportViewModel.loadAllPotholes()
            delay(30000L)
        }
    }

    // Refresh on resume
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                reportViewModel.loadAllPotholes()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Shared Location updates from LocationService
    val locationService = remember { com.example.services.LocationService.getInstance(context) }
    val locationState by locationService.locationState.collectAsStateWithLifecycle()

    LaunchedEffect(locationState.hasFix, locationState.latitude, locationState.longitude) {
        if (locationState.hasFix && (locationState.latitude != 0.0 || locationState.longitude != 0.0)) {
            val pt = GeoPoint(locationState.latitude, locationState.longitude)
            userLocation = pt
            if (mapViewInstance != null && !followUserLocation) {
                mapViewInstance?.controller?.animateTo(pt)
                mapViewInstance?.controller?.setZoom(16.0)
                followUserLocation = true
            } else if (followUserLocation) {
                mapViewInstance?.controller?.animateTo(pt)
            }
        }
    }

    DisposableEffect(locationPermissionsState.allPermissionsGranted) {
        if (locationPermissionsState.allPermissionsGranted) {
            locationService.startLocationUpdates()
        }
        onDispose { }
    }

    // Process raw list from state
    val rawPotholes = remember(allPotholesState) {
        if (allPotholesState is ReportUiState.Success) {
            (allPotholesState as ReportUiState.Success).reports
        } else {
            emptyList()
        }
    }

    // Extract unique wards for filter dropdown
    val availableWards = remember(rawPotholes) {
        rawPotholes.mapNotNull { it.ward }.distinct().sorted()
    }

    // Apply search query and active filters
    val filteredPotholes = remember(rawPotholes, searchQuery, severityFilter, statusFilter, wardFilter, sourceFilter) {
        rawPotholes.filter { p ->
            // Search query filter
            val query = searchQuery.trim().lowercase()
            val matchesSearch = query.isEmpty() ||
                    (p.address?.lowercase()?.contains(query) == true) ||
                    (p.ward?.lowercase()?.contains(query) == true) ||
                    (p.location?.lowercase()?.contains(query) == true) ||
                    (p.potholeCode?.lowercase()?.contains(query) == true) ||
                    (p.description?.lowercase()?.contains(query) == true)

            // Severity filter
            val matchesSeverity = when (severityFilter) {
                "HIGH" -> p.severity.equals("HIGH", ignoreCase = true)
                "MEDIUM" -> p.severity.equals("MEDIUM", ignoreCase = true)
                "LOW" -> p.severity.equals("LOW", ignoreCase = true)
                else -> true
            }

            // Status filter
            val matchesStatus = when (statusFilter) {
                "NEW" -> p.status.equals("new", ignoreCase = true)
                "IN_PROGRESS" -> p.status.equals("in_progress", ignoreCase = true) || p.status.equals("in progress", ignoreCase = true)
                "FIXED" -> p.status.equals("fixed", ignoreCase = true) || p.status.equals("repaired", ignoreCase = true)
                else -> true
            }

            // Ward filter
            val matchesWard = wardFilter == "ALL" || p.ward.equals(wardFilter, ignoreCase = true)

            // Source filter
            val matchesSource = when (sourceFilter) {
                "CITIZEN" -> p.source.equals("citizen", ignoreCase = true)
                "AUTO" -> p.source.equals("auto", ignoreCase = true) || p.source.equals("sensor", ignoreCase = true)
                else -> true
            }

            matchesSearch && matchesSeverity && matchesStatus && matchesWard && matchesSource
        }
    }

    // Count active filters
    val activeFilterCount = remember(severityFilter, statusFilter, wardFilter, sourceFilter) {
        var count = 0
        if (severityFilter != "ALL") count++
        if (statusFilter != "ALL") count++
        if (wardFilter != "ALL") count++
        if (sourceFilter != "ALL") count++
        count
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
            // Main OpenStreetMap View
            PotholeMap(
                modifier = Modifier.fillMaxSize(),
                interactive = true,
                showCurrentLocation = true,
                showControls = true,
                centerOnUser = true,
                potholes = filteredPotholes,
                selectedPothole = selectedPothole,
                onPotholeSelected = { selectedPothole = it },
                onMapViewCreated = { mapViewInstance = it },
                reportViewModel = reportViewModel
            )

            // Location permission denied banner
            if (!locationPermissionsState.allPermissionsGranted) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .align(Alignment.TopCenter)
                        .padding(top = 80.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOff,
                            contentDescription = "Location Denied",
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Location access denied. Displaying Visakhapatnam map.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { locationPermissionsState.launchMultiplePermissionRequest() }) {
                            Text("Grant")
                        }
                    }
                }
            }

            // Top Search Bar & Controls Overlay
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.TopCenter)
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search road, ward, address...") },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear Search")
                            }
                        }
                    }
                }

                // Results counter chip if query or filter applied
                if (searchQuery.isNotEmpty() || activeFilterCount > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Text(
                            text = "Showing ${filteredPotholes.size} pothole(s)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Right side floating map action buttons
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Filter button with badge
                BadgedBox(
                    badge = {
                        if (activeFilterCount > 0) {
                            Badge { Text("$activeFilterCount") }
                        }
                    }
                ) {
                    SmallFloatingActionButton(
                        onClick = { showFilterDialog = true },
                        containerColor = if (activeFilterCount > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        contentColor = if (activeFilterCount > 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    ) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter Potholes")
                    }
                }

                // Refresh button
                SmallFloatingActionButton(
                    onClick = { reportViewModel.loadAllPotholes() },
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh Map Data")
                }

                // North reset button
                SmallFloatingActionButton(
                    onClick = {
                        mapViewInstance?.let { map ->
                            map.mapOrientation = 0f
                            map.invalidate()
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    Icon(Icons.Default.Explore, contentDescription = "Reset North")
                }
            }

            // Bottom Right My Location button
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 90.dp, end = 16.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        if (locationPermissionsState.allPermissionsGranted) {
                            userLocation?.let { pt ->
                                followUserLocation = true
                                mapViewInstance?.controller?.animateTo(pt)
                                mapViewInstance?.controller?.setZoom(17.0)
                            }
                        } else {
                            locationPermissionsState.launchMultiplePermissionRequest()
                        }
                    },
                    containerColor = if (followUserLocation) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    contentColor = if (followUserLocation) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (locationPermissionsState.allPermissionsGranted) Icons.Default.MyLocation else Icons.Default.LocationOff,
                        contentDescription = "Center on My Location"
                    )
                }
            }
        }
    }

    // Filter Dialog
    if (showFilterDialog) {
        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Filter Potholes", fontWeight = FontWeight.Bold)
                    TextButton(onClick = {
                        severityFilter = "ALL"
                        statusFilter = "ALL"
                        wardFilter = "ALL"
                        sourceFilter = "ALL"
                    }) {
                        Text("Reset")
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Severity
                    Text("Severity", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = severityFilter == "ALL", onClick = { severityFilter = "ALL" }, label = { Text("All") })
                        FilterChip(selected = severityFilter == "HIGH", onClick = { severityFilter = "HIGH" }, label = { Text("High") })
                        FilterChip(selected = severityFilter == "MEDIUM", onClick = { severityFilter = "MEDIUM" }, label = { Text("Medium") })
                        FilterChip(selected = severityFilter == "LOW", onClick = { severityFilter = "LOW" }, label = { Text("Low") })
                    }

                    // Status
                    Text("Status", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = statusFilter == "ALL", onClick = { statusFilter = "ALL" }, label = { Text("All") })
                        FilterChip(selected = statusFilter == "NEW", onClick = { statusFilter = "NEW" }, label = { Text("New") })
                        FilterChip(selected = statusFilter == "IN_PROGRESS", onClick = { statusFilter = "IN_PROGRESS" }, label = { Text("In Progress") })
                        FilterChip(selected = statusFilter == "FIXED", onClick = { statusFilter = "FIXED" }, label = { Text("Fixed") })
                    }

                    // Source
                    Text("Source", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = sourceFilter == "ALL", onClick = { sourceFilter = "ALL" }, label = { Text("All") })
                        FilterChip(selected = sourceFilter == "CITIZEN", onClick = { sourceFilter = "CITIZEN" }, label = { Text("Citizen") })
                        FilterChip(selected = sourceFilter == "AUTO", onClick = { sourceFilter = "AUTO" }, label = { Text("Auto Detected") })
                    }

                    // Ward
                    if (availableWards.isNotEmpty()) {
                        Text("Ward", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilterChip(
                                selected = wardFilter == "ALL",
                                onClick = { wardFilter = "ALL" },
                                label = { Text("All Wards") }
                            )
                            availableWards.forEach { ward ->
                                FilterChip(
                                    selected = wardFilter == ward,
                                    onClick = { wardFilter = ward },
                                    label = { Text(ward) }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showFilterDialog = false }) {
                    Text("Apply Filters")
                }
            }
        )
    }

    // Bottom Sheet for Marker Details
    if (selectedPothole != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedPothole = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            PotholeDetailsContent(
                pothole = selectedPothole!!,
                onDismiss = { selectedPothole = null }
            )
        }
    }
}

@Composable
fun PotholeDetailsContent(pothole: PotholeReport, onDismiss: () -> Unit) {
    val severityColor = when (pothole.severity.uppercase()) {
        "HIGH" -> Color(0xFFD32F2F)
        "MEDIUM" -> Color(0xFFF57C00)
        "LOW" -> Color(0xFF2E7D32)
        else -> Color(0xFFF57C00)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Photo Header
        if (!pothole.photoUrl.isNullAndBlank()) {
            val context = LocalContext.current
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(pothole.photoUrl)
                    .crossfade(true)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build(),
                contentDescription = "Pothole Photo",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop,
                loading = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                    }
                }
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.AddAPhoto,
                        contentDescription = "No Photo",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No photo attached",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Title & Status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = pothole.address ?: pothole.location ?: "Pothole Report",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Surface(
                color = severityColor.copy(alpha = 0.15f),
                shape = CircleShape
            ) {
                Text(
                    text = pothole.severity.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } + " Severity",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = severityColor,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        Spacer(modifier = Modifier.height(16.dp))

        // Details Grid
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            PotholeDetailItem(
                icon = Icons.Default.Info,
                label = "Status",
                value = pothole.status.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            )

            PotholeDetailItem(
                icon = Icons.Default.Verified,
                label = "Confidence Score",
                value = "${pothole.confidenceScore.toInt()}%"
            )

            PotholeDetailItem(
                icon = Icons.Default.LocationCity,
                label = "Ward",
                value = "${pothole.ward ?: "Unknown"} ${pothole.wardNo?.let { "(No. $it)" } ?: ""}"
            )

            PotholeDetailItem(
                icon = Icons.Default.DateRange,
                label = "Reported On",
                value = pothole.createdAt?.take(10) ?: "Recently"
            )

            PotholeDetailItem(
                icon = Icons.Default.CellTower,
                label = "Source",
                value = if (pothole.source.equals("auto", ignoreCase = true)) "Automated Detection" else "Citizen Report"
            )

            PotholeDetailItem(
                icon = Icons.Default.PinDrop,
                label = "Coordinates",
                value = "${String.format("%.6f", pothole.lat)}, ${String.format("%.6f", pothole.lng)}"
            )

            if (!pothole.description.isNullOrBlank()) {
                PotholeDetailItem(
                    icon = Icons.Default.Notes,
                    label = "Description",
                    value = pothole.description
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
fun PotholeDetailItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// Helpers
private fun String?.isNullAndBlank(): Boolean = this.isNullOrEmpty() || this.trim().isEmpty()


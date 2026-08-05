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
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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
import androidx.compose.ui.graphics.Color
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

enum class Severity(val color: Color, val label: String) {
    HIGH(Color(0xFFD32F2F), "High"),
    MEDIUM(Color(0xFFF57C00), "Medium"),
    LOW(Color(0xFF2E7D32), "Low"),
    REPAIRED(Color(0xFF2E7D32), "Repaired")
}

data class MapCluster(
    val centerLat: Double,
    val centerLng: Double,
    val items: List<PotholeReport>,
    val highestSeverity: String
)


// Default map center: Visakhapatnam
val VISAKHAPATNAM_CENTER = GeoPoint(17.7293, 83.3152)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PotholeMap(
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
    showCurrentLocation: Boolean = true,
    showControls: Boolean = true,
    centerOnUser: Boolean = true,
    onMapClick: (() -> Unit)? = null,
    potholes: List<PotholeReport>? = null,
    selectedPothole: PotholeReport? = null,
    onPotholeSelected: ((PotholeReport) -> Unit)? = null,
    onMapViewCreated: ((MapView) -> Unit)? = null,
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

    // Location state
    var userLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var initialCentered by remember { mutableStateOf(false) }
    var currentZoom by remember { mutableDoubleStateOf(16.0) }
    var mapViewInstance by remember { mutableStateOf<MapView?>(null) }

    // Observe reports fromViewModel if potholes list is not directly supplied
    val allPotholesState by reportViewModel.allPotholesState.collectAsStateWithLifecycle()

    val rawPotholes = remember(potholes, allPotholesState) {
        potholes ?: if (allPotholesState is ReportUiState.Success) {
            (allPotholesState as ReportUiState.Success).reports
        } else {
            emptyList()
        }
    }

    // Auto refresh data every 30s
    LaunchedEffect(Unit) {
        while (isActive) {
            reportViewModel.loadAllPotholes()
            delay(30000L)
        }
    }

    // Refresh data on lifecycle ON_RESUME
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                reportViewModel.loadAllPotholes()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // MapView Lifecycle observer
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapViewInstance?.onResume()
                Lifecycle.Event.ON_PAUSE -> mapViewInstance?.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapViewInstance?.onDetach()
        }
    }

    // Shared Location updates from LocationService
    val locationService = remember { com.example.services.LocationService.getInstance(context) }
    val locationState by locationService.locationState.collectAsStateWithLifecycle()

    LaunchedEffect(locationState.hasFix, locationState.latitude, locationState.longitude) {
        if (locationState.hasFix && (locationState.latitude != 0.0 || locationState.longitude != 0.0)) {
            val pt = GeoPoint(locationState.latitude, locationState.longitude)
            userLocation = pt
            if (centerOnUser && !initialCentered && mapViewInstance != null) {
                mapViewInstance?.controller?.animateTo(pt)
                mapViewInstance?.controller?.setZoom(16.0)
                initialCentered = true
            }
        }
    }

    DisposableEffect(showCurrentLocation, locationPermissionsState.allPermissionsGranted) {
        if (showCurrentLocation && locationPermissionsState.allPermissionsGranted) {
            locationService.startLocationUpdates()
        }
        onDispose { }
    }

    // Calculate clusters based on current zoom
    val clusters = remember(rawPotholes, currentZoom) {
        clusterPotholes(rawPotholes, currentZoom)
    }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                    controller.setZoom(16.0)
                    controller.setCenter(userLocation ?: VISAKHAPATNAM_CENTER)

                    if (interactive) {
                        setMultiTouchControls(true)

                        if (showControls) {
                            // Rotation gesture overlay
                            val rotationOverlay = RotationGestureOverlay(this)
                            rotationOverlay.isEnabled = true
                            overlays.add(rotationOverlay)

                            // Compass overlay
                            val compassOverlay = CompassOverlay(ctx, InternalCompassOrientationProvider(ctx), this)
                            compassOverlay.enableCompass()
                            overlays.add(compassOverlay)
                        }

                        addMapListener(object : MapListener {
                            override fun onScroll(event: ScrollEvent?): Boolean = false
                            override fun onZoom(event: ZoomEvent?): Boolean {
                                currentZoom = zoomLevelDouble
                                return false
                            }
                        })
                    } else {
                        setMultiTouchControls(false)
                        val gestureDetector = GestureDetector(ctx, object : GestureDetector.SimpleOnGestureListener() {
                            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                                onMapClick?.invoke()
                                return true
                            }
                        })
                        @SuppressLint("ClickableViewAccessibility")
                        setOnTouchListener { _, event ->
                            gestureDetector.onTouchEvent(event)
                            true // Consume touch to prevent map scrolling/zooming in preview
                        }
                    }

                    mapViewInstance = this
                    onMapViewCreated?.invoke(this)
                }
            },
            update = { mapView ->
                if (mapView.repository == null) return@AndroidView

                // Keep static overlays (rotation & compass if present)
                val staticOverlayCount = if (interactive && showControls) 2 else 0
                val staticOverlays = mapView.overlays.take(staticOverlayCount)
                mapView.overlays.clear()
                mapView.overlays.addAll(staticOverlays)

                // Add user location marker
                if (showCurrentLocation) {
                    userLocation?.let { uLoc ->
                        val userMarker = Marker(mapView).apply {
                            position = uLoc
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            icon = createUserLocationDrawable(context)
                            title = "Current Location"
                            if (!interactive) {
                                setOnMarkerClickListener { _, _ ->
                                    onMapClick?.invoke()
                                    true
                                }
                            }
                        }
                        mapView.overlays.add(userMarker)
                    }
                }

                // Add clusters / pothole markers
                clusters.forEach { cluster ->
                    val marker = Marker(mapView).apply {
                        position = GeoPoint(cluster.centerLat, cluster.centerLng)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                        if (cluster.items.size == 1) {
                            val pothole = cluster.items.first()
                            icon = createColoredPinDrawable(context, getSeverityColorInt(pothole.severity, pothole.status))
                            title = pothole.address ?: "Pothole Report"
                            snippet = "Severity: ${pothole.severity}"
                            setOnMarkerClickListener { _, _ ->
                                if (interactive) {
                                    onPotholeSelected?.invoke(pothole)
                                    mapView.controller.animateTo(GeoPoint(pothole.lat, pothole.lng))
                                } else {
                                    onMapClick?.invoke()
                                }
                                true
                            }
                        } else {
                            icon = createClusterDrawable(context, cluster.items.size, getSeverityColorInt(cluster.highestSeverity, "new"))
                            title = "Cluster (${cluster.items.size} Potholes)"
                            setOnMarkerClickListener { _, _ ->
                                if (interactive) {
                                    val targetZoom = (mapView.zoomLevelDouble + 2.5).coerceAtMost(19.0)
                                    mapView.controller.animateTo(GeoPoint(cluster.centerLat, cluster.centerLng))
                                    mapView.controller.zoomTo(targetZoom)
                                } else {
                                    onMapClick?.invoke()
                                }
                                true
                            }
                        }
                    }
                    mapView.overlays.add(marker)
                }

                mapView.invalidate()
            }
        )
    }
}

// Helper methods for pin/cluster icons and calculations

fun getSeverityColorInt(severity: String?, status: String?): Int {
    if (status.equals("fixed", ignoreCase = true) || status.equals("repaired", ignoreCase = true)) {
        return android.graphics.Color.rgb(46, 125, 50) // Green
    }
    return when (severity?.uppercase()) {
        "HIGH" -> android.graphics.Color.rgb(211, 47, 47)  // Red
        "MEDIUM" -> android.graphics.Color.rgb(245, 124, 0) // Orange
        "LOW" -> android.graphics.Color.rgb(46, 125, 50)   // Green
        else -> android.graphics.Color.rgb(245, 124, 0)     // Orange default
    }
}

fun createColoredPinDrawable(context: Context, colorInt: Int): Drawable {
    val density = context.resources.displayMetrics.density
    val sizePx = (38 * density).toInt()
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val center = sizePx / 2f
    val radius = sizePx / 2.2f

    val paintBody = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorInt
        style = Paint.Style.FILL
    }
    val paintBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3f * density
    }
    val paintCenter = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }

    canvas.drawCircle(center, center, radius, paintBody)
    canvas.drawCircle(center, center, radius, paintBorder)
    canvas.drawCircle(center, center, radius * 0.45f, paintCenter)

    return BitmapDrawable(context.resources, bitmap)
}

fun createClusterDrawable(context: Context, count: Int, colorInt: Int): Drawable {
    val density = context.resources.displayMetrics.density
    val sizePx = (46 * density).toInt()
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val center = sizePx / 2f
    val radius = sizePx / 2.2f

    val paintFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorInt
        style = Paint.Style.FILL
    }
    val paintBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f * density
    }
    val paintText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 16f * density
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    canvas.drawCircle(center, center, radius, paintFill)
    canvas.drawCircle(center, center, radius, paintBorder)

    val textY = center - ((paintText.descent() + paintText.ascent()) / 2)
    canvas.drawText(count.toString(), center, textY, paintText)

    return BitmapDrawable(context.resources, bitmap)
}

fun createUserLocationDrawable(context: Context): Drawable {
    val density = context.resources.displayMetrics.density
    val sizePx = (34 * density).toInt()
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val center = sizePx / 2f

    val paintHalo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(70, 33, 150, 243)
        style = Paint.Style.FILL
    }
    val paintWhite = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    val paintBlue = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(33, 150, 243)
        style = Paint.Style.FILL
    }

    canvas.drawCircle(center, center, sizePx / 2f, paintHalo)
    canvas.drawCircle(center, center, sizePx / 3f, paintWhite)
    canvas.drawCircle(center, center, sizePx / 4.5f, paintBlue)

    return BitmapDrawable(context.resources, bitmap)
}

fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return r * c
}

fun clusterPotholes(
    potholes: List<PotholeReport>,
    zoomLevel: Double
): List<MapCluster> {
    if (potholes.isEmpty()) return emptyList()

    if (zoomLevel >= 15.0) {
        return potholes.map { pothole ->
            MapCluster(
                centerLat = pothole.lat,
                centerLng = pothole.lng,
                items = listOf(pothole),
                highestSeverity = pothole.severity
            )
        }
    }

    val distanceThresholdMeters = when {
        zoomLevel < 10.0 -> 12000.0
        zoomLevel < 12.0 -> 4000.0
        zoomLevel < 14.0 -> 1000.0
        else -> 300.0
    }

    val clusters = mutableListOf<MutableList<PotholeReport>>()

    for (pothole in potholes) {
        var addedToCluster = false
        for (cluster in clusters) {
            val first = cluster.first()
            val dist = calculateDistanceMeters(first.lat, first.lng, pothole.lat, pothole.lng)
            if (dist < distanceThresholdMeters) {
                cluster.add(pothole)
                addedToCluster = true
                break
            }
        }
        if (!addedToCluster) {
            clusters.add(mutableListOf(pothole))
        }
    }

    return clusters.map { group ->
        val avgLat = group.map { it.lat }.average()
        val avgLng = group.map { it.lng }.average()
        val hasHigh = group.any { it.severity.equals("HIGH", ignoreCase = true) }
        val hasMedium = group.any { it.severity.equals("MEDIUM", ignoreCase = true) }
        val highest = when {
            hasHigh -> "HIGH"
            hasMedium -> "MEDIUM"
            else -> "LOW"
        }
        MapCluster(
            centerLat = avgLat,
            centerLng = avgLng,
            items = group,
            highestSeverity = highest
        )
    }
}

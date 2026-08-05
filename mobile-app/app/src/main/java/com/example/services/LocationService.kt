package com.example.services

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class LocationState(
    val location: Location? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val accuracy: Float = 0f,
    val provider: String = "GPS",
    val address: String = "Locating...",
    val ward: String = "Unknown",
    val speedKmh: Float = 0f,
    val timestamp: Long = 0L,
    val hasFix: Boolean = false
)

class LocationService private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(appContext)

    private val _locationState = MutableStateFlow(LocationState())
    val locationState: StateFlow<LocationState> = _locationState.asStateFlow()

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var locationCallback: LocationCallback? = null
    private var isUpdating = false

    companion object {
        @Volatile
        private var INSTANCE: LocationService? = null

        fun getInstance(context: Context): LocationService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocationService(context.applicationContext).also { INSTANCE = it }
            }
        }

        /**
         * Clean reverse geocoded address formatter with priority:
         * Road -> Area (SubLocality) -> Locality -> City/District (SubAdmin/AdminArea)
         * Removes adjacent duplicate tokens (e.g. "Bheemunipatnam, Bheemunipatnam" -> "Bheemunipatnam").
         */
        fun formatAddress(address: Address?): String {
            if (address == null) return "Unknown Location"

            val road = when {
                !address.thoroughfare.isNullOrBlank() -> {
                    if (!address.subThoroughfare.isNullOrBlank()) "${address.subThoroughfare} ${address.thoroughfare}" else address.thoroughfare
                }
                else -> null
            }

            val area = address.subLocality?.takeIf { it.isNotBlank() }
            val locality = address.locality?.takeIf { it.isNotBlank() }
            val cityOrDistrict = (address.subAdminArea ?: address.adminArea)?.takeIf { it.isNotBlank() }

            val rawParts = listOfNotNull(road, area, locality, cityOrDistrict)

            val cleanParts = mutableListOf<String>()
            for (part in rawParts) {
                val trimmed = part.trim()
                if (cleanParts.isEmpty() || !cleanParts.last().equals(trimmed, ignoreCase = true)) {
                    cleanParts.add(trimmed)
                }
            }

            return if (cleanParts.isNotEmpty()) cleanParts.joinToString(", ") else "Location identified"
        }
    }

    fun hasLocationPermission(): Boolean {
        val fine = androidx.core.content.ContextCompat.checkSelfPermission(
            appContext,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        val coarse = androidx.core.content.ContextCompat.checkSelfPermission(
            appContext,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        return fine || coarse
    }

    fun isLocationEnabled(): Boolean {
        val locationManager = appContext.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager ?: return false
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                locationManager.isLocationEnabled
            } else {
                locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
            }
        } catch (e: Exception) {
            false
        }
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        if (!hasLocationPermission() || !isLocationEnabled()) return
        if (isUpdating) return
        isUpdating = true

        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { loc ->
                    if (loc != null) processNewLocation(loc)
                }
        } catch (e: Exception) {
            // SecurityException or location disabled
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
            .setMinUpdateIntervalMillis(1000L)
            .setMinUpdateDistanceMeters(1f)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val bestLoc = result.locations.minByOrNull { it.accuracy } ?: return
                processNewLocation(bestLoc)
            }
        }
        locationCallback = callback

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                callback,
                appContext.mainLooper
            )
        } catch (e: Exception) {
            isUpdating = false
        }
    }

    fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
        locationCallback = null
        isUpdating = false
    }

    @SuppressLint("MissingPermission")
    fun forceRefresh() {
        if (!hasLocationPermission() || !isLocationEnabled()) return
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { loc ->
                    if (loc != null) processNewLocation(loc)
                }
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun processNewLocation(location: Location) {
        val currentState = _locationState.value
        // Ignore GPS fixes with accuracy worse than 20 meters whenever a better location exists
        if (currentState.hasFix && location.accuracy > 20f && currentState.accuracy <= 20f) {
            return
        }

        val speedKmh = if (location.hasSpeed()) location.speed * 3.6f else 0f

        _locationState.value = currentState.copy(
            location = location,
            latitude = location.latitude,
            longitude = location.longitude,
            accuracy = location.accuracy,
            provider = location.provider ?: "GPS",
            speedKmh = speedKmh,
            timestamp = location.time,
            hasFix = true
        )

        serviceScope.launch(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(appContext, Locale.getDefault())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocation(location.latitude, location.longitude, 1) { addresses ->
                        val addr = addresses.firstOrNull()
                        val formatted = formatAddress(addr)
                        val wardName = addr?.subLocality ?: addr?.locality ?: "Ward Unknown"
                        _locationState.value = _locationState.value.copy(
                            address = formatted,
                            ward = wardName
                        )
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    val addr = addresses?.firstOrNull()
                    val formatted = formatAddress(addr)
                    val wardName = addr?.subLocality ?: addr?.locality ?: "Ward Unknown"
                    _locationState.value = _locationState.value.copy(
                        address = formatted,
                        ward = wardName
                    )
                }
            } catch (e: Exception) {
                if (_locationState.value.address == "Locating...") {
                    _locationState.value = _locationState.value.copy(
                        address = String.format(Locale.US, "%.4f, %.4f", location.latitude, location.longitude)
                    )
                }
            }
        }
    }
}

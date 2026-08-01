package com.example.services

import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

class LocationService(private val context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Requests a fresh, live location fix (not the cached lastLocation, which is
     * often null or stale). Returns null if permission isn't granted, no fix could
     * be obtained within LOCATION_TIMEOUT_MS (e.g. indoors with no GPS signal —
     * getCurrentLocation() has no built-in timeout and can otherwise hang
     * indefinitely), or the fix otherwise fails — callers must handle that
     * explicitly rather than defaulting to 0.0,0.0.
     */
    suspend fun requestFreshLocation(): Location? {
        if (!hasLocationPermission()) return null
        return try {
            val cancellationTokenSource = CancellationTokenSource()
            withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationTokenSource.token
                ).await()
            } ?: run {
                cancellationTokenSource.cancel()
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        private const val LOCATION_TIMEOUT_MS = 15_000L
    }
}

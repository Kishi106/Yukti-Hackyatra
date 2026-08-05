package com.example.viewmodels

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.services.DrivingManager
import com.example.services.DrivingService
import com.example.services.NotificationService
import kotlinx.coroutines.launch

class DrivingViewModel(application: Application) : AndroidViewModel(application) {
    val isDriving = DrivingManager.isDriving
    val drivingTimeSeconds = DrivingManager.drivingTimeSeconds
    val currentAddress = DrivingManager.currentAddress
    val currentSpeedKmh = DrivingManager.currentSpeedKmh
    val currentSpeedString = DrivingManager.currentSpeedString
    val distanceTravelledKm = DrivingManager.distanceTravelledKm
    val accelerometerStatus = DrivingManager.accelerometerStatus
    val gyroscopeStatus = DrivingManager.gyroscopeStatus
    val gpsStatus = DrivingManager.gpsStatus
    val latitude = DrivingManager.latitude
    val longitude = DrivingManager.longitude
    val gpsAccuracy = DrivingManager.gpsAccuracy
    val possibleDetections = DrivingManager.possibleDetections
    val confirmedReports = DrivingManager.confirmedReports
    val ignoredDetections = DrivingManager.ignoredDetections
    val hasGyroscope = DrivingManager.hasGyroscope
    val drivingSessionState = DrivingManager.drivingSessionState
    
    val isDeveloperMode = DrivingManager.isDeveloperMode
    val debugAccelX = DrivingManager.debugAccelX
    val debugAccelY = DrivingManager.debugAccelY
    val debugAccelZ = DrivingManager.debugAccelZ
    val debugGyroX = DrivingManager.debugGyroX
    val debugGyroY = DrivingManager.debugGyroY
    val debugGyroZ = DrivingManager.debugGyroZ
    val debugConfidence = DrivingManager.debugConfidence
    val debugThreshold = DrivingManager.debugThreshold
    val debugCooldownActive = DrivingManager.debugCooldownActive
    val debugLastRejectionReason = DrivingManager.debugLastRejectionReason
    val debugCooldownRemaining = DrivingManager.debugCooldownRemaining
    val debugDetectionState = DrivingManager.debugDetectionState
    val debugDetectionTriggered = DrivingManager.debugDetectionTriggered
    val debugLastDetectionTimestamp = DrivingManager.debugLastDetectionTimestamp
    val debugCurrentDetectionStage = DrivingManager.debugCurrentDetectionStage

    fun fetchCurrentAddress() {
        // Will be updated by foreground service if driving, otherwise we can keep old logic.
        // For simplicity and to not break "Recent Activity" map loading, let's just observe.
    }

    fun startDriving() {
        if (isDriving.value) return
        val intent = Intent(getApplication(), DrivingService::class.java)
        getApplication<Application>().startService(intent)
    }

    fun stopDriving() {
        if (!isDriving.value) return
        val intent = Intent(getApplication(), DrivingService::class.java).apply {
            action = "STOP_DRIVING"
        }
        getApplication<Application>().startService(intent)
    }
}

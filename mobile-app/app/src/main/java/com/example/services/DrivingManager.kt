package com.example.services

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow

object DrivingManager {
    val isDriving = MutableStateFlow(false)
    val drivingSessionState = MutableStateFlow("Stopped")
    val drivingTimeSeconds = MutableStateFlow(0)
    
    val currentSpeedKmh = MutableStateFlow(0f)
    val currentSpeedString = MutableStateFlow("Waiting for movement...")
    val distanceTravelledKm = MutableStateFlow(0f)
    
    val accelerometerStatus = MutableStateFlow("Disabled")
    val gyroscopeStatus = MutableStateFlow("Disabled")
    val gpsStatus = MutableStateFlow("Disabled")
    
    val currentAddress = MutableStateFlow("Waiting for GPS...")
    val ward = MutableStateFlow("Unknown")
    val latitude = MutableStateFlow(0.0)
    val longitude = MutableStateFlow(0.0)
    val gpsAccuracy = MutableStateFlow(0f)
    val gpsProvider = MutableStateFlow("GPS")
    val gpsTimestamp = MutableStateFlow(0L)
    
    // User-facing Detection Counters
    val possibleDetections = MutableStateFlow(0)
    val confirmedReports = MutableStateFlow(0)
    val ignoredDetections = MutableStateFlow(0)
    
    // Internal Developer Statistics (Never shown as ignored count to citizens)
    val sensorSamplesCount = MutableStateFlow(0L)
    val filteredEventsCount = MutableStateFlow(0L)
    val detectionCandidatesCount = MutableStateFlow(0L)
    val algorithmRejectionsCount = MutableStateFlow(0L)
    
    val hasGyroscope = MutableStateFlow(false)
    
    // Developer Mode & Debugging State
    val isDeveloperMode = MutableStateFlow(false)
    val debugAccelX = MutableStateFlow(0f)
    val debugAccelY = MutableStateFlow(0f)
    val debugAccelZ = MutableStateFlow(0f)
    val debugLinearAccelX = MutableStateFlow(0f)
    val debugLinearAccelY = MutableStateFlow(0f)
    val debugLinearAccelZ = MutableStateFlow(0f)
    val debugGyroX = MutableStateFlow(0f)
    val debugGyroY = MutableStateFlow(0f)
    val debugGyroZ = MutableStateFlow(0f)
    
    val debugImpactMagnitude = MutableStateFlow(0f)
    val debugVerticalJerk = MutableStateFlow(0f)
    
    val debugConfidence = MutableStateFlow(0)
    val debugThreshold = MutableStateFlow(65)
    val debugCooldownActive = MutableStateFlow(false)
    val debugCooldownRemaining = MutableStateFlow(0L)
    val debugLastRejectionReason = MutableStateFlow("None")
    
    val debugDetectionState = MutableStateFlow("Idle")
    val debugDetectionTriggered = MutableStateFlow("No")
    val debugLastDetectionTimestamp = MutableStateFlow(0L)
    val debugCurrentDetectionStage = MutableStateFlow("Monitoring")
    
    // Developer Mode Statuses
    val devReporterId = MutableStateFlow("cit_app_user")
    val devSupabaseStatus = MutableStateFlow("Connected")
    val devStorageStatus = MutableStateFlow("Ready")
    val devInsertStatus = MutableStateFlow("Idle")
    val devPhotoUploadStatus = MutableStateFlow("Idle")
    val devMapCenter = MutableStateFlow("17.7293, 83.3152")
    val devMapZoom = MutableStateFlow(16.0)
    val devMarkerCount = MutableStateFlow(0)
    val devFollowGps = MutableStateFlow(true)
    val devTileStatus = MutableStateFlow("Loaded")

    private val _potholeDetected = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val potholeDetected = _potholeDetected.asSharedFlow()

    var activeDetectionId: Long? = null

    /**
     * Called ONLY when a candidate passes threshold and a confirmation dialog / notification is presented to the user.
     */
    fun triggerCandidatePothole(detectionId: Long) {
        activeDetectionId = detectionId
        possibleDetections.value += 1
        detectionCandidatesCount.value += 1
        _potholeDetected.tryEmit(Unit)
    }

    /**
     * Called when user presses Report / Confirm.
     */
    fun userConfirmedDetection() {
        if (activeDetectionId != null) {
            confirmedReports.value += 1
            activeDetectionId = null
        }
    }

    /**
     * Called ONLY when user explicitly presses Ignore or dismisses notification/dialog.
     */
    fun userIgnoredDetection() {
        if (activeDetectionId != null) {
            ignoredDetections.value += 1
            activeDetectionId = null
        }
    }

    fun incrementTime() {
        drivingTimeSeconds.value += 1
    }

    fun resetSession() {
        drivingTimeSeconds.value = 0
        distanceTravelledKm.value = 0f
        currentSpeedKmh.value = 0f
        currentSpeedString.value = "Waiting for movement..."
        possibleDetections.value = 0
        confirmedReports.value = 0
        ignoredDetections.value = 0
        
        sensorSamplesCount.value = 0L
        filteredEventsCount.value = 0L
        detectionCandidatesCount.value = 0L
        algorithmRejectionsCount.value = 0L
        
        drivingSessionState.value = "Stopped"
        accelerometerStatus.value = "Disabled"
        gyroscopeStatus.value = "Disabled"
        gpsStatus.value = "Disabled"
        activeDetectionId = null
        
        debugConfidence.value = 0
        debugCooldownActive.value = false
        debugCooldownRemaining.value = 0L
        debugDetectionState.value = "Idle"
        debugLastRejectionReason.value = "None"
    }
}

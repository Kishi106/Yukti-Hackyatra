package com.example.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.UserPrefs
import com.example.network.ApiResult
import com.example.network.PotholeRepository
import com.example.services.LocationService
import com.example.services.NotificationService
import com.example.services.SensorService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DrivingViewModel(application: Application) : AndroidViewModel(application) {

    private val sensorService = SensorService(application)
    private val locationService = LocationService(application)
    private val notificationService = NotificationService(application)
    private val potholeRepository = PotholeRepository()
    private val userPrefs = UserPrefs(application)

    private val _isDriving = MutableStateFlow(false)
    val isDriving: StateFlow<Boolean> = _isDriving.asStateFlow()

    private val _drivingTimeSeconds = MutableStateFlow(0)
    val drivingTimeSeconds: StateFlow<Int> = _drivingTimeSeconds.asStateFlow()

    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            sensorService.potholeDetected.collect {
                // An uncaught exception here would terminate this collector permanently
                // (the whole point of `collect` is it keeps running for the ViewModel's
                // lifetime) — silently killing auto-detection for the rest of the driving
                // session with no visible error. Guard the whole handler against that.
                try {
                    // Fetch a real, live location fix. If we can't get one (no permission,
                    // or the fix failed), skip this detection entirely rather than
                    // submitting a fake 0.0, 0.0 report.
                    val location = locationService.requestFreshLocation() ?: return@collect

                    val reporterId = userPrefs.getUserId()

                    // Auto-submit the detection to the backend. The sensor flow only signals
                    // that a spike occurred (no magnitude carried through), so severity
                    // defaults to "medium" here.
                    val result = potholeRepository.submitReport(
                        lat = location.latitude,
                        lng = location.longitude,
                        severity = "medium",
                        source = "auto",
                        ward = null,
                        photoUrl = null,
                        reporterId = reporterId
                    )

                    // Only notify once the report actually exists server-side, and pass its
                    // real id through so the notification's confirm action can reference it.
                    if (result is ApiResult.Success) {
                        notificationService.showPotholeAlertNotification(result.data.id)
                    }
                } catch (e: Exception) {
                    // Nothing meaningful to show the user for a background detection
                    // failure — just don't let it take down future detections.
                }
            }
        }
    }

    fun startDriving() {
        if (_isDriving.value) return
        _isDriving.value = true
        sensorService.startMonitoring()
        startTimer()
    }

    fun stopDriving() {
        if (!_isDriving.value) return
        _isDriving.value = false
        sensorService.stopMonitoring()
        stopTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                _drivingTimeSeconds.value += 1
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        _drivingTimeSeconds.value = 0
    }

    override fun onCleared() {
        super.onCleared()
        sensorService.stopMonitoring()
        timerJob?.cancel()
    }
}

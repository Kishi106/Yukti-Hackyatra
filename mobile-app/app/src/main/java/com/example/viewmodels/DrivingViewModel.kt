package com.example.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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

    private val _isDriving = MutableStateFlow(false)
    val isDriving: StateFlow<Boolean> = _isDriving.asStateFlow()

    private val _drivingTimeSeconds = MutableStateFlow(0)
    val drivingTimeSeconds: StateFlow<Int> = _drivingTimeSeconds.asStateFlow()

    private var timerJob: Job? = null
    
    init {
        viewModelScope.launch {
            sensorService.potholeDetected.collect {
                // Fetch location immediately when detected
                val location = locationService.getCurrentLocation()
                val lat = location?.latitude ?: 0.0
                val lon = location?.longitude ?: 0.0

                // Auto-submit the detection to the backend. The sensor flow only signals
                // that a spike occurred (no magnitude carried through), so severity defaults
                // to "medium" here.
                potholeRepository.submitReport(
                    lat = lat,
                    lng = lon,
                    severity = "medium",
                    source = "auto",
                    ward = null,
                    photoUrl = null
                )

                // Show notification within 10 seconds
                notificationService.showPotholeAlertNotification()
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

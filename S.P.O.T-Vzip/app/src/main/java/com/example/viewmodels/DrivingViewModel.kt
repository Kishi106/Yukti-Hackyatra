package com.example.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.services.LocationService
import com.example.services.NotificationService
import com.example.services.SensorService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

class DrivingViewModel(application: Application) : AndroidViewModel(application) {

    private val sensorService = SensorService(application)
    private val locationService = LocationService(application)
    private val notificationService = NotificationService(application)

    private val _isDriving = MutableStateFlow(false)
    val isDriving: StateFlow<Boolean> = _isDriving.asStateFlow()

    private val _drivingTimeSeconds = MutableStateFlow(0)
    val drivingTimeSeconds: StateFlow<Int> = _drivingTimeSeconds.asStateFlow()

    private val _currentAddress = MutableStateFlow("Fetching Location...")
    val currentAddress: StateFlow<String> = _currentAddress.asStateFlow()

    private var timerJob: Job? = null
    
    init {
        viewModelScope.launch {
            sensorService.potholeDetected.collect {
                // Fetch location immediately when detected
                val location = locationService.getCurrentLocation()
                val lat = location?.latitude ?: 0.0
                val lon = location?.longitude ?: 0.0
                // We could save this to a local draft here
                
                // Show notification within 10 seconds
                notificationService.showPotholeAlertNotification()
            }
        }
    }

    fun fetchCurrentAddress() {
        viewModelScope.launch {
            val location = locationService.getCurrentLocation()
            if (location != null) {
                withContext(Dispatchers.IO) {
                    try {
                        val geocoder = android.location.Geocoder(getApplication(), java.util.Locale.getDefault())
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            geocoder.getFromLocation(location.latitude, location.longitude, 1) { addresses ->
                                val address = addresses.firstOrNull()
                                if (address != null) {
                                    val addressText = listOfNotNull(address.subLocality ?: address.locality, address.locality ?: address.adminArea)
                                        .joinToString(", ")
                                    _currentAddress.value = addressText.ifBlank { "Location found" }
                                } else {
                                    _currentAddress.value = "Lat: ${String.format("%.4f", location.latitude)}, Lng: ${String.format("%.4f", location.longitude)}"
                                }
                            }
                        } else {
                            @Suppress("DEPRECATION")
                            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                            val address = addresses?.firstOrNull()
                            if (address != null) {
                                val addressText = listOfNotNull(address.subLocality ?: address.locality, address.locality ?: address.adminArea)
                                    .joinToString(", ")
                                _currentAddress.value = addressText.ifBlank { "Location found" }
                            } else {
                                _currentAddress.value = "Lat: ${String.format("%.4f", location.latitude)}, Lng: ${String.format("%.4f", location.longitude)}"
                            }
                        }
                    } catch (e: Exception) {
                        _currentAddress.value = "Lat: ${String.format("%.4f", location.latitude)}, Lng: ${String.format("%.4f", location.longitude)}"
                    }
                }
            } else {
                _currentAddress.value = "Location Unavailable"
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

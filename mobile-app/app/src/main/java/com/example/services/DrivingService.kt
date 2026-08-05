package com.example.services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.*
import kotlin.math.abs
import kotlin.math.sqrt

class DrivingService : Service(), SensorEventListener {

    private val CHANNEL_ID = "driving_mode_channel"
    private val NOTIFICATION_ID = 2001

    private lateinit var sensorManager: SensorManager
    private var linearAccelSensor: Sensor? = null
    private var accelSensor: Sensor? = null
    private var gyroSensor: Sensor? = null

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var timerJob: Job? = null
    private var locationCollectorJob: Job? = null
    
    private var lastLocation: Location? = null
    private var lastDetectionTime = 0L
    private val cooldownMs = 2000L

    // High pass filter gravity storage for fallback accelerometer
    private val gravity = FloatArray(3)
    private var hasGravity = false
    
    // Jerk calculation state
    private var prevLinearAccelMag = 0f
    private var prevTimestampNs = 0L
    
    // Impact duration tracking
    private var impactStartTimeMs = 0L
    private var isImpacting = false

    // Gyroscope values
    private var lastGyroX = 0f
    private var lastGyroY = 0f
    private var lastGyroZ = 0f

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        linearAccelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        
        DrivingManager.hasGyroscope.value = gyroSensor != null
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP_DRIVING") {
            stopSelf()
            return START_NOT_STICKY
        }

        DrivingManager.resetSession()
        lastDetectionTime = 0L
        lastLocation = null
        DrivingManager.isDriving.value = true
        DrivingManager.drivingSessionState.value = "Driving"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                createNotification(),
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, createNotification())
        }
        
        startSensors()
        startLocationTracking()
        startTimer()
        
        return START_STICKY
    }

    private fun createNotification(): android.app.Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, DrivingService::class.java).apply {
            action = "STOP_DRIVING"
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Driving Mode Active")
            .setContentText("Monitoring road conditions with high-precision sensors")
            .setSmallIcon(R.drawable.spot_v_logo)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_view, "Open App", openAppPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Driving", stopPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Driving Mode",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun startSensors() {
        DrivingManager.accelerometerStatus.value = "Monitoring"
        
        // Register Linear Acceleration if available, otherwise raw Accelerometer
        if (linearAccelSensor != null) {
            sensorManager.registerListener(this, linearAccelSensor, SensorManager.SENSOR_DELAY_GAME)
        } else if (accelSensor != null) {
            sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_GAME)
        } else {
            DrivingManager.accelerometerStatus.value = "Unavailable"
        }

        if (gyroSensor != null) {
            DrivingManager.gyroscopeStatus.value = "Monitoring"
            sensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_GAME)
        } else {
            DrivingManager.gyroscopeStatus.value = "Unavailable"
        }
    }

    private fun stopSensors() {
        sensorManager.unregisterListener(this)
        DrivingManager.accelerometerStatus.value = "Disabled"
        DrivingManager.gyroscopeStatus.value = "Disabled"
    }

    private fun startLocationTracking() {
        DrivingManager.gpsStatus.value = "Monitoring"
        val locationService = LocationService.getInstance(this)
        locationService.startLocationUpdates()

        locationCollectorJob = serviceScope.launch {
            locationService.locationState.collect { locState ->
                if (locState.hasFix && locState.location != null) {
                    val loc = locState.location
                    DrivingManager.latitude.value = locState.latitude
                    DrivingManager.longitude.value = locState.longitude
                    DrivingManager.gpsAccuracy.value = locState.accuracy
                    DrivingManager.gpsProvider.value = locState.provider
                    DrivingManager.gpsTimestamp.value = locState.timestamp
                    DrivingManager.currentAddress.value = locState.address
                    DrivingManager.ward.value = locState.ward
                    DrivingManager.currentSpeedKmh.value = locState.speedKmh

                    if (locState.speedKmh < 2.0f) {
                        DrivingManager.currentSpeedString.value = "Waiting for movement..."
                    } else {
                        DrivingManager.currentSpeedString.value =
                            String.format(java.util.Locale.US, "%.1f km/h", locState.speedKmh)
                    }

                    if (lastLocation != null) {
                        val distKm = lastLocation!!.distanceTo(loc) / 1000f
                        if (locState.speedKmh >= 2.0f && distKm < 0.5f) {
                            DrivingManager.distanceTravelledKm.value += distKm
                        }
                    }
                    lastLocation = loc
                }
            }
        }
    }

    private fun startTimer() {
        timerJob = serviceScope.launch {
            while (isActive) {
                delay(1000L)
                if (DrivingManager.drivingSessionState.value == "Driving") {
                    DrivingManager.incrementTime()
                }
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_GYROSCOPE -> {
                lastGyroX = event.values[0]
                lastGyroY = event.values[1]
                lastGyroZ = event.values[2]
            }

            Sensor.TYPE_LINEAR_ACCELERATION, Sensor.TYPE_ACCELEROMETER -> {
                // 1. VEHICLE SPEED CHECK (Speed >= 2.0 km/h)
                val currentSpeedKmh = DrivingManager.currentSpeedKmh.value
                val gpsAccuracy = DrivingManager.gpsAccuracy.value

                if (currentSpeedKmh < 2.0f) {
                    isImpacting = false
                    DrivingManager.debugConfidence.value = 0
                    DrivingManager.debugDetectionState.value = "Vehicle Stationary (< 2 km/h)"
                    return
                }

                if (gpsAccuracy > 20.0f) {
                    isImpacting = false
                    DrivingManager.debugConfidence.value = 0
                    DrivingManager.debugDetectionState.value = "Low GPS Accuracy (> 20m)"
                    return
                }

                // Increment sample count only when vehicle is moving at valid speed
                DrivingManager.sensorSamplesCount.value += 1

                // 2. LINEAR ACCELERATION EXTRACTION
                val lx: Float
                val ly: Float
                val lz: Float

                if (event.sensor.type == Sensor.TYPE_LINEAR_ACCELERATION) {
                    lx = event.values[0]
                    ly = event.values[1]
                    lz = event.values[2]
                } else {
                    // High Pass Filter on raw Accelerometer to extract linear acceleration
                    val alpha = 0.8f
                    if (!hasGravity) {
                        gravity[0] = event.values[0]
                        gravity[1] = event.values[1]
                        gravity[2] = event.values[2]
                        hasGravity = true
                    } else {
                        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0]
                        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1]
                        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2]
                    }
                    lx = event.values[0] - gravity[0]
                    ly = event.values[1] - gravity[1]
                    lz = event.values[2] - gravity[2]
                }

                // 3. VERTICAL IMPACT ISOLATION & PHONE HANDLING FILTER
                val verticalImpact = abs(lz)
                val horizontalImpact = sqrt(lx * lx + ly * ly)
                val gyroMag = sqrt(lastGyroX * lastGyroX + lastGyroY * lastGyroY + lastGyroZ * lastGyroZ)

                // Phone handling filter: reject if phone is rotated, tilted, picked up, or horizontal force dominates
                if (gyroMag > 1.2f || (horizontalImpact > (2.5f * verticalImpact) && horizontalImpact > 3.0f)) {
                    isImpacting = false
                    DrivingManager.debugConfidence.value = 0
                    DrivingManager.debugDetectionState.value = "Phone Handling / Rotation Filtered"
                    DrivingManager.filteredEventsCount.value += 1
                    return
                }

                // 4. INCREASED IMPACT THRESHOLD CHECK (minimum 3.5 m/s² vertical shock)
                if (verticalImpact < 3.5f) {
                    isImpacting = false
                    DrivingManager.debugConfidence.value = 0
                    DrivingManager.debugDetectionState.value = "Monitoring Road"
                    DrivingManager.filteredEventsCount.value += 1
                    return
                }

                // 5. VERTICAL JERK & SHARP SPIKE DURATION CHECK
                val currentTimestampNs = event.timestamp
                val dt = if (prevTimestampNs > 0 && currentTimestampNs > prevTimestampNs) {
                    (currentTimestampNs - prevTimestampNs) / 1000000000.0f
                } else {
                    0.02f
                }
                prevTimestampNs = currentTimestampNs

                val verticalJerk = abs(verticalImpact - prevLinearAccelMag) / dt.coerceAtLeast(0.005f)
                prevLinearAccelMag = verticalImpact

                val currentTimeMs = System.currentTimeMillis()
                if (verticalImpact >= 3.5f) {
                    if (!isImpacting) {
                        isImpacting = true
                        impactStartTimeMs = currentTimeMs
                    }
                } else {
                    isImpacting = false
                }
                val impactDurationMs = if (isImpacting) (currentTimeMs - impactStartTimeMs) else 100L

                // Reject continuous vibrations lasting longer than 400 ms (e.g. rough unpaved road or pocket noise)
                if (impactDurationMs > 400L) {
                    isImpacting = false
                    DrivingManager.debugConfidence.value = 0
                    DrivingManager.debugDetectionState.value = "Continuous Vibration (> 400ms)"
                    DrivingManager.filteredEventsCount.value += 1
                    return
                }

                // 6. COOLDOWN CHECK (2.0 seconds)
                val isCooldownActive = (currentTimeMs - lastDetectionTime) <= cooldownMs
                val cooldownRemaining = if (isCooldownActive) ((cooldownMs - (currentTimeMs - lastDetectionTime)) / 1000L) else 0L
                DrivingManager.debugCooldownActive.value = isCooldownActive
                DrivingManager.debugCooldownRemaining.value = cooldownRemaining

                // 7. CONFIDENCE CALCULATION (Independent of GPS Speed)
                // Impact score (0 - 40 pts)
                val impactScore = ((verticalImpact - 3.5f) / 6.0f).coerceIn(0f, 1f) * 40f
                // Jerk score (0 - 25 pts)
                val jerkScore = ((verticalJerk - 20.0f) / 60.0f).coerceIn(0f, 1f) * 25f
                // Spike duration score (0 - 15 pts)
                val durationScore = if (impactDurationMs in 80L..350L) 15f else if (impactDurationMs in 50L..400L) 8f else 0f
                // Gyroscope stability score (0 - 10 pts)
                val gyroScore = if (gyroMag < 0.6f) 10f else if (gyroMag < 1.2f) 5f else 0f
                // GPS accuracy score (0 - 10 pts)
                val gpsScore = if (gpsAccuracy < 10.0f) 10f else if (gpsAccuracy < 20.0f) 5f else 0f

                val confidence = (impactScore + jerkScore + durationScore + gyroScore + gpsScore).toInt().coerceIn(0, 100)
                DrivingManager.debugConfidence.value = confidence

                val threshold = 65

                // 8. FINAL DETECTION EVALUATION
                if (confidence >= threshold && verticalJerk >= 20.0f) {
                    if (!isCooldownActive) {
                        lastDetectionTime = currentTimeMs
                        val detectionId = currentTimeMs

                        // Trigger candidate ONLY
                        DrivingManager.triggerCandidatePothole(detectionId)
                        DrivingManager.debugDetectionState.value = "Candidate Triggered ($confidence%)"

                        // Show Notification Alert
                        val notificationService = NotificationService(this)
                        notificationService.showPotholeAlertNotification()

                        // Schedule auto-timeout: if no user response within 12s, treat as ignored
                        serviceScope.launch {
                            delay(12000L)
                            if (DrivingManager.activeDetectionId == detectionId) {
                                DrivingManager.userIgnoredDetection()
                                notificationService.dismissNotification()
                            }
                        }
                    } else {
                        DrivingManager.algorithmRejectionsCount.value += 1
                        DrivingManager.debugDetectionState.value = "Cooldown Active (${cooldownRemaining}s)"
                    }
                } else {
                    DrivingManager.algorithmRejectionsCount.value += 1
                    DrivingManager.debugDetectionState.value = "Monitoring ($confidence%)"
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        super.onDestroy()
        DrivingManager.isDriving.value = false
        DrivingManager.drivingSessionState.value = "Stopped"
        
        stopSensors()
        LocationService.getInstance(this).stopLocationUpdates()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

package com.example.services

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.math.sqrt

class SensorService(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    
    private val _potholeDetected = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val potholeDetected = _potholeDetected.asSharedFlow()

    private var lastDetectionTime = 0L
    private val cooldownMs = 10000L // 10 seconds cooldown
    
    // Threshold for pothole detection (g-force variation)
    private val POTHOLE_THRESHOLD = 4.0f

    fun startMonitoring() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stopMonitoring() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            
            // Calculate total acceleration minus gravity (~9.8)
            val acceleration = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
            val gForce = Math.abs(acceleration - SensorManager.GRAVITY_EARTH)
            
            if (gForce > POTHOLE_THRESHOLD) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastDetectionTime > cooldownMs) {
                    lastDetectionTime = currentTime
                    _potholeDetected.tryEmit(Unit)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used
    }
}

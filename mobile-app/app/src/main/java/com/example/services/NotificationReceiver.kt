package com.example.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notificationService = NotificationService(context)
        when (intent.action) {
            "DISMISS_POTHOLE" -> {
                DrivingManager.userIgnoredDetection()
                notificationService.dismissNotification()
                Toast.makeText(context, "Detection ignored.", Toast.LENGTH_SHORT).show()
            }
            "CONFIRM_POTHOLE" -> {
                DrivingManager.userConfirmedDetection()
                notificationService.dismissNotification()
                Toast.makeText(context, "Detection confirmed.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

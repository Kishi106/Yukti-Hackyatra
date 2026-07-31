package com.example.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "DISMISS_POTHOLE") {
            val notificationService = NotificationService(context)
            notificationService.dismissNotification()
            Toast.makeText(context, "Detection ignored.", Toast.LENGTH_SHORT).show()
        }
    }
}

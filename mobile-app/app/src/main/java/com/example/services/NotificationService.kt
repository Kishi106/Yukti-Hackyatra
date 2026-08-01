package com.example.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class NotificationService(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Pothole Detection Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for suspected potholes"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showPotholeAlertNotification(potholeId: String) {
        val confirmIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = "CONFIRM_POTHOLE"
            putExtra("pothole_id", potholeId)
        }
        val confirmPendingIntent = PendingIntent.getBroadcast(
            context, 2, confirmIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val noIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = "DISMISS_POTHOLE"
        }
        val noPendingIntent = PendingIntent.getBroadcast(
            context, 1, noIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠ Possible Pothole Detected")
            .setContentText("Did you encounter a pothole?")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_menu_edit, "Yes, that's a pothole", confirmPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "NO", noPendingIntent)

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    fun dismissNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }

    companion object {
        const val CHANNEL_ID = "pothole_alerts"
        const val NOTIFICATION_ID = 1001
    }
}

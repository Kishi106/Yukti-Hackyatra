package com.example.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.network.PotholeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notificationService = NotificationService(context)

        when (intent.action) {
            "DISMISS_POTHOLE" -> {
                notificationService.dismissNotification()
                Toast.makeText(context, "Detection ignored.", Toast.LENGTH_SHORT).show()
            }
            "CONFIRM_POTHOLE" -> {
                notificationService.dismissNotification()
                val potholeId = intent.getStringExtra("pothole_id")
                if (potholeId == null) {
                    Toast.makeText(context, "Couldn't record feedback.", Toast.LENGTH_SHORT).show()
                    return
                }

                // BroadcastReceivers can't launch long-running coroutines directly — the
                // receiver instance (and process priority) may be torn down as soon as
                // onReceive() returns. goAsync() keeps the process alive until finish().
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val repository = PotholeRepository()
                        repository.confirmPothole(potholeId)
                        Toast.makeText(context, "Thanks! Feedback recorded.", Toast.LENGTH_SHORT).show()
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}

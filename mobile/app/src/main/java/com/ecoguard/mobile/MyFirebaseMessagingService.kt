package com.ecoguard.mobile

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        remoteMessage.notification?.let {
            val title = it.title ?: "EcoGuard Alert"
            val body = it.body ?: "A sensor threshold has been breached."
            sendNotification(title, body)
        }
    }

    private fun sendNotification(title: String, message: String) {
        // Create a notification channel (required for Android 8.0+)
        createNotificationChannel()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // In a real app, you might handle this case, but for this project, we assume permission is granted.
            return
        }

        val notificationManager = NotificationManagerCompat.from(this)
        val groupKey = "ecoguard_alerts_group"
        val notificationId = System.currentTimeMillis().toInt()

        // Individual notification (grouped)
        val builder = NotificationCompat.Builder(this, "SENSOR_ALERTS")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setGroup(groupKey) // Group all alerts together
            .setGroupSummary(false)

        // Summary notification (shows count)
        val summaryBuilder = NotificationCompat.Builder(this, "SENSOR_ALERTS")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("EcoGuard Alerts")
            .setContentText("New sensor threshold alerts")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setGroup(groupKey)
            .setGroupSummary(true)
            .setAutoCancel(true)

        // Show individual notification
        notificationManager.notify(notificationId, builder.build())
        
        // Show/update summary (use fixed ID for summary)
        notificationManager.notify(999, summaryBuilder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Sensor Alerts"
            val descriptionText = "Notifications for sensor threshold alerts"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("SENSOR_ALERTS", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}

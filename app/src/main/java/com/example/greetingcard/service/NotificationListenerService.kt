package com.example.greetingcard.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.greetingcard.data.NotificationDatabase
import com.example.greetingcard.data.NotificationEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

const val TAG = "AI:NotifListener"

class NotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var database: NotificationDatabase

//    override fun onCreate() {
//        super.onCreate()
//        database = NotificationDatabase.getDatabase(this)
//        Log.i("NotificationListener", "Service created")
//    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        database = NotificationDatabase.getDatabase(this)
        Log.i(TAG, "connected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)

        val notification = sbn.notification
        val extras = notification.extras

        val title = extras.getString("android.title") ?: "No title"
        val description = extras.getString("android.text") ?: "No description"
        val packageName = sbn.packageName
        val timestamp = System.currentTimeMillis()

        Log.i(TAG, "Notification from $packageName: $title")

        // Save to database
        serviceScope.launch {
            try {
                val notificationEntity =
                        NotificationEntity(
                                title = title,
                                description = description,
                                packageName = packageName,
                                timestamp = timestamp
                        )
                database.notificationDao().insertNotification(notificationEntity)
                Log.i(TAG, "Saved notification to database")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving notification", e)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)
        // We don't need to do anything when notifications are removed
    }
}

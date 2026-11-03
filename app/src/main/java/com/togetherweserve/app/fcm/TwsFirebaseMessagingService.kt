package com.togetherweserve.app.fcm

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.togetherweserve.app.MainActivity
import com.togetherweserve.app.R
import com.togetherweserve.app.ui.eventdetail.EventDetailActivity
import com.togetherweserve.app.utils.NotificationItem
import com.togetherweserve.app.utils.NotificationStore
import com.togetherweserve.app.utils.NotificationTopics
import com.togetherweserve.app.utils.SessionManager

/**
 * FR9 - Real-time push notifications (Firebase Cloud Messaging).
 *
 * The Cloud Function publishes a DATA-ONLY message to the "new_events" topic
 * whenever an organiser publishes an event, so this service is always invoked
 * (app in foreground or background). Each alert is:
 *   1. stored locally so it appears in the in-app Notifications list, and
 *   2. shown as a system notification that opens the event when tapped.
 */
class TwsFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(TAG, "FCM token refreshed")
        // The app uses a topic, so no token needs storing - just keep the subscription alive.
        NotificationTopics.apply(SessionManager(applicationContext).eventRemindersEnabled)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.i(TAG, "Message received: data=${message.data}")

        // Respect the "Event reminders" switch in Settings.
        if (!SessionManager(applicationContext).eventRemindersEnabled) {
            Log.d(TAG, "Event reminders disabled in Settings - ignoring message")
            return
        }

        val title = message.data["title"] ?: message.notification?.title ?: "TogetherWeServe"
        val body = message.data["body"] ?: message.notification?.body ?: ""
        val eventId = message.data["eventId"]?.takeIf { it.isNotBlank() }
        val now = System.currentTimeMillis()

        NotificationStore(applicationContext).add(NotificationItem(now, title, body, eventId, now))
        showNotification(title, body, eventId, now.toInt())
    }

    private fun showNotification(title: String, body: String, eventId: String?, notificationId: Int) {
        // Android 13+: without the runtime permission the OS silently drops the notification.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "POST_NOTIFICATIONS not granted - alert saved to the in-app list only")
            return
        }

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, getString(R.string.app_name), NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }

        // Tapping opens the event (with Home underneath so Back behaves sensibly).
        val stack = TaskStackBuilder.create(this).addNextIntent(Intent(this, MainActivity::class.java))
        if (eventId != null) {
            stack.addNextIntent(Intent(this, EventDetailActivity::class.java)
                .putExtra(EventDetailActivity.EXTRA_EVENT_ID, eventId))
        }
        val pendingIntent = stack.getPendingIntent(
            notificationId, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(notificationId, notification)
        Log.i(TAG, "System notification shown: $title")
    }

    companion object {
        private const val TAG = "TWS-FCM"
        private const val CHANNEL_ID = "tws_default_channel"
    }
}

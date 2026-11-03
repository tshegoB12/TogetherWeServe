package com.togetherweserve.app.utils

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging

/**
 * Subscribes/unsubscribes this installation from the FCM "new_events" topic so
 * the "Event reminders" switch in Settings genuinely controls push delivery.
 */
object NotificationTopics {
    const val NEW_EVENTS = "new_events"
    private const val TAG = "TWS-Topics"

    fun apply(enabled: Boolean) {
        val messaging = FirebaseMessaging.getInstance()
        val task = if (enabled) messaging.subscribeToTopic(NEW_EVENTS)
        else messaging.unsubscribeFromTopic(NEW_EVENTS)
        task.addOnCompleteListener { result ->
            Log.i(TAG, "Topic '$NEW_EVENTS' ${if (enabled) "subscribe" else "unsubscribe"} success=${result.isSuccessful}")
        }
    }
}

package com.togetherweserve.app.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * Persists received push notifications locally so the Notifications screen can
 * list them (also works offline). Backed by SharedPreferences + JSON.
 */
class NotificationStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun all(): List<NotificationItem> = NotificationCodec.decode(prefs.getString(KEY, null))

    fun add(item: NotificationItem) {
        val updated = NotificationCodec.prepend(all(), item)
        prefs.edit().putString(KEY, NotificationCodec.encode(updated)).apply()
        Log.d(TAG, "Stored notification '${item.title}' (total=${updated.size})")
    }

    fun clear() {
        prefs.edit().remove(KEY).apply()
        Log.d(TAG, "Notification history cleared")
    }

    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) =
        prefs.registerOnSharedPreferenceChangeListener(listener)

    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) =
        prefs.unregisterOnSharedPreferenceChangeListener(listener)

    companion object {
        const val KEY = "notification_history"
        private const val PREFS_NAME = "tws_notifications"
        private const val TAG = "TWS-NotifStore"
    }
}

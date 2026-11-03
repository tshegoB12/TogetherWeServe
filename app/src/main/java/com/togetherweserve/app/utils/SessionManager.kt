package com.togetherweserve.app.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Small wrapper around SharedPreferences for locally persisted, non-sensitive
 * user state: selected language, cached role and notification preferences.
 * Authentication itself is handled entirely by Firebase Auth.
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var role: String
        get() = prefs.getString(KEY_ROLE, "volunteer") ?: "volunteer"
        set(value) = prefs.edit().putString(KEY_ROLE, value).apply()

    var language: String
        get() = prefs.getString(KEY_LANGUAGE, "en") ?: "en"
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value).apply()

    var eventRemindersEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIF_EVENTS, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIF_EVENTS, value).apply()

    var chatMessagesEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIF_CHAT, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIF_CHAT, value).apply()

    fun isFavorite(eventId: String): Boolean =
        prefs.getStringSet(KEY_FAVORITES, emptySet())?.contains(eventId) == true

    fun setFavorite(eventId: String, favorite: Boolean) {
        val favorites = (prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()).toMutableSet()
        if (favorite) favorites.add(eventId) else favorites.remove(eventId)
        prefs.edit().putStringSet(KEY_FAVORITES, favorites).apply()
    }

    fun favoriteCount(): Int = prefs.getStringSet(KEY_FAVORITES, emptySet())?.size ?: 0

    fun clear() = prefs.edit().clear().apply()

    companion object {
        private const val PREFS_NAME = "tws_session"
        private const val KEY_ROLE = "role"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_NOTIF_EVENTS = "notif_events"
        private const val KEY_NOTIF_CHAT = "notif_chat"
        private const val KEY_FAVORITES = "favorite_events"
    }
}

package com.togetherweserve.app.utils

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/** One push alert received from Firebase Cloud Messaging, kept for the in-app Notifications list. */
data class NotificationItem(
    val id: Long,
    val title: String,
    val body: String,
    val eventId: String?,
    val receivedAt: Long
)

/**
 * Pure (Android-free) JSON helpers so the notification history logic can be
 * unit tested on the JVM in GitHub Actions.
 */
object NotificationCodec {
    const val MAX_ITEMS = 50
    private val gson = Gson()
    private val listType = object : TypeToken<List<NotificationItem>>() {}.type

    fun encode(items: List<NotificationItem>): String = gson.toJson(items)

    /** Returns an empty list for null/blank/corrupt JSON instead of crashing. */
    fun decode(json: String?): List<NotificationItem> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            gson.fromJson<List<NotificationItem>>(json, listType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Newest first, capped at [MAX_ITEMS] so storage can never grow without limit. */
    fun prepend(existing: List<NotificationItem>, item: NotificationItem): List<NotificationItem> =
        (listOf(item) + existing).take(MAX_ITEMS)
}

package com.togetherweserve.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Local cache of the current user's own registrations, shown on the
 *  My Schedule screen. status = confirmed | pending_sync | cancelled. */
@Entity(tableName = "registrations")
data class RegistrationEntity(
    @PrimaryKey val registrationId: String,
    val eventId: String,
    val eventTitle: String,
    val eventDateTime: Long,
    val groupId: String?,
    val status: String
)

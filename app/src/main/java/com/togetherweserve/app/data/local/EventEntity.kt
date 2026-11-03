package com.togetherweserve.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Local cache of an event, used so the Home feed and Event Detail screens
 *  keep working when the device has no connectivity. */
@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val eventId: String,
    val title: String,
    val cause: String,
    val description: String,
    val dateTime: Long,
    val location: String,
    val slotsAvailable: Int,
    val volunteersGoing: Int,
    val organiserId: String,
    val organiserName: String
)

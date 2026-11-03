package com.togetherweserve.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A queued action (join / cancel) created while the device was offline.
 * The repository replays these against the REST API once connectivity
 * returns, then removes them and updates the matching RegistrationEntity.
 */
@Entity(tableName = "pending_actions")
data class PendingActionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventId: String,
    val actionType: String, // "join" or "cancel"
    val groupCode: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

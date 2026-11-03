package com.togetherweserve.app.data.local

import androidx.room.*

@Dao
interface PendingActionDao {
    @Query("SELECT * FROM pending_actions ORDER BY createdAt ASC")
    suspend fun getAll(): List<PendingActionEntity>

    @Insert
    suspend fun insert(action: PendingActionEntity): Long

    @Delete
    suspend fun delete(action: PendingActionEntity)
}

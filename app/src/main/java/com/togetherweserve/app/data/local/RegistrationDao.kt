package com.togetherweserve.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RegistrationDao {
    @Query("SELECT * FROM registrations ORDER BY eventDateTime ASC")
    fun observeAll(): Flow<List<RegistrationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(registrations: List<RegistrationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(registration: RegistrationEntity)

    @Query("UPDATE registrations SET status = :status WHERE registrationId = :id")
    suspend fun updateStatus(id: String, status: String)

    /** Removes a queued offline registration once the real server copy exists. */
    @Query("DELETE FROM registrations WHERE registrationId = :id")
    suspend fun deleteById(id: String)

    /** Removes "local_" placeholders that now have a server-side registration (prevents duplicates). */
    @Query("DELETE FROM registrations WHERE substr(registrationId, 1, 6) = 'local_' AND eventId IN (:eventIds)")
    suspend fun deleteLocalDuplicates(eventIds: List<String>)

    @Query("DELETE FROM registrations")
    suspend fun clear()
}

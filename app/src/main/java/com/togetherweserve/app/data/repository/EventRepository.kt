package com.togetherweserve.app.data.repository

import android.content.Context
import android.util.Log
import com.togetherweserve.app.data.local.AppDatabase
import com.togetherweserve.app.data.local.EventEntity
import com.togetherweserve.app.data.local.PendingActionEntity
import com.togetherweserve.app.data.local.RegistrationEntity
import com.togetherweserve.app.data.remote.RetrofitClient
import com.togetherweserve.app.data.remote.models.CreateGroupRequest
import com.togetherweserve.app.data.remote.models.RegisterRequest
import com.togetherweserve.app.utils.ConnectivityObserver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException

/**
 * Single source of truth for events and registrations.
 *
 * Read path: always read from RoomDB (Flow), which is refreshed from the
 * REST API whenever the device is online.
 *
 * Write path (join / cancel): if online, call the API directly and cache
 * the result. If offline, queue a PendingActionEntity and optimistically
 * update the local registration as "pending_sync" so My Schedule reflects
 * it immediately; syncPendingActions() replays the queue once reconnected
 * (triggered app-wide by the network callback in TogetherWeServeApp).
 */
class EventRepository(context: Context) {

    private val appContext = context.applicationContext
    private val db = AppDatabase.getInstance(appContext)
    private val api = RetrofitClient.api

    fun observeEvents(): Flow<List<EventEntity>> = db.eventDao().observeAll()
    fun observeSchedule(): Flow<List<RegistrationEntity>> = db.registrationDao().observeAll()

    suspend fun refreshEvents(cause: String? = null, query: String? = null) {
        if (!ConnectivityObserver.isOnline(appContext)) {
            Log.d(TAG, "refreshEvents skipped - offline, showing Room cache")
            return
        }
        try {
            val remote = api.getEvents(cause, query)
            db.eventDao().upsertAll(remote.map {
                EventEntity(it.eventId, it.title, it.cause, it.description, it.dateTime,
                    it.location, it.slotsAvailable, it.volunteersGoing, it.organiserId, it.organiserName)
            })
            Log.i(TAG, "refreshEvents OK - cached ${remote.size} events in Room")
        } catch (e: Exception) {
            // Network/API failure - keep showing whatever is already cached in Room
            // instead of crashing the app.
            Log.e(TAG, "refreshEvents failed - keeping cached events", e)
        }
    }

    suspend fun refreshSchedule(userId: String) {
        if (!ConnectivityObserver.isOnline(appContext)) return
        try {
            val remote = api.getSchedule(userId)
            db.registrationDao().upsertAll(remote.map {
                RegistrationEntity(it.registrationId, it.eventId, it.eventTitle, it.eventDateTime, it.groupId, it.status)
            })
            // The server copy now exists, so drop any offline placeholder for the same events.
            db.registrationDao().deleteLocalDuplicates(remote.map { it.eventId })
            Log.i(TAG, "refreshSchedule OK - ${remote.size} registrations cached")
        } catch (e: Exception) {
            Log.e(TAG, "refreshSchedule failed - keeping cached schedule", e)
        }
    }

    suspend fun getEvent(eventId: String): EventEntity? {
        if (ConnectivityObserver.isOnline(appContext)) {
            return try {
                val remote = api.getEvent(eventId)
                EventEntity(remote.eventId, remote.title, remote.cause, remote.description,
                    remote.dateTime, remote.location, remote.slotsAvailable, remote.volunteersGoing,
                    remote.organiserId, remote.organiserName).also { db.eventDao().upsert(it) }
            } catch (e: Exception) {
                Log.w(TAG, "getEvent($eventId) API failed - falling back to Room cache", e)
                db.eventDao().getById(eventId)
            }
        }
        Log.d(TAG, "getEvent($eventId) offline - reading Room cache")
        return db.eventDao().getById(eventId)
    }

    /** Individual or group join. Returns true if it went straight to the
     *  server, false if it was queued for later sync while offline. */
    suspend fun joinEvent(eventId: String, eventTitle: String, eventDateTime: Long, groupCode: String?): Boolean {
        return if (ConnectivityObserver.isOnline(appContext)) {
            val mode = if (groupCode != null) "group" else "individual"
            val response = api.registerForEvent(eventId, RegisterRequest(mode, groupCode))
            db.registrationDao().upsert(
                RegistrationEntity(response.registrationId, eventId, eventTitle, eventDateTime, groupCode, "confirmed")
            )
            Log.i(TAG, "joinEvent online OK ($mode) for event $eventId")
            true
        } else {
            db.pendingActionDao().insert(PendingActionEntity(eventId = eventId, actionType = "join", groupCode = groupCode))
            db.registrationDao().upsert(
                RegistrationEntity("local_$eventId", eventId, eventTitle, eventDateTime, groupCode, "pending_sync")
            )
            Log.i(TAG, "joinEvent OFFLINE - queued pending action for event $eventId")
            false
        }
    }

    suspend fun createGroup(eventId: String, groupName: String) = api.createGroup(eventId, CreateGroupRequest(groupName))

    /**
     * Replays everything queued while offline. Guarded by a Mutex so the app-wide
     * network callback and the Schedule screen can never submit the same action twice.
     */
    suspend fun syncPendingActions() {
        syncMutex.withLock {
            if (!ConnectivityObserver.isOnline(appContext)) return@withLock
            val pending = db.pendingActionDao().getAll()
            if (pending.isEmpty()) return@withLock
            Log.i(TAG, "Sync started - ${pending.size} queued action(s)")
            for (action in pending) {
                try {
                    if (action.actionType == "join") {
                        val mode = if (action.groupCode != null) "group" else "individual"
                        api.registerForEvent(action.eventId, RegisterRequest(mode, action.groupCode))
                        db.registrationDao().updateStatus("local_${action.eventId}", "confirmed")
                        Log.i(TAG, "Synced queued join for event ${action.eventId}")
                    }
                    db.pendingActionDao().delete(action)
                } catch (e: HttpException) {
                    if (e.code() in 400..499) {
                        // The server rejected it for good (e.g. already registered / event full):
                        // drop it so it does not retry forever.
                        Log.w(TAG, "Server rejected queued action ${action.id} (HTTP ${e.code()}) - discarding", e)
                        db.registrationDao().deleteById("local_${action.eventId}")
                        db.pendingActionDao().delete(action)
                    } else {
                        Log.e(TAG, "Server error while syncing action ${action.id} - will retry", e)
                    }
                } catch (e: Exception) {
                    // Leave it queued; will retry on the next sync pass.
                    Log.e(TAG, "Sync failed for action ${action.id} - will retry later", e)
                }
            }
            Log.i(TAG, "Sync finished")
        }
    }

    companion object {
        private const val TAG = "TWS-Repository"
        private val syncMutex = Mutex()
    }
}

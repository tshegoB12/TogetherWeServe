package com.togetherweserve.app.data.remote

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.togetherweserve.app.data.remote.models.*
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.http.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Raw Cloud Firestore REST endpoints (https://firestore.googleapis.com).
 * Free on the Spark plan - no Cloud Functions / Blaze plan needed.
 * The signed-in user's Firebase ID token is sent as a Bearer token by
 * RetrofitClient, so the Firestore security rules (firestore.rules)
 * decide what each user may read/write.
 */
interface FirestoreRest {
    @POST("./documents:runQuery")
    suspend fun runQuery(@Body body: JsonObject): JsonArray

    @GET("documents/{path}")
    suspend fun getDoc(@Path("path", encoded = true) path: String): JsonObject

    @POST("documents/{collection}")
    suspend fun createDoc(@Path("collection", encoded = true) collection: String, @Body body: JsonObject): JsonObject

    @PATCH("documents/{path}")
    suspend fun patchDoc(
        @Path("path", encoded = true) path: String,
        @Query("updateMask.fieldPaths") fields: List<String>,
        @Body body: JsonObject
    ): JsonObject

    @POST("./documents:commit")
    suspend fun commit(@Body body: JsonObject): JsonObject
}

/**
 * Implements the app's ApiService on top of the Firestore REST API, so the
 * repository/UI code is unchanged. Business rules that used to live in the
 * Cloud Function (slot check, invite-code lookup) run here, and permission
 * rules (organiser-only events, own-schedule-only) live in firestore.rules.
 */
class FirestoreApi(private val rest: FirestoreRest, private val projectId: String) : ApiService {

    private val tag = "TWS-FirestoreApi"
    private val dbPath get() = "projects/$projectId/databases/(default)/documents"

    // ---------- Firestore typed-value helpers ----------
    private fun str(v: String) = JsonObject().apply { addProperty("stringValue", v) }
    private fun int(v: Long) = JsonObject().apply { addProperty("integerValue", v.toString()) }
    private fun bool(v: Boolean) = JsonObject().apply { addProperty("booleanValue", v) }
    private fun nul() = JsonObject().apply { addProperty("nullValue", "NULL_VALUE") }
    private fun now() = JsonObject().apply {
        val f = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        addProperty("timestampValue", f.format(Date()))
    }
    private fun arr(vararg v: JsonObject) = JsonObject().apply {
        add("arrayValue", JsonObject().apply { add("values", JsonArray().apply { v.forEach { add(it) } }) })
    }
    private fun doc(vararg f: Pair<String, JsonObject>) = JsonObject().apply {
        add("fields", JsonObject().apply { f.forEach { add(it.first, it.second) } })
    }

    private fun JsonObject.fields(): JsonObject = getAsJsonObject("fields") ?: JsonObject()
    private fun JsonObject.s(k: String): String? =
        fields().getAsJsonObject(k)?.get("stringValue")?.takeIf { !it.isJsonNull }?.asString
    private fun JsonObject.l(k: String): Long =
        fields().getAsJsonObject(k)?.get("integerValue")?.asString?.toLongOrNull() ?: 0L
    private fun JsonObject.docId(): String = get("name").asString.substringAfterLast('/')

    private fun httpError(code: Int): Nothing =
        throw HttpException(Response.error<Any>(code, "".toResponseBody(null)))

    private fun uid(): String = FirebaseAuth.getInstance().currentUser?.uid ?: httpError(401)

    private fun filter(field: String, value: JsonObject) = JsonObject().apply {
        add("fieldFilter", JsonObject().apply {
            add("field", JsonObject().apply { addProperty("fieldPath", field) })
            addProperty("op", "EQUAL")
            add("value", value)
        })
    }

    /** Runs a structured query with AND-ed equality filters; returns matching documents. */
    private suspend fun runFilter(collection: String, filters: List<JsonObject>, limit: Int? = null): List<JsonObject> {
        val sq = JsonObject().apply {
            add("from", JsonArray().apply { add(JsonObject().apply { addProperty("collectionId", collection) }) })
            if (filters.size == 1) add("where", filters[0])
            else if (filters.size > 1) add("where", JsonObject().apply {
                add("compositeFilter", JsonObject().apply {
                    addProperty("op", "AND")
                    add("filters", JsonArray().apply { filters.forEach { add(it) } })
                })
            })
            if (limit != null) addProperty("limit", limit)
        }
        val result = rest.runQuery(JsonObject().apply { add("structuredQuery", sq) })
        return result.mapNotNull { it.asJsonObject.getAsJsonObject("document") }
    }

    private fun toEventDto(d: JsonObject, going: Int = 0) = EventDto(
        eventId = d.docId(), title = d.s("title") ?: "", cause = d.s("cause") ?: "",
        description = d.s("description") ?: "", dateTime = d.l("dateTime"),
        location = d.s("location") ?: "", slotsAvailable = d.l("slotsAvailable").toInt(),
        volunteersGoing = going, organiserId = d.s("organiserId") ?: "",
        organiserName = d.s("organiserName") ?: ""
    )

    private suspend fun confirmedCount(eventId: String): Int =
        runFilter("registrations", listOf(filter("eventId", str(eventId)), filter("status", str("confirmed")))).size

    // ---------- ApiService ----------
    override suspend fun getEvents(cause: String?, query: String?): List<EventDto> {
        val filters = if (cause != null) listOf(filter("cause", str(cause))) else emptyList()
        var events = runFilter("events", filters, 100).map { toEventDto(it) }.sortedBy { it.dateTime }
        if (!query.isNullOrBlank()) {
            val q = query.lowercase()
            events = events.filter { it.title.lowercase().contains(q) || it.cause.lowercase().contains(q) }
        }
        Log.i(tag, "getEvents -> ${events.size} events")
        return events
    }

    override suspend fun getEvent(eventId: String): EventDto =
        toEventDto(rest.getDoc("events/$eventId"), confirmedCount(eventId))

    override suspend fun createEvent(body: CreateEventRequest): CreateEventResponse {
        val user = FirebaseAuth.getInstance().currentUser ?: httpError(401)
        val created = rest.createDoc("events", doc(
            "title" to str(body.title), "cause" to str(body.cause),
            "description" to str(body.description), "dateTime" to int(body.dateTime),
            "location" to str(body.location), "slotsAvailable" to int(body.slotsAvailable.toLong()),
            "organiserId" to str(user.uid),
            "organiserName" to str(user.displayName ?: "Organiser"),
            "createdAt" to now()
        ))
        Log.i(tag, "createEvent OK ${created.docId()}")
        return CreateEventResponse(created.docId())
    }

    override suspend fun registerForEvent(eventId: String, body: RegisterRequest): RegisterResponse {
        val userId = uid()
        val event = try { rest.getDoc("events/$eventId") } catch (e: HttpException) { throw e }
        var groupDocId: String? = null
        if (body.mode == "group") {
            val code = body.groupCode ?: httpError(400)
            val group = runFilter("groups", listOf(filter("eventId", str(eventId)), filter("inviteCode", str(code))), 1)
                .firstOrNull() ?: httpError(404)
            groupDocId = group.docId()
        }
        // Server-side style business rule: refuse when the event is full.
        if (confirmedCount(eventId) >= event.l("slotsAvailable")) httpError(409)

        if (groupDocId != null) {
            rest.commit(JsonObject().apply {
                add("writes", JsonArray().apply {
                    add(JsonObject().apply {
                        add("transform", JsonObject().apply {
                            addProperty("document", "$dbPath/groups/$groupDocId")
                            add("fieldTransforms", JsonArray().apply {
                                add(JsonObject().apply {
                                    addProperty("fieldPath", "memberIds")
                                    add("appendMissingElements", JsonObject().apply {
                                        add("values", JsonArray().apply { add(str(userId)) })
                                    })
                                })
                            })
                        })
                    })
                })
            })
        }
        val reg = rest.createDoc("registrations", doc(
            "eventId" to str(eventId), "userId" to str(userId),
            "groupId" to (groupDocId?.let { str(it) } ?: nul()),
            "status" to str("confirmed"), "createdAt" to now()
        ))
        Log.i(tag, "registerForEvent OK ${reg.docId()}")
        return RegisterResponse(reg.docId())
    }

    override suspend fun createGroup(eventId: String, body: CreateGroupRequest): CreateGroupResponse {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // no ambiguous characters
        val code = "TWS-" + (1..4).map { chars.random() }.joinToString("")
        val created = rest.createDoc("groups", doc(
            "groupName" to str(body.groupName), "inviteCode" to str(code),
            "eventId" to str(eventId), "memberIds" to arr(str(uid())), "createdAt" to now()
        ))
        return CreateGroupResponse(groupCode = code, groupId = created.docId())
    }

    override suspend fun getSchedule(userId: String): List<RegistrationDto> {
        if (userId != uid()) httpError(403)
        return runFilter("registrations", listOf(filter("userId", str(userId)))).map { r ->
            val eventId = r.s("eventId") ?: ""
            val event = try { rest.getDoc("events/$eventId") } catch (e: Exception) { null }
            RegistrationDto(
                registrationId = r.docId(), eventId = eventId,
                eventTitle = event?.s("title") ?: "Unknown event",
                eventDateTime = event?.l("dateTime") ?: 0L,
                groupId = r.s("groupId"), status = r.s("status") ?: "confirmed"
            )
        }
    }

    override suspend fun updateSettings(userId: String, body: SettingsUpdateRequest) {
        if (userId != uid()) httpError(403)
        val f = mutableListOf<Pair<String, JsonObject>>()
        body.language?.let { f += "language" to str(it) }
        body.eventRemindersEnabled?.let { f += "eventRemindersEnabled" to bool(it) }
        body.chatMessagesEnabled?.let { f += "chatMessagesEnabled" to bool(it) }
        if (f.isEmpty()) return
        rest.patchDoc("users/$userId", f.map { it.first }, doc(*f.toTypedArray()))
    }

    /**
     * Makes sure users/{uid} exists (role = volunteer by default) and returns the
     * role stored there. An admin promotes someone to "organiser" in the Firebase
     * console; firestore.rules stop users editing their own role.
     */
    suspend fun loadOrCreateRole(): String {
        val user = FirebaseAuth.getInstance().currentUser ?: return "volunteer"
        return try {
            rest.getDoc("users/${user.uid}").s("role") ?: "volunteer"
        } catch (e: HttpException) {
            if (e.code() != 404) throw e
            rest.patchDoc("users/${user.uid}", listOf("displayName", "email", "role"), doc(
                "displayName" to str(user.displayName ?: ""), "email" to str(user.email ?: ""),
                "role" to str("volunteer")
            ))
            Log.i(tag, "Created users/${user.uid} profile")
            "volunteer"
        }
    }
}
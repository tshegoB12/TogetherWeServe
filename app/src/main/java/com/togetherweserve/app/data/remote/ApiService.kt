package com.togetherweserve.app.data.remote

import com.togetherweserve.app.data.remote.models.*
import retrofit2.http.*

/**
 * Custom REST API implemented over the Cloud Firestore REST API (see FirestoreApi).
 * The Android client never talks to Firestore/FCM directly - every call
 * here goes through this HTTPS layer, which verifies the Firebase ID
 * token supplied in the Authorization header before touching the database.
 */
interface ApiService {

    @GET("events")
    suspend fun getEvents(
        @Query("cause") cause: String? = null,
        @Query("q") query: String? = null
    ): List<EventDto>

    @GET("events/{id}")
    suspend fun getEvent(@Path("id") eventId: String): EventDto

    @POST("events")
    suspend fun createEvent(@Body body: CreateEventRequest): CreateEventResponse

    @POST("events/{id}/register")
    suspend fun registerForEvent(
        @Path("id") eventId: String,
        @Body body: RegisterRequest
    ): RegisterResponse

    @POST("events/{id}/group")
    suspend fun createGroup(
        @Path("id") eventId: String,
        @Body body: CreateGroupRequest
    ): CreateGroupResponse

    @GET("users/{id}/schedule")
    suspend fun getSchedule(@Path("id") userId: String): List<RegistrationDto>

    @PATCH("users/{id}/settings")
    suspend fun updateSettings(
        @Path("id") userId: String,
        @Body body: SettingsUpdateRequest
    )
}
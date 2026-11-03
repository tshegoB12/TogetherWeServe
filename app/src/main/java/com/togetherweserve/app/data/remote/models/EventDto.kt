package com.togetherweserve.app.data.remote.models

data class EventDto(
    val eventId: String,
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

data class CreateEventRequest(
    val title: String,
    val cause: String,
    val description: String,
    val dateTime: Long,
    val location: String,
    val slotsAvailable: Int
)

data class CreateEventResponse(val eventId: String)

data class RegisterRequest(
    val mode: String, // "individual" or "group"
    val groupCode: String? = null
)

data class RegisterResponse(val registrationId: String)

data class CreateGroupRequest(val groupName: String)

data class CreateGroupResponse(val groupCode: String, val groupId: String)

data class RegistrationDto(
    val registrationId: String,
    val eventId: String,
    val eventTitle: String,
    val eventDateTime: Long,
    val groupId: String?,
    val status: String
)

data class SettingsUpdateRequest(
    val language: String? = null,
    val eventRemindersEnabled: Boolean? = null,
    val chatMessagesEnabled: Boolean? = null
)

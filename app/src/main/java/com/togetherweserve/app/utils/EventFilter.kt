package com.togetherweserve.app.utils

import com.togetherweserve.app.data.local.EventEntity

object EventFilter {
    fun filter(events: List<EventEntity>, query: String, selectedCauses: List<String>): List<EventEntity> {
        val q = query.trim().lowercase()
        return events.filter { event ->
            val matchesQuery = q.isBlank() || event.title.lowercase().contains(q) ||
                    event.cause.lowercase().contains(q) || event.location.lowercase().contains(q)
            val matchesCause = selectedCauses.isEmpty() || selectedCauses.contains(event.cause)
            matchesQuery && matchesCause
        }
    }
}

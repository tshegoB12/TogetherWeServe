package com.togetherweserve.app.utils

/** Volunteer Impact dashboard maths (kept pure so it is unit tested). */
object ImpactCalculator {
    /** Assumed service hours credited per confirmed event. */
    const val HOURS_PER_EVENT = 2

    fun confirmedCount(statuses: List<String>): Int = statuses.count { it == "confirmed" }

    fun estimatedHours(confirmedEvents: Int): Int = confirmedEvents.coerceAtLeast(0) * HOURS_PER_EVENT
}

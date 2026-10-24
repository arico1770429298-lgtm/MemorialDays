package com.example.memorialdays.domain

import java.time.LocalDate

/**
 * Display ordering shared by the home list and the widgets: today and upcoming
 * days first (nearest first), then past days (most recent first).
 */
data class EventSections(
    val upcoming: List<MemorialEvent>,
    val past: List<MemorialEvent>
) {
    companion object {
        fun of(events: List<MemorialEvent>, today: LocalDate): EventSections {
            val upcoming = events
                .filter { !it.date.isBefore(today) }
                .sortedWith(compareBy<MemorialEvent>({ it.date }, { it.id }))
            val past = events
                .filter { it.date.isBefore(today) }
                .sortedWith(compareByDescending<MemorialEvent> { it.date }.thenBy { it.id })
            return EventSections(upcoming = upcoming, past = past)
        }
    }
}

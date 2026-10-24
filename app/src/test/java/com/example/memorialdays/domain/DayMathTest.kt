package com.example.memorialdays.domain

import com.example.memorialdays.data.EventEntity
import com.example.memorialdays.data.toDomain
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DayMathTest {

    @Test
    fun futureDayCountsForward() {
        val today = LocalDate.of(2026, 10, 23)
        val result = DayMath.count(LocalDate.of(2026, 11, 2), today)
        assertEquals(DayStatus.FUTURE, result.status)
        assertEquals(10L, result.days)
        assertEquals(10L, result.magnitude)
    }

    @Test
    fun sameDayIsToday() {
        val today = LocalDate.of(2026, 10, 23)
        val result = DayMath.count(today, today)
        assertEquals(DayStatus.TODAY, result.status)
        assertEquals(0L, result.days)
        assertEquals(0L, result.magnitude)
    }

    @Test
    fun pastDayCountsBackward() {
        val today = LocalDate.of(2026, 10, 23)
        val result = DayMath.count(LocalDate.of(2026, 10, 18), today)
        assertEquals(DayStatus.PAST, result.status)
        assertEquals(-5L, result.days)
        assertEquals(5L, result.magnitude)
    }

    @Test
    fun leapYearDayIsHandledByJavaTime() {
        val today = LocalDate.of(2024, 2, 28)
        val result = DayMath.count(LocalDate.of(2024, 3, 1), today)
        assertEquals(DayStatus.FUTURE, result.status)
        assertEquals(2L, result.days)
    }

    @Test
    fun yearBoundaryIsHandled() {
        val result = DayMath.count(
            target = LocalDate.of(2027, 1, 1),
            today = LocalDate.of(2026, 12, 31)
        )
        assertEquals(1L, result.days)
        assertEquals(DayStatus.FUTURE, result.status)
    }

    @Test
    fun sectionsPutUpcomingFirstAndPastMostRecentFirst() {
        val today = LocalDate.of(2026, 10, 23)
        val events = listOf(
            event(1, "far future", LocalDate.of(2027, 1, 1)),
            event(2, "today", today),
            event(3, "near future", LocalDate.of(2026, 10, 30)),
            event(4, "recent past", LocalDate.of(2026, 10, 20)),
            event(5, "old past", LocalDate.of(2020, 1, 1))
        )

        val sections = EventSections.of(events, today)

        assertEquals(listOf("today", "near future", "far future"), sections.upcoming.map { it.title })
        assertEquals(listOf("recent past", "old past"), sections.past.map { it.title })
    }

    @Test
    fun malformedStoredDateIsIgnoredInsteadOfCrashing() {
        val event = EventEntity(
            id = 9,
            title = "broken",
            targetDate = "not-a-date"
        )
        assertNull(event.toDomain())
    }

    private fun event(id: Long, title: String, date: LocalDate) = MemorialEvent(
        id = id,
        title = title,
        date = date,
        note = null
    )
}

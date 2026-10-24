package com.example.memorialdays.domain

import com.nlf.calendar.Lunar
import com.nlf.calendar.LunarYear
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class EventDateResolverTest {

    @Test
    fun lunarNewYearRollsToNextOccurrence() {
        val cny2025 = lunarSolar(2025, 1, 1)
        val event = MemorialEvent(
            id = 1L,
            title = "春节",
            date = cny2025,
            note = null,
            isYearly = true,
            isLunar = true
        )

        assertEquals(
            cny2025,
            EventDateResolver.resolve(event, LocalDate.of(2025, 1, 1)).date
        )
        assertEquals(
            LocalDate.of(2026, 2, 17),
            EventDateResolver.resolve(event, LocalDate.of(2025, 1, 30)).date
        )
    }

    @Test
    fun missingLeapMonthFallsBackToOrdinaryMonth() {
        val leapStart = lunarSolar(2023, -2, 1)
        val event = MemorialEvent(
            id = 2L,
            title = "闰二月",
            date = leapStart,
            note = null,
            isYearly = true,
            isLunar = true
        )

        val resolved = EventDateResolver.resolve(event, LocalDate.of(2025, 1, 1))
        assertEquals(lunarSolar(2025, 2, 1), resolved.date)
    }

    @Test
    fun newYearsEveUsesTheRealLastDayOfTheMonth() {
        val lastDay = LunarYear.fromYear(2025).getMonth(12)?.dayCount ?: 29
        val eve = lunarSolar(2025, 12, lastDay)
        val event = MemorialEvent(
            id = 3L,
            title = "除夕",
            date = eve,
            note = null,
            isYearly = true,
            isLunar = true
        )

        val resolved = EventDateResolver.resolve(event, LocalDate.of(2026, 1, 1))
        assertEquals(eve, resolved.date)
    }

    @Test
    fun solarYearlyEventRollsForward() {
        val event = MemorialEvent(
            id = 4L,
            title = "纪念日",
            date = LocalDate.of(2020, 10, 23),
            note = null,
            isYearly = true
        )

        assertEquals(
            LocalDate.of(2026, 10, 23),
            EventDateResolver.resolve(event, LocalDate.of(2026, 10, 1)).date
        )
    }

    @Test
    fun birthdayExampleCountsElapsedAndNext() {
        val birth = LocalDate.of(1990, 1, 1)
        val today = LocalDate.of(2026, 10, 23)
        assertEquals(13444L, ChronoUnit.DAYS.between(birth, today))

        val event = MemorialEvent(
            id = 5L,
            title = "示例事件",
            date = birth,
            note = null,
            isYearly = true,
            eventType = EventType.BIRTHDAY
        )
        val next = EventDateResolver.resolve(event, today).date
        assertEquals(LocalDate.of(2027, 1, 1), next)
        assertEquals(70L, ChronoUnit.DAYS.between(today, next))
    }

    private fun lunarSolar(year: Int, month: Int, day: Int): LocalDate {
        val solar = Lunar.fromYmd(year, month, day).solar
        return LocalDate.of(solar.year, solar.month, solar.day)
    }
}

package com.example.memorialdays.domain

import com.nlf.calendar.Lunar
import com.nlf.calendar.LunarYear
import java.time.LocalDate
import java.time.MonthDay
import java.time.ZoneId
import java.util.Date

data class EventOccurrence(
    val date: LocalDate,
    val lunarMonthLabel: String,
    val lunarDayLabel: String,
    val lunarIsLeap: Boolean
)

/**
 * Resolves the next occurrence of an event.
 *
 * 6tail lunar 1.7.4 does not expose LunarMonth.getDay(int); the documented
 * conversion is Lunar.fromYmd(year, month, day).solar. Negative months mean a
 * leap month. If the requested leap month does not exist in a year we fall back
 * to the ordinary month; if the requested day does not exist (lunar month of 29
 * days) we clamp to the last day, which also handles the New Year's Eve edge.
 */
object EventDateResolver {

    fun resolve(event: MemorialEvent, today: LocalDate): EventOccurrence {
        val resolvedDate = when {
            event.isLunar -> resolveLunar(event.date, today)
            event.isYearly -> resolveSolarYearly(event.date, today)
            else -> event.date
        }
        return occurrenceOf(resolvedDate)
    }

    fun occurrenceOf(date: LocalDate): EventOccurrence {
        val lunar = lunarOf(date)
        return EventOccurrence(
            date = date,
            lunarMonthLabel = lunar.monthInChinese,
            lunarDayLabel = lunar.dayInChinese,
            lunarIsLeap = lunar.month < 0
        )
    }

    private fun resolveSolarYearly(original: LocalDate, today: LocalDate): LocalDate {
        val monthDay = MonthDay.from(original)
        val thisYear = monthDay.atYear(today.year)
        return if (thisYear.isBefore(today)) monthDay.atYear(today.year + 1) else thisYear
    }

    private fun resolveLunar(original: LocalDate, today: LocalDate): LocalDate {
        val base = lunarOf(original)
        val baseMonth = base.month
        val baseDay = base.day
        val currentLunarYear = lunarOf(today).year
        val candidates = (currentLunarYear..currentLunarYear + 2)
            .mapNotNull { year -> resolveLunarDate(year, baseMonth, baseDay) }
        return candidates.firstOrNull { !it.isBefore(today) }
            ?: candidates.lastOrNull()
            ?: original
    }

    private fun resolveLunarDate(lunarYear: Int, month: Int, day: Int): LocalDate? {
        return runCatching {
            val year = LunarYear.fromYear(lunarYear)
            val lunarMonth = year.getMonth(month)
                ?: if (month < 0) year.getMonth(-month) else null
                ?: return@runCatching null
            val safeDay = day.coerceIn(1, lunarMonth.dayCount)
            val solar = Lunar.fromYmd(lunarYear, lunarMonth.month, safeDay).solar
            LocalDate.of(solar.year, solar.month, solar.day)
        }.getOrNull()
    }

    private fun lunarOf(date: LocalDate): Lunar {
        val instant = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
        return Lunar.fromDate(Date.from(instant))
    }
}

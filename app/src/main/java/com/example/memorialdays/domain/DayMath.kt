package com.example.memorialdays.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class DayStatus {
    PAST,
    TODAY,
    FUTURE
}

data class DayCount(
    val status: DayStatus,
    val days: Long
) {
    val magnitude: Long
        get() = if (days < 0L) -days else days
}

/**
 * All day arithmetic goes through java.time. No millisecond subtraction is used
 * anywhere in the app, which keeps the result correct across DST and leap years.
 */
object DayMath {
    fun count(target: LocalDate, today: LocalDate = LocalDate.now()): DayCount {
        val days = ChronoUnit.DAYS.between(today, target)
        val status = when {
            days > 0L -> DayStatus.FUTURE
            days < 0L -> DayStatus.PAST
            else -> DayStatus.TODAY
        }
        return DayCount(status = status, days = days)
    }
}

package com.example.memorialdays.domain

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

object DisplayFormat {
    const val COMPACT_DATE_PATTERN = "yyyy.MM.dd"

    fun mediumDateFormatter(locale: Locale): DateTimeFormatter =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)

    fun mediumDate(date: LocalDate, locale: Locale): String =
        mediumDateFormatter(locale).format(date)

    fun patternDate(date: LocalDate, pattern: String, locale: Locale): String =
        DateTimeFormatter.ofPattern(pattern, locale).format(date)

    fun compactDate(date: LocalDate, locale: Locale): String =
        patternDate(date, COMPACT_DATE_PATTERN, locale)

    fun weekday(date: LocalDate, locale: Locale): String =
        date.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
}

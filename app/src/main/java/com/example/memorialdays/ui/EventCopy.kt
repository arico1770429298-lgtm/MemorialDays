package com.example.memorialdays.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.memorialdays.R
import com.example.memorialdays.domain.DayStatus
import com.example.memorialdays.domain.EventType

@StringRes
fun EventType.labelRes(): Int = when (this) {
    EventType.GENERAL -> R.string.type_general
    EventType.BIRTHDAY -> R.string.type_birthday
    EventType.MEMORIAL -> R.string.type_memorial
    EventType.ANNIVERSARY -> R.string.type_anniversary
}

@StringRes
fun statusLabelRes(status: DayStatus): Int = when (status) {
    DayStatus.PAST -> R.string.status_past
    DayStatus.TODAY -> R.string.status_today
    DayStatus.FUTURE -> R.string.status_remaining
}

@Composable
fun EventUi.baseLunarLabel(): String = stringResource(
    if (baseLunarIsLeap) R.string.lunar_date_leap_format else R.string.lunar_date_format,
    baseLunarMonthLabel,
    baseLunarDayLabel
)

/** Full date line for list cards: 公历 1990.01.01 · 农历腊月初八 */
@Composable
fun EventUi.baseDateInfoFull(): String = stringResource(
    R.string.date_info_full_format,
    baseSolarLabel,
    baseLunarLabel()
)

/** Compact date line for the hero card: 1990.01.01 · 农历腊月初八 */
@Composable
fun EventUi.baseDateInfoCompact(): String = stringResource(
    R.string.date_info_compact_format,
    baseSolarLabel,
    baseLunarLabel()
)

/** Short left-column status: 已出生 / 已相伴 / 还有 / 已过 / 就是今天 */
@Composable
fun EventUi.listStatus(): String {
    if (isElapsedType && elapsedDays != null) {
        return stringResource(
            if (event.eventType == EventType.BIRTHDAY) {
                R.string.status_born
            } else {
                R.string.status_together
            }
        )
    }
    return stringResource(statusLabelRes(count.status))
}

/** Bottom status line of the hero card. */
@Composable
fun EventUi.heroStatus(): String {
    val elapsed = elapsedDays
    if (isElapsedType && elapsed != null) {
        return stringResource(
            if (event.eventType == EventType.BIRTHDAY) {
                R.string.born_days_format
            } else {
                R.string.together_days_format
            },
            elapsed
        )
    }
    return when (count.status) {
        DayStatus.TODAY -> stringResource(R.string.status_today)
        DayStatus.FUTURE -> stringResource(R.string.days_remaining_format, count.days)
        DayStatus.PAST -> stringResource(R.string.days_past_format, count.magnitude)
    }
}

/** Extra bottom line for recurring birthdays / anniversaries. */
@Composable
fun EventUi.nextOccurrenceCopy(): String? {
    if (!isElapsedType || elapsedDays == null) return null
    if (!(event.isYearly || event.isLunar)) return null
    return when (event.eventType) {
        EventType.BIRTHDAY -> if (count.status == DayStatus.TODAY) {
            stringResource(R.string.birthday_today)
        } else {
            stringResource(R.string.next_birthday_format, count.days)
        }

        EventType.ANNIVERSARY -> if (count.status == DayStatus.TODAY) {
            stringResource(R.string.anniversary_today)
        } else {
            stringResource(R.string.next_anniversary_format, count.days)
        }

        else -> null
    }
}

@Composable
fun EventUi.eventContentDescription(): String =
    if (count.status == DayStatus.TODAY && !isElapsedType) {
        stringResource(R.string.content_desc_event_today, event.title)
    } else {
        stringResource(
            R.string.content_desc_event_days,
            event.title,
            listStatus(),
            displayDays
        )
    }

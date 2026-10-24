package com.example.memorialdays.widget

import android.content.Context
import com.example.memorialdays.R
import com.example.memorialdays.data.MemorialDatabase
import com.example.memorialdays.data.toDomain
import com.example.memorialdays.domain.DayMath
import com.example.memorialdays.domain.DayStatus
import com.example.memorialdays.domain.DisplayFormat
import com.example.memorialdays.domain.EventDateResolver
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class WidgetItem(
    val title: String,
    val status: DayStatus,
    val magnitude: Long,
    val dateLabel: String
)

internal data class WidgetSnapshot(
    val items: List<WidgetItem>,
    val failed: Boolean
)

internal object WidgetData {
    private const val MAX_ITEMS = 4

    suspend fun load(context: Context): WidgetSnapshot = withContext(Dispatchers.IO) {
        runCatching {
            val appContext = context.applicationContext
            val rows = MemorialDatabase.get(appContext).eventDao().getAll()
            val today = LocalDate.now()
            val locale = appContext.resources.configuration.locales[0]
            val resolved = rows.mapNotNull { it.toDomain() }.map { event ->
                val occurrence = EventDateResolver.resolve(event, today)
                val count = DayMath.count(occurrence.date, today)
                Triple(event, occurrence, count)
            }
            val upcoming = resolved
                .filter { it.third.status != DayStatus.PAST }
                .sortedBy { it.second.date }
            val past = resolved
                .filter { it.third.status == DayStatus.PAST }
                .sortedByDescending { it.second.date }
            (upcoming + past)
                .take(MAX_ITEMS)
                .map { (event, occurrence, count) ->
                    WidgetItem(
                        title = event.title,
                        status = count.status,
                        magnitude = count.magnitude,
                        dateLabel = DisplayFormat.compactDate(occurrence.date, locale)
                    )
                }
        }.fold(
            onSuccess = { WidgetSnapshot(items = it, failed = false) },
            onFailure = { WidgetSnapshot(items = emptyList(), failed = true) }
        )
    }
}

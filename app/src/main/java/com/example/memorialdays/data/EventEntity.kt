package com.example.memorialdays.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.memorialdays.domain.EventType
import com.example.memorialdays.domain.MemorialEvent
import java.time.LocalDate

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "targetDate")
    val targetDate: String,
    @ColumnInfo(name = "note")
    val note: String? = null,
    @ColumnInfo(name = "createdAt")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "isYearly", defaultValue = "0")
    val isYearly: Boolean = false,
    @ColumnInfo(name = "eventType", defaultValue = "'GENERAL'")
    val eventType: String = EventType.GENERAL.storage,
    @ColumnInfo(name = "isLunar", defaultValue = "0")
    val isLunar: Boolean = false,
    @ColumnInfo(name = "sortOrder", defaultValue = "0")
    val sortOrder: Int = 0,
    @ColumnInfo(name = "isPinned", defaultValue = "0")
    val isPinned: Boolean = false,
    @ColumnInfo(name = "color", defaultValue = "0")
    val color: Int = 0
)

/**
 * Malformed rows are skipped instead of crashing the list. Dates are stored as
 * ISO-8601 text so lexicographic ordering is also chronological ordering.
 */
fun EventEntity.toDomain(): MemorialEvent? {
    val date = runCatching { LocalDate.parse(targetDate) }.getOrNull() ?: return null
    val safeTitle = title.trim()
    if (safeTitle.isEmpty()) return null
    return MemorialEvent(
        id = id,
        title = safeTitle,
        date = date,
        note = note?.trim()?.takeIf { it.isNotEmpty() },
        isYearly = isYearly,
        eventType = EventType.fromStorage(eventType),
        isLunar = isLunar,
        sortOrder = sortOrder,
        isPinned = isPinned,
        color = color
    )
}

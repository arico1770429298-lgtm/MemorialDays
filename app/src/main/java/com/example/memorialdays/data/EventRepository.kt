package com.example.memorialdays.data

import androidx.room.withTransaction
import com.example.memorialdays.domain.EventType
import com.example.memorialdays.domain.MemorialEvent
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class EventRepository(
    private val database: MemorialDatabase,
    private val dao: EventDao
) {

    val events: Flow<List<MemorialEvent>> = dao.observeAll()
        .map { rows -> rows.mapNotNull { it.toDomain() } }

    suspend fun save(
        id: Long,
        title: String,
        date: LocalDate,
        note: String?,
        isYearly: Boolean = false,
        isLunar: Boolean = false,
        color: Int = 0
    ): Result<Unit> = runCatching {
        val safeTitle = title.trim()
        require(safeTitle.isNotEmpty()) { "empty title" }
        require(safeTitle.length <= MAX_TITLE_LENGTH) { "title too long" }
        require(date.year in MIN_YEAR..MAX_YEAR) { "date out of range" }
        val safeNote = note
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.take(MAX_NOTE_LENGTH)
        val isoDate = date.toString()
        val type = EventType.detect(safeTitle)

        if (id == 0L) {
            val maxOrder = dao.getMaxSortOrder()
            val nextOrder = if (maxOrder != null && maxOrder > 0) maxOrder + 1 else 0
            dao.insert(
                EventEntity(
                    title = safeTitle,
                    targetDate = isoDate,
                    note = safeNote,
                    isYearly = isYearly || isLunar,
                    eventType = type.storage,
                    isLunar = isLunar,
                    sortOrder = nextOrder,
                    color = color
                )
            )
        } else {
            val existing = dao.getById(id) ?: error("event not found")
            val updated = dao.update(
                existing.copy(
                    title = safeTitle,
                    targetDate = isoDate,
                    note = safeNote,
                    isYearly = isYearly || isLunar,
                    eventType = type.storage,
                    isLunar = isLunar,
                    color = color
                )
            )
            check(updated > 0) { "event not found" }
        }
    }

    suspend fun delete(event: MemorialEvent): Result<Unit> = runCatching {
        val deleted = dao.deleteById(event.id)
        check(deleted > 0) { "event not found" }
    }

    suspend fun setPinned(id: Long, pinned: Boolean): Result<Unit> = runCatching {
        val updated = dao.setPinned(id, pinned)
        check(updated > 0) { "event not found" }
    }

    suspend fun reorder(orderedIds: List<Long>): Result<Unit> = runCatching {
        database.withTransaction {
            val rows = dao.getAll().associateBy { it.id }
            val updates = orderedIds.mapIndexedNotNull { index, id ->
                rows[id]?.copy(sortOrder = index)
            }
            if (updates.isNotEmpty()) {
                dao.updateAll(updates)
            }
        }
    }

    suspend fun allRows(): List<EventEntity> = dao.getAll()

    suspend fun replaceAll(rows: List<EventEntity>): Result<Int> = runCatching {
        database.withTransaction {
            dao.deleteAll()
            dao.insertAll(rows)
        }
        rows.size
    }

    companion object {
        const val MAX_TITLE_LENGTH = 40
        const val MAX_NOTE_LENGTH = 80
        const val MIN_YEAR = 1900
        const val MAX_YEAR = 2100
    }
}

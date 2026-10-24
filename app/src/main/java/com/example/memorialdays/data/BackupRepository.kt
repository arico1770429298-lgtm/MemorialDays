package com.example.memorialdays.data

import android.content.Context
import android.net.Uri
import com.example.memorialdays.domain.EventType
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class EmptyBackupException : IllegalStateException("empty backup")

/**
 * SAF-based JSON backup. No storage permission is requested: the caller hands us
 * a content:// URI from CreateDocument/OpenDocument and we stream through
 * ContentResolver.
 */
class BackupRepository(
    context: Context,
    private val repository: EventRepository
) {
    private val appContext = context.applicationContext

    suspend fun export(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val rows = repository.allRows()
            val events = JSONArray()
            rows.forEach { row ->
                events.put(
                    JSONObject().apply {
                        put("title", row.title)
                        put("targetDate", row.targetDate)
                        put("note", row.note.orEmpty())
                        put("isYearly", row.isYearly)
                        put("eventType", row.eventType)
                        put("isLunar", row.isLunar)
                        put("sortOrder", row.sortOrder)
                        put("isPinned", row.isPinned)
                        put("color", row.color)
                    }
                )
            }
            val root = JSONObject().apply {
                put("format", FORMAT)
                put("version", VERSION)
                put("events", events)
            }
            val stream = appContext.contentResolver.openOutputStream(uri)
                ?: error("openOutputStream failed")
            stream.bufferedWriter(Charsets.UTF_8).use { writer ->
                writer.write(root.toString(2))
            }
            rows.size
        }
    }

    suspend fun import(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val stream = appContext.contentResolver.openInputStream(uri)
                ?: error("openInputStream failed")
            val text = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val root = JSONObject(text)
            check(root.optString("format") == FORMAT) { "wrong backup format" }
            val array = root.optJSONArray("events") ?: JSONArray()
            val rows = buildList {
                for (index in 0 until array.length()) {
                    val obj = array.optJSONObject(index) ?: continue
                    val title = obj.optString("title")
                        .trim()
                        .take(EventRepository.MAX_TITLE_LENGTH)
                    if (title.isEmpty()) continue
                    val date = runCatching {
                        LocalDate.parse(obj.optString("targetDate"))
                    }.getOrNull() ?: continue
                    if (date.year !in EventRepository.MIN_YEAR..EventRepository.MAX_YEAR) continue
                    val note = obj.optString("note")
                        .trim()
                        .takeIf { it.isNotEmpty() }
                        ?.take(EventRepository.MAX_NOTE_LENGTH)
                    add(
                        EventEntity(
                            title = title,
                            targetDate = date.toString(),
                            note = note,
                            isYearly = obj.optBoolean("isYearly", false),
                            eventType = EventType.fromStorage(
                                obj.optString("eventType")
                            ).storage,
                            isLunar = obj.optBoolean("isLunar", false),
                            sortOrder = obj.optInt("sortOrder", 0).coerceAtLeast(0),
                            isPinned = obj.optBoolean("isPinned", false),
                            color = obj.optInt("color", 0)
                        )
                    )
                }
            }
            if (rows.isEmpty()) throw EmptyBackupException()
            repository.replaceAll(rows).getOrThrow()
            rows.size
        }
    }

    companion object {
        private const val FORMAT = "memorialdays.backup"
        private const val VERSION = 1
    }
}

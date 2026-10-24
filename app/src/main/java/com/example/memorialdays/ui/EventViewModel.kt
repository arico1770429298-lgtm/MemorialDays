package com.example.memorialdays.ui

import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.memorialdays.MemorialApplication
import com.example.memorialdays.R
import com.example.memorialdays.data.BackupRepository
import com.example.memorialdays.data.EmptyBackupException
import com.example.memorialdays.data.EventRepository
import com.example.memorialdays.domain.DayCount
import com.example.memorialdays.domain.DayMath
import com.example.memorialdays.domain.DayStatus
import com.example.memorialdays.domain.DisplayFormat
import com.example.memorialdays.domain.EventDateResolver
import com.example.memorialdays.domain.EventType
import com.example.memorialdays.domain.MemorialEvent
import com.example.memorialdays.share.PosterData
import com.example.memorialdays.share.PosterRenderer
import com.example.memorialdays.share.PosterShare
import com.example.memorialdays.share.PosterStyle
import com.example.memorialdays.widget.WidgetUpdater
import java.io.File
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class EventUi(
    val event: MemorialEvent,
    val resolvedDate: LocalDate,
    val count: DayCount,
    val baseSolarLabel: String,
    val baseLunarMonthLabel: String,
    val baseLunarDayLabel: String,
    val baseLunarIsLeap: Boolean,
    val elapsedDays: Long?,
    val displayDays: Long,
    val isElapsedType: Boolean,
    @param:StringRes val typeLabelRes: Int,
    @param:StringRes val subtitleRes: Int,
    val subtitleArgs: List<Any> = emptyList()
)

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Ready(
        val items: List<EventUi>,
        val today: LocalDate
    ) : HomeUiState {
        val isEmpty: Boolean
            get() = items.isEmpty()
    }

    data class Error(@param:StringRes val messageRes: Int) : HomeUiState
}

data class UiMessage(
    @param:StringRes val messageRes: Int,
    val args: List<Any> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
class EventViewModel(
    private val repository: EventRepository,
    private val backupRepository: BackupRepository,
    private val posterRenderer: PosterRenderer,
    private val widgetUpdater: WidgetUpdater
) : ViewModel() {

    private val reload = MutableStateFlow(0L)
    private val saveThrottle = ActionThrottle()
    private val deleteThrottle = ActionThrottle()
    private val backupThrottle = ActionThrottle()

    /**
     * Emits today, then sleeps until the next local midnight. It is only
     * collected while the UI is subscribed, so nothing wakes up in background.
     */
    private val todayFlow: Flow<LocalDate> = flow {
        while (true) {
            val today = LocalDate.now()
            emit(today)
            val now = LocalDateTime.now()
            val nextMidnight = today.plusDays(1).atStartOfDay()
            val waitMillis = Duration.between(now, nextMidnight)
                .toMillis()
                .coerceAtLeast(1_000L)
            delay(waitMillis)
        }
    }

    val homeState: StateFlow<HomeUiState> = reload
        .flatMapLatest {
            combine(repository.events, todayFlow) { events, today ->
                withContext(Dispatchers.Default) {
                    buildState(events, today)
                }
            }.catch {
                emit(HomeUiState.Error(R.string.error_load_failed))
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = HomeUiState.Loading
        )

    private val _messages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 4)
    val messages = _messages.asSharedFlow()

    private fun buildState(events: List<MemorialEvent>, today: LocalDate): HomeUiState {
        val locale = Locale.getDefault()
        val resolved = events.map { event ->
            val occurrence = EventDateResolver.resolve(event, today)
            val base = EventDateResolver.occurrenceOf(event.date)
            val count = DayMath.count(occurrence.date, today)
            val isElapsedType = event.eventType == EventType.BIRTHDAY ||
                event.eventType == EventType.ANNIVERSARY
            val elapsedDays = if (isElapsedType && !event.date.isAfter(today)) {
                ChronoUnit.DAYS.between(event.date, today)
            } else {
                null
            }
            val (subtitleRes, subtitleArgs) = subtitleFor(
                event = event,
                isElapsedType = isElapsedType,
                elapsedDays = elapsedDays,
                count = count
            )
            EventUi(
                event = event,
                resolvedDate = occurrence.date,
                count = count,
                baseSolarLabel = DisplayFormat.compactDate(event.date, locale),
                baseLunarMonthLabel = base.lunarMonthLabel,
                baseLunarDayLabel = base.lunarDayLabel,
                baseLunarIsLeap = base.lunarIsLeap,
                elapsedDays = elapsedDays,
                displayDays = elapsedDays ?: count.magnitude,
                isElapsedType = isElapsedType,
                typeLabelRes = event.eventType.labelRes(),
                subtitleRes = subtitleRes,
                subtitleArgs = subtitleArgs
            )
        }
        val ordered = resolved.sortedWith(
            compareBy<EventUi> { !it.event.isPinned }
                .thenBy { it.event.sortOrder }
                .thenBy { it.resolvedDate }
        )
        return HomeUiState.Ready(items = ordered, today = today)
    }

    /**
     * Hero-card subtitle, fully resolved here so composables only render.
     *
     * Recurring birthdays / anniversaries (isYearly or isLunar) show the
     * countdown (距下次生日/纪念日还有 N 天) while the large numeral stays the
     * elapsed total, so the two numbers can never repeat. Everything else —
     * plain countdowns and one-shot birthdays/anniversaries — keeps the
     * 还有 N 天 / 已过 N 天 copy.
     */
    private fun subtitleFor(
        event: MemorialEvent,
        isElapsedType: Boolean,
        elapsedDays: Long?,
        count: DayCount
    ): Pair<Int, List<Any>> {
        if (isElapsedType && elapsedDays != null && (event.isYearly || event.isLunar)) {
            return when (event.eventType) {
                EventType.BIRTHDAY ->
                    if (count.status == DayStatus.TODAY) {
                        R.string.birthday_today to emptyList<Any>()
                    } else {
                        R.string.next_birthday_format to listOf<Any>(count.days)
                    }

                else ->
                    if (count.status == DayStatus.TODAY) {
                        R.string.anniversary_today to emptyList<Any>()
                    } else {
                        R.string.next_anniversary_format to listOf<Any>(count.days)
                    }
            }
        }
        return when (count.status) {
            DayStatus.TODAY -> R.string.status_today to emptyList<Any>()
            DayStatus.FUTURE -> 0 to emptyList<Any>()
            DayStatus.PAST -> 0 to emptyList<Any>()
        }
    }

    suspend fun save(
        id: Long,
        title: String,
        date: LocalDate,
        note: String?,
        isYearly: Boolean = false,
        isLunar: Boolean = false,
        color: Int = 0
    ): Result<Unit> {
        if (!saveThrottle.allow()) {
            return Result.success(Unit)
        }
        val result = withContext(Dispatchers.IO) {
            repository.save(
                id = id,
                title = title,
                date = date,
                note = note,
                isYearly = isYearly,
                isLunar = isLunar,
                color = color
            )
        }
        if (result.isSuccess) {
            widgetUpdater.requestUpdate()
        }
        return result
    }

    fun delete(event: MemorialEvent) {
        viewModelScope.launch {
            if (!deleteThrottle.allow()) return@launch
            val result = withContext(Dispatchers.IO) {
                repository.delete(event)
            }
            if (result.isSuccess) {
                widgetUpdater.requestUpdate()
                _messages.emit(
                    UiMessage(
                        messageRes = R.string.snackbar_deleted,
                        args = listOf(event.title)
                    )
                )
            } else {
                _messages.emit(UiMessage(R.string.error_delete_failed))
            }
        }
    }

    fun reorder(orderedIds: List<Long>) {
        if (orderedIds.isEmpty()) return
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                repository.reorder(orderedIds)
            }
            if (result.isSuccess) {
                widgetUpdater.requestUpdate()
            } else {
                _messages.emit(UiMessage(R.string.error_save_failed))
            }
        }
    }

    /**
     * Long-press (without dragging) toggles the pin flag. Pinned events sort
     * ahead of everything else, so the pinned event becomes the hero card.
     */
    fun togglePin(event: MemorialEvent) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                repository.setPinned(event.id, !event.isPinned)
            }
            if (result.isSuccess) {
                widgetUpdater.requestUpdate()
            } else {
                _messages.emit(UiMessage(R.string.error_save_failed))
            }
        }
    }

    fun retry() {
        reload.update { it + 1L }
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            if (!backupThrottle.allow()) return@launch
            val result = backupRepository.export(uri)
            if (result.isSuccess) {
                _messages.emit(
                    UiMessage(
                        messageRes = R.string.backup_exported,
                        args = listOf(result.getOrNull() ?: 0)
                    )
                )
            } else {
                _messages.emit(UiMessage(R.string.backup_failed))
            }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            if (!backupThrottle.allow()) return@launch
            val result = backupRepository.import(uri)
            if (result.isSuccess) {
                widgetUpdater.requestUpdate()
                _messages.emit(
                    UiMessage(
                        messageRes = R.string.backup_imported,
                        args = listOf(result.getOrNull() ?: 0)
                    )
                )
            } else if (result.exceptionOrNull() is EmptyBackupException) {
                _messages.emit(UiMessage(R.string.backup_empty))
            } else {
                _messages.emit(UiMessage(R.string.backup_failed))
            }
        }
    }

    suspend fun createPoster(data: PosterData, style: PosterStyle): Result<PosterShare> =
        withContext(Dispatchers.IO) {
            posterRenderer.render(data, style)
        }

    fun cleanupPoster(file: File) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                posterRenderer.cleanup(file)
            }
        }
    }

    companion object {
        fun factory(application: MemorialApplication): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                EventViewModel(
                    repository = application.container.repository,
                    backupRepository = application.container.backupRepository,
                    posterRenderer = application.container.posterRenderer,
                    widgetUpdater = application.container.widgetUpdater
                )
            }
        }
    }
}

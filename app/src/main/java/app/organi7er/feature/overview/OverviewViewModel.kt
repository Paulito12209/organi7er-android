package app.organi7er.feature.overview

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.organi7er.core.data.EventRepository
import app.organi7er.core.data.NoteRepository
import app.organi7er.core.data.NotificationRepository
import app.organi7er.core.data.SettingsRepository
import app.organi7er.core.data.TaskRepository
import app.organi7er.core.data.TimeProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class OverviewViewModel @Inject constructor(
    events: EventRepository,
    tasks: TaskRepository,
    notes: NoteRepository,
    notifications: NotificationRepository,
    settings: SettingsRepository,
    private val time: TimeProvider,
    private val saved: SavedStateHandle,
) : ViewModel() {
    /** Gewählter Tag; null = heute (folgt dem Datumswechsel um Mitternacht). */
    private val selected = MutableStateFlow(saved.get<Long>(KEY_DAY)?.let(LocalDate::ofEpochDay))

    private val data = combine(events.events, tasks.tasks, notes.notes, notifications.entries, settings.settings) { e, t, n, l, s ->
        Data(e, t, n, l, s.weekStart)
    }

    val state: StateFlow<OverviewState> = combine(data, selected, time.minuteTicks) { d, sel, now ->
        OverviewBuilder.build(now, sel ?: now.toLocalDate(), d.weekStart, d.events, d.tasks, d.notes, d.log)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OverviewState())

    fun selectDay(date: LocalDate) = set(date)

    /** Andere KW: heute, wenn sie in der Woche liegt, sonst deren erster Tag. */
    fun selectWeek(week: WeekUi) {
        val today = time.now().toLocalDate()
        set(if (!today.isBefore(week.start) && today.isBefore(week.start.plusDays(7))) today else week.start)
    }

    private fun set(date: LocalDate) {
        val today = time.now().toLocalDate()
        selected.value = date.takeIf { it != today }
        saved[KEY_DAY] = selected.value?.toEpochDay()
    }

    private data class Data(
        val events: List<app.organi7er.core.model.Event>,
        val tasks: List<app.organi7er.core.model.Task>,
        val notes: List<app.organi7er.core.model.NoteWithImages>,
        val log: List<app.organi7er.core.model.NotificationEntry>,
        val weekStart: Int,
    )

    private companion object {
        const val KEY_DAY = "day"
    }
}

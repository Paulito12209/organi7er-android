package app.organi7er.feature.overview

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.organi7er.core.data.BriefingKind
import app.organi7er.core.data.BriefingRepository
import app.organi7er.core.data.EventRepository
import app.organi7er.core.data.NoteRepository
import app.organi7er.core.data.TaskRepository
import app.organi7er.core.data.TimeProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

@HiltViewModel
class OverviewViewModel @Inject constructor(
    events: EventRepository,
    tasks: TaskRepository,
    notes: NoteRepository,
    briefing: BriefingRepository,
    time: TimeProvider,
    private val saved: SavedStateHandle,
) : ViewModel() {
    private val _selection = MutableStateFlow(
        OverviewSelection(saved.get<Int>(KEY_OFFSET) ?: 0, saved.get<Boolean>(KEY_WEEK) ?: false),
    )
    val selection: StateFlow<OverviewSelection> = _selection

    private val data = combine(events.events, events.calendars, tasks.tasks, notes.notes, briefing.briefings) { e, c, t, n, b ->
        Data(e, c, t, n, b)
    }

    val state: StateFlow<OverviewState> = combine(data, _selection, time.minuteTicks) { d, sel, now ->
        OverviewBuilder.build(now, sel, d.events, d.calendars, d.tasks, d.notes, d.tiles)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OverviewState())

    fun prev() = set(_selection.value.prev())
    fun next() = set(_selection.value.next())

    fun selectTile(kind: BriefingKind) = set(
        when (kind) {
            BriefingKind.TODAY -> OverviewSelection(0)
            BriefingKind.TOMORROW -> OverviewSelection(1)
            BriefingKind.WEEK -> OverviewSelection(0, week = true)
        },
    )

    private fun set(s: OverviewSelection) {
        _selection.update { s }
        saved[KEY_OFFSET] = s.dayOffset
        saved[KEY_WEEK] = s.week
    }

    private data class Data(
        val events: List<app.organi7er.core.model.Event>,
        val calendars: List<app.organi7er.core.model.CalendarEntity>,
        val tasks: List<app.organi7er.core.model.Task>,
        val notes: List<app.organi7er.core.model.NoteWithImages>,
        val tiles: List<app.organi7er.core.data.Briefing>,
    )

    private companion object {
        const val KEY_OFFSET = "offset"
        const val KEY_WEEK = "week"
    }
}

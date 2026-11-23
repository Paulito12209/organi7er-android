package app.organi7er.feature.tasks

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.organi7er.core.data.TaskRepository
import app.organi7er.core.data.TimeProvider
import app.organi7er.core.designsystem.OrganiTheme
import app.organi7er.core.designsystem.Spacing
import app.organi7er.core.designsystem.components.DraftRow
import app.organi7er.core.designsystem.components.Hint
import app.organi7er.core.designsystem.components.Ms
import app.organi7er.core.designsystem.components.Overline
import app.organi7er.core.designsystem.components.SegmentedControl
import app.organi7er.core.designsystem.components.SegmentedList
import app.organi7er.core.designsystem.components.SoftIconButton
import app.organi7er.core.designsystem.components.TextLink
import app.organi7er.core.model.Dates
import app.organi7er.core.model.Dates.toLocal
import app.organi7er.core.model.Priority
import app.organi7er.core.model.Status
import app.organi7er.core.model.Task
import app.organi7er.feature.common.TaskList
import app.organi7er.feature.common.TaskUi
import app.organi7er.feature.common.UiMapper
import app.organi7er.feature.editor.EditorRequest
import app.organi7er.feature.editor.LocalAppActions
import app.organi7er.navigation.ContentBottomPadding
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

enum class TaskSort(val label: String) { DUE("Fälligkeit"), PRIORITY("Priorität") }

@Immutable
data class TaskGroup(val title: String, val items: List<TaskUi>)

@Immutable
data class TasksState(
    val segment: Int = 1,
    val sort: TaskSort = TaskSort.DUE,
    val drafts: List<Task> = emptyList(),
    val groups: List<TaskGroup> = emptyList(),
    val recentDone: List<TaskUi> = emptyList(),
    val olderDone: List<TaskUi> = emptyList(),
    val showOlder: Boolean = false,
    val now: LocalDateTime = LocalDateTime.now(),
)

/** Reine Gruppierung: Überfällig, Heute, Diese Woche, Später. */
object TaskLogic {
    fun groups(tasks: List<Task>, now: LocalDateTime, sort: TaskSort): List<TaskGroup> {
        val today = now.toLocalDate()
        val weekEnd = today.plusDays(6)
        val active = tasks.filter { it.status == Status.ACTIVE && it.dueAt != null }
        val comparator: Comparator<Task> = when (sort) {
            TaskSort.DUE -> compareBy<Task>({ it.dueAt }, { if (it.priority == Priority.HIGH) 0 else 1 })
            TaskSort.PRIORITY -> compareBy<Task>({ if (it.priority == Priority.HIGH) 0 else 1 }, { it.dueAt })
        }
        fun bucket(name: String, filter: (LocalDate) -> Boolean, showDate: Boolean) =
            active.filter { filter(it.dueAt!!.toLocalDate()) }.sortedWith(comparator)
                .takeIf { it.isNotEmpty() }
                ?.let { list -> TaskGroup(name, list.map { UiMapper.task(it, today, showDate, now) }) }
        return listOfNotNull(
            bucket("Überfällig", { it.isBefore(today) }, true),
            bucket("Heute", { it == today }, false),
            bucket("Diese Woche", { it.isAfter(today) && !it.isAfter(weekEnd) }, true),
            bucket("Später", { it.isAfter(weekEnd) }, true),
        )
    }

    /** Erledigte der letzten 30 Tage und ältere (einklappbar). */
    fun done(tasks: List<Task>, now: LocalDateTime): Pair<List<TaskUi>, List<TaskUi>> {
        val today = now.toLocalDate()
        val limit = now.minusDays(30)
        val done = tasks.filter { it.status == Status.DONE }.sortedByDescending { it.doneAt ?: it.updatedAt }
        val (recent, older) = done.partition { (it.doneAt ?: it.updatedAt).toLocal().isAfter(limit) }
        return recent.map { UiMapper.task(it, today, true, now) } to older.map { UiMapper.task(it, today, true, now) }
    }
}

@HiltViewModel
class TasksViewModel @Inject constructor(
    repo: TaskRepository,
    time: TimeProvider,
    private val saved: SavedStateHandle,
) : ViewModel() {
    private val segment = MutableStateFlow(saved.get<Int>("segment") ?: 1)
    private val sort = MutableStateFlow(saved.get<String>("sort")?.let(TaskSort::valueOf) ?: TaskSort.DUE)
    private val showOlder = MutableStateFlow(false)

    val state: StateFlow<TasksState> = combine(repo.tasks, time.minuteTicks, segment, sort, showOlder) { tasks, now, seg, s, older ->
        val (recent, old) = TaskLogic.done(tasks, now)
        TasksState(
            segment = seg,
            sort = s,
            drafts = tasks.filter { it.status == Status.DRAFT }.sortedByDescending { it.createdAt },
            groups = TaskLogic.groups(tasks, now, s),
            recentDone = recent,
            olderDone = old,
            showOlder = older,
            now = now,
        )
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TasksState())

    fun segment(i: Int) {
        segment.value = i
        saved["segment"] = i
    }

    fun toggleSort() {
        val next = if (sort.value == TaskSort.DUE) TaskSort.PRIORITY else TaskSort.DUE
        sort.value = next
        saved["sort"] = next.name
    }

    fun toggleOlder() {
        showOlder.value = !showOlder.value
    }
}

@Composable
fun TasksScreen(vm: TasksViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val actions = LocalAppActions.current
    val c = OrganiTheme.colors

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = Spacing.screen, end = Spacing.screen, bottom = ContentBottomPadding),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item(key = "head") {
            Row(Modifier.statusBarsPadding().padding(top = 8.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Aufgaben", Modifier.weight(1f).semantics { heading() }, style = OrganiTheme.type.pageTitle, color = c.text)
                if (state.segment == 1) {
                    SoftIconButton(Ms.swapVert, "Sortieren nach ${if (state.sort == TaskSort.DUE) TaskSort.PRIORITY.label else TaskSort.DUE.label}", vm::toggleSort)
                }
            }
        }
        item(key = "segment") { SegmentedControl(listOf("Entwürfe", "Aktiv", "Erledigt"), state.segment, vm::segment) }

        when (state.segment) {
            0 -> {
                item(key = "hint") { Hint("Gedanken ohne Datum. Erst wenn du einen Entwurf planst, wird er aktiv.") }
                if (state.drafts.isEmpty()) {
                    item(key = "empty") { Hint("Keine Entwürfe. Aufgaben ohne Datum landen hier.") }
                } else {
                    item(key = "drafts") {
                        SegmentedList {
                            state.drafts.forEachIndexed { i, t ->
                                DraftRow(
                                    title = t.title,
                                    meta = Dates.ago(t.createdAt, state.now),
                                    onPlan = { actions.openEditor(EditorRequest.PlanTask(t.id)) },
                                    onClick = { actions.openEditor(EditorRequest.EditTask(t.id)) },
                                    index = i,
                                    count = state.drafts.size,
                                )
                            }
                        }
                    }
                }
            }
            1 -> {
                if (state.groups.isEmpty()) {
                    item(key = "empty") { Hint("Alles erledigt. Neue Aufgaben legst du unten rechts an.") }
                }
                items(state.groups, key = { it.title }) { group ->
                    Column(Modifier.animateContentSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Overline(group.title, color = if (group.title == "Überfällig") c.dangerText else c.textSoft)
                        TaskList(group.items)
                    }
                }
            }
            else -> {
                if (state.recentDone.isEmpty() && state.olderDone.isEmpty()) {
                    item(key = "empty") { Hint("Noch nichts erledigt.") }
                }
                if (state.recentDone.isNotEmpty()) {
                    item(key = "recent") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Overline("Letzte 30 Tage")
                            TaskList(state.recentDone)
                        }
                    }
                }
                if (state.olderDone.isNotEmpty()) {
                    item(key = "older") {
                        Column(Modifier.animateContentSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            TextLink(if (state.showOlder) "Ältere ausblenden" else "Ältere anzeigen (${state.olderDone.size})", vm::toggleOlder)
                            if (state.showOlder) TaskList(state.olderDone)
                        }
                    }
                }
            }
        }
    }
}

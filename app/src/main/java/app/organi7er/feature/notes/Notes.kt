package app.organi7er.feature.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.organi7er.core.data.NoteRepository
import app.organi7er.core.data.TimeProvider
import app.organi7er.core.designsystem.OrganiTheme
import app.organi7er.core.designsystem.Spacing
import app.organi7er.core.designsystem.components.DateDivider
import app.organi7er.core.designsystem.components.DraftRow
import app.organi7er.core.designsystem.components.FilterTile
import app.organi7er.core.designsystem.components.Hint
import app.organi7er.core.designsystem.components.Ms
import app.organi7er.core.designsystem.components.SearchField
import app.organi7er.core.designsystem.components.SegmentedControl
import app.organi7er.core.designsystem.components.SegmentedList
import app.organi7er.core.designsystem.components.SoftIconButton
import app.organi7er.core.model.Dates
import app.organi7er.core.model.NoteWithImages
import app.organi7er.core.model.Status
import app.organi7er.feature.common.AppHeader
import app.organi7er.feature.common.NoteFeedCard
import app.organi7er.feature.common.UiMapper
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

enum class NoteFilter(val label: String) { ALL("Alle"), IMAGES("Mit Bild"), TEXT("Nur Text") }

/** Ein Eintrag im Feed: Tagestrenner oder Notiz – flach für LazyColumn. */
@Immutable
sealed interface FeedItem {
    val key: String

    data class Day(val label: String) : FeedItem {
        override val key = "d-$label"
    }

    data class Item(val note: NoteWithImages) : FeedItem {
        override val key = "n-${note.note.id}"
    }
}

@Immutable
data class NotesState(
    val segment: Int = 1,
    val query: String = "",
    val filter: NoteFilter = NoteFilter.ALL,
    val newestFirst: Boolean = true,
    val counts: Map<NoteFilter, Int> = emptyMap(),
    val feed: List<FeedItem> = emptyList(),
    val drafts: List<NoteWithImages> = emptyList(),
    val today: LocalDate = LocalDate.now(),
    val now: LocalDateTime = LocalDateTime.now(),
)

object NotesLogic {
    fun matches(n: NoteWithImages, query: String): Boolean {
        if (query.isBlank()) return true
        val q = query.trim().lowercase()
        return n.note.title.lowercase().contains(q) || n.note.body.lowercase().contains(q)
    }

    fun feed(notes: List<NoteWithImages>, today: LocalDate, newestFirst: Boolean): List<FeedItem> =
        UiMapper.noteDays(notes, today, newestFirst).flatMap { day -> listOf(FeedItem.Day(day.label)) + day.notes.map { FeedItem.Item(it) } }
}

@HiltViewModel
class NotesViewModel @Inject constructor(
    repo: NoteRepository,
    time: TimeProvider,
    private val saved: SavedStateHandle,
) : ViewModel() {
    private val segment = MutableStateFlow(saved.get<Int>("segment") ?: 1)
    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(NoteFilter.ALL)
    private val newestFirst = MutableStateFlow(true)

    private val ui = combine(segment, query, filter, newestFirst) { s, q, f, n -> UiInput(s, q, f, n) }

    val state: StateFlow<NotesState> = combine(repo.notes, time.minuteTicks, ui) { notes, now, input ->
        val today = now.toLocalDate()
        val status = if (input.segment == 2) Status.DONE else Status.ACTIVE
        val searched = notes.filter { it.note.status == status && NotesLogic.matches(it, input.query) }
        val counts = mapOf(
            NoteFilter.ALL to searched.size,
            NoteFilter.IMAGES to searched.count { it.images.isNotEmpty() },
            NoteFilter.TEXT to searched.count { it.images.isEmpty() },
        )
        val filtered = searched.filter {
            when (input.filter) {
                NoteFilter.ALL -> true
                NoteFilter.IMAGES -> it.images.isNotEmpty()
                NoteFilter.TEXT -> it.images.isEmpty()
            }
        }
        NotesState(
            segment = input.segment,
            query = input.query,
            filter = input.filter,
            newestFirst = input.newestFirst,
            counts = counts,
            feed = NotesLogic.feed(if (input.segment == 2) searched else filtered, today, input.newestFirst),
            drafts = notes.filter { it.note.status == Status.DRAFT }.sortedByDescending { it.note.createdAt },
            today = today,
            now = now,
        )
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotesState())

    fun segment(i: Int) {
        segment.value = i
        saved["segment"] = i
    }

    fun query(q: String) {
        query.value = q
    }

    fun filter(f: NoteFilter) {
        filter.value = f
    }

    fun toggleSort() {
        newestFirst.value = !newestFirst.value
    }

    private data class UiInput(val segment: Int, val query: String, val filter: NoteFilter, val newestFirst: Boolean)
}

@Composable
fun NotesScreen(vm: NotesViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val actions = LocalAppActions.current
    val c = OrganiTheme.colors

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = Spacing.page, end = Spacing.page, bottom = ContentBottomPadding),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "head") {
            AppHeader("Notizen") {
                if (state.segment != 0) {
                    SoftIconButton(Ms.swapVert, if (state.newestFirst) "Älteste zuerst" else "Neueste zuerst", vm::toggleSort)
                }
            }
        }
        item(key = "segment") { SegmentedControl(listOf("Entwürfe", "Aktiv", "Erledigt"), state.segment, vm::segment, Modifier.padding(bottom = 4.dp)) }

        when (state.segment) {
            0 -> {
                item(key = "hint") { Hint("Schnelle Gedanken, noch ohne Titel oder Ordnung.") }
                if (state.drafts.isEmpty()) {
                    item(key = "empty") { Hint("Keine Entwürfe.") }
                } else {
                    item(key = "drafts") {
                        SegmentedList {
                            state.drafts.forEachIndexed { i, n ->
                                DraftRow(
                                    title = n.note.title.ifBlank { "„${n.note.body.take(60)}${if (n.note.body.length > 60) " …" else ""}“" },
                                    meta = Dates.ago(n.note.createdAt, state.now),
                                    onPlan = null,
                                    onClick = { actions.openNote(n.note.id) },
                                    index = i,
                                    count = state.drafts.size,
                                )
                            }
                        }
                    }
                }
            }
            else -> {
                item(key = "search") { SearchField(state.query, vm::query, "Notiz oder Bild suchen") }
                if (state.segment == 1) {
                    item(key = "filters") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NoteFilter.entries.forEach { f ->
                                FilterTile(f.label, state.counts[f] ?: 0, state.filter == f, { vm.filter(f) }, Modifier.weight(1f))
                            }
                        }
                    }
                } else {
                    item(key = "done-hint") { Hint("Abgeschlossene Notizen bleiben durchsuchbar.") }
                }
                if (state.feed.isEmpty()) {
                    item(key = "empty") { Hint(if (state.query.isNotBlank()) "Nichts gefunden." else "Noch keine Notizen.") }
                }
                items(state.feed, key = { it.key }, contentType = { it::class }) { item ->
                    when (item) {
                        is FeedItem.Day -> DateDivider(item.label, Modifier.padding(top = 4.dp))
                        is FeedItem.Item -> NoteFeedCard(item.note, state.today)
                    }
                }
            }
        }
    }
}

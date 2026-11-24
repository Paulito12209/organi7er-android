package app.organi7er.feature.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.organi7er.core.data.EventRepository
import app.organi7er.core.data.NoteRepository
import app.organi7er.core.data.TaskRepository
import app.organi7er.core.designsystem.OrganiShapes
import app.organi7er.core.designsystem.OrganiTheme
import app.organi7er.core.designsystem.Spacing
import app.organi7er.core.designsystem.components.EntryType
import app.organi7er.core.designsystem.components.GlassCircleButton
import app.organi7er.core.designsystem.components.Ms
import app.organi7er.core.designsystem.components.MsIcon
import app.organi7er.core.designsystem.components.SegmentedList
import app.organi7er.core.designsystem.components.aura
import app.organi7er.core.designsystem.components.liquidGlass
import app.organi7er.core.designsystem.components.icon
import app.organi7er.core.designsystem.components.segmentItem
import app.organi7er.core.model.Dates
import app.organi7er.core.model.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

/** Ein Treffer der Suche. */
data class SearchHit(val type: EntryType, val id: Long, val title: String, val meta: String)

/** Treffer, nach Art gruppiert (Termine, Aufgaben, Notizen). */
data class SearchState(val query: String = "", val groups: List<Pair<String, List<SearchHit>>> = emptyList())

@HiltViewModel
class SearchViewModel @Inject constructor(
    events: EventRepository,
    tasks: TaskRepository,
    notes: NoteRepository,
) : ViewModel() {
    val query = MutableStateFlow("")

    val state: StateFlow<SearchState> = combine(query, events.events, tasks.tasks, notes.notes) { q, ev, ts, ns ->
        val needle = q.trim()
        if (needle.isEmpty()) return@combine SearchState(q)
        fun String.hit() = contains(needle, ignoreCase = true)
        val eventHits = ev
            .filter { it.title.hit() || it.location.hit() || it.note.hit() }
            .sortedByDescending { it.start }
            .map { e ->
                val meta = e.start?.let { s -> "${Dates.shortDate(s.toLocalDate())}${if (e.allDay) "" else " · ${Dates.time(s)}"}" } ?: "Entwurf"
                SearchHit(EntryType.Event, e.id, e.title.ifBlank { "Ohne Titel" }, listOf(meta, e.location).filter { it.isNotBlank() }.joinToString(" · "))
            }
        val taskHits = ts
            .filter { it.title.hit() || it.note.hit() || it.listName.hit() }
            .sortedBy { it.status == Status.DONE }
            .map { t ->
                val meta = when {
                    t.status == Status.DONE -> "Erledigt"
                    t.status == Status.DRAFT -> "Entwurf"
                    t.dueAt != null -> "Fällig ${Dates.shortDate(t.dueAt.toLocalDate())}"
                    else -> "Aktiv"
                }
                SearchHit(EntryType.Task, t.id, t.title.ifBlank { "Ohne Titel" }, meta)
            }
        val noteHits = ns
            .map { it.note }
            .filter { it.title.hit() || it.body.hit() }
            .sortedByDescending { it.updatedAt }
            .map { n ->
                val title = n.title.ifBlank { n.body.lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty() }.ifBlank { "Ohne Titel" }
                SearchHit(EntryType.Note, n.id, title, snippet(n.body, needle))
            }
        SearchState(
            q,
            listOf("Termine" to eventHits, "Aufgaben" to taskHits, "Notizen" to noteHits).filter { it.second.isNotEmpty() },
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchState())

    /** Ausschnitt rund um den Treffer, damit man sieht, warum die Notiz gefunden wurde. */
    private fun snippet(body: String, needle: String): String {
        val flat = body.replace('\n', ' ').trim()
        val i = flat.indexOf(needle, ignoreCase = true)
        if (i < 0) return flat.take(80)
        val from = (i - 24).coerceAtLeast(0)
        return (if (from > 0) "…" else "") + flat.substring(from).take(80)
    }
}

/**
 * Suche über alles: oben das Suchfeld über die volle Breite (Logo und Menüknopf sind weg),
 * darunter die Treffer nach Art gruppiert. Tippen öffnet den Eintrag und schließt die Suche.
 */
@Composable
fun SearchScreen(vm: SearchViewModel, onOpen: (SearchHit) -> Unit, onClose: () -> Unit) {
    val c = OrganiTheme.colors
    val state by vm.state.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    BackHandler(onBack = onClose)
    LaunchedEffect(Unit) { focus.requestFocus() }

    Column(Modifier.fillMaxSize().aura().statusBarsPadding().imePadding()) {
        Row(
            Modifier.fillMaxWidth().padding(start = Spacing.page, end = Spacing.page, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Wie die Pille im Kopf, nur über die ganze Breite.
            Row(
                Modifier
                    .weight(1f)
                    .height(48.dp)
                    .liquidGlass(OrganiShapes.pill)
                    .padding(start = 16.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MsIcon(Ms.search, null, tint = c.textMuted, size = 20.dp)
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text("Suchen", style = OrganiTheme.type.body, color = c.textSoft, maxLines = 1)
                    BasicTextField(
                        value = query,
                        onValueChange = { vm.query.value = it },
                        singleLine = true,
                        textStyle = OrganiTheme.type.body.copy(color = c.text),
                        cursorBrush = SolidColor(c.text),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                        modifier = Modifier.fillMaxWidth().focusRequester(focus).semantics { contentDescription = "Suchen" },
                    )
                }
                if (query.isNotEmpty()) {
                    Box(
                        Modifier.size(36.dp).clip(OrganiShapes.circle).clickable(onClickLabel = "Suche leeren") { vm.query.value = "" },
                        contentAlignment = Alignment.Center,
                    ) { MsIcon(Ms.close, "Suche leeren", tint = c.textMuted, size = 18.dp) }
                }
            }
            GlassCircleButton(Ms.close, "Suche schließen", onClose, size = 42.dp)
        }

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = Spacing.page, end = Spacing.page, top = 22.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            when {
                query.isBlank() -> item(key = "hint") { Message("Termine, Aufgaben und Notizen durchsuchen.") }
                state.groups.isEmpty() && state.query == query -> item(key = "empty") { Message("Nichts gefunden für „${query.trim()}“.") }
                else -> state.groups.forEach { (title, hits) ->
                    item(key = title) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                "$title · ${hits.size}",
                                Modifier.padding(horizontal = 4.dp).semantics { heading() },
                                style = OrganiTheme.type.label.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                                color = c.textMuted,
                            )
                            SegmentedList {
                                hits.forEachIndexed { i, h -> HitRow(h, i, hits.size) { onOpen(h) } }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Message(text: String) {
    Text(text, Modifier.padding(horizontal = 4.dp), style = OrganiTheme.type.rowMeta, color = OrganiTheme.colors.textSoft)
}

@Composable
private fun HitRow(h: SearchHit, index: Int, count: Int, onClick: () -> Unit) {
    val c = OrganiTheme.colors
    Row(
        Modifier
            .segmentItem(index, count, minHeight = 60.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Box(Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(c.surface2), contentAlignment = Alignment.Center) {
            MsIcon(h.type.icon, null, tint = c.text, size = 18.dp)
        }
        Column(Modifier.weight(1f)) {
            Text(h.title, style = OrganiTheme.type.rowTitle, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (h.meta.isNotBlank()) Text(h.meta, style = OrganiTheme.type.rowMeta, color = c.textSoft, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

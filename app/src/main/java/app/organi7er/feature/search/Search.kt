package app.organi7er.feature.search

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
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
import app.organi7er.core.designsystem.components.icon
import app.organi7er.core.designsystem.components.liquidGlass
import app.organi7er.core.designsystem.components.rememberDictation
import app.organi7er.core.designsystem.components.segmentItem
import app.organi7er.core.model.Dates
import app.organi7er.core.model.Dates.toInstantLocal
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

/** Ein Treffer der Suche. [rank]: 0 = Titel beginnt mit dem Begriff, 1 = Titel enthält ihn, 2 = nur im Text. */
data class SearchHit(
    val type: EntryType,
    val id: Long,
    val title: String,
    val meta: String,
    val rank: Int = 2,
    val time: Long = 0L,
)

/** Wonach die Treffer sortiert werden. */
enum class SearchSort(val label: String) { RELEVANCE("Relevanz"), NEWEST("Neueste"), OLDEST("Älteste"), ALPHA("A–Z") }

/** Treffer, nach Art gruppiert (Termine, Aufgaben, Notizen). */
data class SearchState(
    val query: String = "",
    val type: EntryType? = null,
    val sort: SearchSort = SearchSort.RELEVANCE,
    val groups: List<Pair<String, List<SearchHit>>> = emptyList(),
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    events: EventRepository,
    tasks: TaskRepository,
    notes: NoteRepository,
) : ViewModel() {
    val query = MutableStateFlow("")
    /** null = alle Arten. */
    val type = MutableStateFlow<EntryType?>(null)
    val sort = MutableStateFlow(SearchSort.RELEVANCE)

    private val filters = combine(query, type, sort) { q, t, s -> Triple(q, t, s) }

    val state: StateFlow<SearchState> = combine(filters, events.events, tasks.tasks, notes.notes) { (q, only, sort), ev, ts, ns ->
        val needle = q.trim()
        if (needle.isEmpty()) return@combine SearchState(q, only, sort)
        fun String.hit() = contains(needle, ignoreCase = true)
        fun rank(title: String) = when {
            title.startsWith(needle, ignoreCase = true) -> 0
            title.hit() -> 1
            else -> 2
        }
        val eventHits = if (only != null && only != EntryType.Event) emptyList() else ev
            .filter { it.title.hit() || it.location.hit() || it.note.hit() }
            .map { e ->
                val meta = e.start?.let { s -> "${Dates.shortDate(s.toLocalDate())}${if (e.allDay) "" else " · ${Dates.time(s)}"}" } ?: "Entwurf"
                SearchHit(
                    EntryType.Event, e.id, e.title.ifBlank { "Ohne Titel" },
                    listOf(meta, e.location).filter { it.isNotBlank() }.joinToString(" · "),
                    rank(e.title), (e.start?.toInstantLocal() ?: e.createdAt).toEpochMilli(),
                )
            }
        val taskHits = if (only != null && only != EntryType.Task) emptyList() else ts
            .filter { it.title.hit() || it.note.hit() || it.listName.hit() }
            .map { t ->
                val meta = when {
                    t.status == Status.DONE -> "Erledigt"
                    t.status == Status.DRAFT -> "Entwurf"
                    t.dueAt != null -> "Fällig ${Dates.shortDate(t.dueAt.toLocalDate())}"
                    else -> "Aktiv"
                }
                // Erledigte rutschen bei „Relevanz“ nach hinten.
                SearchHit(EntryType.Task, t.id, t.title.ifBlank { "Ohne Titel" }, meta, rank(t.title) + if (t.status == Status.DONE) 3 else 0, t.updatedAt.toEpochMilli())
            }
        val noteHits = if (only != null && only != EntryType.Note) emptyList() else ns
            .map { it.note }
            .filter { it.title.hit() || it.body.hit() }
            .map { n ->
                val title = n.title.ifBlank { n.body.lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty() }.ifBlank { "Ohne Titel" }
                SearchHit(EntryType.Note, n.id, title, snippet(n.body, needle), rank(title), n.updatedAt.toEpochMilli())
            }
        fun List<SearchHit>.ordered() = when (sort) {
            SearchSort.RELEVANCE -> sortedWith(compareBy<SearchHit> { it.rank }.thenByDescending { it.time })
            SearchSort.NEWEST -> sortedByDescending { it.time }
            SearchSort.OLDEST -> sortedBy { it.time }
            SearchSort.ALPHA -> sortedBy { it.title.lowercase() }
        }
        SearchState(
            q, only, sort,
            listOf("Termine" to eventHits.ordered(), "Aufgaben" to taskHits.ordered(), "Notizen" to noteHits.ordered()).filter { it.second.isNotEmpty() },
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
 * Suche über alles. Oben Titel und Regler-Knopf, in der Mitte die Treffer, unten direkt über der
 * Tastatur die Eingabezeile wie im Dock (Lupe · Text · Welle) mit dem Schließen-Knopf daneben.
 * Der Regler blendet über der Eingabezeile Filter (Art) und Sortierung ein.
 *
 * [progress] läuft beim Öffnen von 0 bis 1: Kopf und Treffer blenden ein, die Eingabezeile gleitet von unten herein.
 */
@Composable
fun SearchScreen(
    vm: SearchViewModel,
    active: Boolean,
    progress: () -> Float,
    onOpen: (SearchHit) -> Unit,
    onClose: () -> Unit,
    onMessage: (String) -> Unit,
) {
    val c = OrganiTheme.colors
    val state by vm.state.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val type by vm.type.collectAsStateWithLifecycle()
    val sort by vm.sort.collectAsStateWithLifecycle()
    var tuning by rememberSaveable { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    // Zurück schließt zuerst die Filterleiste, dann die Suche.
    BackHandler(active) { if (tuning) tuning = false else onClose() }
    // Erst wenn die Suche ganz offen ist, kommen Cursor und Tastatur.
    LaunchedEffect(active) { if (active) focus.requestFocus() }

    val dictate = rememberDictation(
        onResult = { spoken -> vm.query.value = (vm.query.value.trimEnd() + " " + spoken).trim() },
        onUnavailable = { onMessage("Diktat ist auf diesem Gerät nicht verfügbar") },
    )
    // Wer in den Treffern scrollt, will lesen: Tastatur weg.
    val hideOnScroll = remember(keyboard) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && kotlin.math.abs(available.y) > 6f) keyboard?.hide()
                return Offset.Zero
            }
        }
    }
    val filtered = type != null || sort != SearchSort.RELEVANCE

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        // Kopf: blendet mit leichtem Absinken ein.
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = Spacing.page, end = Spacing.page, top = 8.dp)
                .graphicsLayer {
                    val a = ((progress() - 0.45f) / 0.55f).coerceIn(0f, 1f)
                    alpha = a
                    translationY = (1f - a) * -12.dp.toPx()
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Suche",
                Modifier.weight(1f).semantics { heading() },
                style = OrganiTheme.type.pageTitle.copy(fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.03).em),
                color = c.text,
            )
            // Regler: Filter und Sortierung über der Eingabezeile ein- und ausblenden. Offen oder Filter aktiv: gefüllt.
            GlassCircleButton(
                Ms.tune,
                if (tuning) "Filter ausblenden" else "Filtern und sortieren",
                { tuning = !tuning },
                size = 42.dp,
                filled = tuning || filtered,
            )
        }

        LazyColumn(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .nestedScroll(hideOnScroll)
                .graphicsLayer {
                    val a = ((progress() - 0.55f) / 0.45f).coerceIn(0f, 1f)
                    alpha = a
                    translationY = (1f - a) * 24.dp.toPx()
                },
            contentPadding = PaddingValues(start = Spacing.page, end = Spacing.page, top = 18.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            when {
                query.isBlank() -> item(key = "hint") {
                    Message(
                        when (type) {
                            EntryType.Event -> "Termine durchsuchen."
                            EntryType.Task -> "Aufgaben durchsuchen."
                            EntryType.Note -> "Notizen durchsuchen."
                            null -> "Termine, Aufgaben und Notizen durchsuchen."
                        },
                    )
                }
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

        // Unten: Filterleiste (optional) und Eingabezeile, immer direkt über Tastatur bzw. Navigationsleiste.
        Column(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
                .padding(start = 12.dp, end = 12.dp, bottom = 8.dp)
                .graphicsLayer {
                    // Gleitet von unten herein, etwas früher als der Rest – die Eingabe ist das Wichtigste.
                    val a = ((progress() - 0.3f) / 0.5f).coerceIn(0f, 1f)
                    alpha = a
                    translationY = (1f - a) * 56.dp.toPx()
                },
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AnimatedVisibility(
                tuning,
                enter = expandVertically(spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow), expandFrom = Alignment.Bottom) + fadeIn(),
                exit = shrinkVertically(spring(stiffness = Spring.StiffnessMedium), shrinkTowards = Alignment.Bottom) + fadeOut(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChipRow("Art") {
                        Chip("Alle", type == null) { vm.type.value = null }
                        Chip("Termine", type == EntryType.Event, EntryType.Event.icon) { vm.type.value = EntryType.Event }
                        Chip("Aufgaben", type == EntryType.Task, EntryType.Task.icon) { vm.type.value = EntryType.Task }
                        Chip("Notizen", type == EntryType.Note, EntryType.Note.icon) { vm.type.value = EntryType.Note }
                    }
                    ChipRow("Sortierung") {
                        SearchSort.entries.forEach { s -> Chip(s.label, sort == s) { vm.sort.value = s } }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Eingabe-Pille wie im Dock: Lupe · Text · Welle.
                Row(
                    Modifier
                        .weight(1f)
                        .height(56.dp)
                        .clip(OrganiShapes.pill)
                        .background(c.surface2),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.padding(start = 18.dp, end = 10.dp)) { MsIcon(Ms.search, null, tint = c.textMuted, size = 21.dp) }
                    BasicTextField(
                        value = query,
                        onValueChange = { vm.query.value = it },
                        singleLine = true,
                        textStyle = OrganiTheme.type.body.copy(fontSize = 17.sp, color = c.text),
                        cursorBrush = SolidColor(c.text),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                        modifier = Modifier.weight(1f).focusRequester(focus).semantics { contentDescription = "Suchen" },
                        decorationBox = { inner ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (query.isEmpty()) Text("Suchen", style = OrganiTheme.type.body.copy(fontSize = 17.sp), color = c.textSoft, maxLines = 1)
                                inner()
                            }
                        },
                    )
                    if (query.isNotEmpty()) {
                        Box(
                            Modifier.size(40.dp).clip(OrganiShapes.circle).clickable(role = Role.Button, onClickLabel = "Suche leeren") { vm.query.value = "" },
                            contentAlignment = Alignment.Center,
                        ) { MsIcon(Ms.close, "Suche leeren", tint = c.textMuted, size = 18.dp) }
                    }
                    // Wellen-Knopf: diktieren, wie im Dock.
                    Box(
                        Modifier
                            .padding(end = 6.dp)
                            .size(44.dp)
                            .clip(OrganiShapes.circle)
                            .clickable(role = Role.Button, onClickLabel = "Diktieren", onClick = dictate)
                            .semantics { contentDescription = "Diktieren" },
                        contentAlignment = Alignment.Center,
                    ) { MsIcon(Ms.graphicEq, null, tint = if (c.isDark) Color.White else c.text, size = 26.dp) }
                }
                // Schließen sitzt rechts neben der Eingabe, nicht mehr oben.
                Box(
                    Modifier
                        .size(56.dp)
                        .liquidGlass(OrganiShapes.circle)
                        .clickable(role = Role.Button, onClickLabel = "Suche schließen", onClick = onClose)
                        .semantics { contentDescription = "Suche schließen" },
                    contentAlignment = Alignment.Center,
                ) { MsIcon(Ms.close, null, tint = c.text, size = 22.dp) }
            }
        }
    }
}

@Composable
private fun ChipRow(label: String, content: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(label, Modifier.padding(horizontal = 4.dp), style = OrganiTheme.type.tileCaption, color = OrganiTheme.colors.textSoft)
        content()
    }
}

/** Filter-Pille: gewählt gefüllt (schwarz bzw. im Dunkeln weiß), sonst Liquid Glass – wie die Reiter. */
@Composable
private fun Chip(label: String, selected: Boolean, icon: Int? = null, onClick: () -> Unit) {
    val c = OrganiTheme.colors
    Row(
        Modifier
            .height(38.dp)
            .then(if (selected) Modifier.clip(OrganiShapes.pill).background(c.text) else Modifier.liquidGlass(OrganiShapes.pill))
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) MsIcon(icon, null, tint = if (selected) c.bg else c.textMuted, size = 16.dp)
        Text(
            label,
            style = OrganiTheme.type.label.copy(fontSize = 14.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium),
            color = if (selected) c.bg else c.textMuted,
            maxLines = 1,
        )
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

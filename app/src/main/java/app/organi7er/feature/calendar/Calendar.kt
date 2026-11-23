package app.organi7er.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.organi7er.core.data.EventRepository
import app.organi7er.core.data.SettingsRepository
import app.organi7er.core.data.TimeProvider
import app.organi7er.core.designsystem.OrganiShapes
import app.organi7er.core.designsystem.OrganiTheme
import app.organi7er.core.designsystem.Spacing
import app.organi7er.core.designsystem.components.CalendarEventItem
import app.organi7er.core.designsystem.components.DatePickerSheet
import app.organi7er.core.designsystem.components.DraftRow
import app.organi7er.core.designsystem.components.Hint
import app.organi7er.core.designsystem.components.Legend
import app.organi7er.core.designsystem.components.Ms
import app.organi7er.core.designsystem.components.MsIcon
import app.organi7er.core.designsystem.components.NowLine
import app.organi7er.core.designsystem.components.PillButton
import app.organi7er.core.designsystem.components.PillVariant
import app.organi7er.core.designsystem.components.SegmentedControl
import app.organi7er.core.designsystem.components.SegmentedList
import app.organi7er.core.designsystem.parseHexColor
import app.organi7er.core.model.CalendarEntity
import app.organi7er.core.model.Dates
import app.organi7er.core.model.Event
import app.organi7er.core.model.EventSubStatus
import app.organi7er.core.model.Status
import app.organi7er.feature.common.EventList
import app.organi7er.feature.common.EventUi
import app.organi7er.feature.common.UiMapper
import app.organi7er.feature.editor.EditorRequest
import app.organi7er.feature.editor.LocalAppActions
import app.organi7er.navigation.ContentBottomPadding
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

@Immutable
sealed interface DayItem {
    val key: String

    data class Entry(val ui: EventUi, val past: Boolean) : DayItem {
        override val key = "e${ui.event.id}"
    }

    data class Now(val time: String) : DayItem {
        override val key = "now"
    }
}

@Immutable
data class CalendarState(
    val today: LocalDate = LocalDate.now(),
    val selected: LocalDate = LocalDate.now(),
    val weekStart: Int = 1,
    val segment: Int = 1,
    val day: List<DayItem> = emptyList(),
    val drafts: List<Event> = emptyList(),
    val done: List<EventUi> = emptyList(),
    val calendars: List<CalendarEntity> = emptyList(),
    /** Kalenderfarben je Tag (max. drei) für die Punkte in der Wochenleiste. */
    val dots: Map<LocalDate, List<String>> = emptyMap(),
    val now: LocalDateTime = LocalDateTime.now(),
)

object CalendarLogic {
    fun day(events: List<Event>, calendars: List<CalendarEntity>, date: LocalDate, now: LocalDateTime): List<DayItem> {
        val list = events
            .filter { it.status != Status.DRAFT && it.start != null && it.occursOn(date) }
            .sortedWith(compareBy({ !it.allDay }, { it.start }))
        val items = mutableListOf<DayItem>()
        var nowPlaced = date != now.toLocalDate()
        list.forEach { e ->
            if (!nowPlaced && !e.allDay && e.start!!.isAfter(now)) {
                items += DayItem.Now(Dates.time(now))
                nowPlaced = true
            }
            items += DayItem.Entry(UiMapper.event(e, calendars), past = e.effectiveEnd!!.isBefore(now))
        }
        if (!nowPlaced && list.isNotEmpty()) items += DayItem.Now(Dates.time(now))
        return items
    }

    fun dots(events: List<Event>, calendars: List<CalendarEntity>, from: LocalDate, days: Long): Map<LocalDate, List<String>> {
        val active = events.filter { it.status != Status.DRAFT && it.start != null && it.subStatus != EventSubStatus.CANCELLED }
        return (0 until days).associate { i ->
            val d = from.plusDays(i)
            d to active.filter { it.occursOn(d) }.map { UiMapper.calendarColor(it, calendars) }.distinct().take(3)
        }
    }

    fun done(events: List<Event>, calendars: List<CalendarEntity>, now: LocalDateTime): List<EventUi> = events
        .filter { it.start != null && it.effectiveStatus(now) == Status.DONE }
        .sortedByDescending { it.start }
        .take(60)
        .map { e ->
            val d = e.start!!.toLocalDate()
            UiMapper.event(e, calendars).copy(time = Dates.weekdayShort(d), timeSub = Dates.numeric(d), meta = UiMapper.eventRangeMeta(e))
        }
}

@HiltViewModel
class CalendarViewModel @Inject constructor(
    events: EventRepository,
    settings: SettingsRepository,
    time: TimeProvider,
    private val saved: SavedStateHandle,
) : ViewModel() {
    private val selected = MutableStateFlow(saved.get<String>("date")?.let(LocalDate::parse) ?: LocalDate.now())
    private val segment = MutableStateFlow(saved.get<Int>("segment") ?: 1)

    val state: StateFlow<CalendarState> = combine(
        combine(events.events, events.calendars) { e, c -> e to c },
        settings.settings,
        time.minuteTicks,
        selected,
        segment,
    ) { (events, calendars), s, now, date, seg ->
        val weekStart = Dates.weekStart(date, s.weekStart)
        CalendarState(
            today = now.toLocalDate(),
            selected = date,
            weekStart = s.weekStart,
            segment = seg,
            day = CalendarLogic.day(events, calendars, date, now),
            drafts = events.filter { it.status == Status.DRAFT }.sortedByDescending { it.createdAt },
            done = if (seg == 2) CalendarLogic.done(events, calendars, now) else emptyList(),
            calendars = calendars,
            dots = CalendarLogic.dots(events, calendars, weekStart.minusWeeks(1), 21),
            now = now,
        )
    }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarState())

    fun select(date: LocalDate) {
        selected.value = date
        saved["date"] = date.toString()
    }

    fun segment(i: Int) {
        segment.value = i
        saved["segment"] = i
    }
}

private const val PAGES = 2001
private const val CENTER = 1000

@Composable
fun CalendarScreen(vm: CalendarViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val actions = LocalAppActions.current
    val c = OrganiTheme.colors
    var monthPicker by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = Spacing.screen, end = Spacing.screen, bottom = ContentBottomPadding),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item(key = "head") {
            Row(Modifier.statusBarsPadding().padding(top = 8.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(
                        Modifier
                            .heightIn(min = 32.dp)
                            .clip(OrganiShapes.pill)
                            .clickable(role = Role.Button, onClickLabel = "Monat wählen") { monthPicker = true },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(Dates.monthYear(state.selected).uppercase(), style = OrganiTheme.type.overline, color = c.textSoft)
                        MsIcon(Ms.expandMore, null, tint = c.textSoft, size = 16.dp)
                    }
                    Text("Termine", Modifier.semantics { heading() }, style = OrganiTheme.type.pageTitle, color = c.text)
                }
                if (state.segment == 1 && state.selected != state.today) {
                    PillButton("Heute", { vm.select(state.today) }, variant = PillVariant.Soft, small = true)
                }
            }
        }
        item(key = "segment") {
            SegmentedControl(listOf("Entwürfe", "Aktiv", "Erledigt"), state.segment, vm::segment)
        }
        when (state.segment) {
            0 -> {
                item(key = "draft-hint") { Hint("Termin-Ideen ohne feste Zeit. Mit „Planen“ bekommen sie Datum und Uhrzeit und werden aktiv.") }
                if (state.drafts.isEmpty()) {
                    item(key = "draft-empty") { Hint("Keine Entwürfe. Lege einen Termin mit „Noch ohne feste Zeit“ an.") }
                } else {
                    item(key = "drafts") {
                        SegmentedList {
                            state.drafts.forEachIndexed { i, e ->
                                DraftRow(
                                    title = e.title,
                                    meta = e.note.ifBlank { Dates.ago(e.createdAt, state.now).replaceFirstChar(Char::uppercase) + " notiert" },
                                    onPlan = { actions.openEditor(EditorRequest.PlanEvent(e.id)) },
                                    onClick = { actions.openEditor(EditorRequest.EditEvent(e.id)) },
                                    index = i,
                                    count = state.drafts.size,
                                )
                            }
                        }
                    }
                }
            }
            1 -> {
                item(key = "week") { WeekStrip(state, vm::select) }
                if (state.calendars.isNotEmpty()) {
                    item(key = "legend") { Legend(state.calendars.map { it.name to parseHexColor(it.colorHex) }) }
                }
                item(key = "day-title") {
                    Text(
                        Dates.relativeDay(state.selected, state.today).let { if (it == Dates.weekdayLong(state.selected)) Dates.longDay(state.selected) else "$it · ${Dates.dayMonth(state.selected)}" },
                        Modifier.semantics { heading() },
                        style = OrganiTheme.type.title,
                        color = c.text,
                    )
                }
                if (state.day.isEmpty()) {
                    item(key = "empty") { Hint("Keine Termine an diesem Tag.") }
                } else {
                    items(state.day, key = { it.key }, contentType = { it::class }) { item ->
                        when (item) {
                            is DayItem.Now -> NowLine(item.time)
                            is DayItem.Entry -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    item.ui.time,
                                    Modifier.width(44.dp).padding(top = 14.dp),
                                    style = OrganiTheme.type.caption,
                                    color = c.textSoft,
                                )
                                val color = remember(item.ui.colorHex) { parseHexColor(item.ui.colorHex) }
                                CalendarEventItem(
                                    color = color,
                                    title = item.ui.event.title,
                                    meta = UiMapper.eventRangeMeta(item.ui.event),
                                    tag = item.ui.tag,
                                    dimmed = item.ui.event.subStatus == EventSubStatus.CANCELLED,
                                    onClick = { actions.openEditor(EditorRequest.EditEvent(item.ui.event.id)) },
                                )
                            }
                        }
                    }
                }
            }
            else -> {
                if (state.done.isEmpty()) {
                    item(key = "done-empty") { Hint("Noch keine vergangenen Termine.") }
                } else {
                    item(key = "done") { EventList(state.done) }
                }
            }
        }
    }

    if (monthPicker) DatePickerSheet(state.selected, { monthPicker = false }, { vm.select(it) })
}

/** Wochenleiste ohne Container: Wischen wechselt die Woche. */
@Composable
private fun WeekStrip(state: CalendarState, onSelect: (LocalDate) -> Unit) {
    val baseWeek = remember(state.today, state.weekStart) { Dates.weekStart(state.today, state.weekStart) }
    val selectedWeek = Dates.weekStart(state.selected, state.weekStart)
    val targetPage = CENTER + ChronoUnit.WEEKS.between(baseWeek, selectedWeek).toInt()
    val pager = rememberPagerState(initialPage = targetPage) { PAGES }

    val latest by rememberUpdatedState(state)
    val select by rememberUpdatedState(onSelect)
    LaunchedEffect(targetPage) { if (pager.currentPage != targetPage) pager.animateScrollToPage(targetPage) }
    LaunchedEffect(pager, baseWeek) {
        snapshotFlow { pager.settledPage }.collect { page ->
            val s = latest
            val week = baseWeek.plusWeeks((page - CENTER).toLong())
            val current = Dates.weekStart(s.selected, s.weekStart)
            if (week != current) {
                // Gleicher Wochentag in der neuen Woche – enthält sie heute, dann heute.
                val offset = ChronoUnit.DAYS.between(current, s.selected)
                select(if (!s.today.isBefore(week) && s.today.isBefore(week.plusDays(7))) s.today else week.plusDays(offset))
            }
        }
    }
    HorizontalPager(pager, Modifier.fillMaxWidth(), beyondViewportPageCount = 1) { page ->
        val week = baseWeek.plusWeeks((page - CENTER).toLong())
        Row(Modifier.fillMaxWidth()) {
            (0L..6L).forEach { i ->
                val d = week.plusDays(i)
                DayCell(d, d == state.selected, d == state.today, state.dots[d].orEmpty(), Modifier.weight(1f)) { onSelect(d) }
            }
        }
    }
}

@Composable
private fun DayCell(date: LocalDate, selected: Boolean, today: Boolean, dots: List<String>, modifier: Modifier, onClick: () -> Unit) {
    val c = OrganiTheme.colors
    Column(
        modifier
            .heightIn(min = 64.dp)
            .clip(OrganiShapes.medium)
            .selectable(selected, role = Role.Tab, onClick = onClick)
            .semantics { contentDescription = Dates.longDay(date) + if (today) ", heute" else "" }
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(Dates.weekdayShort(date), style = OrganiTheme.type.caption, color = c.textSoft)
        Box(
            Modifier
                .size(36.dp)
                .clip(OrganiShapes.circle)
                .background(if (selected) c.tileSelected else androidx.compose.ui.graphics.Color.Transparent)
                .then(if (today && !selected) Modifier.border(1.5.dp, c.brandBlue, OrganiShapes.circle) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Text("${date.dayOfMonth}", style = OrganiTheme.type.label.copy(fontSize = OrganiTheme.type.body.fontSize), color = if (selected) c.onTileSelected else c.text)
        }
        Row(Modifier.height(4.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            dots.forEach { hex ->
                val color = remember(hex) { parseHexColor(hex) }
                Box(Modifier.size(4.dp).clip(OrganiShapes.circle).background(color))
            }
        }
    }
}

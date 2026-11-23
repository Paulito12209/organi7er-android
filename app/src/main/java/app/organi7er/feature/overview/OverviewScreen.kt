package app.organi7er.feature.overview

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.organi7er.core.data.BriefingIcon
import app.organi7er.core.designsystem.OrganiTheme
import app.organi7er.core.designsystem.Spacing
import app.organi7er.core.designsystem.components.BriefingTile
import app.organi7er.core.designsystem.components.CaptureButton
import app.organi7er.core.designsystem.components.EntryType
import app.organi7er.core.designsystem.components.Hint
import app.organi7er.core.designsystem.components.Ms
import app.organi7er.core.designsystem.components.NextEventCard
import app.organi7er.core.designsystem.components.Overline
import app.organi7er.core.designsystem.components.PlainIconButton
import app.organi7er.core.designsystem.components.SectionHeader
import app.organi7er.core.designsystem.components.SoftIconButton
import app.organi7er.core.designsystem.components.TileStats
import app.organi7er.core.model.Dates
import app.organi7er.feature.common.EventList
import app.organi7er.feature.common.NoteFeed
import app.organi7er.feature.common.TaskList
import app.organi7er.feature.editor.EditorRequest
import app.organi7er.feature.editor.LocalAppActions
import app.organi7er.navigation.ContentBottomPadding

private const val TAB_CALENDAR = 1
private const val TAB_TASKS = 2
private const val TAB_NOTES = 3

@Composable
fun OverviewScreen(vm: OverviewViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val selection by vm.selection.collectAsStateWithLifecycle()
    val actions = LocalAppActions.current
    val day = state.today.plusDays(selection.dayOffset.toLong())

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = Spacing.screen, end = Spacing.screen, bottom = ContentBottomPadding),
        verticalArrangement = Arrangement.spacedBy(Spacing.section),
    ) {
        item(key = "head", contentType = "head") {
            Column(Modifier.statusBarsPadding().padding(top = 6.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Wordmark(Modifier.weight(1f))
                    SoftIconButton(Ms.settings, "Einstellungen", actions.openSettings)
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = 18.dp - 6.dp, bottom = 14.dp - 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PlainIconButton(Ms.chevronLeft, "Vorheriger Tag", vm::prev, enabled = state.canPrev, iconSize = 16.dp)
                    Text(
                        state.dateLabel,
                        Modifier.widthIn(min = 176.dp).semantics { liveRegion = LiveRegionMode.Polite },
                        style = OrganiTheme.type.body.copy(fontSize = OrganiTheme.type.label.fontSize.times(14f / 13f)),
                        color = OrganiTheme.colors.textMuted,
                        textAlign = TextAlign.Center,
                    )
                    PlainIconButton(Ms.chevronRight, "Nächster Tag", vm::next, enabled = state.canNext, iconSize = 16.dp)
                }
                Tiles(state, vm)
            }
        }
        item(key = "capture", contentType = "capture") {
            Column {
                Overline("Erfassen", Modifier.padding(bottom = 14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    CaptureButton(EntryType.Event, { actions.openEditor(EditorRequest.NewEvent(day)) })
                    CaptureButton(EntryType.Task, { actions.openEditor(EditorRequest.NewTask(if (selection.week) state.today else day)) })
                    CaptureButton(EntryType.Note, { actions.openEditor(EditorRequest.NewNote) })
                }
            }
        }
        items(state.sections, key = { it.key }, contentType = { it::class }) { section ->
            when (section) {
                is OverviewSection.Next -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader("Als Nächstes", action = "Kalender", onAction = { actions.openTab(TAB_CALENDAR) })
                    val e = section.hero.event
                    NextEventCard(
                        timeRange = "${Dates.time(e.start!!)}–${Dates.time(e.effectiveEnd!!)}",
                        untilLabel = section.until,
                        title = e.title,
                        location = e.location,
                        duration = section.hero.timeSub,
                        tag = section.hero.tag,
                        onOpen = { actions.openEditor(EditorRequest.EditEvent(e.id)) },
                    )
                    if (section.rest.isNotEmpty()) EventList(section.rest, Modifier.padding(top = 0.dp))
                }
                is OverviewSection.Events -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(section.title, action = "Kalender", onAction = { actions.openTab(TAB_CALENDAR) })
                    EventList(section.items)
                }
                is OverviewSection.Tasks -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(section.title, action = if (section.more > 0) "Alle (+${section.more})" else "Alle", onAction = { actions.openTab(TAB_TASKS) })
                    TaskList(section.items)
                }
                is OverviewSection.Hint -> Hint(section.text)
            }
        }
        if (state.notes.isNotEmpty()) {
            item(key = "notes", contentType = "notes") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader("Notizen", action = "Alle", onAction = { actions.openTab(TAB_NOTES) })
                    NoteFeed(state.notes, state.today)
                }
            }
        }
    }
}

@Composable
private fun Wordmark(modifier: Modifier = Modifier) {
    val c = OrganiTheme.colors
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    Text(
        "Organi7er",
        modifier
            .semantics { heading() }
            .drawBehind {
                // Gelber Unterstrich hinter der „7“ (Index 6) – aus dem echten Textlayout.
                val box = layout?.getBoundingBox(6) ?: return@drawBehind
                val h = 7.dp.toPx()
                drawRoundRect(
                    c.accent,
                    topLeft = Offset(box.left - 1.dp.toPx(), box.bottom - h - 4.dp.toPx()),
                    size = Size(box.width + 2.dp.toPx(), h),
                    cornerRadius = CornerRadius(3.5.dp.toPx()),
                )
            },
        style = OrganiTheme.type.wordmark,
        color = c.text,
        onTextLayout = { layout = it },
    )
}

/** Lässt eine Zeile über den Seitenrand hinaus bis an die Bildschirmkante laufen. */
private fun Modifier.bleed(horizontal: Dp) = layout { measurable, constraints ->
    val extra = horizontal.roundToPx()
    val placeable = measurable.measure(constraints.copy(minWidth = constraints.maxWidth + extra * 2, maxWidth = constraints.maxWidth + extra * 2))
    layout(constraints.maxWidth, placeable.height) { placeable.place(-extra, 0) }
}

@Composable
private fun Tiles(state: OverviewState, vm: OverviewViewModel) {
    val listState = rememberLazyListState()
    val selectedIndex = state.tiles.indexOfFirst { it.kind == state.selectedTile }
    LaunchedEffect(selectedIndex) { if (selectedIndex >= 0) listState.animateScrollToItem(selectedIndex) }
    @OptIn(ExperimentalFoundationApi::class)
    val fling = rememberSnapFlingBehavior(listState, SnapPosition.Start)
    LazyRow(
        state = listState,
        flingBehavior = fling,
        modifier = Modifier.fillMaxWidth().bleed(Spacing.screen),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(state.tiles, key = { it.kind }) { b ->
            BriefingTile(
                title = b.title,
                stats = TileStats(b.events, b.tasks, b.notes),
                big = b.big,
                bigIcon = when (b.icon) {
                    BriefingIcon.SCHEDULE -> Ms.schedule
                    BriefingIcon.CALENDAR -> Ms.calendarToday
                    BriefingIcon.CHECKLIST -> Ms.checklist
                },
                caption = b.caption,
                progress = b.progress,
                selected = b.kind == state.selectedTile,
                onClick = { vm.selectTile(b.kind) },
            )
        }
    }
}

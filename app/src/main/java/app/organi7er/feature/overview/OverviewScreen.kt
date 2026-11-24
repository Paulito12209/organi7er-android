package app.organi7er.feature.overview

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.organi7er.core.designsystem.OrganiShapes
import app.organi7er.core.designsystem.OrganiTheme
import app.organi7er.core.designsystem.Spacing
import app.organi7er.core.designsystem.components.EntryType
import app.organi7er.core.designsystem.components.InsetCard
import app.organi7er.core.designsystem.components.LeadingCircle
import app.organi7er.core.designsystem.components.LedgerRow
import app.organi7er.core.designsystem.components.Ms
import app.organi7er.core.designsystem.components.MsIcon
import app.organi7er.core.designsystem.components.SectionHeader
import app.organi7er.core.designsystem.components.SegmentedControl
import app.organi7er.core.designsystem.components.glass
import app.organi7er.core.designsystem.components.icon
import app.organi7er.core.designsystem.components.shortLabel
import app.organi7er.core.designsystem.components.softShadow
import app.organi7er.feature.common.AppHeader
import app.organi7er.feature.editor.AppActions
import app.organi7er.feature.editor.EditorRequest
import app.organi7er.feature.editor.LocalAppActions
import app.organi7er.navigation.ContentBottomPadding
import kotlin.math.roundToInt

private const val TAB_CALENDAR = 1

private val SmallTile = 32.dp
private val TileGap = 5.dp

@Composable
fun OverviewScreen(vm: OverviewViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val actions = LocalAppActions.current
    val density = LocalDensity.current
    var feedTab by rememberSaveable { mutableIntStateOf(0) }

    // Ganz oben weiter nach unten ziehen → Benachrichtigungen.
    val pullThreshold = with(density) { 96.dp.toPx() }
    var pull by remember { mutableFloatStateOf(0f) }
    val pullConnection = remember(actions) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (pull > 0f && available.y < 0f) {
                    val used = maxOf(available.y, -pull)
                    pull += used
                    return Offset(0f, used)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y > 0f) {
                    pull = (pull + available.y * 0.5f).coerceAtMost(pullThreshold * 1.4f)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                val open = pull >= pullThreshold
                pull = 0f
                if (open) actions.openNotifications()
                return Velocity.Zero
            }
        }
    }
    val pullShown by animateFloatAsState(pull, spring(stiffness = Spring.StiffnessMedium), label = "pull")

    // Von rechts nach links wischen → Einstellungen.
    val swipeThreshold = with(density) { 90.dp.toPx() }

    Box(Modifier.fillMaxSize()) {
        PullHint((pullShown / pullThreshold).coerceIn(0f, 1f), Modifier.align(Alignment.TopCenter))
        LazyColumn(
            Modifier
                .fillMaxSize()
                .offset { IntOffset(0, pullShown.roundToInt()) }
                .nestedScroll(pullConnection)
                .pointerInput(actions) {
                    var dx = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { dx = 0f },
                        onDragEnd = { if (dx < -swipeThreshold) actions.openSettings() },
                        onHorizontalDrag = { _, d -> dx += d },
                    )
                },
            contentPadding = PaddingValues(start = Spacing.screen, end = Spacing.screen, bottom = ContentBottomPadding),
        ) {
            item(key = "head", contentType = "head") { AppHeader("Übersicht") }
            item(key = "tiles", contentType = "tiles") {
                DayTiles(state.days, state.selected, vm::selectDay, { actions.openTab(TAB_CALENDAR) }, Modifier.padding(top = 32.dp))
            }
            item(key = "weeks", contentType = "weeks") { WeekPicker(state, vm::selectWeek, Modifier.padding(top = 12.dp)) }
            item(key = "notif", contentType = "notif") { NotificationRow(state.unread, actions.openNotifications, Modifier.padding(top = 16.dp)) }
            item(key = "focus", contentType = "focus") {
                Column(Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader("Im Fokus")
                    if (state.focus.isEmpty()) {
                        Text(
                            "Fokussiere Termine, Aufgaben oder Notizen beim Bearbeiten – sie erscheinen dann hier.",
                            Modifier.fillMaxWidth().glass(RoundedCornerShape(22.dp), shadow = false).padding(16.dp),
                            style = OrganiTheme.type.rowMeta,
                            color = OrganiTheme.colors.textSoft,
                        )
                    } else {
                        LazyRow(
                            Modifier.fillMaxWidth().bleed(),
                            contentPadding = PaddingValues(horizontal = Spacing.screen, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(state.focus, key = { "${it.type}-${it.id}" }) { FocusCard(it) { open(actions, it) } }
                        }
                    }
                }
            }
            item(key = "feed", contentType = "feed") {
                Column(Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader("Verlauf")
                    SegmentedControl(listOf("Zuletzt geöffnet", "Zuletzt erstellt"), feedTab, { feedTab = it })
                    val list = if (feedTab == 0) state.opened else state.created
                    if (list.isEmpty()) {
                        Text(
                            if (feedTab == 0) "Noch nichts geöffnet." else "Noch nichts erstellt.",
                            Modifier.padding(horizontal = 4.dp),
                            style = OrganiTheme.type.rowMeta,
                            color = OrganiTheme.colors.textSoft,
                        )
                    } else {
                        InsetCard(padding = 10.dp) {
                            list.forEach { e ->
                                LedgerRow(
                                    title = e.title,
                                    subtitle = e.meta,
                                    middle = e.whenLabel,
                                    onClick = { open(actions, e) },
                                    leading = { LeadingCircle(size = 38.dp) { MsIcon(e.type.icon, null, tint = OrganiTheme.colors.text, size = 18.dp) } },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun open(actions: AppActions, e: EntryUi) = when (e.type) {
    EntryType.Event -> actions.openEditor(EditorRequest.EditEvent(e.id))
    EntryType.Task -> actions.openEditor(EditorRequest.EditTask(e.id))
    EntryType.Note -> actions.openNote(e.id)
}

/** Lässt eine Zeile über den Seitenrand bis an die Bildschirmkante laufen. */
private fun Modifier.bleed() = layout { measurable, constraints ->
    val extra = Spacing.screen.roundToPx()
    val w = constraints.maxWidth + extra * 2
    val placeable = measurable.measure(constraints.copy(minWidth = w, maxWidth = w))
    layout(constraints.maxWidth, placeable.height) { placeable.place(-extra, 0) }
}

@Composable
private fun PullHint(progress: Float, modifier: Modifier = Modifier) {
    if (progress <= 0f) return
    val c = OrganiTheme.colors
    Row(
        modifier
            .statusBarsPadding()
            .padding(top = 12.dp)
            .graphicsLayer {
                alpha = progress
                scaleX = 0.8f + 0.2f * progress
                scaleY = 0.8f + 0.2f * progress
            }
            .glass(OrganiShapes.pill)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        MsIcon(Ms.notifications, null, tint = c.text, size = 16.dp)
        Text(if (progress >= 1f) "Loslassen für Benachrichtigungen" else "Benachrichtigungen", style = OrganiTheme.type.label, color = c.text)
    }
}

// ------------------------------------------------------------------ Tageskacheln

@Composable
private fun DayTiles(
    days: List<DayTileUi>,
    selected: java.time.LocalDate,
    onSelect: (java.time.LocalDate) -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (days.isEmpty()) return
    BoxWithConstraints(modifier.fillMaxWidth()) {
        // Bei großer Schrift werden die schmalen Kacheln breiter, damit Zahlen nicht abgeschnitten werden.
        val small = SmallTile * LocalDensity.current.fontScale.coerceIn(1f, 1.5f)
        val big = maxWidth - small * (days.size - 1) - TileGap * (days.size - 1)
        Row(Modifier.fillMaxWidth().height(big.coerceAtLeast(108.dp)), horizontalArrangement = Arrangement.spacedBy(TileGap)) {
            days.forEach { d ->
                val active = d.date == selected
                val w by animateDpAsState(if (active) big else small, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow), label = "tile")
                val open = ((w - small) / (big - small)).coerceIn(0f, 1f)
                DayTile(d, active, open, Modifier.width(w).fillMaxHeight()) { if (active) onOpen() else onSelect(d.date) }
            }
        }
    }
}

@Composable
private fun DayTile(d: DayTileUi, active: Boolean, open: Float, modifier: Modifier, onClick: () -> Unit) {
    val c = OrganiTheme.colors
    val shape = RoundedCornerShape(if (open > 0.5f) 26.dp else 16.dp)
    val bg = if (active) {
        Modifier
            .softShadow(shape, c.shadowFloat)
            .clip(shape)
            .background(Brush.linearGradient(listOf(c.statSelectedA, c.statSelectedB)))
            .border(1.dp, Color.White.copy(alpha = 0.28f), shape)
    } else {
        Modifier.glass(shape, shadow = false)
    }
    Box(
        modifier
            .then(bg)
            .selectable(active, role = Role.Tab, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = d.a11y },
    ) {
        if (open > 0.35f) {
            BigTileContent(d, Modifier.graphicsLayer { alpha = ((open - 0.35f) / 0.65f).coerceIn(0f, 1f) })
        } else {
            SmallTileContent(d, Modifier.graphicsLayer { alpha = (1f - open / 0.35f).coerceIn(0f, 1f) })
        }
    }
}

@Composable
private fun BigTileContent(d: DayTileUi, modifier: Modifier) {
    Column(modifier.fillMaxSize().padding(horizontal = 13.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(d.short, Modifier.weight(1f), style = OrganiTheme.type.statValue.copy(fontSize = 25.sp), color = Color.White, maxLines = 1)
            Text(d.headline, style = OrganiTheme.type.caption, color = Color.White.copy(alpha = 0.72f), maxLines = 1)
        }
        Spacer(Modifier.weight(1f))
        if (d.nextLabel.isNotBlank()) Text(d.nextLabel, style = OrganiTheme.type.tileCaption, color = Color.White.copy(alpha = 0.7f), maxLines = 1)
        Text(d.nextTitle, style = OrganiTheme.type.label.copy(fontSize = 14.sp), color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Row(Modifier.padding(top = 7.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Count(Ms.calendarToday, d.events, Color.White.copy(alpha = 0.85f))
            Count(Ms.checklist, d.tasks, Color.White.copy(alpha = 0.85f))
            Count(Ms.description, d.notes, Color.White.copy(alpha = 0.85f))
        }
    }
}

@Composable
private fun SmallTileContent(d: DayTileUi, modifier: Modifier) {
    val c = OrganiTheme.colors
    Column(modifier.fillMaxSize().padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(d.letter, style = OrganiTheme.type.tileCaption, color = c.textSoft)
        Text(
            d.number,
            style = OrganiTheme.type.label.copy(fontSize = 15.sp, fontWeight = if (d.isToday) FontWeight.SemiBold else FontWeight.Medium),
            color = if (d.isToday) c.link else c.text,
        )
        Spacer(Modifier.weight(1f))
        StackCount(Ms.calendarToday, d.events)
        Spacer(Modifier.height(5.dp))
        StackCount(Ms.checklist, d.tasks)
    }
}

@Composable
private fun Count(@DrawableRes icon: Int, n: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        MsIcon(icon, null, tint = color, size = 12.dp)
        Text("$n", style = OrganiTheme.type.tileStat, color = color)
    }
}

@Composable
private fun StackCount(@DrawableRes icon: Int, n: Int) {
    val c = OrganiTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        MsIcon(icon, null, tint = c.textSoft, size = 11.dp)
        Text(if (n == 0) "–" else "$n", style = OrganiTheme.type.tileStat, color = if (n == 0) c.textSoft else c.text)
    }
}

// ------------------------------------------------------------------ KW-Auswahl

@Composable
private fun WeekPicker(state: OverviewState, onSelect: (WeekUi) -> Unit, modifier: Modifier = Modifier) {
    val c = OrganiTheme.colors
    val list = rememberLazyListState(initialFirstVisibleItemIndex = (state.selectedWeek - 3).coerceAtLeast(0))
    LaunchedEffect(state.selectedWeek, state.weeks.size) {
        if (state.weeks.isNotEmpty()) list.animateScrollToItem((state.selectedWeek - 3).coerceAtLeast(0))
    }
    LazyRow(
        modifier.fillMaxWidth().bleed(),
        state = list,
        contentPadding = PaddingValues(horizontal = Spacing.screen),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        itemsIndexed(state.weeks, key = { _, w -> w.start.toEpochDay() }) { i, w ->
            val sel = i == state.selectedWeek
            Box(
                Modifier
                    .heightIn(min = 36.dp)
                    .then(
                        if (sel) {
                            Modifier.softShadow(OrganiShapes.pill, c.shadowSoft).clip(OrganiShapes.pill).background(c.glassSolid).border(1.dp, c.glassEdge, OrganiShapes.pill)
                        } else {
                            Modifier.clip(OrganiShapes.pill)
                        },
                    )
                    .selectable(sel, role = Role.Tab, onClick = { onSelect(w) })
                    .semantics { contentDescription = "Kalenderwoche ${w.number}" }
                    .padding(horizontal = if (sel) 14.dp else 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (sel) "KW ${w.number}" else "${w.number}",
                    style = OrganiTheme.type.label.copy(fontSize = 13.5.sp, fontWeight = if (sel || w.isCurrent) FontWeight.SemiBold else FontWeight.Normal),
                    color = when {
                        sel -> c.onGlassSolid
                        w.isCurrent -> c.link
                        else -> c.textSoft
                    },
                )
            }
        }
    }
}

// ------------------------------------------------------------------ Benachrichtigungen

@Composable
private fun NotificationRow(unread: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = OrganiTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .glass(OrganiShapes.pill)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 10.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier.size(30.dp).clip(OrganiShapes.circle).background(if (unread > 0) c.danger else c.glassChip),
            contentAlignment = Alignment.Center,
        ) {
            if (unread > 0) {
                Text(if (unread > 99) "99+" else "$unread", style = OrganiTheme.type.tag, color = Color.White)
            } else {
                MsIcon(Ms.notifications, null, tint = c.textMuted, size = 16.dp)
            }
        }
        Text(
            if (unread > 0) "Ungelesene Benachrichtigungen" else "Keine neuen Benachrichtigungen",
            Modifier.weight(1f),
            style = OrganiTheme.type.body.copy(fontSize = 14.sp),
            color = if (unread > 0) c.text else c.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        MsIcon(Ms.chevronRight, null, tint = c.textSoft, size = 18.dp)
    }
}

// ------------------------------------------------------------------ Im Fokus

@Composable
private fun FocusCard(e: EntryUi, onClick: () -> Unit) {
    val c = OrganiTheme.colors
    val shape = RoundedCornerShape(22.dp)
    Column(
        Modifier
            .width(148.dp)
            .heightIn(min = 112.dp)
            .glass(shape)
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            MsIcon(e.type.icon, null, tint = c.textSoft, size = 13.dp)
            Text(e.type.shortLabel, style = OrganiTheme.type.tileCaption, color = c.textSoft)
        }
        Text(
            e.title,
            Modifier.padding(top = 6.dp),
            style = OrganiTheme.type.rowTitle.copy(fontSize = 14.5.sp, lineHeight = 18.sp),
            color = c.text,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.weight(1f))
        Text(e.meta.substringAfter(" · ", ""), style = OrganiTheme.type.tileCaption, color = c.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

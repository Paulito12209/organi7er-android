package app.organi7er.feature.overview

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
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
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawOutline
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.organi7er.core.designsystem.DarkOrganiColors
import app.organi7er.core.designsystem.LocalOrganiColors
import app.organi7er.core.designsystem.OrganiShapes
import app.organi7er.core.designsystem.OrganiTheme
import app.organi7er.core.designsystem.Primitives
import app.organi7er.core.designsystem.Spacing
import app.organi7er.core.designsystem.components.DotText
import app.organi7er.core.designsystem.components.EntryType
import app.organi7er.core.designsystem.components.InsetCard
import app.organi7er.core.designsystem.components.LeadingCircle
import app.organi7er.core.designsystem.components.LedgerRow
import app.organi7er.core.designsystem.components.Ms
import app.organi7er.core.designsystem.components.MsIcon
import app.organi7er.core.designsystem.components.SectionHeader
import app.organi7er.core.designsystem.components.SegmentedControl
import app.organi7er.core.designsystem.components.SegmentedList
import app.organi7er.core.designsystem.components.segmentItem
import app.organi7er.core.designsystem.components.MeetingButton
import app.organi7er.core.designsystem.components.floatShadow
import app.organi7er.core.designsystem.components.glass
import app.organi7er.core.designsystem.components.icon
import app.organi7er.core.designsystem.components.liquidGlass
import app.organi7er.core.designsystem.components.shortLabel
import app.organi7er.core.designsystem.components.softShadow
import app.organi7er.feature.common.AppHeader
import app.organi7er.feature.editor.AppActions
import app.organi7er.feature.editor.EditorRequest
import app.organi7er.feature.editor.LocalAppActions
import app.organi7er.navigation.ContentBottomPadding
import kotlin.math.roundToInt

private const val TAB_CALENDAR = 1

@Composable
fun OverviewScreen(vm: OverviewViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val actions = LocalAppActions.current
    val density = LocalDensity.current
    var feedTab by rememberSaveable { mutableIntStateOf(0) }

    // Von rechts nach links wischen → Einstellungen.
    val swipeThreshold = with(density) { 90.dp.toPx() }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier
                .fillMaxSize()
                .pointerInput(actions) {
                    var dx = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { dx = 0f },
                        onDragEnd = { if (dx < -swipeThreshold) actions.openSettings() },
                        onHorizontalDrag = { _, d -> dx += d },
                    )
                },
            contentPadding = PaddingValues(start = Spacing.page, end = Spacing.page, bottom = ContentBottomPadding),
        ) {
            // Kopfbereich auf dunklem Hintergrund mit Punktlandschaft: Kopfzeile, Tageskacheln, KW-Auswahl.
            // Immer im dunklen Farbschema, damit Schrift und Glas auf dem Schwarz lesbar bleiben.
            item(key = "hero", contentType = "hero") {
                CompositionLocalProvider(LocalOrganiColors provides DarkOrganiColors) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .bleed()
                            .heroBackground()
                            // Unten kein Abstand: der Bereich endet genau an der Unterkante der KW-Pille.
                            .padding(horizontal = Spacing.page),
                    ) {
                        AppHeader("Übersicht", halo = true)
                        DayTiles(
                            days = state.days,
                            selected = state.selected,
                            tall = state.unread == 0,
                            onSelect = vm::selectDay,
                            onOpen = { actions.openTab(TAB_CALENDAR) },
                            modifier = Modifier.padding(top = 28.dp).bleed(Spacing.page - TileEdge),
                        )
                        WeekPicker(state, vm::selectWeek, Modifier.padding(top = 18.dp))
                    }
                }
            }
            // Nur bei ungelesenen Benachrichtigungen – sonst fällt die Zeile samt Abstand weg.
            if (state.unread > 0) {
                item(key = "notif", contentType = "notif") { NotificationRow(state.unread, actions.openNotifications, Modifier.padding(top = 22.dp)) }
            }
            // „Im Fokus“ nur, wenn wirklich etwas fokussiert ist.
            if (state.focus.isNotEmpty()) {
                item(key = "focus", contentType = "focus") {
                    Column(Modifier.padding(top = 34.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        SectionTitle("Im Fokus")
                        LazyRow(
                            Modifier.fillMaxWidth().bleed(),
                            contentPadding = PaddingValues(horizontal = Spacing.page, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(state.focus, key = { "${it.type}-${it.id}" }) { FocusCard(it) { open(actions, it) } }
                        }
                    }
                }
            }
            item(key = "feed", contentType = "feed") {
                Column(Modifier.padding(top = 34.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SectionTitle("Verlauf")
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
                        // Wie die Einstellungen: einzelne Flächen, außen 16, innen 4 abgerundet, kleine Lücke.
                        SegmentedList {
                            list.forEachIndexed { i, e -> FeedRow(e, i, list.size) { open(actions, e) } }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        Modifier.padding(horizontal = 2.dp).semantics { heading() },
        style = OrganiTheme.type.headline.copy(fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
        color = OrganiTheme.colors.text,
    )
}

/** Verlaufszeile: Icon, Titel, darunter Art („Notiz“) und ggf. ein „Entwurf“-Chip, rechts die Zeit. */
@Composable
private fun FeedRow(e: EntryUi, index: Int, count: Int, onClick: () -> Unit) {
    val c = OrganiTheme.colors
    val parts = e.meta.split(" · ")
    val kind = parts.first()
    val draft = parts.any { it == "Entwurf" }
    val rest = parts.drop(1).filter { it != "Entwurf" }.joinToString(" · ")
    Row(
        Modifier
            .segmentItem(index, count, minHeight = 64.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Box(Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(c.surface2), contentAlignment = Alignment.Center) {
            MsIcon(e.type.icon, null, tint = c.text, size = 18.dp)
        }
        Column(Modifier.weight(1f)) {
            Text(e.title, style = OrganiTheme.type.rowTitle, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(if (rest.isBlank()) kind else "$kind · $rest", Modifier.weight(1f, fill = false), style = OrganiTheme.type.rowMeta, color = c.textSoft, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (draft) {
                    Text(
                        "Entwurf",
                        Modifier.clip(OrganiShapes.pill).border(1.dp, c.textSoft.copy(alpha = 0.5f), OrganiShapes.pill).padding(horizontal = 8.dp, vertical = 1.dp),
                        style = OrganiTheme.type.tileStat,
                        color = c.textMuted,
                    )
                }
            }
        }
        if (e.whenLabel.isNotBlank()) Text(e.whenLabel, style = OrganiTheme.type.rowMeta, color = c.textSoft, maxLines = 1)
    }
}

private fun open(actions: AppActions, e: EntryUi) = when (e.type) {
    EntryType.Event -> actions.openEditor(EditorRequest.EditEvent(e.id))
    EntryType.Task -> actions.openEditor(EditorRequest.EditTask(e.id))
    EntryType.Note -> actions.openNote(e.id)
}

/** Abstand der äußeren Tageskacheln zum Bildschirmrand. */
private val TileEdge = 8.dp

/** Lässt eine Zeile über den Seitenrand hinaus laufen (Standard: bis an die Bildschirmkante). */
private fun Modifier.bleed(by: androidx.compose.ui.unit.Dp = Spacing.page) = layout { measurable, constraints ->
    val extra = by.roundToPx()
    val w = constraints.maxWidth + extra * 2
    val placeable = measurable.measure(constraints.copy(minWidth = w, maxWidth = w))
    layout(constraints.maxWidth, placeable.height) { placeable.place(-extra, 0) }
}

// ------------------------------------------------------------------ Tageskacheln

/**
 * Sieben Tage als Stapel ohne Lücken: der gewählte Tag liegt vorn als große Kachel,
 * die Tage davor schieben sich von links, die danach von rechts dahinter. Alle Kacheln sind gleich hoch;
 * die schmalen sind nur an ihrer Außenseite abgerundet (links bzw. rechts), innen gerade.
 */
@Composable
private fun DayTiles(
    days: List<DayTileUi>,
    selected: java.time.LocalDate,
    tall: Boolean,
    onSelect: (java.time.LocalDate) -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (days.isEmpty()) return
    val active = days.indexOfFirst { it.date == selected }.coerceAtLeast(0)
    BoxWithConstraints(modifier.fillMaxWidth()) {
        // Sichtbarer Streifen je kleiner Kachel; wächst mit großer Schrift.
        val step = StripWidth * LocalDensity.current.fontScale.coerceIn(1f, 1.4f)
        val big = (maxWidth - step * (days.size - 1)).coerceAtLeast(104.dp)
        // Ohne Benachrichtigungszeile darf der Block höher werden.
        val height by animateDpAsState(if (tall) big + 44.dp else big, label = "tilesH")
        val spec = spring<androidx.compose.ui.unit.Dp>(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
        Box(Modifier.fillMaxWidth().height(height)) {
            days.forEachIndexed { i, d ->
                val isActive = i == active
                val depth = kotlin.math.abs(i - active)
                val targetX = when {
                    i < active -> step * i
                    isActive -> step * active
                    else -> step * active + big - TileOverlap + step * (i - active - 1)
                }
                val x by animateDpAsState(targetX, spec, label = "x$i")
                val w by animateDpAsState(if (isActive) big else step + TileOverlap, spec, label = "w$i")
                val open = ((w - step - TileOverlap) / (big - step - TileOverlap)).coerceIn(0f, 1f)
                DayTile(
                    d = d,
                    active = isActive,
                    open = open,
                    depth = depth,
                    stripOnRight = i > active,
                    strip = step,
                    modifier = Modifier
                        // Weiter vom aktiven Tag entfernt = weiter hinten.
                        .zIndex(if (isActive) 100f else (10 - depth).toFloat())
                        .offset(x = x)
                        .width(w)
                        .fillMaxHeight(),
                ) { if (isActive) onOpen() else onSelect(d.date) }
            }
        }
    }
}

private val StripWidth = 31.dp
private val TileOverlap = 20.dp
private val TileRadius = 28.dp

@Composable
private fun DayTile(
    d: DayTileUi,
    active: Boolean,
    open: Float,
    depth: Int,
    stripOnRight: Boolean,
    strip: androidx.compose.ui.unit.Dp,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val c = OrganiTheme.colors
    // Aktiv: rundum abgerundet. Schmal: nur die sichtbare Außenseite rund, die verdeckte Seite gerade.
    val shape = when {
        active || open > 0.5f -> RoundedCornerShape(TileRadius)
        stripOnRight -> RoundedCornerShape(topEnd = TileRadius, bottomEnd = TileRadius)
        else -> RoundedCornerShape(topStart = TileRadius, bottomStart = TileRadius)
    }
    // Schmale Kacheln deckend, je tiefer desto etwas dunkler (hell) bzw. heller (dunkel).
    val base = if (c.isDark) Color(0xFF141416) else Color.White
    val shade = if (c.isDark) Color.White.copy(alpha = 0.018f * (6 - depth.coerceAtMost(6))) else Color.Black.copy(alpha = 0.022f * depth)
    val bg = if (active) {
        Modifier
            .floatShadow(shape, c.shadowFloat)
            .clip(shape)
            .frostedGlass(shape)
    } else {
        Modifier
            .softShadow(shape, c.shadowSoft)
            .clip(shape)
            .background(base)
            .background(shade)
            .border(1.dp, if (c.isDark) Color.White.copy(alpha = 0.07f) else Color.Black.copy(alpha = 0.05f), shape)
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
            SmallTileContent(
                d,
                Modifier
                    .align(if (stripOnRight) Alignment.CenterEnd else Alignment.CenterStart)
                    .width(strip)
                    .graphicsLayer { alpha = (1f - open / 0.35f).coerceIn(0f, 1f) },
            )
        }
    }
}

/** Mattglas der aktiven Tageskachel: Grundton oben → unten. */
private val Frost100 = Color(0xFFF7F8F9)
private val Frost200 = Color(0xFFE9EBEE)
private val Frost300 = Color(0xFFDCDFE3)

/**
 * Helles Mattglas wie die „Logo Design“-Pille der Vorlage: leicht grauer Kern,
 * nach außen ein weicher, fast weißer Lichtrand und oben ein sanfter Glanz.
 */
private fun Modifier.frostedGlass(shape: androidx.compose.ui.graphics.Shape): Modifier = drawWithCache {
    val outline = shape.createOutline(size, layoutDirection, this)
    val base = Brush.verticalGradient(0f to Frost100, 0.55f to Frost200, 1f to Frost300)
    val sheen = Brush.verticalGradient(0f to Color.White.copy(alpha = 0.75f), 0.35f to Color.White.copy(alpha = 0f))
    // Lichtrand: viele zarte Striche übereinander ergeben einen weichen Verlauf zum Rand hin (Hälfte liegt außerhalb und wird abgeschnitten).
    val rim = (1..14).map { k -> (k * 2.4f).dp.toPx() to 0.07f } + (2.dp.toPx() to 0.85f)
    onDrawWithContent {
        drawOutline(outline, base)
        rim.forEach { (w, a) -> drawOutline(outline, Color.White.copy(alpha = a), style = androidx.compose.ui.graphics.drawscope.Stroke(w)) }
        drawRect(sheen)
        drawContent()
    }
}

/** Schrift auf der silbernen Kachel. */
private val OnSilver = Color(0xFF0A0A0B)

@Composable
private fun BigTileContent(d: DayTileUi, modifier: Modifier) {
    val soft = OnSilver.copy(alpha = 0.6f)
    Column(modifier.fillMaxSize().padding(start = 18.dp, end = 14.dp, top = 16.dp, bottom = 16.dp)) {
        Row(Modifier.fillMaxWidth()) {
            // Datum groß, darunter „Heute“ / „Morgen“ / Wochentag.
            Column(Modifier.weight(1f)) {
                // Wochentag als Schrift, die Tageszahl im Punktraster.
                val day = d.short.substringBefore(' ')
                val num = d.short.substringAfter(' ', "")
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(day, style = OrganiTheme.type.statValue.copy(fontSize = 30.sp, lineHeight = 32.sp, fontWeight = FontWeight.Medium), color = OnSilver, maxLines = 1)
                    if (num.isNotBlank()) DotText(num, Modifier.padding(bottom = 7.dp), height = 21.dp, color = OnSilver)
                }
                Text(d.headline, Modifier.padding(top = 2.dp), style = OrganiTheme.type.label, color = soft, maxLines = 1)
            }
            // Nur mit Link: Symbol der Plattform (Meet, Zoom, Teams …) oben rechts, Tippen öffnet das Meeting.
            if (d.nextLink.isNotBlank()) MeetingButton(d.nextLink, Modifier.padding(start = 10.dp), size = 38.dp)
        }
        Spacer(Modifier.weight(1f))
        // Zähler waagerecht, dicht über „Als Nächstes“.
        Row(Modifier.padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Count(Ms.calendarToday, d.events)
            Count(Ms.checklist, d.tasks)
            Count(Ms.description, d.notes)
        }
        if (d.nextLabel.isNotBlank()) Text(d.nextLabel, style = OrganiTheme.type.tileCaption.copy(fontSize = 12.sp), color = soft, maxLines = 1)
        Text(
            d.nextTitle,
            style = OrganiTheme.type.title.copy(fontSize = 18.sp, lineHeight = 22.sp),
            color = OnSilver,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SmallTileContent(d: DayTileUi, modifier: Modifier) {
    val c = OrganiTheme.colors
    Column(modifier.fillMaxHeight().padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(d.letter, style = OrganiTheme.type.tileCaption, color = c.textSoft)
        DotText(d.number, Modifier.padding(top = 4.dp), height = 11.dp, color = if (d.isToday) c.text else c.textMuted)
        if (d.isToday) Box(Modifier.padding(top = 3.dp).size(4.dp).clip(OrganiShapes.circle).background(Primitives.Yellow500))
        Spacer(Modifier.weight(1f))
        StackCount(Ms.calendarToday, d.events)
        Spacer(Modifier.height(6.dp))
        StackCount(Ms.checklist, d.tasks)
    }
}

/** Zähler der aktiven Kachel: Symbol in einem zarten Silberkreis, die Zahl im Punktraster rechts daneben. */
@Composable
private fun Count(@DrawableRes icon: Int, n: Int) {
    val shape = OrganiShapes.circle
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Box(
            Modifier
                .size(30.dp)
                .clip(shape)
                .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.6f), OnSilver.copy(alpha = 0.04f))))
                .border(1.dp, OnSilver.copy(alpha = 0.13f), shape),
            contentAlignment = Alignment.Center,
        ) {
            MsIcon(icon, null, tint = OnSilver.copy(alpha = 0.7f), size = 14.dp)
        }
        DotText("$n", height = 9.dp, color = OnSilver.copy(alpha = 0.85f))
    }
}

@Composable
private fun StackCount(@DrawableRes icon: Int, n: Int) {
    val c = OrganiTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        MsIcon(icon, null, tint = c.textSoft, size = 12.dp)
        DotText(if (n == 0) "-" else "$n", height = 8.dp, color = if (n == 0) c.textSoft else c.text)
    }
}

// ------------------------------------------------------------------ KW-Auswahl

private val EdgeFade = 32.dp
private val WeekShape = TrapezoidShape()



/** Die gewählte KW steht immer in der Mitte; links und rechts blenden die Wochen über 32 dp aus. */
@Composable
private fun WeekPicker(state: OverviewState, onSelect: (WeekUi) -> Unit, modifier: Modifier = Modifier) {
    val c = OrganiTheme.colors
    val list = rememberLazyListState(initialFirstVisibleItemIndex = state.selectedWeek)
    LaunchedEffect(state.selectedWeek, state.weeks.size) {
        if (state.weeks.isEmpty()) return@LaunchedEffect
        val index = state.selectedWeek
        if (list.layoutInfo.visibleItemsInfo.none { it.index == index }) list.scrollToItem(index)
        val info = list.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index } ?: return@LaunchedEffect
        val center = (list.layoutInfo.viewportStartOffset + list.layoutInfo.viewportEndOffset) / 2
        list.animateScrollBy((info.offset + info.size / 2 - center).toFloat())
    }
    BoxWithConstraints(modifier.fillMaxWidth().bleed()) {
        // So viel Rand, dass auch die erste und letzte Woche mittig stehen können.
        val side = maxWidth / 2 - 36.dp
        LazyRow(
            Modifier
                .fillMaxWidth()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    val f = (EdgeFade.toPx() / size.width).coerceIn(0f, 0.5f)
                    drawRect(
                        Brush.horizontalGradient(0f to Color.Transparent, f to Color.Black, 1f - f to Color.Black, 1f to Color.Transparent),
                        blendMode = BlendMode.DstIn,
                    )
                },
            state = list,
            contentPadding = PaddingValues(horizontal = side),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            itemsIndexed(state.weeks, key = { _, w -> w.start.toEpochDay() }) { i, w ->
                val sel = i == state.selectedWeek
                Box(
                    Modifier
                        .heightIn(min = 38.dp)
                        .then(
                            if (sel) {
                                // Trapez (oben schmal, unten breit) im selben Glas wie die Such-Pille.
                                Modifier.liquidGlass(WeekShape)
                            } else {
                                Modifier.clip(OrganiShapes.pill)
                            },
                        )
                        .selectable(sel, role = Role.Tab, onClick = { onSelect(w) })
                        .semantics { contentDescription = "Kalenderwoche ${w.number}" }
                        // Gewählt mehr Rand, weil das Trapez oben eingezogen ist.
                        .padding(horizontal = if (sel) 22.dp else 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    // Gewählt wie „Suchen“: „KW“ weiß, die Zahl im gedämpften Weißgrau.
                    val ink = when {
                        sel -> c.textMuted
                        w.isCurrent -> c.text
                        else -> c.textSoft
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (sel) Text("KW", style = OrganiTheme.type.label.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold), color = c.text)
                        DotText("${w.number}", height = 10.dp, color = ink)
                    }
                }
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

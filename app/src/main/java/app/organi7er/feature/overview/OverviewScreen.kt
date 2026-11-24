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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.addOutline
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
            // Hell: das weiße Gegenstück mit dunklen Punkten.
            item(key = "hero", contentType = "hero") {
                run {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .bleed()
                            .heroBackground(OrganiTheme.colors.isDark)
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
                            modifier = Modifier.padding(top = 20.dp).bleed(Spacing.page - TileEdge),
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
    // Aktiv: mit Termin-Bild als Foto unter weißem Halo, sonst hell silbern (dunkel) bzw. dunkel silbern (hell).
    val photo = d.nextImage.takeIf { active }
    val ink = when {
        !active -> c.text
        photo != null || !c.isDark -> Color.White
        else -> OnSilver
    }
    val bg = if (active) {
        Modifier
            .floatShadow(shape, c.shadowFloat)
            .clip(shape)
            .then(if (photo != null || !c.isDark) Modifier else Modifier.frostedGlass(shape))
            .then(if (photo == null && !c.isDark) Modifier.darkSilver(shape) else Modifier)
    } else {
        Modifier
            .softShadow(shape, c.shadowSoft)
            .clip(shape)
            .background(base)
            .background(shade)
            .then(if (d.birthday) Modifier.background(RainbowTint(c.isDark)) else Modifier)
            .border(1.dp, if (c.isDark) Color.White.copy(alpha = 0.07f) else Color.Black.copy(alpha = 0.05f), shape)
    }
    Box(
        modifier
            .then(bg)
            .selectable(active, role = Role.Tab, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = d.a11y },
    ) {
        if (photo != null) {
            coil3.compose.AsyncImage(
                model = java.io.File(photo),
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
            Box(Modifier.matchParentSize().photoHalo(shape))
        }
        if (open > 0.35f) {
            BigTileContent(d, ink, Modifier.graphicsLayer { alpha = ((open - 0.35f) / 0.65f).coerceIn(0f, 1f) })
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

/**
 * Dunkler Regenbogen auf schmalen Geburtstags-Kacheln: gedämpfte Farben von oben nach unten.
 * Deckkraft hier einstellen (dunkel kräftiger, hell zarter).
 */
private fun RainbowTint(dark: Boolean): Brush {
    val a = if (dark) 0.26f else 0.18f
    return Brush.verticalGradient(
        listOf(
            Color(0xFFB0303A).copy(alpha = a),
            Color(0xFFB36A1F).copy(alpha = a),
            Color(0xFF9A9A22).copy(alpha = a),
            Color(0xFF2F8F4E).copy(alpha = a),
            Color(0xFF2A6FB0).copy(alpha = a),
            Color(0xFF6A3FB0).copy(alpha = a),
        ),
    )
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

/** Dunkles Silber der aktiven Kachel im hellen Modus: oben → unten. */
private val Graphite100 = Color(0xFF55585E)
private val Graphite200 = Color(0xFF34363B)
private val Graphite300 = Color(0xFF1F2024)

/** Dunkles Silber (heller Modus): Graphit-Verlauf, zarter Lichtrand und Glanz oben – Schrift weiß. */
private fun Modifier.darkSilver(shape: androidx.compose.ui.graphics.Shape): Modifier = drawWithCache {
    val outline = shape.createOutline(size, layoutDirection, this)
    val base = Brush.verticalGradient(0f to Graphite100, 0.5f to Graphite200, 1f to Graphite300)
    val sheen = Brush.verticalGradient(0f to Color.White.copy(alpha = 0.16f), 0.35f to Color.White.copy(alpha = 0f))
    val rim = (1..8).map { k -> (k * 2.4f).dp.toPx() to 0.035f } + (1.5.dp.toPx() to 0.28f)
    onDrawWithContent {
        drawOutline(outline, base)
        rim.forEach { (w, a) -> drawOutline(outline, Color.White.copy(alpha = a), style = androidx.compose.ui.graphics.drawscope.Stroke(w)) }
        drawRect(sheen)
        drawContent()
    }
}

/** Stärke des weißen Halos über dem Foto (0 = keiner, 1 = sehr milchig) und Breite des Rands. */
private const val HaloStrength = 0.022f
private val HaloWidth = 20.dp

/** Anzahl der übereinandergelegten Schichten des Halos: mehr = weicherer Verlauf. */
private const val HaloSteps = 44

/** Körnung über dem Foto: Anzahl der Punkte und Deckkraft. */
private const val GrainDots = 9000
private const val GrainAlpha = 0.06f

/**
 * Wie die Vorlage „Flasks Per Day“: das Foto liegt hinten, darüber ein milchig-weißer Halo,
 * der vom Rand nach innen ausläuft, eine feine Körnung und unten/oben eine leichte Abdunklung für die weiße Schrift.
 */
private fun Modifier.photoHalo(shape: androidx.compose.ui.graphics.Shape): Modifier = drawWithCache {
    val outline = shape.createOutline(size, layoutDirection, this)
    val shade = Brush.verticalGradient(
        0f to Color.Black.copy(alpha = 0.38f),
        0.32f to Color.Black.copy(alpha = 0.06f),
        0.55f to Color.Black.copy(alpha = 0.10f),
        1f to Color.Black.copy(alpha = 0.62f),
    )
    // Schichten zwischen Außenkante und einem nach innen gerückten Rechteck mit runden Ecken:
    // nahe am Rand liegen viele Schichten übereinander (hell), nach innen immer weniger – ein weicher Auslauf
    // ohne sichtbare Stufen, und auch die Innenkante ist an den Ecken rund.
    val outer = Path().apply { addOutline(outline) }
    val radius = TileRadius.toPx()
    val layers = (0 until HaloSteps).map { k ->
        val inset = HaloWidth.toPx() * (k + 1) / HaloSteps
        Path().apply {
            addPath(outer)
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    inset, inset, size.width - inset, size.height - inset,
                    androidx.compose.ui.geometry.CornerRadius((radius - inset * 0.25f).coerceAtLeast(4f)),
                ),
            )
            fillType = androidx.compose.ui.graphics.PathFillType.EvenOdd
        }
    }
    val bright = Brush.radialGradient(
        listOf(Color.White.copy(alpha = 0.16f), Color.Transparent),
        Offset(size.width * 0.15f, size.height * 0.05f),
        size.width * 0.9f,
    )
    val rnd = java.util.Random(7)
    val light = ArrayList<Offset>(GrainDots / 2)
    val dark = ArrayList<Offset>(GrainDots / 2)
    repeat(GrainDots) { i ->
        val o = Offset(rnd.nextFloat() * size.width, rnd.nextFloat() * size.height)
        if (i % 2 == 0) light += o else dark += o
    }
    val dot = 0.7.dp.toPx()
    onDrawBehind {
        drawRect(shade)
        drawRect(bright)
        layers.forEach { drawPath(it, Color.White.copy(alpha = HaloStrength)) }
        drawOutline(outline, Color.White.copy(alpha = 0.45f), style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx()))
        drawPoints(light, androidx.compose.ui.graphics.PointMode.Points, Color.White.copy(alpha = GrainAlpha), strokeWidth = dot)
        drawPoints(dark, androidx.compose.ui.graphics.PointMode.Points, Color.Black.copy(alpha = GrainAlpha), strokeWidth = dot)
    }
}

/** Schrift auf der silbernen Kachel. */
private val OnSilver = Color(0xFF0A0A0B)

@Composable
private fun BigTileContent(d: DayTileUi, ink: Color, modifier: Modifier) {
    val soft = ink.copy(alpha = if (ink == Color.White) 0.75f else 0.6f)
    Column(modifier.fillMaxSize().padding(start = 18.dp, end = 14.dp, top = 16.dp, bottom = 16.dp)) {
        Row(Modifier.fillMaxWidth()) {
            // Datum groß, darunter „Heute“ / „Morgen“ / Wochentag.
            Column(Modifier.weight(1f)) {
                // Wochentag als Schrift, die Tageszahl im Punktraster.
                val day = d.short.substringBefore(' ')
                val num = d.short.substringAfter(' ', "")
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(day, style = OrganiTheme.type.statValue.copy(fontSize = 30.sp, lineHeight = 32.sp, fontWeight = FontWeight.Medium), color = ink, maxLines = 1)
                    if (num.isNotBlank()) DotText(num, Modifier.padding(bottom = 7.dp), height = 21.dp, color = ink)
                }
                Text(d.headline, Modifier.padding(top = 2.dp), style = OrganiTheme.type.label, color = soft, maxLines = 1)
            }
            // Nur mit Link: Symbol der Plattform (Meet, Zoom, Teams …) oben rechts, Tippen öffnet das Meeting.
            if (d.nextLink.isNotBlank()) MeetingButton(d.nextLink, Modifier.padding(start = 10.dp), size = 38.dp)
        }
        Spacer(Modifier.weight(1f))
        // Zähler waagerecht, dicht über „Als Nächstes“.
        Row(Modifier.padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Count(Ms.calendarToday, d.events, ink)
            Count(Ms.checklist, d.tasks, ink)
            Count(Ms.description, d.notes, ink)
        }
        if (d.nextLabel.isNotBlank()) Text(d.nextLabel, style = OrganiTheme.type.tileCaption.copy(fontSize = 12.sp), color = soft, maxLines = 1)
        Text(
            d.nextTitle,
            style = OrganiTheme.type.title.copy(fontSize = 18.sp, lineHeight = 22.sp),
            color = ink,
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
private fun Count(@DrawableRes icon: Int, n: Int, ink: Color) {
    val light = ink == Color.White
    val shape = OrganiShapes.circle
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Box(
            Modifier
                .size(30.dp)
                .clip(shape)
                .background(
                    if (light) {
                        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.26f), Color.White.copy(alpha = 0.06f)))
                    } else {
                        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.6f), ink.copy(alpha = 0.04f)))
                    },
                )
                .border(1.dp, if (light) Color.White.copy(alpha = 0.35f) else ink.copy(alpha = 0.13f), shape),
            contentAlignment = Alignment.Center,
        ) {
            MsIcon(icon, null, tint = ink.copy(alpha = if (light) 0.95f else 0.7f), size = 14.dp)
        }
        DotText("$n", height = 9.dp, color = ink.copy(alpha = if (light) 1f else 0.85f))
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
                    // Gewählt: „KW“ in hellem Silber (10 % dunkler als die Schrift), die Zahl in voller Schriftfarbe (dunkel: weiß).
                    val kw = if (c.isDark) androidx.compose.ui.graphics.lerp(c.text, Color.Black, 0.10f) else c.textMuted
                    val ink = when {
                        sel -> c.text
                        w.isCurrent -> c.text
                        else -> c.textSoft
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (sel) Text("KW", style = OrganiTheme.type.label.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold), color = kw)
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

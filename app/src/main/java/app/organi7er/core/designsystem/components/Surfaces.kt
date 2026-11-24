package app.organi7er.core.designsystem.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import app.organi7er.R
import app.organi7er.core.designsystem.OrganiShapes
import app.organi7er.core.designsystem.OrganiTheme
import app.organi7er.core.designsystem.Spacing
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.hazeBlur

/** Material Symbols Rounded (offizielle Vektoren, Gewicht 400; *_filled für aktive Reiter, *_light dünn). */
object Ms {
    val home = R.drawable.ms_home
    val homeFilled = R.drawable.ms_home_filled
    val calendarToday = R.drawable.ms_calendar_today
    val calendarTodayFilled = R.drawable.ms_calendar_today_filled
    val eventLight = R.drawable.ms_event_light
    val taskAltLight = R.drawable.ms_task_alt_light
    val editNote = R.drawable.ms_edit_note
    val editNoteLight = R.drawable.ms_edit_note_light
    val checklist = R.drawable.ms_checklist
    val checklistFilled = R.drawable.ms_checklist_filled
    val description = R.drawable.ms_description
    val descriptionFilled = R.drawable.ms_description_filled
    val add = R.drawable.ms_add
    val search = R.drawable.ms_search
    val check = R.drawable.ms_check
    val schedule = R.drawable.ms_schedule
    val locationOn = R.drawable.ms_location_on
    val arrowOutward = R.drawable.ms_arrow_outward
    val photoCamera = R.drawable.ms_photo_camera
    val image = R.drawable.ms_image
    val notifications = R.drawable.ms_notifications
    val notificationsFilled = R.drawable.ms_notifications_filled
    val chevronLeft = R.drawable.ms_chevron_left
    val chevronRight = R.drawable.ms_chevron_right
    val swapVert = R.drawable.ms_swap_vert
    val settings = R.drawable.ms_settings
    val lightMode = R.drawable.ms_light_mode
    val close = R.drawable.ms_close
    val delete = R.drawable.ms_delete
    val expandMore = R.drawable.ms_expand_more
    val flag = R.drawable.ms_flag
    val flagFilled = R.drawable.ms_flag_filled
    val sell = R.drawable.ms_sell
    val upload = R.drawable.ms_upload
    val download = R.drawable.ms_download
    val alarm = R.drawable.ms_alarm
    val graphicEq = R.drawable.ms_graphic_eq
    val tune = R.drawable.ms_tune
    val arrowUpward = R.drawable.ms_arrow_upward
    val subject = R.drawable.ms_subject
}

@Composable
fun MsIcon(@DrawableRes icon: Int, contentDescription: String?, modifier: Modifier = Modifier, tint: Color = OrganiTheme.colors.text, size: Dp = 24.dp) {
    Icon(painterResource(icon), contentDescription, modifier.size(size), tint = tint)
}

// ------------------------------------------------------------------ Tiefe

/** Schatten „soft“: y 8, blur 24, Navy 8 % (dunkel: Schwarz 35 %). */
fun Modifier.softShadow(shape: Shape, color: Color): Modifier =
    dropShadow(shape, Shadow(radius = 24.dp, color = color, offset = DpOffset(0.dp, 8.dp)))

/** Schatten „float“: y 12, blur 32, Navy 18 %. */
fun Modifier.floatShadow(shape: Shape, color: Color): Modifier =
    dropShadow(shape, Shadow(radius = 32.dp, color = color, offset = DpOffset(0.dp, 12.dp)))

/** Ganz leichter Kachelschatten (y 3, blur 10). */
fun Modifier.tileShadow(shape: Shape, color: Color): Modifier =
    dropShadow(shape, Shadow(radius = 10.dp, color = color, offset = DpOffset(0.dp, 3.dp)))

/** Soft-UI-Knopf: vertikaler Verlauf + feine helle Innenkante oben. */
@Composable
fun Modifier.knob(shape: Shape): Modifier {
    val c = OrganiTheme.colors
    val brush = remember(c) { Brush.verticalGradient(listOf(c.knobTop, c.knobBottom)) }
    val edge = remember(c) { Brush.verticalGradient(0f to (if (c.isDark) Color(0x1493BBFD) else Color.White), 0.25f to Color.Transparent) }
    return this
        .softShadow(shape, c.shadowSoft)
        .clip(shape)
        .background(brush)
        .border(1.dp, edge, shape)
}

/** Hintergrund: weiche, große Farbschlieren, damit die Glasflächen Tiefe bekommen. */
@Composable
fun Modifier.aura(glow: Color? = null): Modifier {
    val c = OrganiTheme.colors
    // Schwarz-Weiß: ruhiger Verlauf, oben minimal heller (hell) bzw. leicht aufgehellt (dunkel).
    val top = if (c.isDark) Color(0xFF16161A) else Color.White
    val base = Brush.verticalGradient(0f to top, 0.45f to c.bg, 1f to c.bg)
    if (glow == null) return background(base)
    // Glow wie bei Gemini: ein weicher Schein, der von der Unterkante nach oben ausläuft.
    val alpha = if (c.isDark) 0.6f else 0.25f
    return drawBehind {
        drawRect(base)
        drawRect(
            Brush.radialGradient(
                colors = listOf(glow.copy(alpha = alpha), Color.Transparent),
                center = Offset(size.width / 2f, size.height),
                radius = size.height * 0.7f,
            ),
        )
    }
}

/** Optionale Unschärfe-Quelle für einzelne, ruhende Glasflächen. */
val LocalBackdropHaze = staticCompositionLocalOf<HazeState?> { null }

/**
 * Glasfläche: halbtransparente Fläche `glass` mit heller Kante über der ruhenden Aura.
 *
 * Performance: Echte Hintergrund-Unschärfe (Haze) kostet pro Fläche einen RenderEffect und
 * einen zusätzlichen Layer. Über der statischen Aura sieht sie nicht anders aus als die
 * halbtransparente Fläche. Deshalb blurt nur, wer es ausdrücklich verlangt (`blur = true`),
 * z. B. die Navigationsleiste, unter der Inhalt durchscrollt.
 */
@Composable
fun Modifier.glass(shape: Shape = OrganiShapes.card, fill: Color = OrganiTheme.colors.glass, shadow: Boolean = true, blur: Boolean = false): Modifier {
    val c = OrganiTheme.colors
    val haze = if (blur) LocalBackdropHaze.current else null
    val style = remember {
        HazeBlurStyle {
            blurRadius(22.dp)
            noiseFactor(0f)
        }
    }
    var m = this
    if (shadow) m = m.softShadow(shape, c.shadowSoft)
    m = m.clip(shape)
    if (haze != null) m = m.hazeBlur(HazeInput.Sources(haze), style)
    val sheen = remember(c) {
        Brush.verticalGradient(0f to Color.White.copy(alpha = if (c.isDark) 0.07f else 0.32f), 0.55f to Color.Transparent)
    }
    val edge = remember(c) {
        Brush.linearGradient(
            0f to c.glassEdge,
            0.45f to c.glassLine.copy(alpha = c.glassLine.alpha * 0.35f),
            1f to c.glassEdge.copy(alpha = c.glassEdge.alpha * 0.6f),
        )
    }
    return m.background(fill).background(sheen).border(1.dp, edge, shape)
}

@Composable
fun GlassCard(modifier: Modifier = Modifier, shape: Shape = OrganiShapes.card, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.glass(shape), content = content)
}

// ------------------------------------------------------------------ Listen wie Gmail (M3 segmented list)

/** Form eines Eintrags: Radius 4, außen (erstes oben, letztes unten) 16. */
fun segmentShape(index: Int, count: Int): Shape {
    val outer = 16.dp
    val inner = 4.dp
    return RoundedCornerShape(
        topStart = if (index == 0) outer else inner,
        topEnd = if (index == 0) outer else inner,
        bottomStart = if (index == count - 1) outer else inner,
        bottomEnd = if (index == count - 1) outer else inner,
    )
}

/** Gruppe aus Einträgen auf eigener Fläche, 2 dp Abstand, keine Trennlinien. */
@Composable
fun SegmentedList(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp), content = content)
}

/** Fläche eines Listeneintrags. 16 dp seitlich, 14 dp oben/unten, mind. 64 dp hoch. */
@Composable
fun Modifier.segmentItem(index: Int, count: Int, color: Color = OrganiTheme.colors.insetFill, minHeight: Dp = 64.dp): Modifier {
    val shape = segmentShape(index, count)
    return this
        .fillMaxWidth()
        .heightIn(min = minHeight)
        .clip(shape)
        .background(color)
}

// ------------------------------------------------------------------ Überschriften

@Composable
fun Overline(text: String, modifier: Modifier = Modifier, color: Color = OrganiTheme.colors.textSoft) {
    Text(text.uppercase(), modifier.semantics { heading() }, style = OrganiTheme.type.overline, color = color)
}

/** Abschnittskopf „Als Nächstes“ … mit optionalem Link rechts. */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    val c = OrganiTheme.colors
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f).semantics { heading() }, style = OrganiTheme.type.title, color = c.text)
        if (action != null && onAction != null) {
            TextLink(action, onAction)
        }
    }
}

@Composable
fun Hint(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier.padding(horizontal = Spacing.xs), style = OrganiTheme.type.label.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Normal), color = OrganiTheme.colors.textSoft)
}

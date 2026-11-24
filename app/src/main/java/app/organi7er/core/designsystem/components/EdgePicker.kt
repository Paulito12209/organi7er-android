package app.organi7er.core.designsystem.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import app.organi7er.core.designsystem.OrganiTheme
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt

/** Ein Ziel des Edge-Pickers. [notch] ist die Kerbenfarbe im Dark Mode, solange dieser Reiter aktiv ist. */
data class EdgeTab(
    val label: String,
    @DrawableRes val icon: Int,
    @DrawableRes val iconSelected: Int,
    val notch: EdgeNotch,
)

/**
 * Verlauf der Kerbe im Dark Mode, von innen (links) nach außen (rechts). [inner] ist die Farbe an der Kante,
 * [mid] die Mitte. Am Bildschirmrand läuft der Verlauf in den Hintergrund aus, damit die Kerbe nahtlos wirkt.
 */
data class EdgeNotch(val inner: Color, val mid: Color)

/** Übersicht: dunkelgrauer Verlauf nach Schwarz. */
val EdgeSilver = EdgeNotch(Color(0xFF3E434A), Color(0xFF17181B))
/** Termine: Silber in dunkles Blau. */
val EdgeNavy = EdgeNotch(Color(0xFF6B717A), Color(0xFF1F3B6E))
/** Notizen: Silber in dunkles Gold. */
val EdgeGold = EdgeNotch(Color(0xFF6B717A), Color(0xFF64510F))
/** Aufgaben: Blau in ein etwas dunkleres Blau. */
val EdgeBlue = EdgeNotch(Color(0xFF2960AD), Color(0xFF14284D))

// Maße in dp. Aufgeklappt zeigt die Kerbe die anderen Reiter fest untereinander, ohne Scrollen.
private val OpenWidth = 58.dp
private val OpenHeight = 320.dp
private val OpenCurve = 110.dp
// Eingeklappt nur ein schmaler Griff am Rand.
private val ClosedWidth = 16.dp
private val ClosedHeight = 112.dp
private val ClosedCurve = 40.dp
// Tippfläche am Rand, auch wenn eingeklappt.
private val TapWidth = 24.dp
private val TapHeight = 160.dp
private val IconSize = 20.dp
/** Abstand von Icon-Mitte zu Icon-Mitte: 20 Icon + 2 × 12. */
private val SlotStep = 44.dp
private val TapRadius = 22.dp
private val OpticalShift = 2.dp
private val SwipeThreshold = 48.dp
private val ExclusionHalf = 100.dp

/** So lange bleibt die Kerbe nach der letzten Berührung aufgeklappt. */
private const val AutoCloseMillis = 2000L

/**
 * Kerbe am rechten Rand. [items] sind alle Reiter, [selected] der aktive. Der aktive Reiter wird nicht
 * gezeigt; die Kerbe nimmt seine Farbe an. Eingeklappt ist nur ein Griff zu sehen. Antippen klappt auf,
 * dann wählt ein Tipp auf ein Icon den Reiter. Nach [AutoCloseMillis] ohne Berührung klappt sie wieder zu.
 * Ein Wisch nach links am Rand ruft [onSwipeLeft] auf.
 */
@Composable
fun EdgePicker(
    items: List<EdgeTab>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onSwipeLeft: () -> Unit = {},
) {
    if (items.isEmpty()) return
    val c = OrganiTheme.colors
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val active = items.getOrNull(selected) ?: items.first()
    // Hell: einfarbige dunkle Kerbe. Dunkel: Verlauf, der am Rand in den Hintergrund übergeht.
    val lightNotch = Color(0xFF0A0A0B)
    val edgeColor = if (c.isDark) c.bg else lightNotch

    var open by remember { mutableStateOf(false) }
    var touches by remember { mutableIntStateOf(0) }
    LaunchedEffect(open, touches) {
        if (open) {
            delay(AutoCloseMillis)
            open = false
        }
    }

    val spec = spring<Dp>(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
    val width by animateDpAsState(if (open) OpenWidth else ClosedWidth, spec, label = "edgeW")
    val height by animateDpAsState(if (open) OpenHeight else ClosedHeight, spec, label = "edgeH")
    val curve by animateDpAsState(if (open) OpenCurve else ClosedCurve, spec, label = "edgeC")
    val content by animateFloatAsState(if (open) 1f else 0f, label = "edgeContent")

    val innerColor by animateColorAsState(if (c.isDark) active.notch.inner else lightNotch, tween(320), label = "notchInner")
    val midColor by animateColorAsState(if (c.isDark) active.notch.mid else lightNotch, tween(320), label = "notchMid")
    val notchBrush = Brush.horizontalGradient(
        0f to innerColor,
        0.75f to midColor,
        1f to edgeColor,
    )
    val iconTint = Color.White.copy(alpha = 0.85f)

    val currentOnSelect by rememberUpdatedState(onSelect)
    val currentOnSwipeLeft by rememberUpdatedState(onSwipeLeft)
    val currentSelected by rememberUpdatedState(selected)
    val currentItems by rememberUpdatedState(items)

    val wPx = with(density) { width.toPx() }
    val hPx = with(density) { height.toPx() }
    val curvePx = with(density) { curve.toPx() }
    val stepPx = with(density) { SlotStep.toPx() }
    val tapPx = with(density) { TapRadius.toPx() }
    val shiftPx = with(density) { OpticalShift.toPx() }
    val swipePx = with(density) { SwipeThreshold.toPx() }
    val exclusionPx = with(density) { ExclusionHalf.toPx() }
    val tapWPx = with(density) { TapWidth.toPx() }
    val tapHPx = with(density) { TapHeight.toPx() }

    // Reiter, der unter [y] liegt (Abstand zur Mitte der Kerbe). null, wenn daneben getippt wurde.
    fun tabAt(y: Float, heightPx: Float): Int? {
        val others = currentItems.indices.filter { it != currentSelected }
        val count = others.size
        var best: Int? = null
        var bestDist = Float.MAX_VALUE
        others.forEachIndexed { slot, tabIndex ->
            val slotY = heightPx / 2 + (slot - (count - 1) / 2f) * stepPx
            val dist = abs(y - slotY)
            if (dist < bestDist) {
                bestDist = dist
                best = tabIndex
            }
        }
        return if (bestDist <= tapPx) best else null
    }

    val shape = remember(curve) { NotchShape(curve) }
    val others = items.indices.filter { it != selected }

    Box(
        modifier
            .width(maxOf(width, TapWidth))
            .height(maxOf(height, TapHeight))
            .semantics { contentDescription = "Navigation" }
            // Android erlaubt nur ein Ausschlussfeld von 200 dp: die Mitte, damit der Zurück-Wisch dort nicht greift.
            .systemGestureExclusion { coords ->
                Rect(
                    0f,
                    coords.size.height / 2f - exclusionPx,
                    coords.size.width.toFloat(),
                    coords.size.height / 2f + exclusionPx,
                )
            }
            .pointerInput(Unit) {
                detectTapGestures { p: Offset ->
                    touches++
                    if (!open) {
                        open = true
                        haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                    } else {
                        tabAt(p.y, size.height.toFloat())?.let { idx ->
                            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            currentOnSelect(idx)
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                var dx = 0f
                detectHorizontalDragGestures(
                    onDragStart = { dx = 0f },
                    onDragEnd = { if (dx < -with(density) { SwipeThreshold.toPx() }) currentOnSwipeLeft() },
                    onHorizontalDrag = { _, d -> dx += d },
                )
            },
        contentAlignment = Alignment.BottomEnd,
    ) {
        Box(
            Modifier
                .width(width)
                .height(height)
                .clip(shape)
                .background(notchBrush),
        ) {
            // Die anderen Reiter, fest untereinander. Die Mitte ist der Abstand zwischen ihnen.
            val count = others.size
            others.forEachIndexed { slot, tabIndex ->
                val item = items[tabIndex]
                val y = hPx / 2 + (slot - (count - 1) / 2f) * stepPx
                // Die Kante bestimmt die x-Position: das Icon liegt mittig im freien Raum.
                val edge = notchEdgeX(y, hPx, curvePx, wPx)
                val cx = edge + (wPx - edge) / 2 + shiftPx
                key(tabIndex) {
                    Box(
                        Modifier
                            .offset { IntOffset((cx - with(density) { IconSize.toPx() } / 2).roundToInt(), (y - with(density) { IconSize.toPx() } / 2).roundToInt()) }
                            .size(IconSize)
                            .alpha(content)
                            .semantics {
                                contentDescription = item.label
                                role = Role.Tab
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        MsIcon(item.icon, null, tint = iconTint, size = IconSize)
                    }
                }
            }
        }
    }
}

/** x-Position der inneren Kante auf Höhe [y] (0 im geraden Teil, [w] ganz oben und unten am Rand). */
private fun notchEdgeX(y: Float, h: Float, c: Float, w: Float): Float {
    val yy = if (y > h / 2) h - y else y
    if (yy >= c) return 0f
    if (yy <= 0f) return w
    // Bézier-Parameter t aus y per Bisektion (y(t) ist streng monoton).
    var lo = 0f
    var hi = 1f
    repeat(20) {
        val t = (lo + hi) / 2
        val by = c * (1.5f * t - 1.5f * t * t + t * t * t)
        if (by < yy) lo = t else hi = t
    }
    val t = (lo + hi) / 2
    return w * (1 - t) * (1 - t) * (1 + 2 * t)
}

/** Kerbe: gerade linke Kante, rechts am Bildschirmrand offen, oben und unten weich zum Rand auslaufend. */
private class NotchShape(private val curve: Dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val w = size.width
        val h = size.height
        val t = minOf(with(density) { curve.toPx() }, h / 2)
        val path = Path().apply {
            moveTo(w, 0f)
            cubicTo(w, t * 0.5f, 0f, t * 0.5f, 0f, t)
            lineTo(0f, h - t)
            cubicTo(0f, h - t * 0.5f, w, h - t * 0.5f, w, h)
            close()
        }
        return Outline.Generic(path)
    }
}

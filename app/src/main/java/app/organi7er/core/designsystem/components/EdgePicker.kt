package app.organi7er.core.designsystem.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import app.organi7er.core.designsystem.OrganiShapes
import app.organi7er.core.designsystem.OrganiTheme
import kotlinx.coroutines.delay

/** Ein Ziel des Edge-Pickers. */
data class EdgeTab(val label: String, @DrawableRes val icon: Int, @DrawableRes val iconSelected: Int)

/** Breite der Tippfläche am rechten Rand, auch wenn die Kerbe eingeklappt ist (16 dp Kerbe + 8 dp Luft). */
val EdgePickerHitWidth: Dp = 24.dp

/** So lange bleibt der Picker nach der letzten Berührung offen. */
private const val AutoCloseMillis = 2500L

private val ClosedWidth = 16.dp
private val OpenWidth = 64.dp
private val ClosedHeight = 112.dp
private val OpenHeight = 288.dp
private val ClosedCurve = 40.dp
private val OpenCurve = 56.dp
private val Chip = 46.dp
private val ChipGap = 8.dp

/**
 * Kerbe am rechten Rand mit weich auslaufenden Enden (wie in Paralist). Eingeklappt nur ein
 * 16 dp schmaler Griff; Antippen klappt sie auf, nach [AutoCloseMillis] ohne Berührung wieder zu.
 * Ziehen nach oben/unten wählt ein Ziel, ein Wisch nach links am Rand öffnet das Menü ([onSwipeLeft]).
 */
@Composable
fun EdgePicker(
    items: List<EdgeTab>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onSwipeLeft: () -> Unit = {},
) {
    val c = OrganiTheme.colors
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
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

    val fill = if (c.isDark) Color.White else Color(0xFF0A0A0B)
    val onFill = if (c.isDark) Color(0xFF0A0A0B) else Color.White
    val shape = remember(curve) { NotchShape(curve) }
    val sel = selected.coerceIn(0, items.lastIndex)

    // Sichtbarer Höhenbereich der Chips innerhalb der Kerbe.
    val listHeight = Chip * items.size + ChipGap * (items.size - 1)
    fun slotAt(yPx: Float, heightPx: Float): Int {
        val top = (heightPx - with(density) { listHeight.toPx() }) / 2
        val step = with(density) { (Chip + ChipGap).toPx() }
        return ((yPx - top) / step).toInt().coerceIn(0, items.lastIndex)
    }

    Box(
        modifier
            .width(maxOf(width, EdgePickerHitWidth))
            .height(maxOf(height, 160.dp))
            .semantics { contentDescription = "Navigation" }
            // Der Android-Zurück-Wisch vom rechten Rand darf hier nicht dazwischenfunken.
            .systemGestureExclusion()
            // Tippfläche über die ganze Höhe: Antippen klappt auf (oder wählt, wenn offen).
            .pointerInput(open, items.size) {
                detectTapGestures { p ->
                    touches++
                    if (!open) {
                        open = true
                        haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                    } else if (p.x >= size.width - with(density) { OpenWidth.toPx() }) {
                        val i = slotAt(p.y, size.height.toFloat())
                        if (i != sel) {
                            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            onSelect(i)
                        }
                    }
                }
            }
            // Wisch nach links am Rand: Menü öffnen.
            .pointerInput(onSwipeLeft) {
                var dx = 0f
                val threshold = with(density) { 48.dp.toPx() }
                detectHorizontalDragGestures(
                    onDragStart = { dx = 0f },
                    onDragEnd = { if (dx < -threshold) onSwipeLeft() },
                    onHorizontalDrag = { _, d -> dx += d },
                )
            }
            // Ziehen entlang der Kerbe wählt das Ziel unter dem Finger.
            .pointerInput(open, items.size) {
                if (!open) return@pointerInput
                detectVerticalDragGestures(
                    onDragStart = { touches++ },
                    onDragEnd = { touches++ },
                    onVerticalDrag = { change, _ ->
                        touches++
                        val i = slotAt(change.position.y, size.height.toFloat())
                        if (i != sel) {
                            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            onSelect(i)
                        }
                    },
                )
            },
        // Unten bündig: auch eingeklappt hängt die Kerbe am Dock.
        contentAlignment = Alignment.BottomEnd,
    ) {
        Box(
            Modifier
                .width(width)
                .height(height)
                .clip(shape)
                .background(fill),
            contentAlignment = Alignment.Center,
        ) {
            // Eingeklappt: nur ein kleiner Griff als Hinweis.
            Box(
                Modifier
                    .size(width = 4.dp, height = 28.dp)
                    .graphicsLayer { alpha = 1f - content }
                    .clip(OrganiShapes.pill)
                    .background(onFill.copy(alpha = 0.7f)),
            )
            Column(
                Modifier.fillMaxWidth().graphicsLayer { alpha = content },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(ChipGap, Alignment.CenterVertically),
            ) {
                items.forEachIndexed { i, item ->
                    val active = i == sel
                    Box(
                        Modifier
                            .size(Chip)
                            .clip(RoundedCornerShape(50))
                            .background(if (active) onFill else Color.Transparent)
                            .semantics {
                                contentDescription = item.label
                                role = Role.Tab
                                this.selected = active
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        MsIcon(
                            if (active) item.iconSelected else item.icon,
                            null,
                            tint = if (active) fill else onFill.copy(alpha = 0.78f),
                            size = 21.dp,
                        )
                    }
                }
            }
        }
    }
}

/** Kerbe: gerade linke Kante, rechts am Bildschirmrand offen, oben und unten weich zum Rand auslaufend. */
private class NotchShape(private val curve: Dp) : Shape {
    override fun createOutline(size: androidx.compose.ui.geometry.Size, layoutDirection: LayoutDirection, density: Density): Outline {
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

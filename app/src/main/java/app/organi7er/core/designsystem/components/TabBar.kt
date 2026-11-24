package app.organi7er.core.designsystem.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import app.organi7er.core.designsystem.OrganiShapes
import app.organi7er.core.designsystem.OrganiTheme
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.hazeBlur

data class TabItem(val label: String, @DrawableRes val icon: Int, @DrawableRes val iconSelected: Int)

/** Höhe der Leiste. */
val TabBarHeight: Dp = 64.dp

private val Chip = 44.dp
private val Pad = 7.dp
private val ChipGap = 6.dp
private val GroupGap = 8.dp
private val GroupHeight = Chip + Pad * 2

/** Der aktive Reiter sitzt in einer eigenen runden Kapsel, so hoch wie die Pillen. */
private val Active = GroupHeight
private val NeckHalf = 7.dp
private val PlusSize = 58.dp

/** Geometrie einer Leiste mit aktivem Reiter [active]: Gruppe davor · aktiver Reiter · Gruppe danach. */
private data class BarGeometry(
    val leftW: Dp,
    val activeX: Dp,
    val rightX: Dp,
    val rightW: Dp,
    val total: Dp,
    val centers: List<Dp>,
)

private fun groupWidth(k: Int): Dp = if (k == 0) 0.dp else Chip * k + ChipGap * (k - 1) + Pad * 2

private fun geometry(count: Int, active: Int): BarGeometry {
    val left = active
    val right = count - 1 - active
    val leftW = groupWidth(left)
    val activeX = if (left > 0) leftW + GroupGap else 0.dp
    val rightX = activeX + Active + if (right > 0) GroupGap else 0.dp
    val rightW = groupWidth(right)
    val centers = List(count) { i ->
        when {
            i < active -> Pad + (Chip + ChipGap) * i + Chip / 2
            i == active -> activeX + Active / 2
            else -> rightX + Pad + (Chip + ChipGap) * (i - active - 1) + Chip / 2
        }
    }
    return BarGeometry(leftW, activeX, rightX, rightW, rightX + rightW, centers)
}

/**
 * Leiste aus verschmolzenem Liquid Glass: die Reiter vor dem aktiven in einer Pille, der aktive Reiter
 * in einer eigenen Kapsel, die Reiter danach in einer zweiten Pille – verbunden über schmale Stege.
 * Alle Reiter sind gleich groß; nur der aktive ist gefüllt (hell: weiß, dunkel: schwarz).
 * Rechts daneben, eigenständig, der Plus-Knopf. Mit [hazeState] wird der Inhalt dahinter unscharf.
 */
@Composable
fun FusedTabBar(
    items: List<TabItem>,
    selected: Int,
    onSelect: (Int) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
    addOpen: Boolean = false,
    showTabs: Boolean = true,
    hazeState: HazeState? = null,
) {
    Row(modifier.fillMaxWidth().height(TabBarHeight), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (showTabs) TabShapes(items, selected, onSelect, hazeState)
        }
        AddButton(addOpen, onAdd, hazeState)
    }
}

/** Unscharfes, getöntes Glas mit Lichtkante – Grundlage für Leiste und Plus-Knopf. */
@Composable
private fun Modifier.liquidGlass(shape: Shape, hazeState: HazeState?): Modifier {
    val c = OrganiTheme.colors
    val style = remember {
        HazeBlurStyle {
            blurRadius(22.dp)
            noiseFactor(0f)
        }
    }
    val tint = if (c.isDark) Color(0xFF0B1220).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.45f)
    val sheen = remember(c) {
        Brush.verticalGradient(0f to Color.White.copy(alpha = if (c.isDark) 0.12f else 0.5f), 0.5f to Color.Transparent)
    }
    val rim = remember(c) {
        val k = if (c.isDark) 0.45f else 1f
        Brush.linearGradient(
            0f to Color.White.copy(alpha = 0.9f * k),
            0.45f to Color.White.copy(alpha = 0.15f * k),
            1f to Color.White.copy(alpha = 0.55f * k),
        )
    }
    return this
        .floatShadow(shape, c.shadowFloat)
        .clip(shape)
        .then(if (hazeState != null) Modifier.hazeBlur(HazeInput.Sources(hazeState), style) else Modifier)
        .background(tint)
        .background(sheen)
        .border(1.dp, rim, shape)
}

@Composable
private fun TabShapes(items: List<TabItem>, selected: Int, onSelect: (Int) -> Unit, hazeState: HazeState?) {
    val c = OrganiTheme.colors
    val g = remember(items.size, selected) { geometry(items.size, selected.coerceIn(0, items.lastIndex)) }
    val spec = spring<Dp>(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)
    val leftW by animateDpAsState(g.leftW, spec, label = "leftW")
    val activeX by animateDpAsState(g.activeX, spec, label = "activeX")
    val rightX by animateDpAsState(g.rightX, spec, label = "rightX")
    val rightW by animateDpAsState(g.rightW, spec, label = "rightW")
    val total by animateDpAsState(g.total, spec, label = "total")
    val shape = remember(leftW, activeX, rightX, rightW) { BarShape(leftW, activeX, rightX, rightW) }

    // Gefüllter Reiter: hell weiß mit schwarzem Icon, dunkel schwarz mit weißem Icon.
    val activeFill = if (c.isDark) Color(0xFF05070D) else Color.White
    val activeIcon = if (c.isDark) Color.White else Color(0xFF05070D)
    val idleFill = if (c.isDark) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.22f)
    val idleEdge = Color.White.copy(alpha = if (c.isDark) 0.1f else 0.45f)

    Box(
        Modifier
            .width(total)
            .height(TabBarHeight)
            .liquidGlass(shape, hazeState)
            .selectableGroup(),
    ) {
        items.forEachIndexed { i, item ->
            val isActive = i == selected
            val center by animateDpAsState(g.centers[i], spec, label = "tab$i")
            val fill by animateFloatAsState(if (isActive) 1f else 0f, label = "fill$i")
            Box(
                Modifier
                    .offset(x = center - Chip / 2, y = (TabBarHeight - Chip) / 2)
                    .size(Chip)
                    .then(if (isActive) Modifier.softShadow(OrganiShapes.circle, c.shadowSoft) else Modifier)
                    .clip(OrganiShapes.circle)
                    .drawBehind {
                        drawCircle(idleFill)
                        if (fill > 0f) drawCircle(activeFill, alpha = fill)
                    }
                    .border(1.dp, if (isActive && c.isDark) Color.White.copy(alpha = 0.28f) else idleEdge, OrganiShapes.circle)
                    .selectable(isActive, role = Role.Tab, onClick = { onSelect(i) })
                    .semantics { contentDescription = item.label },
                contentAlignment = Alignment.Center,
            ) {
                MsIcon(
                    if (isActive) item.iconSelected else item.icon,
                    null,
                    tint = if (isActive) activeIcon else c.text.copy(alpha = if (c.isDark) 0.6f else 0.7f),
                    size = 20.dp,
                )
            }
        }
    }
}

@Composable
private fun AddButton(open: Boolean, onClick: () -> Unit, hazeState: HazeState?) {
    val c = OrganiTheme.colors
    val rotation by animateFloatAsState(if (open) 45f else 0f, label = "plus")
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    Box(
        Modifier
            .size(PlusSize)
            .liquidGlass(OrganiShapes.circle, hazeState)
            .then(if (open) Modifier.background(c.accent) else Modifier)
            .clickable(role = Role.Button, onClickLabel = if (open) "Schließen" else "Schnell erfassen") {
                haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.ContextClick)
                onClick()
            }
            .semantics { contentDescription = if (open) "Schnell erfassen schließen" else "Schnell erfassen" },
        contentAlignment = Alignment.Center,
    ) {
        MsIcon(Ms.add, null, Modifier.rotate(rotation), tint = if (open) c.onAccent else c.text, size = 26.dp)
    }
}

/** Ein zusammenhängender Umriss: zwei Pillen + Kreis, an den Übergängen über eingeschnürte Stege verbunden. */
private class BarShape(val leftW: Dp, val activeX: Dp, val rightX: Dp, val rightW: Dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline = with(density) {
        val cy = size.height / 2
        val gh = GroupHeight.toPx()
        val act = Active.toPx()
        val neck = NeckHalf.toPx()
        var path = Path()

        fun pill(x: Float, w: Float) {
            if (w < 1f) return
            val h = minOf(gh, w)
            val p = Path().apply { addRoundRect(RoundRect(x, cy - h / 2, x + w, cy + h / 2, CornerRadius(h / 2))) }
            path = Path.combine(PathOperation.Union, path, p)
        }

        /** Steg zwischen zwei Formen; [strength] blendet ihn beim Zusammenschrumpfen einer Gruppe aus. */
        fun bridge(x1: Float, x2: Float, strength: Float) {
            if (strength <= 0.02f || x2 - x1 < 1f) return
            val n = neck * strength
            val flare = (gh / 2 - 6.dp.toPx()) * strength
            val mid = (x1 + x2) / 2
            val inset = 10.dp.toPx()
            val p = Path().apply {
                moveTo(x1 - inset, cy - flare)
                cubicTo(x1 + 2.dp.toPx(), cy - flare, mid - 4.dp.toPx(), cy - n, mid, cy - n)
                cubicTo(mid + 4.dp.toPx(), cy - n, x2 - 2.dp.toPx(), cy - flare, x2 + inset, cy - flare)
                lineTo(x2 + inset, cy + flare)
                cubicTo(x2 - 2.dp.toPx(), cy + flare, mid + 4.dp.toPx(), cy + n, mid, cy + n)
                cubicTo(mid - 4.dp.toPx(), cy + n, x1 + 2.dp.toPx(), cy + flare, x1 - inset, cy + flare)
                close()
            }
            path = Path.combine(PathOperation.Union, path, p)
        }

        val lw = leftW.toPx()
        val ax = activeX.toPx()
        val rx = rightX.toPx()
        val rw = rightW.toPx()
        val full = groupWidth(1).toPx()

        pill(0f, lw)
        val circle = Path().apply { addOval(androidx.compose.ui.geometry.Rect(ax, cy - act / 2, ax + act, cy + act / 2)) }
        path = Path.combine(PathOperation.Union, path, circle)
        pill(rx, rw)
        bridge(lw, ax, (lw / full).coerceIn(0f, 1f))
        bridge(ax + act, rx, (rw / full).coerceIn(0f, 1f))
        Outline.Generic(path)
    }
}

/** Dezenter Verlauf hinter der Leiste: höchstens 16 dp über die Leiste hinaus, max. 80 % Deckkraft. */
@Composable
fun TabBarFade(height: Dp, modifier: Modifier = Modifier) {
    val bg = OrganiTheme.colors.bg
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .background(Brush.verticalGradient(0f to Color.Transparent, 0.35f to bg.copy(alpha = 0.55f), 1f to bg.copy(alpha = 0.8f))),
    )
}

package app.organi7er.core.designsystem.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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

private val Chip = 46.dp
private val Pad = 9.dp
private val ChipGap = 4.dp
private val PlusSize = 64.dp
private val PlusGap = 12.dp

private fun capsuleWidth(count: Int): Dp = Chip * count + ChipGap * (count - 1) + Pad * 2

/**
 * Eine ruhige Glaskapsel mit runden Reitern; der aktive Reiter ist ein gefüllter Kreis (hell: schwarz,
 * dunkel: weiß) und gleitet beim Wechsel. Rechts daneben, eigenständig, der Plus-Knopf.
 * Mit [hazeState] wird der Inhalt dahinter unscharf.
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
    val barWidth = remember(items.size) { capsuleWidth(items.size) }
    Row(
        modifier.fillMaxWidth().height(TabBarHeight),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Feste Breite: der Plus-Knopf steht immer an derselben Stelle.
        Box(Modifier.width(barWidth), contentAlignment = Alignment.CenterEnd) {
            if (showTabs) TabCapsule(items, selected, onSelect, hazeState)
        }
        Spacer(Modifier.width(PlusGap))
        AddButton(addOpen, onAdd, hazeState)
    }
}

/** Abstand der Plus-Mitte vom rechten Bildschirmrand (für „Neuer Eintrag“). */
fun plusCenterFromEnd(screenWidth: Dp, count: Int): Dp {
    val row = capsuleWidth(count) + PlusGap + PlusSize
    return (screenWidth - row) / 2 + PlusSize / 2
}

/** Unscharfes, getöntes Glas mit Lichtkante – Grundlage für Leiste und Plus-Knopf. */
@Composable
private fun Modifier.liquidGlass(shape: Shape, hazeState: HazeState?): Modifier {
    val c = OrganiTheme.colors
    val style = remember {
        HazeBlurStyle {
            blurRadius(24.dp)
            noiseFactor(0f)
        }
    }
    val tint = if (c.isDark) Color(0xFF1C1C20).copy(alpha = 0.55f) else Color.White.copy(alpha = 0.55f)
    val sheen = remember(c) {
        Brush.verticalGradient(0f to Color.White.copy(alpha = if (c.isDark) 0.10f else 0.5f), 0.55f to Color.Transparent)
    }
    val rim = remember(c) {
        val k = if (c.isDark) 0.4f else 1f
        Brush.linearGradient(
            0f to Color.White.copy(alpha = 0.9f * k),
            0.5f to Color.White.copy(alpha = 0.12f * k),
            1f to Color.White.copy(alpha = 0.5f * k),
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
private fun TabCapsule(items: List<TabItem>, selected: Int, onSelect: (Int) -> Unit, hazeState: HazeState?) {
    val c = OrganiTheme.colors
    val sel = selected.coerceIn(0, items.lastIndex)
    val activeX by animateDpAsState(Pad + (Chip + ChipGap) * sel, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow), label = "activeX")
    val activeFill = if (c.isDark) Color.White else Color(0xFF0A0A0B)
    val activeIcon = if (c.isDark) Color(0xFF0A0A0B) else Color.White

    Box(
        Modifier
            .width(capsuleWidth(items.size))
            .height(TabBarHeight)
            .liquidGlass(OrganiShapes.pill, hazeState)
            .selectableGroup(),
    ) {
        Box(
            Modifier
                .offset(x = activeX, y = (TabBarHeight - Chip) / 2)
                .size(Chip)
                .clip(OrganiShapes.circle)
                .background(activeFill),
        )
        items.forEachIndexed { i, item ->
            val isActive = i == sel
            Box(
                Modifier
                    .offset(x = Pad + (Chip + ChipGap) * i, y = (TabBarHeight - Chip) / 2)
                    .size(Chip)
                    .clip(OrganiShapes.circle)
                    .selectable(isActive, role = Role.Tab, onClick = { onSelect(i) })
                    .semantics { contentDescription = item.label },
                contentAlignment = Alignment.Center,
            ) {
                MsIcon(
                    if (isActive) item.iconSelected else item.icon,
                    null,
                    tint = if (isActive) activeIcon else c.text.copy(alpha = 0.62f),
                    size = 21.dp,
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
            .clickable(role = Role.Button, onClickLabel = if (open) "Schließen" else "Neuer Eintrag") {
                haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.ContextClick)
                onClick()
            }
            .semantics { contentDescription = if (open) "Neuer Eintrag schließen" else "Neuer Eintrag" },
        contentAlignment = Alignment.Center,
    ) {
        MsIcon(Ms.add, null, Modifier.rotate(rotation), tint = c.text, size = 26.dp)
    }
}

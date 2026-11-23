package app.organi7er.core.designsystem.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.organi7er.core.designsystem.OrganiShapes
import app.organi7er.core.designsystem.OrganiTheme
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.hazeBlur

data class TabItem(val label: String, @DrawableRes val icon: Int, @DrawableRes val iconSelected: Int)

val TabBarHeight: Dp = 60.dp

/** Schwebende Navy-Glas-Pille. Aktiver Reiter = weiße Pille mit Icon und Text. */
@Composable
fun FloatingTabBar(
    items: List<TabItem>,
    selected: Int,
    onSelect: (Int) -> Unit,
    hazeState: HazeState?,
    modifier: Modifier = Modifier,
) {
    val c = OrganiTheme.colors
    val shape = OrganiShapes.pill
    val style = remember {
        HazeBlurStyle {
            blurRadius(24.dp)
            noiseFactor(0f)
        }
    }
    Row(
        modifier
            .fillMaxWidth()
            .height(TabBarHeight)
            .floatShadow(shape, c.shadowFloat)
            .clip(shape)
            .then(if (hazeState != null) Modifier.hazeBlur(HazeInput.Sources(hazeState), style) else Modifier)
            .background(c.nav)
            .border(1.dp, Color(0x2493BBFD), shape)
            .padding(5.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEachIndexed { i, item -> Tab(item, i == selected) { onSelect(i) } }
    }
}

@Composable
private fun RowScope.Tab(item: TabItem, active: Boolean, onClick: () -> Unit) {
    val c = OrganiTheme.colors
    val weight by animateFloatAsState(if (active) 2f else 1f, tween(250), label = "tabWeight")
    val bg by animateColorAsState(if (active) Color.White else Color.Transparent, tween(250), label = "tabBg")
    val fg = if (active) Color(0xFF061B3A) else c.navIcon
    Row(
        Modifier
            .weight(weight)
            .fillMaxHeight()
            .then(if (active) Modifier.softShadow(OrganiShapes.pill, Color(0x2E000000)) else Modifier)
            .clip(OrganiShapes.pill)
            .background(bg)
            .selectable(active, role = Role.Tab, onClick = onClick)
            .semantics { contentDescription = item.label }
            .padding(horizontal = if (active) 12.dp else 4.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MsIcon(if (active) item.iconSelected else item.icon, null, tint = fg, size = 20.dp)
        if (active) {
            Text(item.label, style = OrganiTheme.type.label.copy(fontSize = OrganiTheme.type.rowMeta.fontSize), color = fg, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
        }
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

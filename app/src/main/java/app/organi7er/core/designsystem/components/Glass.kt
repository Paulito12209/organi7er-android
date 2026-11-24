package app.organi7er.core.designsystem.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.organi7er.core.designsystem.OrganiTheme
import app.organi7er.core.designsystem.Spacing

/*
 * Glas-Bausteine nach den Referenzen (Frosted Glass):
 *  - GlassPanel: große Glasfläche, Titel links, runder Glas-Knopf rechts.
 *  - InsetCard: fast deckende, helle Innenkarte in einer Glasfläche („Transactions“, „Account Value“).
 *  - LedgerRow: Zeile mit rundem Icon, Titel, Mittelspalte und farbigem Wert rechts.
 *  - WeekStrip + RangeNav: Wochentage als Kreise, darunter „‹ 24. – 30. Nov ›“.
 *  - ActionCircle: runder Aktionsknopf mit Beschriftung darunter („Pay · Add money · Transfer“).
 *  - StatPill: Kennzahl in Pillenform, ausgewählt dunkel getönt.
 */

object GlassShapes {
    val panel = RoundedCornerShape(32.dp)
    val inset = RoundedCornerShape(24.dp)
    val pill = RoundedCornerShape(percent = 50)
}

/**
 * Liquid-Glass für kleine Knöpfe und Pillen im Kopf (Einstellungen, Suche).
 * Licht fällt immer im 45°-Winkel von oben links ein – unabhängig von der Form –, damit runder
 * Knopf und breite Pille gleich beleuchtet aussehen:
 *  - Fläche: getönter Grund, darüber ein Glanz, der oben links beginnt und zur Mitte ausläuft.
 *  - Kante: oben links hell, in der Mitte fast unsichtbar, unten rechts ein schwacher Gegenschein.
 */
@Composable
fun Modifier.liquidGlass(shape: Shape = GlassShapes.pill): Modifier {
    val c = OrganiTheme.colors
    val base = if (c.isDark) c.glassChip else Color.Black.copy(alpha = 0.05f)
    val sheenTop = Color.White.copy(alpha = if (c.isDark) 0.10f else 0.55f)
    val edgeLight = Color.White.copy(alpha = if (c.isDark) 0.30f else 0.95f)
    val edgeMid = if (c.isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.04f)
    val edgeShade = if (c.isDark) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.10f)
    return this
        .clip(shape)
        .drawWithCache {
            // Gleicher Winkel für jede Form: Verlauf über ein Quadrat mit der mittleren Kantenlänge.
            val d = (size.width + size.height) / 2f
            val end = Offset(d, d)
            val sheen = Brush.linearGradient(0f to sheenTop, 0.55f to Color.Transparent, start = Offset.Zero, end = end)
            val edge = Brush.linearGradient(0f to edgeLight, 0.45f to edgeMid, 1f to edgeShade, start = Offset.Zero, end = Offset(size.width, size.height))
            val stroke = 1.dp.toPx()
            val outline = shape.createOutline(
                Size(size.width - stroke, size.height - stroke),
                layoutDirection,
                this,
            )
            onDrawBehind {
                drawRect(base)
                drawRect(sheen)
                translate(stroke / 2f, stroke / 2f) { drawOutline(outline, edge, style = Stroke(stroke)) }
            }
        }
}

/** Runder Glasknopf mit feiner Kante (Pfeil ↗, Schließen ×). */
@Composable
fun GlassCircleButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    filled: Boolean = false,
) {
    val c = OrganiTheme.colors
    Box(modifier.size(maxOf(size, Spacing.touch)), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(size)
                .then(
                    if (filled) {
                        Modifier.clip(GlassShapes.pill).background(c.glassSolid).border(1.dp, c.glassEdge, GlassShapes.pill)
                    } else {
                        Modifier.liquidGlass(GlassShapes.pill)
                    },
                )
                .clickable(role = Role.Button, onClickLabel = contentDescription, onClick = onClick)
                .semantics { this.contentDescription = contentDescription },
            contentAlignment = Alignment.Center,
        ) {
            MsIcon(icon, null, tint = if (filled) c.onGlassSolid else c.text, size = 18.dp)
        }
    }
}

/** Große Glasfläche mit Titel und optionalem Knopf oben rechts. */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    title: String? = null,
    @DrawableRes actionIcon: Int? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    contentPadding: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = OrganiTheme.colors
    Column(
        modifier
            .fillMaxWidth()
            .glass(GlassShapes.panel)
            .padding(contentPadding),
    ) {
        if (title != null || actionIcon != null) {
            Row(Modifier.fillMaxWidth().padding(start = 4.dp, bottom = Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title.orEmpty(),
                    Modifier.weight(1f).semantics { heading() },
                    style = OrganiTheme.type.panelTitle,
                    color = c.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (actionIcon != null && onAction != null) {
                    GlassCircleButton(actionIcon, actionLabel ?: title.orEmpty(), onAction)
                }
            }
        }
        content()
    }
}

/** Helle, fast deckende Innenkarte (sitzt in einer [GlassPanel] oder frei). */
@Composable
fun InsetCard(modifier: Modifier = Modifier, shape: Shape = GlassShapes.inset, padding: Dp = 16.dp, content: @Composable ColumnScope.() -> Unit) {
    val c = OrganiTheme.colors
    Column(
        modifier
            .fillMaxWidth()
            .softShadow(shape, c.shadowSoft)
            .clip(shape)
            .background(c.insetFill)
            .border(1.dp, c.glassEdge, shape)
            .padding(padding),
        content = content,
    )
}

/** Runder Icon-Halter wie in der Transaktionsliste. */
@Composable
fun LeadingCircle(modifier: Modifier = Modifier, fill: Color = OrganiTheme.colors.leadingFill, size: Dp = 40.dp, content: @Composable () -> Unit) {
    Box(
        modifier
            .size(size)
            .clip(GlassShapes.pill)
            .background(fill)
            .border(1.dp, OrganiTheme.colors.glassEdge, GlassShapes.pill),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** Zeile wie „ Apple Inc.   23.08.16   − $164.00“. */
@Composable
fun LedgerRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    middle: String? = null,
    trailing: String? = null,
    trailingColor: Color = OrganiTheme.colors.text,
    dimmed: Boolean = false,
    onClick: (() -> Unit)? = null,
    leading: @Composable () -> Unit,
    trailingContent: (@Composable RowScope.() -> Unit)? = null,
) {
    val c = OrganiTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 6.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        leading()
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = OrganiTheme.type.rowTitle,
                color = if (dimmed) c.textSoft else c.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) Text(subtitle, style = OrganiTheme.type.rowMeta, color = c.textSoft, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (!middle.isNullOrBlank()) {
            Text(middle, Modifier.widthIn(max = 72.dp), style = OrganiTheme.type.rowMeta, color = c.textSoft, maxLines = 1, textAlign = TextAlign.End)
        }
        if (!trailing.isNullOrBlank()) {
            Text(trailing, style = OrganiTheme.type.label.copy(fontSize = 14.sp), color = trailingColor, maxLines = 1)
        }
        trailingContent?.invoke(this)
    }
}

// ------------------------------------------------------------------ Woche

@Immutable
data class WeekDayUi(val offset: Int, val letter: String, val number: String, val busy: Boolean, val label: String)

/** Wochentage als Kreise: oben Kürzel, im Kreis die Zahl; gewählt = gefüllt, Punkt = hat Einträge. */
@Composable
fun WeekStrip(days: List<WeekDayUi>, selected: Int?, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.SpaceBetween) {
        days.forEach { d -> WeekDay(d, d.offset == selected) { onSelect(d.offset) } }
    }
}

@Composable
private fun WeekDay(day: WeekDayUi, active: Boolean, onClick: () -> Unit) {
    val c = OrganiTheme.colors
    val bg by animateColorAsState(if (active) c.glassSolid else c.glassChip, tween(200), label = "dayBg")
    val fg = if (active) c.onGlassSolid else c.text
    Column(
        Modifier
            .width(42.dp)
            .clip(GlassShapes.pill)
            .selectable(active, role = Role.Tab, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = day.label },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(day.letter, style = OrganiTheme.type.tileStat.copy(fontWeight = FontWeight.Normal), color = c.textSoft)
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .size(38.dp)
                .then(if (active) Modifier.softShadow(GlassShapes.pill, c.shadowSoft) else Modifier)
                .clip(GlassShapes.pill)
                .background(bg)
                .border(1.dp, c.glassEdge, GlassShapes.pill),
            contentAlignment = Alignment.Center,
        ) {
            Text(day.number, style = OrganiTheme.type.label.copy(fontSize = 14.sp), color = fg)
        }
        Box(
            Modifier
                .padding(top = 5.dp)
                .size(4.dp)
                .clip(GlassShapes.pill)
                .background(if (day.busy) c.accent else Color.Transparent),
        )
    }
}

/** „‹  Jun 6 – Jun 12  ›“ */
@Composable
fun RangeNav(label: String, onPrev: () -> Unit, onNext: () -> Unit, modifier: Modifier = Modifier, canPrev: Boolean = true, canNext: Boolean = true) {
    val c = OrganiTheme.colors
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        PlainIconButton(Ms.chevronLeft, "Zurück", onPrev, enabled = canPrev, iconSize = 16.dp, tint = c.textMuted)
        Text(
            label,
            Modifier.widthIn(min = 160.dp).semantics { liveRegion = LiveRegionMode.Polite },
            style = OrganiTheme.type.body,
            color = c.text,
            textAlign = TextAlign.Center,
        )
        PlainIconButton(Ms.chevronRight, "Weiter", onNext, enabled = canNext, iconSize = 16.dp, tint = c.textMuted)
    }
}

// ------------------------------------------------------------------ Aktionen

/** Runder Knopf mit Beschriftung darunter. `primary` = weiß gefüllt, sonst Glas. */
@Composable
fun ActionCircle(@DrawableRes icon: Int, label: String, onClick: () -> Unit, modifier: Modifier = Modifier, primary: Boolean = false) {
    val c = OrganiTheme.colors
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(GlassShapes.pill)
                .background(if (primary) c.glassSolid else c.glassChip)
                .border(1.dp, c.glassEdge, GlassShapes.pill),
            contentAlignment = Alignment.Center,
        ) {
            MsIcon(icon, null, tint = if (primary) c.onGlassSolid else c.text, size = 20.dp)
        }
        Text(label, style = OrganiTheme.type.rowMeta, color = c.textMuted, maxLines = 1, softWrap = false)
    }
}

// ------------------------------------------------------------------ Kennzahl

/** Kennzahl als Pille: kleine Beschriftung, großer Wert, kleiner Chip. Ausgewählt = dunkel getönt. */
@Composable
fun StatPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    chip: String? = null,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val c = OrganiTheme.colors
    val shape = RoundedCornerShape(36.dp)
    val fg = if (selected) Color.White else c.text
    val muted = if (selected) Color.White.copy(alpha = 0.72f) else c.textMuted
    Column(
        modifier
            .heightIn(min = 92.dp)
            .then(
                if (selected) {
                    Modifier
                        .softShadow(shape, c.shadowFloat)
                        .clip(shape)
                        .background(Brush.linearGradient(listOf(c.statSelectedA, c.statSelectedB)))
                        .border(1.dp, Color.White.copy(alpha = 0.28f), shape)
                } else {
                    Modifier.glass(shape)
                },
            )
            .then(if (onClick != null) Modifier.selectable(selected, role = Role.Tab, onClick = onClick) else Modifier)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
    ) {
        Text(label, style = OrganiTheme.type.caption, color = muted, maxLines = 1)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(value, Modifier.weight(1f, fill = false), style = OrganiTheme.type.statValue, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (chip != null) {
                Box(
                    Modifier
                        .clip(GlassShapes.pill)
                        .border(1.dp, fg.copy(alpha = 0.35f), GlassShapes.pill)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(chip, style = OrganiTheme.type.tileStat, color = fg, maxLines = 1)
                }
            }
        }
    }
}

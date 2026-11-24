package app.organi7er.core.designsystem.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.organi7er.core.designsystem.OrganiShapes
import app.organi7er.core.designsystem.OrganiTheme
import app.organi7er.core.designsystem.Spacing

private const val ANIM = 220

/** Runder Soft-UI-Knopf, 40 oder 48 dp (Tippfläche immer mind. 48 dp). */
@Composable
fun SoftIconButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    enabled: Boolean = true,
) {
    Box(modifier.size(maxOf(size, Spacing.touch)), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(size)
                .knob(OrganiShapes.circle)
                .clickable(enabled = enabled, role = Role.Button, onClickLabel = contentDescription, onClick = onClick)
                .semantics { this.contentDescription = contentDescription },
            contentAlignment = Alignment.Center,
        ) {
            MsIcon(icon, null, tint = OrganiTheme.colors.text.copy(alpha = if (enabled) 1f else 0.4f), size = 20.dp)
        }
    }
}

/** Dezenter runder Icon-Knopf ohne Fläche (z. B. Datumspfeile). */
@Composable
fun PlainIconButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = OrganiTheme.colors.textSoft,
    iconSize: Dp = 18.dp,
) {
    Box(
        modifier
            .size(Spacing.touch)
            .clip(OrganiShapes.circle)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        MsIcon(icon, null, tint = tint.copy(alpha = if (enabled) tint.alpha else 0.3f), size = iconSize)
    }
}

enum class PillVariant { Primary, Soft, Ghost, Dark, Accent }

@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: PillVariant = PillVariant.Primary,
    small: Boolean = false,
    @DrawableRes icon: Int? = null,
    enabled: Boolean = true,
) {
    val c = OrganiTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, tween(150), label = "press")
    val shape = OrganiShapes.pill
    val (bgMod, fg) = when (variant) {
        PillVariant.Primary -> if (enabled) {
            Modifier.softShadow(shape, c.primary.copy(alpha = 0.28f)).clip(shape).background(c.primary) to c.onPrimary
        } else {
            Modifier.clip(shape).background(c.surface2) to c.textSoft
        }
        PillVariant.Soft -> Modifier.knob(shape) to c.text
        PillVariant.Ghost -> Modifier.clip(shape).border(1.5.dp, c.border, shape) to c.text
        PillVariant.Dark -> Modifier.clip(shape).background(c.hero) to c.onHero
        PillVariant.Accent -> Modifier.clip(shape).background(c.accent) to c.onAccent
    }
    Row(
        modifier
            .heightIn(min = if (small) 36.dp else 48.dp)
            .scale(scale)
            .then(bgMod)
            .clickable(interaction, indication = androidx.compose.foundation.LocalIndication.current, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = if (small) 14.dp else 22.dp),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) MsIcon(icon, null, tint = fg, size = 18.dp)
        Text(text, style = if (small) OrganiTheme.type.buttonSmall else OrganiTheme.type.button, color = if (enabled || variant == PillVariant.Primary) fg else fg.copy(alpha = 0.45f), maxLines = 1)
    }
}

@Composable
fun TextLink(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .heightIn(min = Spacing.touch)
            .clip(OrganiShapes.pill)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = Spacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = OrganiTheme.type.buttonSmall, color = OrganiTheme.colors.link)
    }
}

enum class EntryType { Event, Task, Note }

@Composable
fun EntryType.fill(): Color = when (this) {
    EntryType.Event -> OrganiTheme.colors.typeEvent
    EntryType.Task -> OrganiTheme.colors.typeTask
    EntryType.Note -> OrganiTheme.colors.typeNote
}

@Composable
fun EntryType.onFill(): Color = when (this) {
    EntryType.Event -> OrganiTheme.colors.onTypeEvent
    EntryType.Task -> OrganiTheme.colors.onTypeTask
    EntryType.Note -> OrganiTheme.colors.onTypeNote
}

val EntryType.icon: Int get() = when (this) {
    EntryType.Event -> Ms.eventLight
    EntryType.Task -> Ms.taskAltLight
    EntryType.Note -> Ms.editNoteLight
}

val EntryType.newLabel: String get() = when (this) {
    EntryType.Event -> "Neuer Termin"
    EntryType.Task -> "Neue Aufgabe"
    EntryType.Note -> "Neue Notiz"
}

val EntryType.shortLabel: String get() = when (this) {
    EntryType.Event -> "Termin"
    EntryType.Task -> "Aufgabe"
    EntryType.Note -> "Notiz"
}

/** Runder 66-dp-Knopf in der Typfarbe mit dem Typ-Icon, Beschriftung darunter. */
@Composable
fun CaptureButton(type: EntryType, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = OrganiTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, tween(150), label = "capture")
    Column(
        modifier
            .clickable(interaction, indication = null, role = Role.Button, onClick = onClick)
            .padding(Spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Box(
            Modifier
                .size(66.dp)
                .scale(scale)
                .softShadow(OrganiShapes.circle, c.shadowSoft)
                .clip(OrganiShapes.circle)
                .background(type.fill()),
            contentAlignment = Alignment.Center,
        ) {
            MsIcon(type.icon, null, tint = type.onFill(), size = 24.dp)
        }
        Text(type.newLabel, style = OrganiTheme.type.rowMeta, color = c.textMuted, maxLines = 1, softWrap = false)
    }
}

@Composable
fun SoftSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier, contentDescription: String? = null) {
    val c = OrganiTheme.colors
    val track by animateColorAsState(if (checked) c.primary else c.surface2, tween(ANIM), label = "track")
    val offset by animateDpAsState(if (checked) 20.dp else 0.dp, tween(ANIM), label = "thumb")
    Box(
        modifier
            .size(width = 56.dp, height = Spacing.touch)
            .toggleable(checked, role = Role.Switch, onValueChange = onCheckedChange)
            .semantics { if (contentDescription != null) this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(width = 48.dp, height = 28.dp)
                .clip(OrganiShapes.pill)
                .background(track)
                .then(if (checked) Modifier else Modifier.border(1.5.dp, c.border, OrganiShapes.pill))
                .padding(3.dp),
        ) {
            Box(
                Modifier
                    .offset { androidx.compose.ui.unit.IntOffset(offset.roundToPx(), 0) }
                    .size(22.dp)
                    .softShadow(OrganiShapes.circle, Color(0x40061B3A))
                    .clip(OrganiShapes.circle)
                    .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.White, Color(0xFFE9EEF6)))),
            )
        }
    }
}

/** Rundes Soft-Häkchen (28 dp sichtbar, 48 dp Tippfläche). */
@Composable
fun CheckKnob(checked: Boolean, onToggle: () -> Unit, label: String, modifier: Modifier = Modifier) {
    val c = OrganiTheme.colors
    val bg by animateColorAsState(if (checked) c.primary else Color.Transparent, tween(ANIM), label = "check")
    val iconScale by animateFloatAsState(if (checked) 1f else 0.4f, tween(ANIM), label = "checkScale")
    Box(
        modifier
            .size(Spacing.touch)
            .clip(OrganiShapes.circle)
            .toggleable(checked, role = Role.Checkbox, onValueChange = { onToggle() })
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(28.dp)
                .then(if (checked) Modifier.softShadow(OrganiShapes.circle, c.primary.copy(alpha = 0.35f)) else Modifier.knob(OrganiShapes.circle))
                .clip(OrganiShapes.circle)
                .background(bg)
                .then(if (checked) Modifier else Modifier.border(1.5.dp, c.border, OrganiShapes.circle)),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) MsIcon(Ms.check, null, Modifier.scale(iconScale), tint = Color.White, size = 16.dp)
        }
    }
}

/** Segment-Steuerung: gewähltes Segment hebt sich als heller Knopf ab. */
@Composable
fun SegmentedControl(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val c = OrganiTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .clip(OrganiShapes.pill)
            .background(c.surface2)
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEachIndexed { i, label ->
            val isSel = i == selected
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp)
                    .then(if (isSel) Modifier.knob(OrganiShapes.pill) else Modifier.clip(OrganiShapes.pill))
                    .selectable(isSel, role = Role.Tab, onClick = { onSelect(i) }),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = OrganiTheme.type.label.copy(fontSize = OrganiTheme.type.buttonSmall.fontSize), color = if (isSel) c.text else c.textMuted, maxLines = 1)
            }
        }
    }
}

/** Suchfeld als Glas-Pille. */
@Composable
fun SearchField(value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    val c = OrganiTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .glass(OrganiShapes.pill, fill = c.glassStrong)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MsIcon(Ms.search, null, tint = c.textSoft, size = 18.dp)
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(placeholder, style = OrganiTheme.type.body, color = c.textSoft, maxLines = 1)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = OrganiTheme.type.body.copy(color = c.text),
                cursorBrush = SolidColor(c.brandBlue),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = placeholder },
            )
        }
        if (value.isNotEmpty()) PlainIconButton(Ms.close, "Suche leeren", { onValueChange("") }, iconSize = 18.dp)
    }
}

/** Filterkachel mit Zähler (gewählt = Navy). */
@Composable
fun FilterTile(label: String, count: Int, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = OrganiTheme.colors
    Column(
        modifier
            .heightIn(min = 64.dp)
            .then(if (selected) Modifier.clip(OrganiShapes.medium).background(c.hero) else Modifier.glass(OrganiShapes.medium))
            .selectable(selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = OrganiTheme.type.rowMeta, color = if (selected) c.onHeroMuted else c.textMuted, maxLines = 1)
        Text("$count", style = OrganiTheme.type.filterCount, color = if (selected) c.onHero else c.text)
    }
}

enum class TagTone { Blue, Yellow, Mute, Red, Navy }

@Composable
fun StatusTag(text: String, tone: TagTone, modifier: Modifier = Modifier) {
    val c = OrganiTheme.colors
    val (bg, fg) = when (tone) {
        TagTone.Blue -> c.brandBlue.copy(alpha = 0.16f) to c.link
        TagTone.Yellow -> c.accent to c.onAccent
        TagTone.Mute -> c.surface2 to c.textMuted
        TagTone.Red -> c.danger.copy(alpha = 0.15f) to c.dangerText
        TagTone.Navy -> c.text.copy(alpha = 0.10f) to c.text
    }
    Box(
        modifier
            .height(22.dp)
            .clip(OrganiShapes.pill)
            .background(bg)
            .padding(horizontal = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = OrganiTheme.type.tag, color = fg, maxLines = 1)
    }
}

@Composable
fun PriorityDot(modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(8.dp)
            .clip(OrganiShapes.circle)
            .background(OrganiTheme.colors.accent)
            .semantics { contentDescription = "Wichtig" },
    )
}

/** Datumstrenner aus dem Aldi-Logbuch: Datum mittig, links und rechts eine dünne Wellenlinie. */
@Composable
fun DateDivider(label: String, modifier: Modifier = Modifier) {
    val c = OrganiTheme.colors
    Row(
        modifier.fillMaxWidth().semantics(mergeDescendants = true) { heading() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Wave(Modifier.weight(1f), c.wave)
        Text(label, style = OrganiTheme.type.label.copy(fontSize = OrganiTheme.type.buttonSmall.fontSize), color = c.textSoft, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Wave(Modifier.weight(1f), c.wave)
    }
}

@Composable
private fun Wave(modifier: Modifier, color: Color) {
    Canvas(modifier.height(8.dp).clearAndSetSemantics { }) {
        val seg = 10.dp.toPx()
        val mid = size.height / 2f
        val path = Path().apply { moveTo(0f, mid) }
        var x = 0f
        var up = true
        while (x < size.width) {
            path.quadraticTo(x + seg / 2f, if (up) 0f else size.height, x + seg, mid)
            x += seg
            up = !up
        }
        drawPath(path, color, style = Stroke(width = 1.2.dp.toPx()))
    }
}

/** Kleine Legende „● Arbeit ● Privat …“ */
@Composable
fun Legend(items: List<Pair<String, Color>>, modifier: Modifier = Modifier) {
    val c = OrganiTheme.colors
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        items.forEach { (label, color) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(8.dp).clip(OrganiShapes.circle).background(color))
                Text(label, style = OrganiTheme.type.rowMeta, color = c.textMuted)
            }
        }
    }
}

/** Gelbe „Jetzt“-Linie mit Uhrzeit. */
@Composable
fun NowLine(time: String, modifier: Modifier = Modifier) {
    val c = OrganiTheme.colors
    // Hell: Navy mit weißer Zeit; dunkel: das helle Gegenstück (sehr helles Blau) mit dunkler Zeit.
    val line = if (c.isDark) app.organi7er.core.designsystem.Primitives.Blue100 else app.organi7er.core.designsystem.Primitives.Navy900
    val onLine = if (c.isDark) app.organi7er.core.designsystem.Primitives.Navy900 else Color.White
    Row(
        modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = "Jetzt, $time" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.width(44.dp).clip(OrganiShapes.pill).background(line).padding(vertical = 2.dp), contentAlignment = Alignment.Center) {
            Text(time, style = OrganiTheme.type.tag, color = onLine, textAlign = TextAlign.Center)
        }
        Box(Modifier.size(8.dp).clip(OrganiShapes.circle).background(line))
        Box(Modifier.weight(1f).height(2.dp).offset(x = (-4).dp).clip(OrganiShapes.pill).background(line))
    }
}


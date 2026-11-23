package app.organi7er.core.designsystem.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.organi7er.core.designsystem.OrganiShapes
import app.organi7er.core.designsystem.OrganiTheme
import app.organi7er.core.designsystem.Spacing
import coil3.compose.AsyncImage
import java.io.File

// ------------------------------------------------------------------ Briefing-Kachel

data class TileStats(val events: Int, val tasks: Int, val notes: Int)

/** Kachel im Format 16 : 9, 100 dp hoch (wächst bei großer Schrift mit). */
@Composable
fun BriefingTile(
    title: String,
    stats: TileStats,
    big: String,
    @DrawableRes bigIcon: Int,
    caption: String,
    progress: Float?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = OrganiTheme.colors
    val shape = OrganiShapes.medium
    val fg = if (selected) c.onTileSelected else c.text
    val muted = if (selected) c.onTileSelectedMuted else c.textMuted
    val soft = if (selected) c.onTileSelectedMuted else c.textSoft
    Column(
        modifier
            .width(178.dp)
            .heightIn(min = 100.dp)
            .then(
                if (selected) {
                    Modifier.tileShadow(shape, c.shadowSoft).clip(shape).background(c.tileSelected)
                } else {
                    Modifier.tileShadow(shape, c.shadowSoft.copy(alpha = c.shadowSoft.alpha * 0.6f)).glass(shape, shadow = false)
                },
            )
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics(mergeDescendants = true) {
                this.selected = selected
                contentDescription = "$title: ${stats.events} Termine, ${stats.tasks} Aufgaben, ${stats.notes} Notizen. $big. $caption"
            }
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Text(title, Modifier.weight(1f), style = OrganiTheme.type.tileTitle, color = fg, maxLines = 1)
            Row(Modifier.padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Stat(Ms.calendarToday, stats.events, muted)
                Stat(Ms.checklist, stats.tasks, muted)
                Stat(Ms.description, stats.notes, muted)
            }
        }
        Spacer(Modifier.height(Spacing.sm))
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MsIcon(bigIcon, null, tint = fg.copy(alpha = 0.8f), size = 15.dp)
                Text(big, style = OrganiTheme.type.tileBig, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(caption, style = OrganiTheme.type.tileCaption, color = soft, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (progress != null) {
                Box(
                    Modifier
                        .padding(top = 5.dp)
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(OrganiShapes.pill)
                        .background(if (selected) fg.copy(alpha = 0.18f) else c.text.copy(alpha = 0.12f)),
                ) {
                    Box(Modifier.fillMaxWidth(progress).fillMaxHeight().clip(OrganiShapes.pill).background(c.accent))
                }
            }
        }
    }
}

@Composable
private fun Stat(@DrawableRes icon: Int, count: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        MsIcon(icon, null, tint = color, size = 12.dp)
        Text("$count", style = OrganiTheme.type.tileStat, color = color)
    }
}

// ------------------------------------------------------------------ Termine

/** Große Navy-Karte „Als Nächstes“ mit blauem Lichtschein. */
@Composable
fun NextEventCard(
    timeRange: String,
    untilLabel: String,
    title: String,
    location: String,
    duration: String,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    tag: String? = null,
) {
    val c = OrganiTheme.colors
    val shape = OrganiShapes.card
    Box(
        modifier
            .fillMaxWidth()
            .softShadow(shape, c.shadowSoft)
            .clip(shape)
            .background(c.hero)
            .drawBehind {
                drawCircle(
                    Brush.radialGradient(listOf(Color(0xA63B82F6), Color.Transparent), center = Offset(size.width - 60.dp.toPx(), 50.dp.toPx()), radius = 120.dp.toPx()),
                    radius = 120.dp.toPx(),
                    center = Offset(size.width - 60.dp.toPx(), 50.dp.toPx()),
                )
                drawCircle(
                    Brush.radialGradient(listOf(Color(0x4060A5FA), Color.Transparent), center = Offset(50.dp.toPx(), size.height + 10.dp.toPx()), radius = 100.dp.toPx()),
                    radius = 100.dp.toPx(),
                    center = Offset(50.dp.toPx(), size.height + 10.dp.toPx()),
                )
            }
            .clickable(role = Role.Button, onClickLabel = "Termin öffnen", onClick = onOpen)
            .padding(18.dp),
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(timeRange, Modifier.weight(1f), style = OrganiTheme.type.label.copy(fontWeight = FontWeight.Normal), color = c.onHeroMuted)
                if (tag != null) StatusTag(tag, TagTone.Mute, Modifier.padding(end = 6.dp))
                StatusTag(untilLabel, TagTone.Yellow)
            }
            Spacer(Modifier.height(22.dp))
            Text(title, style = OrganiTheme.type.headline, color = c.onHero, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (location.isNotBlank()) Meta(Ms.locationOn, location, c.onHeroMuted, Modifier.weight(1f, fill = false))
                    Meta(Ms.schedule, duration, c.onHeroMuted)
                }
                Box(
                    Modifier
                        .padding(start = Spacing.sm)
                        .size(44.dp)
                        .clip(OrganiShapes.circle)
                        .background(Color.White.copy(alpha = 0.14f))
                        .border(1.dp, Color.White.copy(alpha = 0.22f), OrganiShapes.circle),
                    contentAlignment = Alignment.Center,
                ) {
                    MsIcon(Ms.arrowOutward, null, tint = Color.White, size = 18.dp)
                }
            }
        }
    }
}

@Composable
private fun Meta(@DrawableRes icon: Int, text: String, color: Color, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        MsIcon(icon, null, tint = color, size = 14.dp)
        Text(text, style = OrganiTheme.type.label.copy(fontWeight = FontWeight.Normal), color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Terminzeile: Uhrzeit, farbiger Balken in Kalenderfarbe, Titel, Ort. Für [SegmentedList]. */
@Composable
fun EventRow(
    time: String,
    timeSub: String,
    barColor: Color,
    title: String,
    meta: String,
    index: Int,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val c = OrganiTheme.colors
    Row(
        modifier
            .segmentItem(index, count)
            .clickable(onClick = onClick)
            .height(IntrinsicSize.Min)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.width(50.dp)) {
            Text(time, style = OrganiTheme.type.label.copy(fontSize = 14.sp), color = c.text, maxLines = 1)
            if (timeSub.isNotBlank()) Text(timeSub, style = OrganiTheme.type.tileStat.copy(fontWeight = FontWeight.Normal), color = c.textSoft, maxLines = 1)
        }
        Box(Modifier.width(3.dp).fillMaxHeight().clip(OrganiShapes.pill).background(barColor))
        Column(Modifier.weight(1f)) {
            Text(title, style = OrganiTheme.type.rowTitle, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (meta.isNotBlank()) Text(meta, style = OrganiTheme.type.rowMeta, color = c.textSoft, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        trailing?.invoke(this)
    }
}

/** Termin in seiner Kalenderfarbe: getönter Hintergrund + farbiger linker Rand. */
@Composable
fun CalendarEventItem(
    color: Color,
    title: String,
    meta: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tag: String? = null,
    dimmed: Boolean = false,
) {
    val c = OrganiTheme.colors
    val tint = remember(color, c) { color.copy(alpha = if (c.isDark) 0.18f else 0.13f).compositeOver(c.surface) }
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(OrganiShapes.medium)
            .background(tint)
            .drawBehind { drawRect(color, size = Size(3.dp.toPx(), size.height)) }
            .clickable(onClick = onClick)
            .padding(start = 17.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    title,
                    Modifier.weight(1f, fill = false),
                    style = OrganiTheme.type.rowTitle.copy(fontSize = 14.5.sp, textDecoration = if (dimmed) TextDecoration.LineThrough else null),
                    color = if (dimmed) c.textSoft else c.text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (tag != null) StatusTag(tag, TagTone.Mute)
            }
            if (meta.isNotBlank()) Text(meta, style = OrganiTheme.type.rowMeta, color = c.textSoft, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ------------------------------------------------------------------ Aufgaben

@Composable
fun TaskRow(
    title: String,
    meta: String,
    done: Boolean,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    index: Int,
    count: Int,
    modifier: Modifier = Modifier,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val c = OrganiTheme.colors
    Row(
        modifier
            .segmentItem(index, count)
            .clickable(onClickLabel = "Bearbeiten", onClick = onClick)
            .padding(start = 6.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        CheckKnob(done, onToggle, if (done) "$title, erledigt" else "$title als erledigt markieren")
        Column(Modifier.weight(1f).padding(start = 4.dp)) {
            Text(
                title,
                style = OrganiTheme.type.rowTitle.copy(textDecoration = if (done) TextDecoration.LineThrough else null),
                color = if (done) c.textSoft else c.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (meta.isNotBlank()) Text(meta, style = OrganiTheme.type.rowMeta, color = c.textSoft, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        trailing?.invoke(this)
    }
}

/** Entwurf: Titel, „vor 3 Tagen“, Knopf „Planen“. */
@Composable
fun DraftRow(title: String, meta: String, onPlan: (() -> Unit)?, onClick: () -> Unit, index: Int, count: Int, modifier: Modifier = Modifier) {
    val c = OrganiTheme.colors
    Row(
        modifier
            .segmentItem(index, count)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = OrganiTheme.type.rowTitle.copy(fontWeight = FontWeight.Normal), color = c.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (meta.isNotBlank()) Text(meta, style = OrganiTheme.type.rowMeta, color = c.textSoft, maxLines = 1)
        }
        if (onPlan != null) PillButton("Planen", onPlan, variant = PillVariant.Soft, small = true)
    }
}

// ------------------------------------------------------------------ Notizen

/** Notizkarte im Logbuch-Muster: links Bilderstapel oder Platzhalter, rechts liniertes Blatt. */
@Composable
fun NoteCard(
    title: String,
    preview: String,
    imagePath: String?,
    imageCount: Int,
    pill: String?,
    onClick: () -> Unit,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = OrganiTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .glass(OrganiShapes.card)
            .clickable(onClickLabel = "Notiz öffnen", onClick = onClick)
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.weight(1f).aspectRatio(1f / 1.3f).clip(OrganiShapes.medium)) {
            if (imagePath != null) {
                AsyncImage(File(imagePath), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Row(
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .height(24.dp)
                        .clip(OrganiShapes.pill)
                        .background(Color(0x8C061B3A))
                        .padding(horizontal = 9.dp)
                        .semantics(mergeDescendants = true) { contentDescription = "$imageCount Bilder" },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    MsIcon(Ms.image, null, tint = Color.White, size = 12.dp)
                    Text("$imageCount", style = OrganiTheme.type.tileStat, color = Color.White)
                }
            } else {
                EmptyMedia(onCamera, onGallery)
            }
        }
        Box(Modifier.weight(1f).aspectRatio(1f / 1.3f)) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(start = 5.dp, bottom = 6.dp)
                    .rotate(1.6f)
                    .clip(OrganiShapes.paper)
                    .background(c.paper)
                    .border(1.dp, c.border, OrganiShapes.paper),
            )
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(top = 6.dp, end = 5.dp)
                    .clip(OrganiShapes.paper)
                    .background(c.paper)
                    .border(1.dp, c.border, OrganiShapes.paper)
                    .paperLines(c.paperLine, c.paperMargin, lineSpacing = 20.dp, marginX = 13.dp, firstLine = 19.dp)
                    .padding(start = 20.dp, end = 8.dp),
            ) {
                val small = OrganiTheme.type.caption.copy(fontSize = 11.5.sp, lineHeight = 20.sp)
                Text(title.ifBlank { "Ohne Titel" }, style = small.copy(fontWeight = FontWeight.SemiBold), color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(preview, style = small, color = c.textMuted, maxLines = 6, overflow = TextOverflow.Ellipsis)
            }
            if (pill != null) {
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 12.dp, bottom = 6.dp)
                        .height(22.dp)
                        .clip(OrganiShapes.pill)
                        .background(c.hero)
                        .padding(horizontal = 9.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(pill, style = OrganiTheme.type.tileCaption.copy(fontWeight = FontWeight.Medium), color = c.onHero, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun EmptyMedia(onCamera: () -> Unit, onGallery: () -> Unit) {
    val c = OrganiTheme.colors
    Column(
        Modifier
            .fillMaxSize()
            .background(c.brandBlue.copy(alpha = 0.07f))
            .drawBehind {
                drawRoundRect(
                    c.brandBlue.copy(alpha = 0.55f),
                    cornerRadius = CornerRadius(20.dp.toPx()),
                    style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx()))),
                )
            }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
    ) {
        Box(
            Modifier
                .size(42.dp)
                .knob(OrganiShapes.circle)
                .clickable(role = Role.Button, onClick = onCamera)
                .semantics { contentDescription = "Foto aufnehmen" },
            contentAlignment = Alignment.Center,
        ) { MsIcon(Ms.photoCamera, null, tint = c.text, size = 20.dp) }
        Text("Foto aufnehmen", style = OrganiTheme.type.caption, color = c.textMuted, maxLines = 1)
        Text(
            "oder aus Galerie",
            Modifier
                .clip(OrganiShapes.pill)
                .clickable(role = Role.Button, onClick = onGallery)
                .padding(horizontal = 6.dp, vertical = 4.dp),
            style = OrganiTheme.type.caption.copy(textDecoration = TextDecoration.Underline),
            color = c.textMuted,
            maxLines = 1,
        )
    }
}

/** Notizblatt: blaue Randlinie und Linien im festen Abstand. */
fun Modifier.paperLines(
    line: Color,
    margin: Color,
    lineSpacing: androidx.compose.ui.unit.Dp,
    marginX: androidx.compose.ui.unit.Dp,
    firstLine: androidx.compose.ui.unit.Dp,
): Modifier = drawBehind {
    val step = lineSpacing.toPx()
    var y = firstLine.toPx()
    while (y < size.height) {
        drawLine(line, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
        y += step
    }
    val x = marginX.toPx()
    drawLine(margin, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.dp.toPx())
}

// ------------------------------------------------------------------ Einstellungen

@Composable
fun SettingsRow(
    @DrawableRes icon: Int,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String?,
    index: Int,
    count: Int,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val c = OrganiTheme.colors
    Row(
        modifier
            .segmentItem(index, count, minHeight = 56.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Box(Modifier.size(34.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(11.dp)).background(iconBg), contentAlignment = Alignment.Center) {
            MsIcon(icon, null, tint = iconTint, size = 18.dp)
        }
        Column(Modifier.weight(1f).widthIn(min = 0.dp)) {
            Text(title, style = OrganiTheme.type.rowTitle, color = c.text)
            if (subtitle != null) Text(subtitle, style = OrganiTheme.type.rowMeta, color = c.textSoft)
        }
        trailing()
    }
}

@Composable
fun SettingsValue(text: String) {
    val c = OrganiTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        if (text.isNotEmpty()) Text(text, style = OrganiTheme.type.buttonSmall.copy(fontWeight = FontWeight.Normal), color = c.textSoft)
        MsIcon(Ms.chevronRight, null, tint = c.textSoft, size = 16.dp)
    }
}

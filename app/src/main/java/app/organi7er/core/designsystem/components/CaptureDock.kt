package app.organi7er.core.designsystem.components

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.organi7er.core.model.Dates
import app.organi7er.core.designsystem.OrganiShapes
import app.organi7er.core.designsystem.OrganiTheme
import app.organi7er.core.designsystem.Primitives
import app.organi7er.core.designsystem.Spacing
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.hazeBlur
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Locale
import kotlinx.coroutines.delay

/** Was das schnelle Erfassen liefert: Typ, ein Text (erste Zeile = Titel), optional Zeitpunkt, Fokus. */
data class QuickEntry(val type: EntryType, val text: String, val at: LocalDateTime?, val focused: Boolean)

/** Erste Zeile des Texts = Titel. */
val QuickEntry.title: String get() = text.trim().lines().firstOrNull().orEmpty().trim()

/** Alle weiteren Zeilen = Notiz bzw. Text. */
val QuickEntry.rest: String get() = text.trim().lines().drop(1).joinToString("\n").trim()

/** Reihenfolge der Typen im Composer. */
private val Types = listOf(EntryType.Event, EntryType.Task, EntryType.Note)

/** Mindesthöhe des eingeklappten Docks (ohne Begrüßung, ohne Navigationsleisten-Abstand). */
val DockMinHeight = 94.dp

private val DockShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)

private fun EntryType.placeholder(): String = when (this) {
    EntryType.Event -> "Neuer Termin"
    EntryType.Task -> "Neue Aufgabe"
    EntryType.Note -> "Neue Notiz"
}

// ------------------------------------------------------------------ Dock

/**
 * Unten angedockte Karte statt Navigationsleiste: Begrüßung („Guten Morgen, Paul“) und das Eingabefeld
 * mit Kamera links und Diktat rechts. Antippen oder Hochziehen öffnet den [QuickComposer].
 * Beim Scrollen nach unten verschwindet die Begrüßung ([showGreeting]).
 */
@Composable
fun CaptureDock(
    type: EntryType,
    onOpen: () -> Unit,
    onCamera: () -> Unit,
    onDictate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = OrganiTheme.colors
    val threshold = with(LocalDensity.current) { 24.dp.toPx() }
    Column(
        modifier
            .fillMaxWidth()
            .shadow(18.dp, DockShape, ambientColor = Color.Black.copy(alpha = 0.18f), spotColor = Color.Black.copy(alpha = 0.18f))
            .clip(DockShape)
            .background(c.surface)
            .pointerInput(onOpen) {
                var dy = 0f
                detectVerticalDragGestures(
                    onDragStart = { dy = 0f },
                    onDragEnd = { if (dy < -threshold) onOpen() },
                    onVerticalDrag = { _, d -> dy += d },
                )
            }
            .navigationBarsPadding()
            .padding(horizontal = Spacing.page)
            .padding(top = 10.dp, bottom = 12.dp),
    ) {
        Box(
            Modifier
                .align(Alignment.CenterHorizontally)
                .size(width = 36.dp, height = 4.dp)
                .clip(OrganiShapes.pill)
                .background(c.textSoft.copy(alpha = 0.35f)),
        )
        Row(
            Modifier
                .padding(top = 12.dp)
                .fillMaxWidth()
                .height(56.dp)
                .clip(OrganiShapes.pill)
                .background(c.surface2)
                .clickable(role = Role.Button, onClickLabel = "Neuen Eintrag erfassen", onClick = onOpen),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DockIcon(Ms.photoCamera, "Foto-Notiz aufnehmen", onCamera, Modifier.padding(start = 4.dp))
            Text(type.placeholder(), Modifier.weight(1f).padding(horizontal = 6.dp), style = OrganiTheme.type.body.copy(fontSize = 17.sp), color = c.textSoft, maxLines = 1)
            DockIcon(Ms.graphicEq, "Diktieren", onDictate, Modifier.padding(end = 4.dp))
        }
    }
}

@Composable
private fun DockIcon(icon: Int, description: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(Spacing.touch)
            .clip(OrganiShapes.circle)
            .clickable(role = Role.Button, onClickLabel = description, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) { MsIcon(icon, null, tint = OrganiTheme.colors.text, size = 24.dp) }
}

// ------------------------------------------------------------------ Diktat

/** Startet die Spracheingabe des Geräts; das erkannte Wort landet in [onResult]. */
@Composable
fun rememberDictation(onResult: (String) -> Unit, onUnavailable: () -> Unit): () -> Unit {
    val result by rememberUpdatedState(onResult)
    val unavailable by rememberUpdatedState(onUnavailable)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val text = r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (r.resultCode == Activity.RESULT_OK && !text.isNullOrBlank()) result(text)
    }
    return remember(launcher) {
        {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            try {
                launcher.launch(intent)
            } catch (_: ActivityNotFoundException) {
                unavailable()
            }
        }
    }
}

// ------------------------------------------------------------------ Composer

/**
 * Hochgeklapptes Eingabeblatt über der Tastatur: oben Termin · Aufgabe · Notiz und ×, darunter der Text,
 * unten + (Details), Liste, Erinnerung, Fokus, rechts Diktat und Senden.
 */
@Composable
fun QuickComposer(
    visible: Boolean,
    type: EntryType,
    onTypeChange: (EntryType) -> Unit,
    startDictation: Boolean,
    onDismiss: () -> Unit,
    onSave: (QuickEntry) -> Unit,
    onDetails: (QuickEntry) -> Unit,
    onMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
) {
    val c = OrganiTheme.colors
    BackHandler(visible, onDismiss)
    Box(modifier.fillMaxSize()) {
        AnimatedVisibility(visible, enter = fadeIn(tween(180)), exit = fadeOut(tween(160))) {
            Box(
                Modifier
                    .fillMaxSize()
                    .then(
                        if (hazeState != null) {
                            Modifier.hazeBlur(HazeInput.Sources(hazeState), remember { HazeBlurStyle { blurRadius(28.dp); noiseFactor(0f) } })
                        } else {
                            Modifier
                        },
                    )
                    .background(if (c.isDark) Color(0x99000000) else Color(0x66FFFFFF))
                    .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
            )
        }
        AnimatedVisibility(
            visible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(tween(240)) { it / 2 } + fadeIn(tween(160)),
            exit = slideOutVertically(tween(200)) { it / 2 } + fadeOut(tween(140)),
        ) {
            ComposerCard(type, onTypeChange, startDictation, onDismiss, onSave, onDetails, onMessage)
        }
    }
}

@Composable
private fun ComposerCard(
    type: EntryType,
    onTypeChange: (EntryType) -> Unit,
    startDictation: Boolean,
    onDismiss: () -> Unit,
    onSave: (QuickEntry) -> Unit,
    onDetails: (QuickEntry) -> Unit,
    onMessage: (String) -> Unit,
) {
    val c = OrganiTheme.colors
    val haptics = LocalHapticFeedback.current
    val askNotifications = rememberNotificationPermission()
    var field by remember { mutableStateOf(TextFieldValue("")) }
    var at by remember { mutableStateOf<LocalDateTime?>(null) }
    var focused by remember { mutableStateOf(false) }
    var picker by remember { mutableStateOf<String?>(null) }
    val focus = remember { FocusRequester() }
    val shape = RoundedCornerShape(32.dp)
    val canSend = field.text.isNotBlank()

    val dictate = rememberDictation(
        onResult = { spoken ->
            val sep = if (field.text.isBlank() || field.text.endsWith(" ") || field.text.endsWith("\n")) "" else " "
            val t = field.text + sep + spoken
            field = TextFieldValue(t, TextRange(t.length))
        },
        onUnavailable = { onMessage("Diktat ist auf diesem Gerät nicht verfügbar") },
    )
    LaunchedEffect(Unit) {
        if (startDictation) {
            dictate()
        } else {
            delay(80)
            runCatching { focus.requestFocus() }
        }
    }

    val entry = { QuickEntry(type, field.text.trim(), at, focused) }

    Column(
        Modifier
            .imePadding()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp)
            .fillMaxWidth()
            .shadow(24.dp, shape, ambientColor = Color.Black.copy(alpha = 0.22f), spotColor = Color.Black.copy(alpha = 0.22f))
            .clip(shape)
            .background(c.surface)
            .border(1.dp, c.border.copy(alpha = 0.5f), shape)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Kopf: Typ wählen, × zum Schließen.
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Types.forEach { t -> TypeChip(t, t == type) { onTypeChange(t) } }
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .size(44.dp)
                    .clip(OrganiShapes.circle)
                    .background(c.surface2)
                    .clickable(role = Role.Button, onClickLabel = "Schließen", onClick = onDismiss)
                    .semantics { contentDescription = "Schließen" },
                contentAlignment = Alignment.Center,
            ) { MsIcon(Ms.close, null, tint = c.text, size = 20.dp) }
        }

        // Text; erste Zeile wird der Titel, der Rest die Notiz.
        Box(Modifier.fillMaxWidth().heightIn(min = 88.dp).padding(horizontal = 4.dp)) {
            if (field.text.isEmpty()) Text(type.placeholder(), style = OrganiTheme.type.headline.copy(fontSize = 20.sp), color = c.textSoft)
            BasicTextField(
                value = field,
                onValueChange = { field = it },
                textStyle = OrganiTheme.type.headline.copy(fontSize = 20.sp, color = c.text),
                cursorBrush = SolidColor(c.brandBlue),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth().focusRequester(focus).semantics { contentDescription = type.placeholder() },
            )
        }

        AnimatedVisibility(at != null) {
            val value = at
            if (value != null) {
                Row(
                    Modifier
                        .clip(OrganiShapes.pill)
                        .background(c.surface2)
                        .clickable(role = Role.Button, onClickLabel = "Erinnerung entfernen") { at = null }
                        .padding(start = 12.dp, end = 6.dp)
                        .heightIn(min = 36.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    MsIcon(Ms.notifications, null, tint = c.text, size = 16.dp)
                    Text(
                        "${Dates.relativeDay(value.toLocalDate(), LocalDate.now()).replaceFirstChar(Char::uppercase)}, ${Dates.time(value)}",
                        style = OrganiTheme.type.label,
                        color = c.text,
                    )
                    MsIcon(Ms.close, null, tint = c.textSoft, size = 16.dp)
                }
            }
        }

        // Werkzeugzeile: + · Liste · Erinnerung · Fokus … Diktat · Senden.
        Row(verticalAlignment = Alignment.CenterVertically) {
            ToolIcon(Ms.add, "Mit Details öffnen") { if (canSend) onDetails(entry()) else onDetails(entry().copy(text = "")) }
            ToolIcon(Ms.formatListBulleted, "Aufzählung einfügen") { field = withBullet(field) }
            ToolIcon(Ms.notifications, "Erinnerung", tint = if (at != null) c.brandBlue else c.text) { picker = "date" }
            ToolIcon(if (focused) Ms.starFilled else Ms.star, if (focused) "Nicht mehr im Fokus" else "In den Fokus", tint = if (focused) Primitives.Yellow600 else c.text) { focused = !focused }
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .size(width = 64.dp, height = 44.dp)
                    .clip(OrganiShapes.pill)
                    .background(c.surface2)
                    .clickable(role = Role.Button, onClickLabel = "Diktieren", onClick = dictate)
                    .semantics { contentDescription = "Diktieren" },
                contentAlignment = Alignment.Center,
            ) { MsIcon(Ms.graphicEq, null, tint = c.text, size = 22.dp) }
            Spacer(Modifier.width(8.dp))
            val sendFill = if (c.isDark) Color.White else Color(0xFF0A0A0B)
            val sendOn = if (c.isDark) Color(0xFF0A0A0B) else Color.White
            Box(
                Modifier
                    .size(44.dp)
                    .clip(OrganiShapes.circle)
                    .background(if (canSend) sendFill else c.surface2)
                    .clickable(enabled = canSend, role = Role.Button, onClickLabel = "Speichern") {
                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                        onSave(entry())
                    }
                    .semantics { contentDescription = "Speichern" },
                contentAlignment = Alignment.Center,
            ) { MsIcon(Ms.arrowUpward, null, tint = if (canSend) sendOn else c.textSoft, size = 22.dp) }
        }
    }

    when (picker) {
        "date" -> DatePickerSheet(at?.toLocalDate() ?: LocalDate.now(), { picker = null }, { d ->
            at = d.atTime(at?.toLocalTime() ?: LocalTime.of(9, 0))
            picker = "time"
        }, minDate = LocalDate.now())
        "time" -> TimePickerSheet(at?.toLocalTime() ?: LocalTime.of(9, 0), { picker = null }) { t ->
            at = (at?.toLocalDate() ?: LocalDate.now()).atTime(t)
            picker = null
            askNotifications()
        }
    }
}

/** Der gewählte Typ zeigt Symbol und Namen, die anderen nur das Symbol. */
@Composable
private fun TypeChip(type: EntryType, active: Boolean, onClick: () -> Unit) {
    val c = OrganiTheme.colors
    val fill = if (c.isDark) Color.White else Color(0xFF0A0A0B)
    val on = if (c.isDark) Color(0xFF0A0A0B) else Color.White
    Row(
        Modifier
            .height(44.dp)
            .clip(OrganiShapes.pill)
            .background(if (active) fill else c.surface2)
            .clickable(role = Role.Tab, onClickLabel = type.newLabel, onClick = onClick)
            .padding(horizontal = if (active) 14.dp else 13.dp)
            .semantics { contentDescription = type.shortLabel },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MsIcon(type.icon, null, tint = if (active) on else c.text, size = 20.dp)
        if (active) Text(type.shortLabel, style = OrganiTheme.type.label.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium), color = on, maxLines = 1)
    }
}

@Composable
private fun ToolIcon(icon: Int, description: String, tint: Color = OrganiTheme.colors.text, onClick: () -> Unit) {
    Box(
        Modifier
            .size(Spacing.touch)
            .clip(OrganiShapes.circle)
            .clickable(role = Role.Button, onClickLabel = description, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) { MsIcon(icon, null, tint = tint, size = 24.dp) }
}

/** Fügt „• “ am Anfang der aktuellen Zeile ein (in einer neuen Zeile, falls der Cursor mitten im Text steht). */
internal fun withBullet(v: TextFieldValue): TextFieldValue {
    val pos = v.selection.start.coerceIn(0, v.text.length)
    val before = v.text.substring(0, pos)
    val after = v.text.substring(pos)
    val insert = if (before.isEmpty() || before.endsWith("\n")) "• " else "\n• "
    return TextFieldValue(before + insert + after, TextRange(pos + insert.length))
}

package app.organi7er.core.designsystem.components

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.organi7er.core.designsystem.OrganiShapes
import app.organi7er.core.designsystem.OrganiTheme
import app.organi7er.core.designsystem.Spacing
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.hazeBlur
import java.time.LocalDateTime
import java.util.Locale

/** Was das schnelle Erfassen liefert: Typ, ein Text (erste Zeile = Titel), optional Zeitpunkt, Fokus. */
data class QuickEntry(val type: EntryType, val text: String, val at: LocalDateTime?, val focused: Boolean)

/** Erste Zeile des Texts = Titel. */
val QuickEntry.title: String get() = text.trim().lines().firstOrNull().orEmpty().trim()

/** Alle weiteren Zeilen = Notiz bzw. Text. */
val QuickEntry.rest: String get() = text.trim().lines().drop(1).joinToString("\n").trim()

/** Reihenfolge der Typen im Dock. */
private val Types = listOf(EntryType.Event, EntryType.Task, EntryType.Note)

/** Gesamthöhe des Docks ohne Navigationsleisten-Abstand: Unschärfe-Band, Typ-Zeile, Eingabezeile. */
val DockMinHeight = 100.dp

/** Höhe des Unschärfe-Bands direkt über dem Dock. */
private val BlurBandHeight = 36.dp

private fun EntryType.placeholder(): String = when (this) {
    EntryType.Event -> "Neuer Termin"
    EntryType.Task -> "Neue Aufgabe"
    EntryType.Note -> "Neue Notiz"
}

// ------------------------------------------------------------------ Dock

/**
 * Unten angedockte Eingabe wie bei Gemini: Typ-Zeile, dann eine Pille mit Kamera links, Texteingabe
 * und Wellen-Knopf rechts. Kein Blatt mehr: Die Tastatur schiebt das Dock einfach nach oben.
 * Der Wellen-Knopf diktiert, „Senden“ auf der Tastatur speichert. Über dem Dock liegt ein
 * weicher Ausblend-Verlauf, der bei offener Tastatur schwächer wird. Der Typ folgt dem Reiter.
 */
@Composable
fun CaptureDock(
    type: EntryType,
    onSave: (QuickEntry) -> Unit,
    onCamera: () -> Unit,
    onMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = OrganiTheme.colors
    val haptics = LocalHapticFeedback.current
    var field by remember { mutableStateOf(TextFieldValue("")) }
    val focus = remember { FocusRequester() }
    val imeVisible = WindowInsets.isImeVisible
    val canSend = field.text.isNotBlank()

    val dictate = rememberDictation(
        onResult = { spoken ->
            val sep = if (field.text.isBlank() || field.text.endsWith(" ") || field.text.endsWith("\n")) "" else " "
            val t = field.text + sep + spoken
            field = TextFieldValue(t, TextRange(t.length))
        },
        onUnavailable = { onMessage("Diktat ist auf diesem Gerät nicht verfügbar") },
    )

    val entry = { QuickEntry(type, field.text.trim(), null, false) }
    val save = {
        if (canSend) {
            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            onSave(entry())
            field = TextFieldValue("")
        }
    }

    // Weiches Ausblenden des Inhalts über dem Dock, ohne Kante; bei offener Tastatur schwächer.
    val bandAlpha by animateFloatAsState(if (imeVisible) 0.15f else 0.6f, tween(250), label = "dockFade")
    val bandTint = Brush.verticalGradient(0f to Color.Transparent, 0.3f to c.bg.copy(alpha = bandAlpha), 1f to c.bg.copy(alpha = bandAlpha))

    Column(
        modifier
            .fillMaxWidth()
            .background(bandTint)
            // Tastatur oder Navigationsleiste, je nachdem, was höher ist: das Dock sitzt immer darüber.
            .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars)),
    ) {
        Spacer(Modifier.fillMaxWidth().height(BlurBandHeight))
        Column(Modifier.padding(horizontal = 12.dp).padding(bottom = 8.dp)) {
            // Eingabe-Pille: Kamera · Text · Wellen-Knopf (weiß, allein rechts).
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(OrganiShapes.pill)
                    .background(c.surface2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DockIcon(Ms.photoCamera, "Foto-Notiz aufnehmen", onCamera, Modifier.padding(start = 4.dp))
                BasicTextField(
                    value = field,
                    onValueChange = { field = it },
                    singleLine = true,
                    textStyle = OrganiTheme.type.body.copy(fontSize = 17.sp, color = c.text),
                    cursorBrush = SolidColor(c.text),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { save() }),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp)
                        .focusRequester(focus)
                        .semantics { contentDescription = type.placeholder() },
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (field.text.isEmpty()) {
                                Text(type.placeholder(), style = OrganiTheme.type.body.copy(fontSize = 17.sp), color = c.textSoft, maxLines = 1)
                            }
                            inner()
                        }
                    },
                )
                // Wellen-Symbol: einziger Sprach-Knopf, nur das Symbol in Weiß, ohne Kreis.
                Box(
                    Modifier
                        .padding(end = 6.dp)
                        .size(44.dp)
                        .clip(OrganiShapes.circle)
                        .clickable(role = Role.Button, onClickLabel = "Diktieren", onClick = dictate)
                        .semantics { contentDescription = "Diktieren" },
                    contentAlignment = Alignment.Center,
                ) { MsIcon(Ms.graphicEq, null, tint = if (c.isDark) Color.White else c.text, size = 26.dp) }
            }
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

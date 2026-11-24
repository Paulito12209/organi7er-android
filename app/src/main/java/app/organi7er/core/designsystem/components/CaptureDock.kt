package app.organi7er.core.designsystem.components

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.organi7er.core.designsystem.OrganiShapes
import app.organi7er.core.designsystem.OrganiTheme
import app.organi7er.core.designsystem.Primitives
import app.organi7er.core.designsystem.Spacing
import app.organi7er.core.model.Dates
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

// ------------------------------------------------------------------ Einstellbare Werte

/** Rundung der aufgeklappten Composer-Karte. */
private val ComposerRadius = 32.dp

/** Rundung der oberen Ecken des Glas-Rahmens (unten läuft er bis zum Gerätrand). */
private val SheetRadius = 34.dp

/** Glanz im Glas-Rahmen (Deckkraft Weiß), fällt von oben links ein wie bei den Glasknöpfen. */
private const val SheenDark = 0.16f
private const val SheenLight = 0.55f

/** Lichtkante des Glas-Rahmens: oben links hell, unten rechts ein schwacher Gegenschein. */
private const val RimDark = 0.42f
private const val RimLight = 0.95f

/**
 * Farbe des Glas-Rahmens je Typ, Verlauf von oben links (Top) nach unten rechts (Bottom).
 * Notiz: dunkles Gelb, Aufgabe: Blau, Termin: dunkleres Blau. Hell jeweils zart getönt.
 * Der Alphawert = wie durchsichtig das Glas ist (kleiner = mehr vom unscharfen Hintergrund sichtbar).
 */
private fun EntryType.glassTop(dark: Boolean): Color = when (this) {
    EntryType.Note -> if (dark) Color(0xFF4A3E0C).copy(alpha = 0.66f) else Color(0xFFFFEC9A).copy(alpha = 0.55f)
    EntryType.Task -> if (dark) Color(0xFF1E3A66).copy(alpha = 0.66f) else Color(0xFFCFE0FA).copy(alpha = 0.58f)
    EntryType.Event -> if (dark) Color(0xFF132645).copy(alpha = 0.70f) else Color(0xFFB7CAEA).copy(alpha = 0.60f)
}

private fun EntryType.glassBottom(dark: Boolean): Color = when (this) {
    EntryType.Note -> if (dark) Color(0xFF1C190E).copy(alpha = 0.84f) else Color(0xFFFFF8DC).copy(alpha = 0.72f)
    EntryType.Task -> if (dark) Color(0xFF0D1524).copy(alpha = 0.84f) else Color(0xFFEEF4FD).copy(alpha = 0.72f)
    EntryType.Event -> if (dark) Color(0xFF090F1B).copy(alpha = 0.86f) else Color(0xFFE4EBF7).copy(alpha = 0.74f)
}

/** Aktives Typ-Symbol: in der Farbe des Glases, nur kräftiger, damit es lesbar bleibt. */
private fun EntryType.activeIcon(dark: Boolean): Color = when (this) {
    EntryType.Note -> if (dark) Color(0xFFE0BC32) else Color(0xFF9A7B00)
    EntryType.Task -> if (dark) Color(0xFF7FB0FF) else Color(0xFF2D64C8)
    EntryType.Event -> if (dark) Color(0xFF5C8FE6) else Color(0xFF1E4A99)
}

/** Innere Composer-Karte: dunkles Silber (Verlauf oben links → unten rechts) bzw. helles Silber. */
private val SilverDarkA = Color(0xFF34363B)
private val SilverDarkB = Color(0xFF1C1D21)
private val SilverLightA = Color(0xFFFFFFFF)
private val SilverLightB = Color(0xFFE9EBEF)

// ------------------------------------------------------------------ Dock

/**
 * Unten angedockter Composer. Eingeklappt eine Pille: Kamera · „Neue Notiz“ · Diktat.
 * Antippen klappt ihn über der Tastatur zur Karte auf (wie die Vorlage): oben die Typ-Symbole
 * (aktiv weiß gefüllt) und ×, darunter der Text, unten + · Details · Erinnerung · Fokus … Diktat · Senden.
 * Der Hintergrund der Karte läuft von der Typfarbe (Notiz: gelb) ins Dunkle bzw. ins Weiße.
 * Der Typ folgt dem Reiter, lässt sich in der Karte aber umschalten.
 */
@Composable
fun CaptureDock(
    type: EntryType,
    onSave: (QuickEntry) -> Unit,
    onDetails: (QuickEntry) -> Unit,
    onCamera: () -> Unit,
    onMessage: (String) -> Unit,
    haze: HazeState,
    modifier: Modifier = Modifier,
) {
    val c = OrganiTheme.colors
    val haptics = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val askNotifications = rememberNotificationPermission()
    var expanded by remember { mutableStateOf(false) }
    var chosen by remember(type) { mutableStateOf(type) }
    var field by remember { mutableStateOf(TextFieldValue("")) }
    var details by remember { mutableStateOf("") }
    var showDetails by remember { mutableStateOf(false) }
    var at by remember { mutableStateOf<LocalDateTime?>(null) }
    var focused by remember { mutableStateOf(false) }
    var picker by remember { mutableStateOf<String?>(null) }
    val focus = remember { FocusRequester() }
    val detailsFocus = remember { FocusRequester() }
    val canSend = field.text.isNotBlank() || details.isNotBlank()

    val dictate = rememberDictation(
        onResult = { spoken ->
            val sep = if (field.text.isBlank() || field.text.endsWith(" ") || field.text.endsWith("\n")) "" else " "
            val t = field.text + sep + spoken
            field = TextFieldValue(t, TextRange(t.length))
        },
        onUnavailable = { onMessage("Diktat ist auf diesem Gerät nicht verfügbar") },
    )

    val entry = { QuickEntry(chosen, listOf(field.text.trim(), details.trim()).filter { it.isNotEmpty() }.joinToString("\n"), at, focused) }
    val reset = {
        field = TextFieldValue("")
        details = ""
        showDetails = false
        at = null
        focused = false
    }
    val collapse = {
        focusManager.clearFocus()
        expanded = false
        chosen = type
    }
    val save = {
        if (canSend) {
            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            onSave(entry())
            reset()
            collapse()
        }
    }
    BackHandler(expanded) { collapse() }

    // Unschärfe unter dem Composer, ohne Abdunklung: an der Oberkante 0 %, in der Mitte 50 %, an der Unterkante 100 %.
    val dockBlur = remember {
        HazeBlurStyle {
            blurRadius(20.dp)
            noiseFactor(0f)
            // Deckkraft der Unschärfe: Oberkante 0 %, Mitte 50 %, Unterkante 100 %.
            mask(Brush.verticalGradient(0f to Color.Transparent, 0.5f to Color.Black.copy(alpha = 0.5f), 1f to Color.Black))
        }
    }
    // Unschärfe hinter dem Glas-Rahmen des aufgeklappten Composers: gleichmäßig, ohne Verlauf.
    val sheetBlur = remember {
        HazeBlurStyle {
            blurRadius(24.dp)
            noiseFactor(0f)
        }
    }

    AnimatedContent(
        expanded,
        modifier.fillMaxWidth(),
        transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
        contentAlignment = Alignment.BottomCenter,
        label = "composer",
    ) { open ->
        if (!open) {
            Column(
                Modifier
                    .fillMaxWidth()
                    // Tastatur oder Navigationsleiste, je nachdem, was höher ist: die Pille sitzt immer darüber.
                    .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars)),
            ) {
                Spacer(Modifier.fillMaxWidth().height(BlurBandHeight))
                // Das Unschärfe-Feld läuft über Pille und Abstand nach unten; die Pille liegt darüber.
                Box(Modifier.hazeBlur(HazeInput.Sources(haze), dockBlur).padding(horizontal = 12.dp).padding(bottom = 8.dp)) {
                    CollapsedPill(
                        placeholder = type.placeholder(),
                        text = field.text,
                        onOpen = { expanded = true },
                        onCamera = onCamera,
                        onDictate = {
                            expanded = true
                            dictate()
                        },
                    )
                }
            }
        } else {
            val dark = c.isDark
            val glassA by animateColorAsState(chosen.glassTop(dark), tween(300), label = "glassTop")
            val glassB by animateColorAsState(chosen.glassBottom(dark), tween(300), label = "glassBottom")
            val sheetShape = RoundedCornerShape(topStart = SheetRadius, topEnd = SheetRadius)
            LaunchedEffect(Unit) {
                delay(60)
                runCatching { focus.requestFocus() }
            }
            // Äußerer Liquid-Glass-Rahmen in der Typfarbe. Er reicht bis zur Unterkante des Geräts und
            // liegt damit auch hinter der Tastatur – zwischen Rahmen und Tastatur bleibt keine Lücke.
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(sheetShape)
                    .hazeBlur(HazeInput.Sources(haze), sheetBlur)
                    .drawWithCache {
                        val sheen = Brush.linearGradient(
                            0f to Color.White.copy(alpha = if (dark) SheenDark else SheenLight),
                            0.45f to Color.Transparent,
                            start = Offset.Zero,
                            end = Offset(size.width * 0.6f, size.height),
                        )
                        val rim = Brush.linearGradient(
                            0f to Color.White.copy(alpha = if (dark) RimDark else RimLight),
                            0.35f to Color.White.copy(alpha = if (dark) 0.05f else 0.30f),
                            1f to Color.White.copy(alpha = if (dark) 0.10f else 0.45f),
                            start = Offset.Zero,
                            end = Offset(size.width, size.height),
                        )
                        val body = Brush.linearGradient(listOf(glassA, glassB), start = Offset.Zero, end = Offset(size.width, size.height))
                        val stroke = 1.2.dp.toPx()
                        val outline = sheetShape.createOutline(Size(size.width - stroke, size.height + SheetRadius.toPx()), layoutDirection, this)
                        onDrawBehind {
                            drawRect(body)
                            drawRect(sheen)
                            translate(stroke / 2f, stroke / 2f) { drawOutline(outline, rim, style = Stroke(stroke)) }
                        }
                    }
                    .padding(start = 14.dp, end = 14.dp, top = 16.dp)
                    // Inhalt über Tastatur bzw. Navigationsleiste, der Glas-Hintergrund läuft darunter weiter.
                    .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
                    .padding(bottom = 14.dp),
            ) {
                // Innere Composer-Karte: dunkles bzw. helles Silber.
                val cardShape = RoundedCornerShape(ComposerRadius)
                Column(
                    Modifier
                        .fillMaxWidth()
                        .softShadow(cardShape, c.shadowSoft)
                        .clip(cardShape)
                        .background(Brush.linearGradient(if (dark) listOf(SilverDarkA, SilverDarkB) else listOf(SilverLightA, SilverLightB)))
                        .border(1.dp, if (dark) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.9f), cardShape)
                        .padding(start = 10.dp, end = 14.dp, top = 8.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // Kopf: Typ-Symbole ohne Knopf (aktiv in der Typfarbe, sonst dezent grau) und × zum Schließen.
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        Types.forEach { t -> TypeIcon(t, t == chosen) { chosen = t } }
                        Spacer(Modifier.weight(1f))
                        RoundIcon(Ms.close, "Schließen", size = 40.dp, iconSize = 20.dp) { collapse() }
                    }

                    // Text; erste Zeile wird der Titel, der Rest die Notiz.
                    Box(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 6.dp), contentAlignment = Alignment.CenterStart) {
                        if (field.text.isEmpty()) Text(chosen.placeholder(), style = OrganiTheme.type.headline.copy(fontSize = 20.sp), color = c.textSoft)
                        BasicTextField(
                            value = field,
                            onValueChange = { field = it },
                            textStyle = OrganiTheme.type.headline.copy(fontSize = 20.sp, color = c.text),
                            cursorBrush = SolidColor(c.text),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            modifier = Modifier.fillMaxWidth().focusRequester(focus).semantics { contentDescription = chosen.placeholder() },
                        )
                    }

                    // Details-Text unter dem Titel, über das Text-Symbol eingeblendet.
                    AnimatedVisibility(showDetails) {
                        Box(Modifier.fillMaxWidth().padding(horizontal = 6.dp)) {
                            if (details.isEmpty()) Text("Details hinzufügen", style = OrganiTheme.type.body, color = c.textSoft)
                            BasicTextField(
                                value = details,
                                onValueChange = { details = it },
                                textStyle = OrganiTheme.type.body.copy(color = c.textMuted),
                                cursorBrush = SolidColor(c.text),
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                                modifier = Modifier.fillMaxWidth().focusRequester(detailsFocus).semantics { contentDescription = "Details" },
                            )
                        }
                    }
                    LaunchedEffect(showDetails) { if (showDetails) runCatching { detailsFocus.requestFocus() } }

                    AnimatedVisibility(at != null) {
                        val value = at
                        if (value != null) {
                            Row(
                                Modifier
                                    .clip(OrganiShapes.pill)
                                    .background(if (dark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.06f))
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

                    // Werkzeugzeile: + · Details · Erinnerung · Fokus … Diktat · Senden.
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RoundIcon(Ms.add, "Mit allen Feldern öffnen", size = 44.dp, iconSize = 22.dp) {
                            val e = entry()
                            reset()
                            collapse()
                            onDetails(e)
                        }
                        Spacer(Modifier.width(4.dp))
                        ToolIcon(Ms.subject, if (showDetails) "Details ausblenden" else "Details hinzufügen", active = showDetails || details.isNotBlank()) {
                            showDetails = !showDetails || details.isNotBlank()
                        }
                        ToolIcon(Ms.notifications, "Erinnerung", active = at != null) { picker = "date" }
                        ToolIcon(Ms.flag, if (focused) "Nicht mehr fokussieren" else "Fokussieren", active = focused, activeTint = Primitives.Yellow500) { focused = !focused }
                        Spacer(Modifier.weight(1f))
                        RoundIcon(Ms.graphicEq, "Diktieren", size = 44.dp, iconSize = 22.dp, onClick = dictate)
                        Spacer(Modifier.width(8.dp))
                        val sendFill = if (dark) Color.White else Color(0xFF0A0A0B)
                        val sendOn = if (dark) Color(0xFF0A0A0B) else Color.White
                        Box(
                            Modifier
                                .size(44.dp)
                                .clip(OrganiShapes.circle)
                                .background(if (canSend) sendFill else if (dark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.06f))
                                .clickable(enabled = canSend, role = Role.Button, onClickLabel = "Speichern") { save() }
                                .semantics { contentDescription = "Speichern" },
                            contentAlignment = Alignment.Center,
                        ) { MsIcon(Ms.arrowUpward, null, tint = if (canSend) sendOn else c.textSoft, size = 22.dp) }
                    }
                }
            }
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

/** Eingeklappte Pille: Kamera · Platzhalter (bzw. angefangener Text) · Diktat. Antippen klappt auf. */
@Composable
private fun CollapsedPill(placeholder: String, text: String, onOpen: () -> Unit, onCamera: () -> Unit, onDictate: () -> Unit) {
    val c = OrganiTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(OrganiShapes.pill)
            .background(if (c.isDark) Color(0xFF1E1F22) else Color.White)
            .border(1.dp, if (c.isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.07f), OrganiShapes.pill)
            .clickable(role = Role.Button, onClickLabel = placeholder, onClick = onOpen),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DockIcon(Ms.photoCamera, "Foto-Notiz aufnehmen", onCamera, Modifier.padding(start = 4.dp))
        Text(
            text.ifBlank { placeholder },
            Modifier.weight(1f).padding(horizontal = 6.dp),
            style = OrganiTheme.type.body.copy(fontSize = 17.sp),
            color = if (text.isBlank()) c.textSoft else c.text,
            maxLines = 1,
        )
        Box(
            Modifier
                .padding(end = 6.dp)
                .size(44.dp)
                .clip(OrganiShapes.circle)
                .clickable(role = Role.Button, onClickLabel = "Diktieren", onClick = onDictate)
                .semantics { contentDescription = "Diktieren" },
            contentAlignment = Alignment.Center,
        ) { MsIcon(Ms.graphicEq, null, tint = c.text, size = 26.dp) }
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

/** Runder Knopf mit zarter Fläche (×, +, Diktat). */
@Composable
private fun RoundIcon(icon: Int, description: String, size: androidx.compose.ui.unit.Dp, iconSize: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    val c = OrganiTheme.colors
    Box(
        Modifier
            .size(size)
            .clip(OrganiShapes.circle)
            .background(if (c.isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.06f))
            .clickable(role = Role.Button, onClickLabel = description, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) { MsIcon(icon, null, tint = c.text, size = iconSize) }
}

/** Typ nur als Symbol, ohne Knopf-Fläche: aktiv in der Typfarbe des Glases, sonst dezent grau. */
@Composable
private fun TypeIcon(type: EntryType, active: Boolean, onClick: () -> Unit) {
    val c = OrganiTheme.colors
    val tint by animateColorAsState(
        if (active) type.activeIcon(c.isDark) else c.text.copy(alpha = if (c.isDark) 0.42f else 0.38f),
        tween(200),
        label = "typeIcon",
    )
    Box(
        Modifier
            .size(Spacing.touch)
            .clip(OrganiShapes.circle)
            .clickable(role = Role.Tab, onClickLabel = type.newLabel, onClick = onClick)
            .semantics { contentDescription = type.shortLabel },
        contentAlignment = Alignment.Center,
    ) { MsIcon(type.icon, null, tint = tint, size = 26.dp) }
}

@Composable
private fun ToolIcon(icon: Int, description: String, active: Boolean = false, activeTint: Color? = null, onClick: () -> Unit) {
    val c = OrganiTheme.colors
    Box(
        Modifier
            .size(Spacing.touch)
            .clip(OrganiShapes.circle)
            .clickable(role = Role.Button, onClickLabel = description, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) { MsIcon(icon, null, tint = if (active) activeTint ?: c.text else c.text.copy(alpha = 0.8f), size = 24.dp) }
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

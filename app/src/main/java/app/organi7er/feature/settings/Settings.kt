package app.organi7er.feature.settings

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.organi7er.BuildConfig
import app.organi7er.core.data.BackupManager
import app.organi7er.core.data.EventRepository
import app.organi7er.core.data.SampleData
import app.organi7er.core.data.SettingsRepository
import app.organi7er.core.designsystem.CalendarPalette
import app.organi7er.core.designsystem.OrganiShapes
import app.organi7er.core.designsystem.OrganiTheme
import app.organi7er.core.designsystem.Spacing
import app.organi7er.core.designsystem.components.ConfirmDialog
import app.organi7er.core.designsystem.components.FormField
import app.organi7er.core.designsystem.components.Ms
import app.organi7er.core.designsystem.components.OrganiSheet
import app.organi7er.core.designsystem.components.Overline
import app.organi7er.core.designsystem.components.PillButton
import app.organi7er.core.designsystem.components.PillVariant
import app.organi7er.core.designsystem.components.SegmentedControl
import app.organi7er.core.designsystem.components.SegmentedList
import app.organi7er.core.designsystem.components.SettingsRow
import app.organi7er.core.designsystem.components.SettingsValue
import app.organi7er.core.designsystem.components.SoftIconButton
import app.organi7er.core.designsystem.components.SoftSwitch
import app.organi7er.core.designsystem.components.TimePickerSheet
import app.organi7er.core.designsystem.components.rememberNotificationPermission
import app.organi7er.core.designsystem.parseHexColor
import app.organi7er.core.model.AppSettings
import app.organi7er.core.model.CalendarEntity
import app.organi7er.core.model.ThemeMode
import app.organi7er.feature.editor.LocalAppActions
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepo: SettingsRepository,
    private val events: EventRepository,
    private val backup: BackupManager,
    private val sample: SampleData,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    val settings: StateFlow<AppSettings> = settingsRepo.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())
    val calendars: StateFlow<List<CalendarEntity>> = events.calendars.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun update(transform: (AppSettings) -> AppSettings) = viewModelScope.launch { settingsRepo.update(transform) }
    fun saveCalendar(c: CalendarEntity) = viewModelScope.launch { events.saveCalendar(c) }
    fun deleteCalendar(c: CalendarEntity) = viewModelScope.launch { events.deleteCalendar(c) }
    fun clearSamples(onDone: () -> Unit) = viewModelScope.launch {
        sample.clearAll()
        onDone()
    }

    fun export(uri: android.net.Uri, onDone: (String) -> Unit) = viewModelScope.launch {
        val result = runCatching {
            val text = backup.export()
            withContext(Dispatchers.IO) { context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) } }
        }
        onDone(if (result.isSuccess) "Sicherung gespeichert" else "Sicherung fehlgeschlagen")
    }

    fun import(uri: android.net.Uri, onDone: (String) -> Unit) = viewModelScope.launch {
        val text = withContext(Dispatchers.IO) { runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } }.getOrNull() }
        if (text == null) {
            onDone("Datei konnte nicht gelesen werden")
            return@launch
        }
        backup.import(text).fold({ onDone("$it Einträge wiederhergestellt") }, { onDone("Keine gültige Organi7er-Sicherung") })
    }
}

private val LeadOptions = listOf(5, 10, 15, 30, 60)

@Composable
fun SettingsScreen(vm: SettingsViewModel = hiltViewModel()) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val calendars by vm.calendars.collectAsStateWithLifecycle()
    val actions = LocalAppActions.current
    val c = OrganiTheme.colors
    val askNotifications = rememberNotificationPermission()
    var dialog by remember { mutableStateOf<String?>(null) }
    val blueBg = c.calWork.copy(alpha = 0.18f)
    val yellowBg = c.accent.copy(alpha = 0.30f)

    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { vm.export(it, actions.showMessage) }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { vm.import(it, actions.showMessage) }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = Spacing.screen)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(Spacing.section),
    ) {
        Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            SoftIconButton(Ms.chevronLeft, "Zurück", actions.back)
            Text("Einstellungen", Modifier.semantics { heading() }, style = OrganiTheme.type.pageTitle, color = c.text)
        }

        Group("Darstellung") {
            SegmentedControl(ThemeMode.entries.map { it.label }, s.themeMode.ordinal, { i -> vm.update { it.copy(themeMode = ThemeMode.entries[i]) } })
        }

        Group("Übersicht") {
            SegmentedList {
                SettingsRow(Ms.home, blueBg, c.link, "Kachel „Morgen“", "im Briefing zeigen", 0, 2) {
                    SoftSwitch(s.showTomorrowTile, { v -> vm.update { it.copy(showTomorrowTile = v) } }, contentDescription = "Kachel Morgen")
                }
                SettingsRow(Ms.calendarToday, blueBg, c.link, "Woche beginnt am", null, 1, 2, onClick = { dialog = "week" }) {
                    SettingsValue(if (s.weekStart == 7) "Sonntag" else "Montag")
                }
            }
        }

        Group("Erinnerungen") {
            SegmentedList {
                SettingsRow(Ms.notifications, yellowBg, c.text, "Vor Terminen", "Standard für neue Termine", 0, 3, onClick = { dialog = "lead" }) {
                    SettingsValue(if (s.reminderLeadMinutes == 60) "1 Std" else "${s.reminderLeadMinutes} Min")
                }
                SettingsRow(
                    Ms.lightMode,
                    yellowBg,
                    c.text,
                    "Morgen-Briefing",
                    "jeden Tag um ${LocalTime.ofSecondOfDay(s.morningBriefingTime * 60L)}",
                    1,
                    3,
                    onClick = { dialog = "time" },
                ) {
                    SoftSwitch(s.morningBriefing, { v ->
                        vm.update { it.copy(morningBriefing = v) }
                        if (v) askNotifications()
                    }, contentDescription = "Morgen-Briefing")
                }
                SettingsRow(Ms.checklist, yellowBg, c.text, "Entwürfe erinnern", "nach 7 Tagen ohne Plan", 2, 3) {
                    SoftSwitch(s.draftReminders, { v ->
                        vm.update { it.copy(draftReminders = v) }
                        if (v) askNotifications()
                    }, contentDescription = "Entwürfe erinnern")
                }
            }
        }

        Group("Konten und Daten") {
            SegmentedList {
                SettingsRow(Ms.calendarToday, c.surface2, c.text, "Kalender", "${calendars.size} Kalender, Farben", 0, 4, onClick = { dialog = "calendars" }) { SettingsValue("") }
                SettingsRow(Ms.upload, c.surface2, c.text, "Sicherung exportieren", "als JSON-Datei", 1, 4, onClick = { exporter.launch("organi7er-${LocalDate.now()}.json") }) { SettingsValue("") }
                SettingsRow(Ms.download, c.surface2, c.text, "Sicherung importieren", "ersetzt alle Daten", 2, 4, onClick = { dialog = "import" }) { SettingsValue("") }
                SettingsRow(Ms.delete, c.danger.copy(alpha = 0.12f), c.dangerText, "Beispieldaten löschen", "alle Termine, Aufgaben und Notizen", 3, 4, onClick = { dialog = "clear" }) { SettingsValue("") }
            }
        }

        if (BuildConfig.DEBUG) {
            Group("Entwicklung") {
                SegmentedList {
                    SettingsRow(Ms.editNote, c.surface2, c.text, "Komponenten-Katalog", "nur im Debug-Build", 0, 1, onClick = actions.openCatalog) { SettingsValue("") }
                }
            }
        }

        Text("Organi7er ${BuildConfig.VERSION_NAME}", Modifier.fillMaxWidth(), style = OrganiTheme.type.label, color = c.textSoft, textAlign = TextAlign.Center)
    }

    when (dialog) {
        "week" -> ChoiceDialog("Woche beginnt am", listOf(1 to "Montag", 7 to "Sonntag"), s.weekStart, { dialog = null }) { v -> vm.update { it.copy(weekStart = v) } }
        "lead" -> ChoiceDialog("Erinnerung vor Terminen", LeadOptions.map { it to if (it == 60) "1 Std" else "$it Min" }, s.reminderLeadMinutes, { dialog = null }) { v -> vm.update { it.copy(reminderLeadMinutes = v) } }
        "time" -> TimePickerSheet(LocalTime.ofSecondOfDay(s.morningBriefingTime * 60L), { dialog = null }) { t ->
            vm.update { it.copy(morningBriefingTime = t.hour * 60 + t.minute, morningBriefing = true) }
            askNotifications()
        }
        "calendars" -> CalendarsSheet(calendars, vm, { dialog = null })
        "import" -> ConfirmDialog("Sicherung importieren?", "Alle aktuellen Termine, Aufgaben und Notizen werden durch die Sicherung ersetzt. Bilder sind nicht enthalten.", "Importieren", { dialog = null }) {
            importer.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
        }
        "clear" -> ConfirmDialog("Alles löschen?", "Termine, Aufgaben, Notizen und Bilder werden entfernt. Kalender bleiben erhalten.", "Löschen", { dialog = null }) {
            vm.clearSamples { actions.showMessage("Daten gelöscht") }
        }
    }
}

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Overline(title, Modifier.padding(horizontal = 4.dp))
        content()
    }
}

@Composable
private fun <T> ChoiceDialog(title: String, options: List<Pair<T, String>>, selected: T, onDismiss: () -> Unit, onPick: (T) -> Unit) {
    val c = OrganiTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.bg,
        title = { Text(title, style = OrganiTheme.type.title) },
        text = {
            Column {
                options.forEach { (value, label) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(OrganiShapes.small)
                            .selectable(value == selected, role = Role.RadioButton) {
                                onPick(value)
                                onDismiss()
                            }
                            .padding(horizontal = 12.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(label, Modifier.weight(1f), style = OrganiTheme.type.body, color = c.text)
                        if (value == selected) Box(Modifier.size(10.dp).clip(OrganiShapes.circle).background(c.primary))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Schließen") } },
    )
}

/** Kalender verwalten: Farbe antippen wechselt durch die Palette, neue Kalender anlegen. */
@Composable
private fun CalendarsSheet(calendars: List<CalendarEntity>, vm: SettingsViewModel, onDismiss: () -> Unit) {
    val c = OrganiTheme.colors
    var name by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    OrganiSheet(onDismiss) {
        Text("Kalender", style = OrganiTheme.type.title, color = c.text)
        SegmentedList {
            calendars.forEachIndexed { i, cal ->
                val color = parseHexColor(cal.colorHex)
                SettingsRow(
                    Ms.calendarToday,
                    color.copy(alpha = 0.22f),
                    color,
                    cal.name,
                    "Farbe antippen zum Wechseln",
                    i,
                    calendars.size,
                    onClick = {
                        val idx = CalendarPalette.choices.indexOf(cal.colorHex)
                        vm.saveCalendar(cal.copy(colorHex = CalendarPalette.choices[(idx + 1) % CalendarPalette.choices.size]))
                    },
                ) {
                    if (calendars.size > 1) PillButton("Entfernen", { vm.deleteCalendar(cal) }, variant = PillVariant.Ghost, small = true)
                }
            }
        }
        Overline("Neuer Kalender")
        FormField(name, { name = it }, "Name, z. B. Familie", icon = Ms.calendarToday)
        PillButton("Hinzufügen", {
            scope.launch {
                vm.saveCalendar(CalendarEntity(name = name.trim(), colorHex = CalendarPalette.choices[calendars.size % CalendarPalette.choices.size]))
                name = ""
            }
        }, Modifier.fillMaxWidth(), enabled = name.isNotBlank())
    }
}

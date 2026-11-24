package app.organi7er.feature.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.organi7er.core.designsystem.OrganiTheme
import app.organi7er.core.designsystem.Spacing
import app.organi7er.core.designsystem.components.ChoiceChips
import app.organi7er.core.designsystem.components.ConfirmDialog
import app.organi7er.core.designsystem.components.DatePickerSheet
import app.organi7er.core.designsystem.components.EntryType
import app.organi7er.core.designsystem.components.FieldLabel
import app.organi7er.core.designsystem.components.FocusToggle
import app.organi7er.core.designsystem.components.FormField
import app.organi7er.core.designsystem.components.Hint
import app.organi7er.core.designsystem.components.Ms
import app.organi7er.core.designsystem.components.OrganiSheet
import app.organi7er.core.designsystem.components.PickerField
import app.organi7er.core.designsystem.components.PickerPair
import app.organi7er.core.designsystem.components.SheetActions
import app.organi7er.core.designsystem.components.SheetHeader
import app.organi7er.core.designsystem.components.SoftSwitch
import app.organi7er.core.designsystem.components.TimePickerSheet
import app.organi7er.core.designsystem.components.TitleField
import app.organi7er.core.designsystem.components.rememberNotificationPermission
import app.organi7er.core.designsystem.parseHexColor
import app.organi7er.core.model.Dates
import app.organi7er.core.model.Event
import app.organi7er.core.model.EventSubStatus
import app.organi7er.core.model.Status
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

private val ReminderOptions = listOf<Int?>(null, 0, 5, 15, 30, 60, 1440)

private fun reminderLabel(m: Int?): String = when (m) {
    null -> "Keine"
    0 -> "Zum Start"
    1440 -> "1 Tag"
    60 -> "1 Std"
    else -> "$m Min"
}

/** Termin anlegen, bearbeiten oder planen (Entwurf → aktiv). */
@Composable
fun EventSheet(request: EditorRequest, vm: EditorViewModel, onDismiss: () -> Unit) {
    val calendars by vm.calendars.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val askNotifications = rememberNotificationPermission()
    val editId = when (request) {
        is EditorRequest.EditEvent -> request.id
        is EditorRequest.PlanEvent -> request.id
        else -> null
    }
    val planning = request is EditorRequest.PlanEvent
    val now = remember { LocalDateTime.now() }
    val defaultStart = remember {
        val base = (request as? EditorRequest.NewEvent)?.date
        if (base == null || base == now.toLocalDate()) now.truncatedTo(ChronoUnit.HOURS).plusHours(1) else base.atTime(9, 0)
    }

    var original by remember { mutableStateOf<Event?>(null) }
    var loaded by remember { mutableStateOf(editId == null) }
    val prefill = request as? EditorRequest.NewEvent
    var title by remember { mutableStateOf(prefill?.title.orEmpty()) }
    var draft by remember { mutableStateOf(false) }
    var date by remember { mutableStateOf(defaultStart.toLocalDate()) }
    var startTime by remember { mutableStateOf(defaultStart.toLocalTime()) }
    var endTime by remember { mutableStateOf(defaultStart.toLocalTime().plusHours(1)) }
    var allDay by remember { mutableStateOf(false) }
    var location by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("") }
    var calendarId by remember { mutableLongStateOf(0L) }
    var reminder by remember { mutableStateOf<Int?>(null) }
    var subStatus by remember { mutableStateOf<EventSubStatus?>(null) }
    var note by remember { mutableStateOf(prefill?.note.orEmpty()) }
    var focused by remember { mutableStateOf(prefill?.focused == true) }
    var picker by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(editId) {
        if (editId != null) {
            vm.event(editId)?.let { e ->
                original = e
                title = e.title
                focused = e.focused
                draft = e.status == Status.DRAFT && !planning
                e.start?.let { s ->
                    date = s.toLocalDate()
                    startTime = s.toLocalTime()
                    endTime = (e.end ?: s.plusHours(1)).toLocalTime()
                }
                allDay = e.allDay
                location = e.location
                link = e.link
                calendarId = e.calendarId
                reminder = e.reminderMinutes ?: if (planning) settings.reminderLeadMinutes else null
                subStatus = e.subStatus
                note = e.note
            }
        } else {
            reminder = settings.reminderLeadMinutes
        }
        loaded = true
    }
    LaunchedEffect(calendars) {
        if (calendarId == 0L || calendars.none { it.id == calendarId }) calendarId = calendars.firstOrNull()?.id ?: 0L
    }
    if (!loaded) return

    val start = date.atTime(startTime)
    var end = date.atTime(endTime)
    if (!end.isAfter(start)) end = end.plusDays(1)
    val existingActive = original?.status == Status.ACTIVE
    val headline = when {
        planning -> "Termin planen"
        original != null -> "Termin bearbeiten"
        else -> "Neuer Termin"
    }

    OrganiSheet(onDismiss) {
        SheetHeader(headline, EntryType.Event, onDismiss)
        // Titelbild des Termins ganz oben.
        original?.imagePath?.let { path ->
            coil3.compose.AsyncImage(
                model = java.io.File(path),
                contentDescription = "Bild zum Termin",
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(24.dp)),
            )
        }
        TitleField(title, { title = it }, "Was steht an?", autoFocus = original == null)

        if (!planning) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Noch ohne feste Zeit", style = OrganiTheme.type.rowTitle, color = OrganiTheme.colors.text)
                    Text("Als Entwurf sammeln und später planen", style = OrganiTheme.type.rowMeta, color = OrganiTheme.colors.textSoft)
                }
                SoftSwitch(draft, { draft = it }, contentDescription = "Als Entwurf speichern")
            }
        }

        AnimatedVisibility(!draft) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                PickerField(Ms.calendarToday, "Datum", Dates.shortDate(date), { picker = "date" })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Ganztägig", Modifier.weight(1f).fillMaxWidth(), style = OrganiTheme.type.body, color = OrganiTheme.colors.textMuted)
                    SoftSwitch(allDay, { allDay = it }, contentDescription = "Ganztägig")
                }
                AnimatedVisibility(!allDay) {
                    PickerPair(
                        { m -> PickerField(Ms.schedule, "Beginn", startTime.toString(), { picker = "start" }, m) },
                        { m -> PickerField(Ms.schedule, "Ende", endTime.toString(), { picker = "end" }, m) },
                    )
                }
                if (original?.start != null && start != original?.start && existingActive) {
                    Hint("Vorher ${Dates.relativeDay(original!!.start!!.toLocalDate(), now.toLocalDate())}, ${Dates.time(original!!.start!!)}")
                }
            }
        }

        FormField(location, { location = it }, "Ort", icon = Ms.locationOn)
        FormField(link, { link = it }, "Link, z. B. Meeting-Link", icon = Ms.arrowOutward)

        if (calendars.isNotEmpty()) {
            FieldLabel("Kalender")
            ChoiceChips(calendars.map { it.id }, calendarId, { id -> calendars.first { it.id == id }.name }, { calendarId = it }, color = { id -> parseHexColor(calendars.first { it.id == id }.colorHex) })
        }

        AnimatedVisibility(!draft) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                FieldLabel("Erinnerung")
                ChoiceChips(ReminderOptions, reminder, ::reminderLabel, {
                    reminder = it
                    if (it != null) askNotifications()
                })
            }
        }

        if (existingActive || planning) {
            FieldLabel("Status")
            ChoiceChips(listOf<EventSubStatus?>(null) + EventSubStatus.entries, subStatus, { it?.label ?: "Fest" }, { subStatus = it })
        }

        FormField(note, { note = it }, "Notiz", icon = Ms.editNote, singleLine = false, minLines = 2)
        FocusToggle(focused) { focused = it }

        SheetActions(
            primary = if (planning) "Planen" else "Speichern",
            enabled = title.isNotBlank(),
            onPrimary = {
                val base = original ?: Event(title = title)
                val newStart = if (allDay) date.atStartOfDay() else start
                val changedTime = original?.start != null && original?.start != newStart
                val event = base.copy(
                    title = title.trim(),
                    start = if (draft) null else newStart,
                    end = if (draft) {
                        null
                    } else if (allDay) {
                        null
                    } else {
                        end
                    },
                    allDay = allDay && !draft,
                    location = location.trim(),
                    link = link.trim(),
                    calendarId = calendarId,
                    reminderMinutes = if (draft) null else reminder,
                    note = note.trim(),
                    focused = focused,
                    status = if (draft) Status.DRAFT else Status.ACTIVE,
                    subStatus = if (draft) null else subStatus,
                    previousStart = when {
                        subStatus == EventSubStatus.POSTPONED && changedTime -> original?.start
                        subStatus == EventSubStatus.POSTPONED -> base.previousStart
                        else -> null
                    },
                )
                vm.saveEvent(event)
                onDismiss()
            },
            secondary = if (original != null) "Löschen" else null,
            onSecondary = { confirmDelete = true },
        )
    }

    when (picker) {
        "date" -> DatePickerSheet(date, { picker = null }, { d ->
            date = d
            if (original?.status == Status.ACTIVE && subStatus == null && original?.start?.toLocalDate() != d) subStatus = EventSubStatus.POSTPONED
        })
        "start" -> TimePickerSheet(startTime, { picker = null }) { t ->
            val duration = Duration.between(startTime, endTime).let { if (it.isNegative || it.isZero) Duration.ofHours(1) else it }
            startTime = t
            endTime = t.plus(duration).let { if (it.isBefore(t)) LocalTime.of(23, 59) else it }
            if (original?.status == Status.ACTIVE && subStatus == null && original?.start?.toLocalTime() != t) subStatus = EventSubStatus.POSTPONED
        }
        "end" -> TimePickerSheet(endTime, { picker = null }) { endTime = it }
    }
    if (confirmDelete && original != null) {
        ConfirmDialog("Termin löschen?", "„${original!!.title}“ wird endgültig entfernt.", "Löschen", { confirmDelete = false }) {
            vm.deleteEvent(original!!.id)
            onDismiss()
        }
    }
}

internal fun LocalDate.isToday(now: LocalDateTime) = this == now.toLocalDate()

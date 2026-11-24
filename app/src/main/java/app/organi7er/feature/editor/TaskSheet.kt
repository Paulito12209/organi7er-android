package app.organi7er.feature.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import app.organi7er.core.designsystem.components.SheetActions
import app.organi7er.core.designsystem.components.SheetHeader
import app.organi7er.core.designsystem.components.TimePickerSheet
import app.organi7er.core.designsystem.components.TitleField
import app.organi7er.core.model.Dates
import app.organi7er.core.model.Priority
import app.organi7er.core.model.Status
import app.organi7er.core.model.Task
import app.organi7er.core.model.TaskSubStatus
import java.time.LocalDate
import java.time.LocalTime

/** Aufgabe anlegen, bearbeiten oder planen. Ohne Datum landet sie in Entwürfe. */
@Composable
fun TaskSheet(request: EditorRequest, vm: EditorViewModel, onDismiss: () -> Unit) {
    val lists by vm.taskLists.collectAsStateWithLifecycle()
    val editId = when (request) {
        is EditorRequest.EditTask -> request.id
        is EditorRequest.PlanTask -> request.id
        else -> null
    }
    val planning = request is EditorRequest.PlanTask
    val today = remember { LocalDate.now() }

    var original by remember { mutableStateOf<Task?>(null) }
    var loaded by remember { mutableStateOf(editId == null) }
    val prefill = request as? EditorRequest.NewTask
    var title by remember { mutableStateOf(prefill?.title.orEmpty()) }
    var note by remember { mutableStateOf(prefill?.note.orEmpty()) }
    var date by remember { mutableStateOf((request as? EditorRequest.NewTask)?.date ?: if (planning) today else null) }
    var time by remember { mutableStateOf<LocalTime?>(null) }
    var priority by remember { mutableStateOf(Priority.NORMAL) }
    var listName by remember { mutableStateOf("") }
    var subStatus by remember { mutableStateOf<TaskSubStatus?>(null) }
    var focused by remember { mutableStateOf(prefill?.focused == true) }
    var picker by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(editId) {
        if (editId != null) {
            vm.task(editId)?.let { t ->
                original = t
                title = t.title
                note = t.note
                date = t.dueAt?.toLocalDate() ?: if (planning) today else null
                time = if (t.dueHasTime) t.dueAt?.toLocalTime() else null
                priority = t.priority
                listName = t.listName
                subStatus = t.subStatus
                focused = t.focused
            }
        }
        loaded = true
    }
    if (!loaded) return

    val headline = when {
        planning -> "Aufgabe planen"
        original != null -> "Aufgabe bearbeiten"
        else -> "Neue Aufgabe"
    }

    OrganiSheet(onDismiss) {
        SheetHeader(headline, EntryType.Task, onDismiss)
        TitleField(title, { title = it }, "Was ist zu tun?", autoFocus = original == null)

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            PickerField(
                Ms.calendarToday,
                "Fällig",
                date?.let { Dates.relativeDay(it, today) } ?: "Kein Datum",
                { picker = "date" },
                onClear = if (date != null && !planning) {
                    (
                        {
                            date = null
                            time = null
                        }
                        )
                } else {
                    null
                },
            )
            AnimatedVisibility(date != null) {
                PickerField(Ms.schedule, "Uhrzeit", time?.toString() ?: "Ganzer Tag", { picker = "time" }, onClear = if (time != null) ({ time = null }) else null)
            }
            if (date == null) Hint("Ohne Datum wird die Aufgabe als Entwurf gesammelt – ganz ohne Druck.")
        }

        FieldLabel("Priorität")
        ChoiceChips(Priority.entries, priority, { if (it == Priority.HIGH) "Wichtig" else "Normal" }, { priority = it })

        FieldLabel("Liste")
        FormField(listName, { listName = it }, "z. B. Privat, Arbeit …", icon = Ms.sell)
        val suggestions = lists.filter { it != listName }.take(6)
        if (suggestions.isNotEmpty()) ChoiceChips(suggestions, "", { it }, { listName = it })

        if (date != null && original?.status != Status.DONE) {
            FieldLabel("Stand")
            ChoiceChips(listOf<TaskSubStatus?>(null) + TaskSubStatus.entries, subStatus, { it?.label ?: "Offen" }, { subStatus = it })
        }

        FormField(note, { note = it }, "Notiz", icon = Ms.editNote, singleLine = false, minLines = 2)
        FocusToggle(focused) { focused = it }

        SheetActions(
            primary = if (planning) "Planen" else "Speichern",
            enabled = title.isNotBlank(),
            onPrimary = {
                val due = date?.let { d -> time?.let { d.atTime(it) } ?: d.atStartOfDay() }
                val base = original ?: Task(title = title)
                val status = when {
                    base.status == Status.DONE -> Status.DONE
                    due == null -> Status.DRAFT
                    else -> Status.ACTIVE
                }
                vm.saveTask(
                    base.copy(
                        title = title.trim(),
                        note = note.trim(),
                        dueAt = due,
                        dueHasTime = time != null,
                        priority = priority,
                        listName = listName.trim(),
                        status = status,
                        subStatus = if (status == Status.ACTIVE) subStatus else null,
                        focused = focused,
                    ),
                )
                onDismiss()
            },
            secondary = if (original != null) "Löschen" else null,
            onSecondary = { confirmDelete = true },
        )
    }

    when (picker) {
        "date" -> DatePickerSheet(date ?: today, { picker = null }, { date = it })
        "time" -> TimePickerSheet(time ?: LocalTime.of(9, 0), { picker = null }) { time = it }
    }
    if (confirmDelete && original != null) {
        ConfirmDialog("Aufgabe löschen?", "„${original!!.title}“ wird endgültig entfernt.", "Löschen", { confirmDelete = false }) {
            vm.deleteTask(original!!.id)
            onDismiss()
        }
    }
}

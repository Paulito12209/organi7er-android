package app.organi7er.feature.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.organi7er.core.designsystem.components.DateDivider
import app.organi7er.core.designsystem.components.EventRow
import app.organi7er.core.designsystem.components.NoteCard
import app.organi7er.core.designsystem.components.PriorityDot
import app.organi7er.core.designsystem.components.SegmentedList
import app.organi7er.core.designsystem.components.StatusTag
import app.organi7er.core.designsystem.components.TagTone
import app.organi7er.core.designsystem.components.TaskRow
import app.organi7er.core.designsystem.parseHexColor
import app.organi7er.core.model.CalendarEntity
import app.organi7er.core.model.Dates
import app.organi7er.core.model.Dates.toLocal
import app.organi7er.core.model.Event
import app.organi7er.core.model.EventSubStatus
import app.organi7er.core.model.NoteWithImages
import app.organi7er.core.model.Priority
import app.organi7er.core.model.Status
import app.organi7er.core.model.Task
import app.organi7er.core.model.TaskSubStatus
import app.organi7er.feature.editor.EditorRequest
import app.organi7er.feature.editor.LocalAppActions
import java.time.LocalDate
import java.time.LocalDateTime

@Immutable
data class EventUi(
    val event: Event,
    val time: String,
    val timeSub: String,
    val meta: String,
    val colorHex: String,
    val tag: String?,
)

@Immutable
data class TaskUi(
    val task: Task,
    val meta: String,
    val tag: String?,
    val tagTone: TagTone,
    val high: Boolean,
    val done: Boolean,
)

@Immutable
data class NoteDay(val label: String, val notes: List<NoteWithImages>)

object UiMapper {
    fun calendarColor(event: Event, calendars: List<CalendarEntity>): String =
        calendars.firstOrNull { it.id == event.calendarId }?.colorHex ?: "#3B82F6"

    fun event(e: Event, calendars: List<CalendarEntity>): EventUi {
        val start = e.start
        val time = when {
            start == null -> "–"
            e.allDay -> "ganzt."
            else -> Dates.time(start)
        }
        val sub = when {
            start == null || e.allDay -> ""
            else -> Dates.duration(start, e.end)
        }
        val meta = buildList {
            if (e.location.isNotBlank()) add(e.location)
            if (e.subStatus == EventSubStatus.POSTPONED && e.previousStart != null) add("vorher ${Dates.time(e.previousStart)}")
        }.joinToString(" · ")
        return EventUi(e, time, sub, meta, calendarColor(e, calendars), e.subStatus?.label)
    }

    /** Termin-Zeile für Listen mit Zeitspanne: „10:00–10:45 · Ort“. */
    fun eventRangeMeta(e: Event): String {
        val start = e.start ?: return e.location
        val range = if (e.allDay) "ganztägig" else "${Dates.time(start)}–${Dates.time(e.effectiveEnd!!)}"
        val extra = buildList {
            if (e.location.isNotBlank()) add(e.location)
            if (e.subStatus == EventSubStatus.POSTPONED && e.previousStart != null) add("vorher ${Dates.time(e.previousStart)}")
        }
        return (listOf(range) + extra).joinToString(" · ")
    }

    fun task(t: Task, today: LocalDate, showDate: Boolean, now: LocalDateTime = LocalDateTime.now()): TaskUi {
        val done = t.status == Status.DONE
        val due = t.dueAt
        val overdue = !done && due != null && due.toLocalDate().isBefore(today)
        val meta = buildList {
            when {
                done && t.doneAt != null -> add("erledigt ${Dates.dueLabel(t.doneAt.toLocal().toLocalDate(), today)}")
                overdue -> add("überfällig · ${Dates.dueLabel(due.toLocalDate(), today)}")
                due != null && showDate && t.dueHasTime -> add("${Dates.dueLabel(due.toLocalDate(), today)}, ${Dates.time(due)}")
                due != null && showDate -> add(Dates.dueLabel(due.toLocalDate(), today))
                due != null && t.dueHasTime -> add(if (due.isBefore(now)) "seit ${Dates.time(due)}" else "bis ${Dates.time(due)}")
            }
            if (t.listName.isNotBlank()) add(t.listName)
        }.joinToString(" · ")
        val (tag, tone) = when (t.subStatus) {
            TaskSubStatus.IN_PROGRESS -> "In Arbeit" to TagTone.Blue
            TaskSubStatus.BLOCKED -> "Geblockt" to TagTone.Red
            TaskSubStatus.POSTPONED -> "Verschoben" to TagTone.Mute
            null -> null to TagTone.Mute
        }
        return TaskUi(t, meta, if (done) null else tag, tone, t.priority == Priority.HIGH && !done, done)
    }

    fun noteDays(notes: List<NoteWithImages>, today: LocalDate, newestFirst: Boolean = true): List<NoteDay> {
        val sorted = if (newestFirst) notes.sortedByDescending { it.note.createdAt } else notes.sortedBy { it.note.createdAt }
        return sorted.groupBy { it.note.createdAt.toLocal().toLocalDate() }
            .map { (day, list) -> NoteDay(Dates.relativeDay(day, today), list) }
    }

    fun notePill(n: NoteWithImages, today: LocalDate): String? = n.note.reminderAt?.let {
        "${Dates.dueLabel(it.toLocalDate(), today).replaceFirstChar(Char::uppercase)}, ${Dates.time(it)}"
    }
}

@Composable
fun TaskList(items: List<TaskUi>, modifier: Modifier = Modifier) {
    val actions = LocalAppActions.current
    SegmentedList(modifier) {
        items.forEachIndexed { i, ui ->
            TaskRow(
                title = ui.task.title,
                meta = ui.meta,
                done = ui.done,
                onToggle = { actions.toggleTask(ui.task) },
                onClick = { actions.openEditor(EditorRequest.EditTask(ui.task.id)) },
                index = i,
                count = items.size,
                trailing = when {
                    ui.tag != null -> ({ StatusTag(ui.tag, ui.tagTone) })
                    ui.high -> ({ PriorityDot() })
                    else -> null
                },
            )
        }
    }
}

@Composable
fun EventList(items: List<EventUi>, modifier: Modifier = Modifier) {
    val actions = LocalAppActions.current
    SegmentedList(modifier) {
        items.forEachIndexed { i, ui ->
            val color = remember(ui.colorHex) { parseHexColor(ui.colorHex) }
            EventRow(
                time = ui.time,
                timeSub = ui.timeSub,
                barColor = color,
                title = ui.event.title,
                meta = ui.meta,
                index = i,
                count = items.size,
                onClick = { actions.openEditor(EditorRequest.EditEvent(ui.event.id)) },
                trailing = ui.tag?.let { t -> { StatusTag(t, TagTone.Mute) } },
            )
        }
    }
}

@Composable
fun NoteFeedCard(n: NoteWithImages, today: LocalDate, actions: app.organi7er.feature.editor.AppActions = LocalAppActions.current) {
    val images = n.sortedImages
    NoteCard(
        title = n.note.title,
        preview = n.note.body,
        imagePath = images.firstOrNull()?.filePath,
        imageCount = images.size,
        pill = UiMapper.notePill(n, today),
        onClick = { actions.openNote(n.note.id) },
        onCamera = { actions.takePhoto(n.note.id) },
        onGallery = { actions.pickImages(n.note.id) },
    )
}

package app.organi7er.feature.overview

import androidx.compose.runtime.Immutable
import app.organi7er.core.data.Briefing
import app.organi7er.core.data.BriefingCalculator
import app.organi7er.core.data.BriefingKind
import app.organi7er.core.model.CalendarEntity
import app.organi7er.core.model.Dates
import app.organi7er.core.model.Dates.toLocal
import app.organi7er.core.model.Event
import app.organi7er.core.model.NoteWithImages
import app.organi7er.core.model.Status
import app.organi7er.core.model.Task
import app.organi7er.feature.common.EventUi
import app.organi7er.feature.common.NoteDay
import app.organi7er.feature.common.TaskUi
import app.organi7er.feature.common.UiMapper
import java.time.LocalDate
import java.time.LocalDateTime

/** Auswahl über Datumspfeile oder Kacheln: Tag (0 = heute … 6) oder die ganze Woche. */
@Immutable
data class OverviewSelection(val dayOffset: Int = 0, val week: Boolean = false) {
    fun prev() = if (week) OverviewSelection() else copy(dayOffset = (dayOffset - 1).coerceAtLeast(0))
    fun next() = if (week) OverviewSelection() else copy(dayOffset = (dayOffset + 1).coerceAtMost(MAX_OFFSET))

    companion object {
        const val MAX_OFFSET = 6
    }
}

@Immutable
sealed interface OverviewSection {
    val key: String

    /** „Als Nächstes“: große Karte + weitere Termine des Tages. */
    data class Next(val hero: EventUi, val until: String, val rest: List<EventUi>) : OverviewSection {
        override val key = "next"
    }

    data class Events(val title: String, val items: List<EventUi>) : OverviewSection {
        override val key = "events-$title"
    }

    data class Tasks(val title: String, val items: List<TaskUi>, val more: Int) : OverviewSection {
        override val key = "tasks-$title"
    }

    data class Hint(val text: String) : OverviewSection {
        override val key = "hint-$text"
    }
}

@Immutable
data class OverviewState(
    val dateLabel: String = "",
    val canPrev: Boolean = false,
    val canNext: Boolean = true,
    val tiles: List<Briefing> = emptyList(),
    val selectedTile: BriefingKind? = BriefingKind.TODAY,
    val sections: List<OverviewSection> = emptyList(),
    val notes: List<NoteDay> = emptyList(),
    val today: LocalDate = LocalDate.now(),
)

/** Reine Ableitung des Übersichts-Inhalts. Leere Abschnitte werden nie als leere Karten gezeigt. */
object OverviewBuilder {
    private const val TASK_LIMIT = 6
    private const val NOTE_LIMIT = 4

    fun build(
        now: LocalDateTime,
        selection: OverviewSelection,
        events: List<Event>,
        calendars: List<CalendarEntity>,
        tasks: List<Task>,
        notes: List<NoteWithImages>,
        tiles: List<Briefing>,
    ): OverviewState {
        val today = now.toLocalDate()
        val active = BriefingCalculator.relevantEvents(events)
        val ui = { e: Event -> UiMapper.event(e, calendars) }
        val sections = mutableListOf<OverviewSection>()

        val dateLabel: String
        val selected: BriefingKind?
        if (selection.week) {
            val end = today.plusDays(6)
            dateLabel = Dates.range(today, end)
            selected = BriefingKind.WEEK
            val rows = (0L..6L).map { today.plusDays(it) }.mapNotNull { day ->
                val dayEvents = active.filter { it.occursOn(day) && it.effectiveEnd!!.isAfter(now) }.sortedBy { it.start }
                if (dayEvents.isEmpty()) return@mapNotNull null
                val main = if (dayEvents.size >= 3) dayEvents.maxBy { java.time.Duration.between(it.start, it.effectiveEnd).toMinutes() } else dayEvents.first()
                EventUi(
                    event = main,
                    time = Dates.weekdayShort(day),
                    timeSub = if (dayEvents.size >= 3) {
                        "${dayEvents.size} Termine"
                    } else if (main.allDay) {
                        "ganzt."
                    } else {
                        Dates.time(main.start!!)
                    },
                    meta = if (dayEvents.size > 1 && dayEvents.size < 3) "+ ${dayEvents.size - 1} weiterer Termin" else UiMapper.eventRangeMeta(main),
                    colorHex = UiMapper.calendarColor(main, calendars),
                    tag = main.subStatus?.label,
                )
            }
            if (rows.isNotEmpty()) sections += OverviewSection.Events("Diese Woche", rows)
            val due = tasks.filter { it.status == Status.ACTIVE && it.dueAt != null && !it.dueAt.toLocalDate().isAfter(end) }
                .sortedWith(compareBy({ it.dueAt }, { it.priority.ordinal * -1 }))
            if (due.isNotEmpty()) sections += OverviewSection.Tasks("Fällig diese Woche", due.take(TASK_LIMIT).map { UiMapper.task(it, today, showDate = true, now) }, due.size - TASK_LIMIT)
            if (sections.isEmpty()) sections += OverviewSection.Hint("Diese Woche ist noch nichts geplant.")
        } else {
            val day = today.plusDays(selection.dayOffset.toLong())
            dateLabel = Dates.longDay(day)
            selected = when (selection.dayOffset) {
                0 -> BriefingKind.TODAY
                1 -> if (tiles.any { it.kind == BriefingKind.TOMORROW }) BriefingKind.TOMORROW else null
                else -> null
            }
            val dayEvents = active.filter { it.occursOn(day) }.sortedWith(compareBy({ !it.allDay }, { it.start }))
            val dayTasks = tasks.filter { t ->
                val due = t.dueAt?.toLocalDate() ?: return@filter false
                when (t.status) {
                    Status.ACTIVE -> if (selection.dayOffset == 0) !due.isAfter(day) else due == day
                    Status.DONE -> due == day && t.doneAt?.toLocal()?.toLocalDate() == today
                    Status.DRAFT -> false
                }
            }.sortedWith(compareBy({ it.status == Status.DONE }, { it.dueAt }, { -it.priority.ordinal }))
            val taskUis = dayTasks.take(TASK_LIMIT).map { UiMapper.task(it, today, showDate = false, now) }
            val weekday = Dates.weekdayLong(day)

            when (selection.dayOffset) {
                0 -> {
                    val upcoming = dayEvents.filter { it.effectiveEnd!!.isAfter(now) }
                    val hero = upcoming.firstOrNull { !it.allDay }
                    if (hero != null) {
                        val until = if (!hero.start!!.isAfter(now)) "läuft" else Dates.until(hero.start, now)
                        sections += OverviewSection.Next(ui(hero), until, upcoming.filter { it.id != hero.id }.map(ui))
                    } else if (upcoming.isNotEmpty()) {
                        sections += OverviewSection.Events("Heute", upcoming.map(ui))
                    }
                    if (upcoming.isEmpty()) sections += OverviewSection.Hint(if (dayEvents.isEmpty()) "Heute keine Termine." else "Keine Termine mehr heute.")
                    if (taskUis.isNotEmpty()) sections += OverviewSection.Tasks("Aufgaben heute", taskUis, dayTasks.size - TASK_LIMIT)
                }
                1 -> {
                    if (dayEvents.isNotEmpty()) sections += OverviewSection.Events("Termine morgen", dayEvents.map(ui))
                    if (taskUis.isNotEmpty()) sections += OverviewSection.Tasks("Aufgaben morgen", taskUis, dayTasks.size - TASK_LIMIT)
                    if (dayEvents.isEmpty() && taskUis.isEmpty()) {
                        sections += OverviewSection.Hint("Für morgen ist noch nichts geplant.")
                    } else if (dayEvents.isEmpty()) {
                        sections += OverviewSection.Hint("Keine Termine an diesem Tag.")
                    }
                }
                else -> {
                    if (dayEvents.isNotEmpty()) sections += OverviewSection.Events("Termine am $weekday", dayEvents.map(ui))
                    if (taskUis.isNotEmpty()) sections += OverviewSection.Tasks("Aufgaben am $weekday", taskUis, dayTasks.size - TASK_LIMIT)
                    if (dayEvents.isEmpty() && taskUis.isEmpty()) {
                        sections += OverviewSection.Hint("An diesem Tag ist nichts geplant.")
                    } else if (dayEvents.isEmpty()) {
                        sections += OverviewSection.Hint("Keine Termine an diesem Tag.")
                    }
                }
            }
        }

        val feed = notes.filter { it.note.status == Status.ACTIVE }.sortedByDescending { it.note.createdAt }.take(NOTE_LIMIT)
        return OverviewState(
            dateLabel = dateLabel,
            canPrev = selection.week || selection.dayOffset > 0,
            canNext = selection.week || selection.dayOffset < OverviewSelection.MAX_OFFSET,
            tiles = tiles,
            selectedTile = selected,
            sections = sections,
            notes = UiMapper.noteDays(feed, today),
            today = today,
        )
    }
}

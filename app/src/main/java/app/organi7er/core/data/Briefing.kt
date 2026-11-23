package app.organi7er.core.data

import androidx.compose.runtime.Immutable
import app.organi7er.core.model.Dates
import app.organi7er.core.model.Event
import app.organi7er.core.model.EventSubStatus
import app.organi7er.core.model.NoteWithImages
import app.organi7er.core.model.Status
import app.organi7er.core.model.Task
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn

enum class BriefingKind { TODAY, TOMORROW, WEEK }

enum class BriefingIcon { SCHEDULE, CALENDAR, CHECKLIST }

@Immutable
data class Briefing(
    val kind: BriefingKind,
    val title: String,
    val events: Int,
    val tasks: Int,
    val notes: Int,
    val big: String,
    val icon: BriefingIcon,
    val caption: String,
    /** 0..1, nur wenn in den nächsten 60 Minuten etwas beginnt. */
    val progress: Float? = null,
)

/** Reine Berechnung der Briefing-Kacheln – ohne Android, gut testbar. */
object BriefingCalculator {
    fun relevantEvents(events: List<Event>): List<Event> =
        events.filter { it.status == Status.ACTIVE && it.start != null && it.subStatus != EventSubStatus.CANCELLED }

    fun activeTasks(tasks: List<Task>): List<Task> = tasks.filter { it.status == Status.ACTIVE && it.dueAt != null }

    fun compute(
        now: LocalDateTime,
        events: List<Event>,
        tasks: List<Task>,
        notes: List<NoteWithImages>,
        showTomorrow: Boolean,
    ): List<Briefing> {
        val today = now.toLocalDate()
        val ev = relevantEvents(events).sortedBy { it.start }
        val open = activeTasks(tasks)
        val reminders = notes.mapNotNull { n -> n.note.reminderAt?.takeIf { n.note.status != Status.DONE } }
        return buildList {
            add(today(now, ev, open, reminders))
            if (showTomorrow) add(tomorrow(today.plusDays(1), ev, open, reminders))
            add(week(now, ev, open, reminders))
        }
    }

    private fun today(now: LocalDateTime, events: List<Event>, tasks: List<Task>, reminders: List<LocalDateTime>): Briefing {
        val day = now.toLocalDate()
        val dayEvents = events.filter { it.occursOn(day) }
        val dueTasks = tasks.count { !it.dueAt!!.toLocalDate().isAfter(day) }
        val noteCount = reminders.count { it.toLocalDate() == day }
        val next = dayEvents.filter { !it.allDay && it.effectiveEnd!!.isAfter(now) }.minByOrNull { it.start!! }
        val base = Briefing(BriefingKind.TODAY, "Heute", dayEvents.size, dueTasks, noteCount, "", BriefingIcon.SCHEDULE, "")
        return when {
            next != null && !next.start!!.isAfter(now) -> base.copy(
                big = "läuft",
                caption = "${next.title} · bis ${Dates.time(next.effectiveEnd!!)}",
            )
            next != null -> {
                val minutes = ChronoUnit.MINUTES.between(now, next.start)
                base.copy(
                    big = if (minutes < 60) "in ${minutes.coerceAtLeast(1)} Min" else "um ${Dates.time(next.start!!)}",
                    caption = "${next.title} · ${Dates.time(next.start!!)}",
                    progress = if (minutes < 60) (1f - minutes / 60f).coerceIn(0.02f, 1f) else null,
                )
            }
            dueTasks > 0 -> base.copy(big = "$dueTasks offen", icon = BriefingIcon.CHECKLIST, caption = "Keine Termine mehr heute")
            else -> base.copy(big = "frei", icon = BriefingIcon.CALENDAR, caption = "Nichts mehr geplant")
        }
    }

    private fun tomorrow(day: LocalDate, events: List<Event>, tasks: List<Task>, reminders: List<LocalDateTime>): Briefing {
        val dayEvents = events.filter { it.occursOn(day) }
        val dueTasks = tasks.count { it.dueAt!!.toLocalDate() == day }
        val base = Briefing(
            BriefingKind.TOMORROW,
            "Morgen",
            dayEvents.size,
            dueTasks,
            reminders.count { it.toLocalDate() == day },
            "",
            BriefingIcon.SCHEDULE,
            "",
        )
        val timed = dayEvents.filter { !it.allDay }
        val first = timed.firstOrNull() ?: dayEvents.firstOrNull()
        return when {
            first == null -> base.copy(
                big = "frei",
                icon = BriefingIcon.CALENDAR,
                caption = if (dueTasks > 0) "$dueTasks ${if (dueTasks == 1) "Aufgabe" else "Aufgaben"}" else "Noch nichts geplant",
            )
            first.allDay -> base.copy(big = "ganztägig", icon = BriefingIcon.CALENDAR, caption = first.title)
            else -> base.copy(
                big = "ab ${Dates.time(first.start!!)}",
                caption = first.title + if (dayEvents.size > 1) " · +${dayEvents.size - 1}" else "",
            )
        }
    }

    private fun week(now: LocalDateTime, events: List<Event>, tasks: List<Task>, reminders: List<LocalDateTime>): Briefing {
        val from = now.toLocalDate()
        val days = (0L..6L).map { from.plusDays(it) }
        val to = days.last()
        val inWeek = events.filter { e -> days.any { e.occursOn(it) } && e.effectiveEnd!!.isAfter(now) }
        val dueTasks = tasks.count { !it.dueAt!!.toLocalDate().isAfter(to) }
        val noteCount = reminders.count { !it.toLocalDate().isBefore(from) && !it.toLocalDate().isAfter(to) }
        val perDay = days.associateWith { d -> events.count { it.occursOn(d) } }
        val busiest = perDay.maxByOrNull { it.value }
        val base = Briefing(BriefingKind.WEEK, "Woche", inWeek.size, dueTasks, noteCount, "", BriefingIcon.CALENDAR, "")
        return when {
            inWeek.isEmpty() -> base.copy(big = "frei", caption = if (dueTasks > 0) "$dueTasks Aufgaben fällig" else "Keine Termine")
            busiest != null && busiest.value >= 3 -> {
                val dayEvents = events.filter { it.occursOn(busiest.key) }
                val longest = dayEvents.maxByOrNull { ChronoUnit.MINUTES.between(it.start, it.effectiveEnd) }
                base.copy(
                    big = "${Dates.weekdayShort(busiest.key)} voll",
                    caption = "${busiest.value} Termine · ${longest?.title.orEmpty()}",
                )
            }
            else -> {
                val next = inWeek.first()
                base.copy(
                    big = "${inWeek.size} ${if (inWeek.size == 1) "Termin" else "Termine"}",
                    caption = "${Dates.weekdayShort(next.start!!.toLocalDate())} · ${next.title}",
                )
            }
        }
    }
}

@Singleton
class BriefingRepository @Inject constructor(
    events: EventRepository,
    tasks: TaskRepository,
    notes: NoteRepository,
    settings: SettingsRepository,
    time: TimeProvider,
) {
    /** Aktualisiert sich bei jeder Datenänderung und jede Minute. */
    val briefings: Flow<List<Briefing>> = combine(
        events.events,
        tasks.tasks,
        notes.notes,
        settings.settings,
        time.minuteTicks,
    ) { e, t, n, s, now -> BriefingCalculator.compute(now, e, t, n, s.showTomorrowTile) }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)
}

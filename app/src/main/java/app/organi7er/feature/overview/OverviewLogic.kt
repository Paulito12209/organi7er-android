package app.organi7er.feature.overview

import androidx.compose.runtime.Immutable
import app.organi7er.core.data.BriefingCalculator
import app.organi7er.core.designsystem.components.EntryType
import app.organi7er.core.model.Dates
import app.organi7er.core.model.Dates.toLocal
import app.organi7er.core.model.Event
import app.organi7er.core.model.NoteWithImages
import app.organi7er.core.model.NotificationEntry
import app.organi7er.core.model.Status
import app.organi7er.core.model.Task
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields

/** Eine Tageskachel. Die gewählte ist groß und quadratisch, die übrigen schmale Streifen. */
@Immutable
data class DayTileUi(
    val date: LocalDate,
    /** „M“, „D“, „M“, „D“, „F“, „S“, „S“. */
    val letter: String,
    /** „Di 24“ in der großen Kachel. */
    val short: String,
    val number: String,
    /** „Heute“, „Morgen“, „Mittwoch“ … */
    val headline: String,
    val events: Int,
    val tasks: Int,
    val notes: Int,
    val isToday: Boolean,
    /** „Als Nächstes · 10:00“, „Aufgabe“ oder leer. */
    val nextLabel: String,
    val nextTitle: String,
    /** Termin hinter „Als Nächstes“ (für den Pfeil-Knopf). */
    val nextEventId: Long? = null,
    /** Link des nächsten Termins (Remote-Meeting); leer = keiner. */
    val nextLink: String = "",
    /** Titelbild des nächsten Termins; null = keins. Füllt dann die aktive Kachel. */
    val nextImage: String? = null,
    /** Mindestens ein Geburtstag an diesem Tag. */
    val birthday: Boolean = false,
    val a11y: String,
)

@Immutable
data class WeekUi(val start: LocalDate, val number: Int, val isCurrent: Boolean)

/** Kurzer Eintrag für „Im Fokus“ und den Verlauf. */
@Immutable
data class EntryUi(val type: EntryType, val id: Long, val title: String, val meta: String, val whenLabel: String = "")

@Immutable
data class OverviewState(
    val today: LocalDate = LocalDate.now(),
    val selected: LocalDate = LocalDate.now(),
    val days: List<DayTileUi> = emptyList(),
    val weeks: List<WeekUi> = emptyList(),
    val selectedWeek: Int = 0,
    val unread: Int = 0,
    val focus: List<EntryUi> = emptyList(),
    val opened: List<EntryUi> = emptyList(),
    val created: List<EntryUi> = emptyList(),
)

/** Reine Ableitung der Übersicht. */
object OverviewBuilder {
    private const val WEEKS_BACK = 8
    private const val WEEKS_AHEAD = 16
    private const val FEED_LIMIT = 8
    private val LETTERS = mapOf(
        DayOfWeek.MONDAY to "M",
        DayOfWeek.TUESDAY to "D",
        DayOfWeek.WEDNESDAY to "M",
        DayOfWeek.THURSDAY to "D",
        DayOfWeek.FRIDAY to "F",
        DayOfWeek.SATURDAY to "S",
        DayOfWeek.SUNDAY to "S",
    )

    fun weekFields(weekStart: Int): WeekFields = if (weekStart == 7) WeekFields.SUNDAY_START else WeekFields.ISO

    fun weekStartOf(date: LocalDate, weekStart: Int): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(weekFields(weekStart).firstDayOfWeek))

    fun build(
        now: LocalDateTime,
        selected: LocalDate,
        weekStart: Int,
        events: List<Event>,
        tasks: List<Task>,
        notes: List<NoteWithImages>,
        log: List<NotificationEntry>,
    ): OverviewState {
        val today = now.toLocalDate()
        val active = BriefingCalculator.relevantEvents(events)
        val fields = weekFields(weekStart)
        val monday = weekStartOf(selected, weekStart)

        val days = (0L..6L).map { o ->
            val day = monday.plusDays(o)
            val dayEvents = active.filter { it.occursOn(day) }.sortedWith(compareBy({ !it.allDay }, { it.start }))
            val dayTasks = tasks.filter { t ->
                val due = t.dueAt?.toLocalDate() ?: return@filter false
                when (t.status) {
                    Status.ACTIVE -> if (day == today) !due.isAfter(day) else due == day
                    Status.DONE -> due == day
                    Status.DRAFT -> false
                }
            }
            val dayNotes = notes.count { it.note.status == Status.ACTIVE && it.note.reminderAt?.toLocalDate() == day }
            val upcoming = if (day == today) dayEvents.filter { it.effectiveEnd!!.isAfter(now) } else dayEvents
            val next = upcoming.firstOrNull { !it.allDay } ?: upcoming.firstOrNull()
            val openTask = dayTasks.firstOrNull { it.status == Status.ACTIVE }
            val (label, title) = when {
                next != null && next.allDay -> "Ganztägig" to next.title
                next != null -> "${if (day == today) "Als Nächstes" else "Erster Termin"} · ${Dates.time(next.start!!)}" to next.title
                openTask != null -> "Aufgabe" to openTask.title
                day.isBefore(today) -> "" to "Vorbei"
                else -> "" to "Nichts geplant"
            }
            val headline = when (ChronoUnit.DAYS.between(today, day)) {
                0L -> "Heute"
                1L -> "Morgen"
                -1L -> "Gestern"
                else -> Dates.weekdayLong(day)
            }
            DayTileUi(
                date = day,
                letter = LETTERS.getValue(day.dayOfWeek),
                short = "${Dates.weekdayShort(day)} ${day.dayOfMonth}",
                number = "${day.dayOfMonth}",
                headline = headline,
                events = dayEvents.size,
                tasks = dayTasks.count { it.status == Status.ACTIVE },
                notes = dayNotes,
                isToday = day == today,
                nextLabel = label,
                nextTitle = title,
                nextEventId = next?.id,
                nextLink = next?.link.orEmpty(),
                nextImage = next?.imagePath,
                birthday = dayEvents.any { it.isBirthday },
                a11y = "${Dates.longDay(day)}: ${dayEvents.size} Termine, ${dayTasks.size} Aufgaben" +
                    if (title.isNotBlank()) ". $label $title" else "",
            )
        }

        val currentWeek = weekStartOf(today, weekStart)
        val weeks = (-WEEKS_BACK..WEEKS_AHEAD).map { w ->
            val start = currentWeek.plusWeeks(w.toLong())
            WeekUi(start, start.plusDays(3).get(fields.weekOfWeekBasedYear()), w == 0)
        }
        val selectedWeek = weeks.indexOfFirst { it.start == monday }.coerceAtLeast(0)

        val nowInstant = now.atZone(java.time.ZoneId.systemDefault()).toInstant()
        val all = buildList {
            events.forEach { e -> add(Item(EntryType.Event, e.id, e.title, eventMeta(e, today), e.focused, e.openedAt, e.createdAt)) }
            tasks.forEach { t -> add(Item(EntryType.Task, t.id, t.title, taskMeta(t, today), t.focused && t.status != Status.DONE, t.openedAt, t.createdAt)) }
            notes.forEach { n -> add(Item(EntryType.Note, n.note.id, n.note.title.ifBlank { n.note.body.lineSequence().firstOrNull().orEmpty().take(40).ifBlank { "Ohne Titel" } }, if (n.note.status == Status.DRAFT) "Notiz · Entwurf" else "Notiz", n.note.focused, n.note.openedAt, n.note.createdAt)) }
        }
        val focus = all.filter { it.focused }.sortedByDescending { it.opened ?: it.created }.map { it.ui("") }
        val opened = all.filter { it.opened != null }.sortedByDescending { it.opened }.take(FEED_LIMIT).map { it.ui(ago(it.opened!!, nowInstant, today)) }
        val created = all.sortedByDescending { it.created }.take(FEED_LIMIT).map { it.ui(ago(it.created, nowInstant, today)) }

        return OverviewState(
            today = today,
            selected = selected,
            days = days,
            weeks = weeks,
            selectedWeek = selectedWeek,
            unread = log.count { !it.read },
            focus = focus,
            opened = opened,
            created = created,
        )
    }

    private data class Item(val type: EntryType, val id: Long, val title: String, val meta: String, val focused: Boolean, val opened: Instant?, val created: Instant) {
        fun ui(whenLabel: String) = EntryUi(type, id, title, meta, whenLabel)
    }

    private fun eventMeta(e: Event, today: LocalDate): String {
        val start = e.start ?: return "Termin · Entwurf"
        val day = Dates.dueLabel(start.toLocalDate(), today).replaceFirstChar(Char::uppercase)
        return listOfNotNull("Termin", if (e.allDay) day else "$day, ${Dates.time(start)}", e.location.ifBlank { null }).joinToString(" · ")
    }

    private fun taskMeta(t: Task, today: LocalDate): String = when {
        t.status == Status.DONE -> "Aufgabe · erledigt"
        t.dueAt == null -> "Aufgabe · Entwurf"
        else -> "Aufgabe · fällig ${Dates.dueLabel(t.dueAt.toLocalDate(), today)}"
    }

    /** „gerade eben“, „vor 5 Min“, „08:12“ (heute), „gestern“, „Mo, 23. Nov.“ */
    fun ago(at: Instant, now: Instant, today: LocalDate): String {
        val minutes = Duration.between(at, now).toMinutes()
        val local = at.toLocal()
        return when {
            minutes < 1 -> "gerade eben"
            minutes < 60 -> "vor $minutes Min"
            local.toLocalDate() == today -> Dates.time(local)
            local.toLocalDate() == today.minusDays(1) -> "gestern"
            else -> Dates.shortDate(local.toLocalDate())
        }
    }
}

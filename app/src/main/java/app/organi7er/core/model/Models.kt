package app.organi7er.core.model

import androidx.compose.runtime.Immutable
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

/** Lebenszyklus, gleich für Termine, Aufgaben und Notizen. */
enum class Status { DRAFT, ACTIVE, DONE }

enum class EventSubStatus(val label: String) { TENTATIVE("Vorläufig"), POSTPONED("Verschoben"), CANCELLED("Abgesagt") }

enum class TaskSubStatus(val label: String) { IN_PROGRESS("In Arbeit"), BLOCKED("Geblockt"), POSTPONED("Verschoben") }

enum class NoteSubStatus(val label: String) { IN_PROGRESS("In Arbeit") }

enum class Priority { NORMAL, HIGH }

enum class ThemeMode(val label: String) { SYSTEM("System"), LIGHT("Hell"), DARK("Dunkel") }

@Immutable
@Entity(tableName = "calendars")
data class CalendarEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorHex: String,
)

@Immutable
@Entity(
    tableName = "events",
    indices = [Index("start"), Index("status"), Index("calendarId")],
)
data class Event(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    /** null bei Entwürfen (Termin-Idee ohne feste Zeit). */
    val start: LocalDateTime? = null,
    val end: LocalDateTime? = null,
    val allDay: Boolean = false,
    val location: String = "",
    val calendarId: Long = 0,
    val reminderMinutes: Int? = null,
    val note: String = "",
    val status: Status = Status.ACTIVE,
    val subStatus: EventSubStatus? = null,
    /** Ursprüngliche Zeit, wenn ein Termin verschoben wurde („vorher 11:00“). */
    val previousStart: LocalDateTime? = null,
    /** „Im Fokus“ auf der Übersicht (angeheftet). */
    val focused: Boolean = false,
    /** Zuletzt geöffnet, für den Verlauf. */
    val openedAt: Instant? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
) {
    /** Ende für Berechnungen: ganztägig = Tagesende, sonst Ende oder Start + 1 h. */
    val effectiveEnd: LocalDateTime?
        get() = when {
            start == null -> null
            allDay -> (end ?: start).toLocalDate().plusDays(1).atStartOfDay()
            else -> end ?: start.plusHours(1)
        }

    /** Vergangene aktive Termine gelten automatisch als erledigt. */
    fun effectiveStatus(now: LocalDateTime): Status = when {
        status == Status.ACTIVE && effectiveEnd?.isBefore(now) == true -> Status.DONE
        else -> status
    }

    fun occursOn(day: LocalDate): Boolean {
        val s = start ?: return false
        val e = effectiveEnd ?: return false
        return !s.toLocalDate().isAfter(day) && e.isAfter(day.atStartOfDay())
    }
}

@Immutable
@Entity(tableName = "tasks", indices = [Index("dueAt"), Index("status")])
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val note: String = "",
    /** null = kein Datum. Ohne Datum ist eine Aufgabe ein Entwurf. */
    val dueAt: LocalDateTime? = null,
    /** false = nur ein Tag, die Uhrzeit wird nicht angezeigt. */
    val dueHasTime: Boolean = false,
    val priority: Priority = Priority.NORMAL,
    val status: Status = Status.ACTIVE,
    val subStatus: TaskSubStatus? = null,
    val doneAt: Instant? = null,
    val listName: String = "",
    /** „Im Fokus“ auf der Übersicht (angeheftet). */
    val focused: Boolean = false,
    /** Zuletzt geöffnet, für den Verlauf. */
    val openedAt: Instant? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)

@Immutable
@Entity(tableName = "notes", indices = [Index("status"), Index("createdAt")])
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val body: String = "",
    val reminderAt: LocalDateTime? = null,
    val status: Status = Status.ACTIVE,
    val subStatus: NoteSubStatus? = null,
    /** „Im Fokus“ auf der Übersicht (angeheftet). */
    val focused: Boolean = false,
    /** Zuletzt geöffnet, für den Verlauf. */
    val openedAt: Instant? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)

@Immutable
@Entity(
    tableName = "note_images",
    foreignKeys = [ForeignKey(entity = Note::class, parentColumns = ["id"], childColumns = ["noteId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("noteId")],
)
data class NoteImage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val noteId: Long,
    val filePath: String,
    val position: Int,
)

@Immutable
data class NoteWithImages(
    @Embedded val note: Note,
    @Relation(parentColumn = "id", entityColumn = "noteId")
    val images: List<NoteImage>,
) {
    val sortedImages: List<NoteImage> get() = images.sortedBy { it.position }
}

/** Mitschrift jeder ausgelösten Benachrichtigung für die Seite „Benachrichtigungen“. */
@Immutable
@Entity(tableName = "notification_log", indices = [Index("createdAt")])
data class NotificationEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Kanal: reminders, briefing, drafts. */
    val kind: String,
    val title: String,
    val text: String,
    val createdAt: Instant = Instant.now(),
    val read: Boolean = false,
)

@Immutable
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val showTomorrowTile: Boolean = true,
    /** 1 = Montag, 7 = Sonntag (java.time.DayOfWeek). */
    val weekStart: Int = 1,
    val reminderLeadMinutes: Int = 15,
    val morningBriefing: Boolean = true,
    /** Minuten nach Mitternacht. */
    val morningBriefingTime: Int = 7 * 60 + 30,
    val draftReminders: Boolean = false,
    val sampleDataSeeded: Boolean = false,
    val profileName: String = "",
    /** Absoluter Pfad im App-Speicher, null = noch kein Profilbild. */
    val profileImage: String? = null,
)

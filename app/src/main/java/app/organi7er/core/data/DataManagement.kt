package app.organi7er.core.data

import androidx.room.withTransaction
import app.organi7er.core.data.db.OrganiDatabase
import app.organi7er.core.designsystem.CalendarPalette
import app.organi7er.core.model.CalendarEntity
import app.organi7er.core.model.Event
import app.organi7er.core.model.EventSubStatus
import app.organi7er.core.model.Note
import app.organi7er.core.model.NoteImage
import app.organi7er.core.model.NoteSubStatus
import app.organi7er.core.model.Priority
import app.organi7er.core.model.Status
import app.organi7er.core.model.Task
import app.organi7er.core.model.TaskSubStatus
import java.time.Instant
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Beispieldaten wie in der HTML-Vorlage, relativ zu „jetzt“ angelegt. */
@Singleton
class SampleData @Inject constructor(
    private val db: OrganiDatabase,
    private val images: ImageStore,
    private val settings: SettingsRepository,
    private val reminders: ReminderScheduler,
) {
    suspend fun seedIfNeeded() {
        val seeded = settings.settings.first().sampleDataSeeded
        if (seeded) return
        if (db.calendarDao().getAll().isEmpty()) seed()
        settings.update { it.copy(sampleDataSeeded = true) }
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        db.eventDao().getAll().forEach { reminders.cancelEvent(it.id) }
        db.noteDao().getAll().forEach { reminders.cancelNote(it.id) }
        db.withTransaction {
            db.notificationDao().deleteAll()
            db.noteDao().deleteAll()
            db.taskDao().deleteAll()
            db.eventDao().deleteAll()
        }
        images.deleteAll()
    }

    private suspend fun seed() = withContext(Dispatchers.IO) {
        val now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)
        val today = now.toLocalDate()
        // Nächster Termin startet in rund 25 Minuten – so zeigt die Übersicht sofort Fortschritt und „in X Min“.
        val soon = now.plusMinutes(25L - (now.minute % 5))
        val ago = { days: Long -> Instant.now().minus(days, ChronoUnit.DAYS) }

        val work = db.calendarDao().upsert(CalendarEntity(name = "Arbeit", colorHex = CalendarPalette.WORK))
        val private = db.calendarDao().upsert(CalendarEntity(name = "Privat", colorHex = CalendarPalette.PRIVATE))
        val health = db.calendarDao().upsert(CalendarEntity(name = "Gesundheit", colorHex = CalendarPalette.HEALTH))

        val tomorrow = today.plusDays(1)
        val friday = today.plusDays(4)
        val events = listOf(
            Event(title = "Laufen", start = now.minusHours(3), end = now.minusHours(3).plusMinutes(40), location = "Stadtpark", calendarId = health),
            Event(title = "Sprint-Review Organi7er", start = soon, end = soon.plusMinutes(45), location = "Google Meet", calendarId = work, reminderMinutes = 15, link = "https://meet.google.com/"),
            Event(title = "Mittag mit Lena", start = soon.plusHours(3), end = soon.plusHours(4), location = "Café Lindner", calendarId = private),
            Event(
                title = "Zahnarzt",
                start = soon.plusHours(5).plusMinutes(30),
                end = soon.plusHours(6),
                location = "Dr. Weber, Hauptstr. 12",
                calendarId = health,
                subStatus = EventSubStatus.POSTPONED,
                previousStart = soon.plusHours(1),
            ),
            Event(title = "Kino", start = soon.plusHours(8).plusMinutes(30), end = soon.plusHours(10).plusMinutes(30), location = "mit Max", calendarId = private, subStatus = EventSubStatus.TENTATIVE),
            Event(title = "Daily und Planung", start = tomorrow.atTime(9, 0), end = tomorrow.atTime(10, 0), location = "Büro", calendarId = work),
            Event(title = "Yoga", start = tomorrow.atTime(18, 30), end = tomorrow.atTime(20, 0), location = "Studio Mitte", calendarId = health),
            Event(title = "Geburtstag Mama", start = today.plusDays(3).atStartOfDay(), allDay = true, calendarId = private, focused = true),
            Event(title = "Frühstück mit dem Team", start = friday.atTime(8, 30), end = friday.atTime(9, 30), location = "Büro", calendarId = work, focused = true),
            Event(title = "Workshop Q1-Planung", start = friday.atTime(10, 0), end = friday.atTime(16, 0), location = "Büro", calendarId = work),
            Event(title = "Retro", start = friday.atTime(16, 30), end = friday.atTime(17, 15), location = "Büro", calendarId = work),
            Event(title = "Afterwork", start = friday.atTime(18, 0), end = friday.atTime(20, 0), location = "Bar Central", calendarId = private),
            Event(title = "Brunch bei Anna", start = today.minusDays(1).atTime(11, 0), end = today.minusDays(1).atTime(13, 0), calendarId = private),
            Event(title = "Kick-off Organi7er", start = today.minusDays(3).atTime(14, 0), end = today.minusDays(3).atTime(15, 0), location = "Büro", calendarId = work),
            Event(title = "Abendessen mit Jonas", status = Status.DRAFT, note = "irgendwann im Dezember", calendarId = private, createdAt = ago(2)),
            Event(title = "Friseur", status = Status.DRAFT, calendarId = private, createdAt = ago(2)),
        )
        events.forEach { e ->
            val id = db.eventDao().insert(e)
            reminders.scheduleEvent(e.copy(id = id))
        }

        val tasks = listOf(
            Task(title = "Steuerunterlagen an Lena", dueAt = today.atTime(18, 0), dueHasTime = true, priority = Priority.HIGH, listName = "Privat", focused = true),
            Task(title = "Komponenten-Screenshots sortieren", dueAt = today.atStartOfDay(), listName = "Organi7er", subStatus = TaskSubStatus.IN_PROGRESS),
            Task(title = "Paket zur Post bringen", dueAt = today.atTime(17, 0), dueHasTime = true, listName = "Privat", subStatus = TaskSubStatus.BLOCKED),
            Task(title = "Rückruf Versicherung", dueAt = today.minusDays(1).atStartOfDay(), listName = "Privat"),
            Task(title = "Farbpalette festlegen", dueAt = today.atStartOfDay(), listName = "Organi7er", status = Status.DONE, doneAt = Instant.now()),
            Task(title = "Compose-Theme anlegen", dueAt = tomorrow.atStartOfDay(), listName = "Organi7er"),
            Task(title = "Rechnung Handy prüfen", dueAt = tomorrow.atStartOfDay(), listName = "Privat"),
            Task(title = "Blumen gießen", dueAt = tomorrow.atStartOfDay(), listName = "Zuhause"),
            Task(title = "Geburtstagsgeschenk Mama", dueAt = today.plusDays(3).atStartOfDay(), priority = Priority.HIGH, listName = "Privat"),
            Task(title = "Steuererklärung abschicken", dueAt = today.plusDays(4).atStartOfDay(), listName = "Privat"),
            Task(title = "Reifen wechseln", dueAt = today.plusDays(5).atStartOfDay(), listName = "Privat", subStatus = TaskSubStatus.POSTPONED),
            Task(title = "Website-Portfolio überarbeiten", status = Status.DRAFT, createdAt = ago(3)),
            Task(title = "Urlaub im Frühjahr?", status = Status.DRAFT, createdAt = ago(8)),
            Task(title = "Fahrrad zur Inspektion", status = Status.DRAFT, createdAt = ago(1)),
            Task(title = "Repo anlegen", dueAt = today.minusDays(1).atStartOfDay(), listName = "Organi7er", status = Status.DONE, doneAt = ago(1)),
        )
        db.taskDao().insertAll(tasks)

        val n1 = db.noteDao().insert(
            Note(title = "Ideen Startseite", body = "Datum mittig mit Pfeilen. Darunter Kacheln für heute, morgen und die Woche – wischbar wie bei PayPal.", createdAt = Instant.now().minus(2, ChronoUnit.HOURS)),
        )
        listOf(0, 2, 3).forEachIndexed { i, v -> db.noteDao().insertImage(NoteImage(noteId = n1, filePath = images.createSampleImage(v), position = i)) }
        val n2 = db.noteDao().insert(
            Note(title = "Whiteboard Planung", body = "Q1: Kalender-Sync, Widgets, Erinnerungen. Danach Teilen von Listen.", createdAt = ago(1), subStatus = NoteSubStatus.IN_PROGRESS),
        )
        db.noteDao().insertImage(NoteImage(noteId = n2, filePath = images.createSampleImage(1), position = 0))
        val shopping = Note(
            title = "Einkauf Wochenende",
            body = "Hafermilch, Brot, Äpfel, Kaffee, Batterien AA, Geschenkpapier.",
            reminderAt = tomorrow.atTime(9, 0),
            createdAt = ago(1).minus(3, ChronoUnit.HOURS),
        )
        reminders.scheduleNote(shopping.copy(id = db.noteDao().insert(shopping)))
        db.noteDao().insert(Note(body = "Gute Idee für den Onboarding-Screen: erst die drei Erfassen-Knöpfe zeigen, dann die Kacheln.", status = Status.DRAFT, createdAt = Instant.now().minus(90, ChronoUnit.MINUTES)))
        db.noteDao().insert(Note(title = "Umzug Checkliste", body = "Alles erledigt, Kaution zurück.", status = Status.DONE, createdAt = ago(12)))
    }
}

// ---------------------------------------------------------------- Sicherung (JSON)

@Serializable
data class BackupFile(
    val version: Int = 1,
    val createdAt: String,
    val calendars: List<BackupCalendar>,
    val events: List<BackupEvent>,
    val tasks: List<BackupTask>,
    val notes: List<BackupNote>,
)

@Serializable data class BackupCalendar(val id: Long, val name: String, val colorHex: String)

@Serializable
data class BackupEvent(
    val id: Long,
    val title: String,
    val start: String?,
    val end: String?,
    val allDay: Boolean,
    val location: String,
    val calendarId: Long,
    val reminderMinutes: Int?,
    val note: String,
    val status: String,
    val subStatus: String?,
    val previousStart: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val link: String = "",
)

@Serializable
data class BackupTask(
    val id: Long,
    val title: String,
    val note: String,
    val dueAt: String?,
    val dueHasTime: Boolean,
    val priority: String,
    val status: String,
    val subStatus: String?,
    val doneAt: Long?,
    val listName: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class BackupNote(
    val id: Long,
    val title: String,
    val body: String,
    val reminderAt: String?,
    val status: String,
    val subStatus: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

@Singleton
class BackupManager @Inject constructor(private val db: OrganiDatabase, private val reminders: ReminderScheduler) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    suspend fun export(): String = withContext(Dispatchers.IO) {
        val file = BackupFile(
            createdAt = LocalDateTime.now().toString(),
            calendars = db.calendarDao().getAll().map { BackupCalendar(it.id, it.name, it.colorHex) },
            events = db.eventDao().getAll().map {
                BackupEvent(
                    it.id, it.title, it.start?.toString(), it.end?.toString(), it.allDay, it.location, it.calendarId, it.reminderMinutes,
                    it.note, it.status.name, it.subStatus?.name, it.previousStart?.toString(), it.createdAt.toEpochMilli(), it.updatedAt.toEpochMilli(), it.link,
                )
            },
            tasks = db.taskDao().getAll().map {
                BackupTask(
                    it.id, it.title, it.note, it.dueAt?.toString(), it.dueHasTime, it.priority.name, it.status.name, it.subStatus?.name,
                    it.doneAt?.toEpochMilli(), it.listName, it.createdAt.toEpochMilli(), it.updatedAt.toEpochMilli(),
                )
            },
            notes = db.noteDao().getAll().map {
                BackupNote(it.id, it.title, it.body, it.reminderAt?.toString(), it.status.name, it.subStatus?.name, it.createdAt.toEpochMilli(), it.updatedAt.toEpochMilli())
            },
        )
        json.encodeToString(BackupFile.serializer(), file)
    }

    /** Ersetzt alle Daten durch die Sicherung. Bilder sind nicht Teil der Sicherung. */
    suspend fun import(text: String): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val file = json.decodeFromString(BackupFile.serializer(), text)
            val dt = { s: String? -> s?.let(LocalDateTime::parse) }
            db.withTransaction {
                db.notificationDao().deleteAll()
                db.noteDao().deleteAll()
                db.taskDao().deleteAll()
                db.eventDao().deleteAll()
                db.calendarDao().deleteAll()
                db.calendarDao().insertAll(file.calendars.map { CalendarEntity(it.id, it.name, it.colorHex) })
                db.eventDao().insertAll(
                    file.events.map {
                        Event(
                            it.id, it.title, dt(it.start), dt(it.end), it.allDay, it.location, it.calendarId, it.reminderMinutes, it.note,
                            Status.valueOf(it.status), it.subStatus?.let(EventSubStatus::valueOf), dt(it.previousStart),
                            createdAt = Instant.ofEpochMilli(it.createdAt), updatedAt = Instant.ofEpochMilli(it.updatedAt), link = it.link,
                        )
                    },
                )
                db.taskDao().insertAll(
                    file.tasks.map {
                        Task(
                            it.id, it.title, it.note, dt(it.dueAt), it.dueHasTime, Priority.valueOf(it.priority), Status.valueOf(it.status),
                            it.subStatus?.let(TaskSubStatus::valueOf), it.doneAt?.let(Instant::ofEpochMilli), it.listName,
                            createdAt = Instant.ofEpochMilli(it.createdAt), updatedAt = Instant.ofEpochMilli(it.updatedAt),
                        )
                    },
                )
                db.noteDao().insertAll(
                    file.notes.map {
                        Note(
                            it.id,
                            it.title,
                            it.body,
                            dt(it.reminderAt),
                            Status.valueOf(it.status),
                            it.subStatus?.let(NoteSubStatus::valueOf),
                            createdAt = Instant.ofEpochMilli(it.createdAt),
                            updatedAt = Instant.ofEpochMilli(it.updatedAt),
                        )
                    },
                )
            }
            db.eventDao().getAll().forEach(reminders::scheduleEvent)
            db.noteDao().getAll().forEach(reminders::scheduleNote)
            file.events.size + file.tasks.size + file.notes.size
        }
    }
}

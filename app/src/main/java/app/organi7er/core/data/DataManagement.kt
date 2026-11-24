package app.organi7er.core.data

import androidx.room.withTransaction
import app.organi7er.R
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

    /**
     * Startzustand: drei Geburtstage (mit Foto) und eine Notiz als Entwurf.
     */
    private suspend fun seed() = withContext(Dispatchers.IO) {
        val now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)

        db.calendarDao().upsert(CalendarEntity(name = "Arbeit", colorHex = CalendarPalette.WORK))
        val private = db.calendarDao().upsert(CalendarEntity(name = "Privat", colorHex = CalendarPalette.PRIVATE))
        db.calendarDao().upsert(CalendarEntity(name = "Gesundheit", colorHex = CalendarPalette.HEALTH))

        // Drei Geburtstage, alle ganztägig und mit Foto; Günter und der eigene stehen im Fokus.
        val year = now.year
        suspend fun birthday(title: String, month: Int, day: Int, photo: Int, name: String, focus: Boolean = false) {
            db.eventDao().insert(
                Event(
                    title = title,
                    start = java.time.LocalDate.of(year, month, day).atStartOfDay(),
                    allDay = true,
                    calendarId = private,
                    focused = focus,
                    imagePath = images.copyRaw(photo, name),
                    isBirthday = true,
                ),
            )
        }
        birthday("Piros Geburtstag", 11, 20, R.raw.photo_piro, "photo_piro.jpg")
        // „Im Fokus“ zeigt das zuletzt Angelegte zuerst: Paul vor Günter anlegen, damit Günter vorne steht.
        birthday("Pauls Geburtstag", 12, 13, R.raw.photo_paul, "photo_paul.jpg", focus = true)
        birthday("Günters Geburtstag", 12, 6, R.raw.photo_guenter, "photo_guenter.jpg", focus = true)

        // Einzige Notiz: ein Entwurf als Augenzwinkern.
        db.noteDao().insert(
            Note(
                title = "Die Organisations-App für Techis und Tech-CEOs",
                body = "Die beste UI und UX und hat alles, was man braucht, um seine Meetings wieder zu verpassen, seine Aufgaben in Angriff zu nehmen und in Notizen zu machen.",
                status = Status.DRAFT,
            ),
        )
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

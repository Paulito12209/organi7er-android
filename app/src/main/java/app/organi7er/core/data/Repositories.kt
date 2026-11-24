package app.organi7er.core.data

import android.net.Uri
import app.organi7er.core.data.db.CalendarDao
import app.organi7er.core.data.db.EventDao
import app.organi7er.core.data.db.NoteDao
import app.organi7er.core.data.db.NotificationDao
import app.organi7er.core.data.db.TaskDao
import app.organi7er.core.model.CalendarEntity
import app.organi7er.core.model.Event
import app.organi7er.core.model.Note
import app.organi7er.core.model.NoteImage
import app.organi7er.core.model.NoteWithImages
import app.organi7er.core.model.NotificationEntry
import app.organi7er.core.model.Status
import app.organi7er.core.model.Task
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

interface EventRepository {
    val events: Flow<List<Event>>
    val calendars: Flow<List<CalendarEntity>>
    suspend fun get(id: Long): Event?
    suspend fun save(event: Event): Long
    suspend fun delete(id: Long)
    suspend fun saveCalendar(calendar: CalendarEntity): Long
    suspend fun deleteCalendar(calendar: CalendarEntity)
    suspend fun setFocused(id: Long, focused: Boolean)
    suspend fun markOpened(id: Long)
}

interface TaskRepository {
    val tasks: Flow<List<Task>>
    suspend fun get(id: Long): Task?
    suspend fun save(task: Task): Long
    suspend fun delete(id: Long)
    suspend fun setDone(id: Long, done: Boolean)
    suspend fun setFocused(id: Long, focused: Boolean)
    suspend fun markOpened(id: Long)
}

interface NoteRepository {
    val notes: Flow<List<NoteWithImages>>
    fun observe(id: Long): Flow<NoteWithImages?>
    suspend fun get(id: Long): Note?
    suspend fun save(note: Note): Long
    suspend fun delete(id: Long)
    suspend fun addImages(noteId: Long, uris: List<Uri>)
    suspend fun addImageFile(noteId: Long, path: String)
    suspend fun removeImage(image: NoteImage)
    suspend fun setFocused(id: Long, focused: Boolean)
    suspend fun markOpened(id: Long)
}

@Singleton
class RoomEventRepository @Inject constructor(
    private val dao: EventDao,
    private val calendarDao: CalendarDao,
    private val reminders: ReminderScheduler,
) : EventRepository {
    override val events = dao.observeAll().distinctUntilChanged()
    override val calendars = calendarDao.observeAll().distinctUntilChanged()

    override suspend fun get(id: Long) = dao.get(id)

    override suspend fun save(event: Event): Long {
        val now = Instant.now()
        val id = if (event.id == 0L) {
            dao.insert(event.copy(createdAt = now, updatedAt = now))
        } else {
            dao.update(event.copy(updatedAt = now, openedAt = dao.get(event.id)?.openedAt ?: event.openedAt))
            event.id
        }
        reminders.scheduleEvent(event.copy(id = id))
        return id
    }

    override suspend fun delete(id: Long) {
        reminders.cancelEvent(id)
        dao.delete(id)
    }

    override suspend fun saveCalendar(calendar: CalendarEntity) = calendarDao.upsert(calendar)

    override suspend fun deleteCalendar(calendar: CalendarEntity) = calendarDao.delete(calendar)

    override suspend fun setFocused(id: Long, focused: Boolean) = dao.setFocused(id, focused)

    override suspend fun markOpened(id: Long) = dao.markOpened(id, Instant.now())
}

@Singleton
class RoomTaskRepository @Inject constructor(private val dao: TaskDao) : TaskRepository {
    override val tasks = dao.observeAll().distinctUntilChanged()

    override suspend fun get(id: Long) = dao.get(id)

    override suspend fun save(task: Task): Long {
        val now = Instant.now()
        // Ohne Datum ist eine aktive Aufgabe ein Entwurf (bewusst ohne Druck).
        val normalized = if (task.dueAt == null && task.status == Status.ACTIVE) task.copy(status = Status.DRAFT, subStatus = null) else task
        return if (normalized.id == 0L) {
            dao.insert(normalized.copy(createdAt = now, updatedAt = now))
        } else {
            dao.update(normalized.copy(updatedAt = now, openedAt = dao.get(normalized.id)?.openedAt ?: normalized.openedAt))
            normalized.id
        }
    }

    override suspend fun delete(id: Long) = dao.delete(id)

    override suspend fun setDone(id: Long, done: Boolean) {
        val task = dao.get(id) ?: return
        val updated = if (done) {
            task.copy(status = Status.DONE, doneAt = Instant.now(), subStatus = null)
        } else {
            task.copy(status = if (task.dueAt == null) Status.DRAFT else Status.ACTIVE, doneAt = null)
        }
        dao.update(updated.copy(updatedAt = Instant.now()))
    }

    override suspend fun setFocused(id: Long, focused: Boolean) = dao.setFocused(id, focused)

    override suspend fun markOpened(id: Long) = dao.markOpened(id, Instant.now())
}

@Singleton
class RoomNoteRepository @Inject constructor(
    private val dao: NoteDao,
    private val images: ImageStore,
    private val reminders: ReminderScheduler,
) : NoteRepository {
    override val notes = dao.observeAll().distinctUntilChanged()

    override fun observe(id: Long) = dao.observe(id).distinctUntilChanged()

    override suspend fun get(id: Long) = dao.get(id)

    override suspend fun save(note: Note): Long {
        val now = Instant.now()
        val id = if (note.id == 0L) {
            dao.insert(note.copy(createdAt = now, updatedAt = now))
        } else {
            dao.update(note.copy(updatedAt = now, openedAt = dao.get(note.id)?.openedAt ?: note.openedAt))
            note.id
        }
        reminders.scheduleNote(note.copy(id = id))
        return id
    }

    override suspend fun delete(id: Long) {
        reminders.cancelNote(id)
        dao.images(id).forEach { images.delete(it.filePath) }
        dao.delete(id)
    }

    override suspend fun addImages(noteId: Long, uris: List<Uri>) {
        var position = dao.images(noteId).maxOfOrNull { it.position + 1 } ?: 0
        uris.forEach { uri ->
            val path = images.importUri(uri) ?: return@forEach
            dao.insertImage(NoteImage(noteId = noteId, filePath = path, position = position++))
        }
        touch(noteId)
    }

    override suspend fun addImageFile(noteId: Long, path: String) {
        val position = dao.images(noteId).maxOfOrNull { it.position + 1 } ?: 0
        dao.insertImage(NoteImage(noteId = noteId, filePath = path, position = position))
        touch(noteId)
    }

    override suspend fun removeImage(image: NoteImage) {
        dao.deleteImage(image.id)
        images.delete(image.filePath)
        touch(image.noteId)
    }

    override suspend fun setFocused(id: Long, focused: Boolean) = dao.setFocused(id, focused)

    override suspend fun markOpened(id: Long) = dao.markOpened(id, Instant.now())

    private suspend fun touch(noteId: Long) {
        dao.get(noteId)?.let { dao.update(it.copy(updatedAt = Instant.now())) }
    }
}

interface NotificationRepository {
    val entries: Flow<List<NotificationEntry>>
    suspend fun log(kind: String, title: String, text: String)
    suspend fun markAllRead()
}

@Singleton
class RoomNotificationRepository @Inject constructor(private val dao: NotificationDao) : NotificationRepository {
    override val entries = dao.observeAll().distinctUntilChanged()

    override suspend fun log(kind: String, title: String, text: String) {
        dao.insert(NotificationEntry(kind = kind, title = title, text = text))
    }

    override suspend fun markAllRead() = dao.markAllRead()
}

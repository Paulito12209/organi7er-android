package app.organi7er.feature.editor

import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.organi7er.core.data.EventRepository
import app.organi7er.core.data.ImageStore
import app.organi7er.core.data.NoteRepository
import app.organi7er.core.data.NotificationRepository
import app.organi7er.core.data.SettingsRepository
import app.organi7er.core.data.TaskRepository
import app.organi7er.core.designsystem.components.EntryType
import app.organi7er.core.designsystem.components.QuickEntry
import app.organi7er.core.designsystem.components.rest
import app.organi7er.core.designsystem.components.title
import app.organi7er.core.model.AppSettings
import app.organi7er.core.model.CalendarEntity
import app.organi7er.core.model.Event
import app.organi7er.core.model.Note
import app.organi7er.core.model.Status
import app.organi7er.core.model.Task
import app.organi7er.feature.common.ShellInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Was das gemeinsame Anlege-/Bearbeiten-Sheet öffnen soll. */
@Immutable
sealed interface EditorRequest {
    data class NewEvent(val date: LocalDate? = null, val title: String = "", val note: String = "", val focused: Boolean = false) : EditorRequest
    data class EditEvent(val id: Long) : EditorRequest
    data class PlanEvent(val id: Long) : EditorRequest
    data class NewTask(val date: LocalDate? = null, val title: String = "", val note: String = "", val focused: Boolean = false) : EditorRequest
    data class EditTask(val id: Long) : EditorRequest
    data class PlanTask(val id: Long) : EditorRequest
    data class NewNote(val title: String = "", val body: String = "", val focused: Boolean = false, val startCamera: Boolean = false) : EditorRequest
}

/**
 * Aktivitätsweiter Zugang zum Speichern. Formularzustand lebt im Sheet;
 * hier liegt nur, was über die Lebensdauer eines Sheets hinaus geht.
 */
@HiltViewModel
class EditorViewModel @Inject constructor(
    private val events: EventRepository,
    private val tasks: TaskRepository,
    private val notes: NoteRepository,
    private val images: ImageStore,
    settings: SettingsRepository,
    notifications: NotificationRepository,
) : ViewModel() {
    /** Ungelesene Benachrichtigungen und Profilbild für den Kopf aller Seiten. */
    val shell: StateFlow<ShellInfo> = combine(notifications.entries, settings.settings) { log, s ->
        ShellInfo(unread = log.count { !it.read }, profileImage = s.profileImage)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ShellInfo())

    val calendars: StateFlow<List<CalendarEntity>> = events.calendars.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val settings: StateFlow<AppSettings> = settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())
    val taskLists: StateFlow<List<String>> = tasks.tasks
        .map { list -> list.map { it.listName }.filter { it.isNotBlank() }.distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    suspend fun event(id: Long): Event? = events.get(id)
    suspend fun task(id: Long): Task? = tasks.get(id)

    fun saveEvent(event: Event) = viewModelScope.launch { events.save(event) }
    fun deleteEvent(id: Long) = viewModelScope.launch { events.delete(id) }
    fun saveTask(task: Task) = viewModelScope.launch { tasks.save(task) }
    fun deleteTask(id: Long) = viewModelScope.launch { tasks.delete(id) }
    fun setTaskDone(id: Long, done: Boolean) = viewModelScope.launch { tasks.setDone(id, done) }

    /** Für „Zuletzt geöffnet“. */
    fun markOpened(type: EntryType, id: Long) = viewModelScope.launch {
        when (type) {
            EntryType.Event -> events.markOpened(id)
            EntryType.Task -> tasks.markOpened(id)
            EntryType.Note -> notes.markOpened(id)
        }
    }

    fun setFocused(type: EntryType, id: Long, focused: Boolean) = viewModelScope.launch {
        when (type) {
            EntryType.Event -> events.setFocused(id, focused)
            EntryType.Task -> tasks.setFocused(id, focused)
            EntryType.Note -> notes.setFocused(id, focused)
        }
    }

    fun createNote(note: Note, uris: List<Uri>, files: List<String>, onCreated: (Long) -> Unit = {}) = viewModelScope.launch {
        val id = notes.save(note)
        if (uris.isNotEmpty()) notes.addImages(id, uris)
        files.forEach { notes.addImageFile(id, it) }
        onCreated(id)
    }

    /**
     * Schnelles Erfassen: erste Zeile = Titel, Rest = Notiz. Ohne Zeitpunkt wird ein Termin oder eine Aufgabe
     * ein Entwurf, mit Zeitpunkt aktiv.
     */
    fun quickAdd(entry: QuickEntry) = viewModelScope.launch {
        val title = entry.title
        val rest = entry.rest
        val at = entry.at
        when (entry.type) {
            EntryType.Event -> events.save(
                Event(
                    title = title,
                    note = rest,
                    start = at,
                    end = at?.plusHours(1),
                    calendarId = calendars.value.firstOrNull()?.id ?: 0L,
                    reminderMinutes = if (at != null) settings.value.reminderLeadMinutes else null,
                    status = if (at == null) Status.DRAFT else Status.ACTIVE,
                    focused = entry.focused,
                ),
            )
            EntryType.Task -> tasks.save(
                Task(
                    title = title,
                    note = rest,
                    dueAt = at,
                    dueHasTime = at != null,
                    status = if (at == null) Status.DRAFT else Status.ACTIVE,
                    focused = entry.focused,
                ),
            )
            EntryType.Note -> notes.save(Note(title = title, body = rest, reminderAt = at, focused = entry.focused))
        }
    }

    fun addImages(noteId: Long, uris: List<Uri>) = viewModelScope.launch { notes.addImages(noteId, uris) }
    fun addImageFile(noteId: Long, path: String) = viewModelScope.launch {
        if (File(path).length() > 0) notes.addImageFile(noteId, path) else images.delete(path)
    }
    fun discardFile(path: String) = viewModelScope.launch { images.delete(path) }

    fun newCameraTarget() = images.newCameraTarget()
}

/** Globale Aktionen, die jeder Bildschirm auslösen kann. */
@Immutable
data class AppActions(
    val openEditor: (EditorRequest) -> Unit = {},
    val toggleTask: (Task) -> Unit = {},
    val openNote: (Long) -> Unit = {},
    val openTab: (Int) -> Unit = {},
    val openSettings: () -> Unit = {},
    val openNotifications: () -> Unit = {},
    val openSearch: () -> Unit = {},
    val openCatalog: () -> Unit = {},
    val back: () -> Unit = {},
    val takePhoto: (noteId: Long) -> Unit = {},
    val pickImages: (noteId: Long) -> Unit = {},
    val showMessage: (String) -> Unit = {},
)

val LocalAppActions = staticCompositionLocalOf { AppActions() }

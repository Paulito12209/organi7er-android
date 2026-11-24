package app.organi7er.core.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import androidx.room.Upsert
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import app.organi7er.core.model.CalendarEntity
import app.organi7er.core.model.Event
import app.organi7er.core.model.Note
import app.organi7er.core.model.NoteImage
import app.organi7er.core.model.NoteWithImages
import app.organi7er.core.model.NotificationEntry
import app.organi7er.core.model.Task
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow

/** Zeiten als Epoch-Millis in der Zeitzone des Geräts – sortier- und vergleichbar in SQL. */
class Converters {
    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime?): Long? = value?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()

    @TypeConverter
    fun toLocalDateTime(value: Long?): LocalDateTime? = value?.let { LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault()) }

    @TypeConverter
    fun fromInstant(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun toInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)
}

@Dao
interface CalendarDao {
    @Query("SELECT * FROM calendars ORDER BY id")
    fun observeAll(): Flow<List<CalendarEntity>>

    @Query("SELECT * FROM calendars ORDER BY id")
    suspend fun getAll(): List<CalendarEntity>

    @Upsert
    suspend fun upsert(calendar: CalendarEntity): Long

    @Delete
    suspend fun delete(calendar: CalendarEntity)

    @Query("DELETE FROM calendars")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CalendarEntity>)
}

@Dao
interface EventDao {
    @Query("UPDATE events SET focused = :focused WHERE id = :id")
    suspend fun setFocused(id: Long, focused: Boolean)

    @Query("UPDATE events SET openedAt = :at WHERE id = :id")
    suspend fun markOpened(id: Long, at: Instant)

    @Query("SELECT * FROM events ORDER BY start IS NULL, start, id")
    fun observeAll(): Flow<List<Event>>

    @Query("SELECT * FROM events")
    suspend fun getAll(): List<Event>

    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun get(id: Long): Event?

    @Insert
    suspend fun insert(event: Event): Long

    @Update
    suspend fun update(event: Event)

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM events")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<Event>)

    @Query("SELECT * FROM events WHERE status = 'ACTIVE' AND reminderMinutes IS NOT NULL AND start > :from")
    suspend fun withUpcomingReminders(from: LocalDateTime): List<Event>
}

@Dao
interface TaskDao {
    @Query("UPDATE tasks SET focused = :focused WHERE id = :id")
    suspend fun setFocused(id: Long, focused: Boolean)

    @Query("UPDATE tasks SET openedAt = :at WHERE id = :id")
    suspend fun markOpened(id: Long, at: Instant)

    @Query("SELECT * FROM tasks ORDER BY dueAt IS NULL, dueAt, id")
    fun observeAll(): Flow<List<Task>>

    @Query("SELECT * FROM tasks")
    suspend fun getAll(): List<Task>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun get(id: Long): Task?

    @Insert
    suspend fun insert(task: Task): Long

    @Update
    suspend fun update(task: Task)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM tasks")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<Task>)
}

@Dao
interface NoteDao {
    @Query("UPDATE notes SET focused = :focused WHERE id = :id")
    suspend fun setFocused(id: Long, focused: Boolean)

    @Query("UPDATE notes SET openedAt = :at WHERE id = :id")
    suspend fun markOpened(id: Long, at: Instant)

    @Transaction
    @Query("SELECT * FROM notes ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<NoteWithImages>>

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :id")
    fun observe(id: Long): Flow<NoteWithImages?>

    @Query("SELECT * FROM notes")
    suspend fun getAll(): List<Note>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun get(id: Long): Note?

    @Insert
    suspend fun insert(note: Note): Long

    @Update
    suspend fun update(note: Note)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM notes")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<Note>)

    @Query("SELECT * FROM note_images WHERE noteId = :noteId ORDER BY position")
    suspend fun images(noteId: Long): List<NoteImage>

    @Query("SELECT * FROM note_images")
    suspend fun allImages(): List<NoteImage>

    @Insert
    suspend fun insertImage(image: NoteImage): Long

    @Query("DELETE FROM note_images WHERE id = :id")
    suspend fun deleteImage(id: Long)

    @Query("SELECT * FROM notes WHERE status = 'ACTIVE' AND reminderAt > :from")
    suspend fun withUpcomingReminders(from: LocalDateTime): List<Note>
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notification_log ORDER BY createdAt DESC LIMIT 200")
    fun observeAll(): Flow<List<NotificationEntry>>

    @Insert
    suspend fun insert(entry: NotificationEntry): Long

    @Query("UPDATE notification_log SET read = 1 WHERE read = 0")
    suspend fun markAllRead()

    @Query("DELETE FROM notification_log")
    suspend fun deleteAll()
}

/** v2: Fokus + zuletzt geöffnet je Eintrag, Mitschrift der Benachrichtigungen. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        listOf("events", "tasks", "notes").forEach { t ->
            db.execSQL("ALTER TABLE `$t` ADD COLUMN `focused` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `$t` ADD COLUMN `openedAt` INTEGER")
        }
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `notification_log` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `kind` TEXT NOT NULL, " +
                "`title` TEXT NOT NULL, `text` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `read` INTEGER NOT NULL)",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_notification_log_createdAt` ON `notification_log` (`createdAt`)")
    }
}

/** v3: Link je Termin (Remote-Meetings). */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `events` ADD COLUMN `link` TEXT NOT NULL DEFAULT ''")
    }
}

@Database(
    entities = [CalendarEntity::class, Event::class, Task::class, Note::class, NoteImage::class, NotificationEntry::class],
    version = 3,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class OrganiDatabase : RoomDatabase() {
    abstract fun calendarDao(): CalendarDao
    abstract fun eventDao(): EventDao
    abstract fun taskDao(): TaskDao
    abstract fun noteDao(): NoteDao
    abstract fun notificationDao(): NotificationDao
}

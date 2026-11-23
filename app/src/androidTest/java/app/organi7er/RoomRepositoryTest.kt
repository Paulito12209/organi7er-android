package app.organi7er

import android.net.Uri
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.organi7er.core.data.ReminderScheduler
import app.organi7er.core.data.RoomEventRepository
import app.organi7er.core.data.db.OrganiDatabase
import app.organi7er.core.model.CalendarEntity
import app.organi7er.core.model.Event
import app.organi7er.core.model.Note
import app.organi7er.core.model.Status
import java.time.LocalDateTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomRepositoryTest {
    private lateinit var db: OrganiDatabase
    private val noReminders = object : ReminderScheduler {
        override fun scheduleEvent(event: Event) = Unit
        override fun cancelEvent(id: Long) = Unit
        override fun scheduleNote(note: Note) = Unit
        override fun cancelNote(id: Long) = Unit
    }

    @Before fun open() {
        db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, OrganiDatabase::class.java).build()
    }

    @After fun close() = db.close()

    @Test
    fun terminSpeichernLadenUndSortieren() = runTest {
        val repo = RoomEventRepository(db.eventDao(), db.calendarDao(), noReminders)
        val cal = repo.saveCalendar(CalendarEntity(name = "Arbeit", colorHex = "#3B82F6"))
        val start = LocalDateTime.of(2026, 11, 23, 10, 0)
        repo.save(Event(title = "B", start = start.plusHours(2), calendarId = cal))
        repo.save(Event(title = "A", start = start, end = start.plusMinutes(45), calendarId = cal))
        repo.save(Event(title = "Entwurf", status = Status.DRAFT, calendarId = cal))
        val all = repo.events.first()
        assertEquals(listOf("A", "B", "Entwurf"), all.map { it.title })
        assertEquals(start.plusMinutes(45), all.first().end)
        @Suppress("UNUSED_VARIABLE")
        val unused = Uri.EMPTY
    }
}

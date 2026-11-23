package app.organi7er

import app.organi7er.core.data.BriefingCalculator
import app.organi7er.core.data.BriefingKind
import app.organi7er.core.model.Event
import app.organi7er.core.model.EventSubStatus
import app.organi7er.core.model.Status
import app.organi7er.core.model.Task
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class BriefingCalculatorTest {
    private val now = LocalDateTime.of(2026, 11, 23, 9, 35)

    @Test
    fun `naechster Termin in 25 Minuten zeigt Countdown und gelben Fortschritt`() {
        val events = listOf(Event(id = 1, title = "Sprint-Review", start = now.plusMinutes(25), end = now.plusMinutes(70)))
        val today = BriefingCalculator.compute(now, events, emptyList(), emptyList(), true).first()
        assertEquals(BriefingKind.TODAY, today.kind)
        assertEquals("in 25 Min", today.big)
        assertEquals("Sprint-Review · 10:00", today.caption)
        assertNotNull(today.progress)
    }

    @Test
    fun `kein Fortschrittsbalken wenn der Termin spaeter als 60 Minuten beginnt`() {
        val events = listOf(Event(id = 1, title = "Mittag", start = now.plusHours(3), end = now.plusHours(4)))
        val today = BriefingCalculator.compute(now, events, emptyList(), emptyList(), true).first()
        assertEquals("um 12:35", today.big)
        assertNull(today.progress)
    }

    @Test
    fun `laufender Termin und abgesagte Termine`() {
        val events = listOf(
            Event(id = 1, title = "Workshop", start = now.minusMinutes(10), end = now.plusMinutes(50)),
            Event(id = 2, title = "Abgesagt", start = now.plusMinutes(5), end = now.plusMinutes(30), subStatus = EventSubStatus.CANCELLED),
        )
        val today = BriefingCalculator.compute(now, events, emptyList(), emptyList(), true).first()
        assertEquals("läuft", today.big)
        assertEquals(1, today.events)
    }

    @Test
    fun `morgen zeigt ersten Termin und Kachel laesst sich ausblenden`() {
        val tomorrow = now.toLocalDate().plusDays(1)
        val events = listOf(Event(id = 1, title = "Daily und Planung", start = tomorrow.atTime(9, 0), end = tomorrow.atTime(10, 0)))
        val tiles = BriefingCalculator.compute(now, events, emptyList(), emptyList(), true)
        assertEquals("ab 09:00", tiles[1].big)
        assertEquals(2, BriefingCalculator.compute(now, events, emptyList(), emptyList(), false).size)
    }

    @Test
    fun `woche erkennt vollen Tag`() {
        val fr = now.toLocalDate().plusDays(4)
        val events = (0..3).map { Event(id = it.toLong(), title = "T$it", start = fr.atTime(9 + it * 2, 0), end = fr.atTime(10 + it * 2, 0)) }
        val week = BriefingCalculator.compute(now, events, emptyList(), emptyList(), true).last()
        assertEquals("Fr voll", week.big)
        assertEquals(4, week.events)
    }

    @Test
    fun `Aufgabenzaehler zaehlt ueberfaellige zu heute`() {
        val tasks = listOf(
            Task(id = 1, title = "a", dueAt = now.minusDays(1)),
            Task(id = 2, title = "b", dueAt = now),
            Task(id = 3, title = "c", dueAt = now, status = Status.DONE),
            Task(id = 4, title = "d"),
        )
        val today = BriefingCalculator.compute(now, emptyList(), tasks, emptyList(), true).first()
        assertEquals(2, today.tasks)
        assertEquals("2 offen", today.big)
    }
}

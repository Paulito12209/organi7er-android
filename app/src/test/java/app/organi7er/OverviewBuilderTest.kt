package app.organi7er

import app.organi7er.core.designsystem.components.EntryType
import app.organi7er.core.model.Event
import app.organi7er.core.model.NotificationEntry
import app.organi7er.core.model.Status
import app.organi7er.core.model.Task
import app.organi7er.feature.overview.OverviewBuilder
import app.organi7er.core.model.Meeting
import app.organi7er.core.model.MeetingPlatform
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OverviewBuilderTest {
    /** Montag, 23. November 2026 (KW 48). */
    private val now = LocalDateTime.of(2026, 11, 23, 9, 35)
    private val today = now.toLocalDate()
    private fun instant(t: LocalDateTime) = t.atZone(ZoneId.systemDefault()).toInstant()

    private val events = listOf(
        Event(id = 1, title = "Laufen", start = now.minusHours(1), end = now.minusMinutes(20)),
        Event(id = 2, title = "Sprint-Review", start = now.plusMinutes(25), end = now.plusMinutes(70), focused = true, link = "meet.google.com/abc"),
        Event(id = 3, title = "Zahnarzt", start = now.plusDays(3).withHour(14), end = now.plusDays(3).withHour(15)),
    )
    private val tasks = listOf(
        Task(id = 10, title = "Angebot", dueAt = today.atStartOfDay(), openedAt = instant(now.minusMinutes(5)), createdAt = instant(now.minusDays(2))),
        Task(id = 11, title = "Erledigt", dueAt = today.atStartOfDay(), status = Status.DONE, focused = true, createdAt = instant(now.minusHours(1))),
    )

    private fun build(selected: LocalDate = today, weekStart: Int = 1, log: List<NotificationEntry> = emptyList()) =
        OverviewBuilder.build(now, selected, weekStart, events, tasks, emptyList(), log)

    @Test
    fun `sieben Tage der gewaehlten Woche mit Buchstaben und Zaehlern`() {
        val s = build()
        assertEquals(7, s.days.size)
        assertEquals(listOf("M", "D", "M", "D", "F", "S", "S"), s.days.map { it.letter })
        val mon = s.days.first()
        assertEquals(today, mon.date)
        assertEquals(3 - 1, mon.events) // Laufen + Sprint-Review (Zahnarzt ist Donnerstag)
        assertEquals(1, mon.tasks) // nur offene
        assertEquals("Als Nächstes · 10:00", mon.nextLabel)
        assertEquals("Sprint-Review", mon.nextTitle)
        assertEquals(1, s.days[3].events)
    }

    @Test
    fun `naechster Termin traegt seinen Link fuer den Pfeil`() {
        val mon = build().days.first()
        assertEquals(2L, mon.nextEventId)
        assertEquals("meet.google.com/abc", mon.nextLink)
        assertEquals("", build().days[3].nextLink) // Zahnarzt ohne Link
    }

    @Test
    fun `Link ohne Schema bekommt https`() {
        assertEquals("https://meet.google.com/abc", Meeting.normalize(" meet.google.com/abc "))
        assertEquals("https://zoom.us/j/1", Meeting.normalize("https://zoom.us/j/1"))
        assertEquals("tel:+491234", Meeting.normalize("tel:+491234"))
    }

    @Test
    fun `Plattform am Host erkannt`() {
        assertEquals(MeetingPlatform.GoogleMeet, Meeting.platform("meet.google.com/abc-defg-hij"))
        assertEquals(MeetingPlatform.Zoom, Meeting.platform("https://us02web.zoom.us/j/123"))
        assertEquals(MeetingPlatform.Teams, Meeting.platform("https://teams.microsoft.com/l/meetup-join/x"))
        assertEquals(MeetingPlatform.Webex, Meeting.platform("https://acme.webex.com/meet/x"))
        assertEquals(MeetingPlatform.Other, Meeting.platform("https://notzoom.us.example.com"))
    }

    @Test
    fun `Woche ab Sonntag beginnt mit Sonntag`() {
        val s = build(weekStart = 7)
        assertEquals(today.minusDays(1), s.days.first().date)
        assertEquals("S", s.days.first().letter)
    }

    @Test
    fun `KW-Auswahl markiert aktuelle Woche`() {
        val s = build()
        val w = s.weeks[s.selectedWeek]
        assertEquals(48, w.number)
        assertTrue(w.isCurrent)
        val later = build(selected = today.plusWeeks(2))
        assertEquals(50, later.weeks[later.selectedWeek].number)
    }

    @Test
    fun `Fokus ohne erledigte Aufgaben, Verlauf nach Zeit`() {
        val s = build()
        assertEquals(listOf(EntryType.Event to 2L), s.focus.map { it.type to it.id })
        assertEquals(listOf("Angebot"), s.opened.map { it.title })
        assertEquals("vor 5 Min", s.opened.first().whenLabel)
        assertEquals("Erledigt", s.created.first { it.type == EntryType.Task }.title)
    }

    @Test
    fun `ungelesene Benachrichtigungen werden gezaehlt`() {
        val log = listOf(
            NotificationEntry(id = 1, kind = "reminders", title = "a", text = ""),
            NotificationEntry(id = 2, kind = "reminders", title = "b", text = "", read = true),
        )
        assertEquals(1, build(log = log).unread)
    }
}

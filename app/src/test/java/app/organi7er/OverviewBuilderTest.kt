package app.organi7er

import app.organi7er.core.data.BriefingCalculator
import app.organi7er.core.data.BriefingKind
import app.organi7er.core.model.Event
import app.organi7er.core.model.Task
import app.organi7er.feature.overview.OverviewBuilder
import app.organi7er.feature.overview.OverviewSection
import app.organi7er.feature.overview.OverviewSelection
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OverviewBuilderTest {
    private val now = LocalDateTime.of(2026, 11, 23, 9, 35)
    private val events = listOf(
        Event(id = 1, title = "Laufen", start = now.minusHours(1), end = now.minusMinutes(20)),
        Event(id = 2, title = "Sprint-Review", start = now.plusMinutes(25), end = now.plusMinutes(70)),
        Event(id = 3, title = "Mittag", start = now.plusHours(4), end = now.plusHours(5)),
    )
    private val tiles = BriefingCalculator.compute(now, events, emptyList(), emptyList(), true)

    private fun build(sel: OverviewSelection, tasks: List<Task> = emptyList()) =
        OverviewBuilder.build(now, sel, events, emptyList(), tasks, emptyList(), tiles)

    @Test
    fun `heute zeigt Als Naechstes mit dem kommenden Termin und den restlichen`() {
        val state = build(OverviewSelection())
        val next = state.sections.first() as OverviewSection.Next
        assertEquals("Sprint-Review", next.hero.event.title)
        assertEquals(listOf("Mittag"), next.rest.map { it.event.title })
        assertEquals("in 25 Min", next.until)
        assertEquals(BriefingKind.TODAY, state.selectedTile)
        assertFalse(state.canPrev)
    }

    @Test
    fun `anderer Tag ohne Inhalt zeigt nur einen leisen Hinweis`() {
        val state = build(OverviewSelection(dayOffset = 2))
        assertEquals(1, state.sections.size)
        assertTrue(state.sections.first() is OverviewSection.Hint)
        assertEquals(null, state.selectedTile)
    }

    @Test
    fun `nur Aufgaben am Tag zeigt Aufgaben und Hinweis ohne Termine`() {
        val day = now.toLocalDate().plusDays(2)
        val state = build(OverviewSelection(dayOffset = 2), listOf(Task(id = 1, title = "Mails", dueAt = day.atStartOfDay())))
        assertTrue(state.sections[0] is OverviewSection.Tasks)
        assertEquals("Keine Termine an diesem Tag.", (state.sections[1] as OverviewSection.Hint).text)
    }

    @Test
    fun `Pfeile sind auf heute bis plus sechs Tage begrenzt`() {
        var sel = OverviewSelection()
        repeat(10) { sel = sel.next() }
        assertEquals(6, sel.dayOffset)
        assertFalse(build(sel).canNext)
        repeat(10) { sel = sel.prev() }
        assertEquals(0, sel.dayOffset)
    }

    @Test
    fun `Woche zeigt Zeitraum`() {
        val state = build(OverviewSelection(week = true))
        assertEquals("23.–29. November", state.dateLabel)
        assertEquals(BriefingKind.WEEK, state.selectedTile)
    }
}

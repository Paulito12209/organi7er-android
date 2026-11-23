package app.organi7er

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import app.organi7er.core.data.RoomTaskRepository
import app.organi7er.core.model.Priority
import app.organi7er.core.model.Status
import app.organi7er.core.model.Task
import app.organi7er.core.model.TaskSubStatus
import app.organi7er.feature.common.UiMapper
import app.organi7er.feature.tasks.TaskLogic
import app.organi7er.feature.tasks.TaskSort
import app.organi7er.feature.tasks.TasksViewModel
import java.time.LocalDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class TaskLogicTest {
    private val now = LocalDateTime.of(2026, 11, 23, 9, 35)
    private val today = now.toLocalDate()

    @Test
    fun `gruppiert nach Ueberfaellig Heute Diese Woche Spaeter`() {
        val tasks = listOf(
            Task(id = 1, title = "alt", dueAt = now.minusDays(2)),
            Task(id = 2, title = "heute", dueAt = now),
            Task(id = 3, title = "do", dueAt = now.plusDays(3)),
            Task(id = 4, title = "später", dueAt = now.plusDays(20)),
            Task(id = 5, title = "entwurf", status = Status.DRAFT),
            Task(id = 6, title = "fertig", dueAt = now, status = Status.DONE),
        )
        val groups = TaskLogic.groups(tasks, now, TaskSort.DUE)
        assertEquals(listOf("Überfällig", "Heute", "Diese Woche", "Später"), groups.map { it.title })
        assertEquals(listOf("heute"), groups[1].items.map { it.task.title })
    }

    @Test
    fun `Prioritaet sortiert wichtige zuerst`() {
        val tasks = listOf(
            Task(id = 1, title = "normal", dueAt = now.withHour(8)),
            Task(id = 2, title = "wichtig", dueAt = now.withHour(18), priority = Priority.HIGH),
        )
        assertEquals("wichtig", TaskLogic.groups(tasks, now, TaskSort.PRIORITY).single().items.first().task.title)
        assertEquals("normal", TaskLogic.groups(tasks, now, TaskSort.DUE).single().items.first().task.title)
    }

    @Test
    fun `Meta und Unterstatus`() {
        val ui = UiMapper.task(Task(id = 1, title = "Paket", dueAt = now.withHour(17).withMinute(0), dueHasTime = true, listName = "Privat", subStatus = TaskSubStatus.BLOCKED), today, showDate = false, now)
        assertEquals("bis 17:00 · Privat", ui.meta)
        assertEquals("Geblockt", ui.tag)
        val overdue = UiMapper.task(Task(id = 2, title = "x", dueAt = now.minusDays(1)), today, showDate = false, now)
        assertEquals("überfällig · gestern", overdue.meta)
    }
}

class TaskRepositoryTest {
    @Test
    fun `Aufgabe ohne Datum wird als Entwurf gespeichert und Erledigen ist umkehrbar`() = runTest {
        val dao = FakeTaskDao()
        val repo = RoomTaskRepository(dao)
        val id = repo.save(Task(title = "Idee"))
        assertEquals(Status.DRAFT, repo.get(id)!!.status)

        val planned = repo.save(repo.get(id)!!.copy(dueAt = LocalDateTime.now(), status = Status.ACTIVE))
        repo.setDone(planned, true)
        assertEquals(Status.DONE, repo.get(planned)!!.status)
        repo.setDone(planned, false)
        assertEquals(Status.ACTIVE, repo.get(planned)!!.status)
        assertEquals(null, repo.get(planned)!!.doneAt)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class TasksViewModelTest {
    private val now = LocalDateTime.of(2026, 11, 23, 9, 35)

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `Sortierung und Segment werden im Zustand abgebildet`() = runTest {
        val repo = FakeTaskRepository(
            listOf(
                Task(id = 1, title = "normal", dueAt = now.withHour(8)),
                Task(id = 2, title = "wichtig", dueAt = now.withHour(18), priority = Priority.HIGH),
                Task(id = 3, title = "entwurf", status = Status.DRAFT),
            ),
        )
        val vm = TasksViewModel(repo, FakeTime(now), SavedStateHandle())
        vm.state.test {
            var s = awaitItem()
            while (s.groups.isEmpty()) s = awaitItem()
            assertEquals("normal", s.groups.single().items.first().task.title)
            vm.toggleSort()
            s = awaitItem()
            while (s.sort != TaskSort.PRIORITY) s = awaitItem()
            assertEquals("wichtig", s.groups.single().items.first().task.title)
            vm.segment(0)
            s = awaitItem()
            while (s.segment != 0) s = awaitItem()
            assertEquals(listOf("entwurf"), s.drafts.map { it.title })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Erledigen verschiebt die Aufgabe in Erledigt`() = runTest {
        val repo = FakeTaskRepository(listOf(Task(id = 1, title = "a", dueAt = now)))
        val vm = TasksViewModel(repo, FakeTime(now), SavedStateHandle())
        repo.setDone(1, true)
        val state = vm.state.first { it.recentDone.isNotEmpty() || it.groups.isEmpty() && repo.flow.value.first().status == Status.DONE }
        assertEquals(0, state.groups.size)
    }
}

package app.organi7er

import app.organi7er.core.data.TaskRepository
import app.organi7er.core.data.TimeProvider
import app.organi7er.core.data.db.TaskDao
import app.organi7er.core.model.Status
import app.organi7er.core.model.Task
import java.time.Instant
import java.time.LocalDateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

class FakeTime(private val now: LocalDateTime) : TimeProvider {
    override fun now() = now
    override val minuteTicks: Flow<LocalDateTime> = flowOf(now)
}

/** DAO im Speicher – damit lässt sich das echte Repository ohne Android testen. */
class FakeTaskDao : TaskDao {
    val items = MutableStateFlow<List<Task>>(emptyList())
    private var nextId = 1L
    override fun observeAll() = items
    override suspend fun getAll() = items.value
    override suspend fun get(id: Long) = items.value.firstOrNull { it.id == id }
    override suspend fun insert(task: Task): Long {
        val id = nextId++
        items.value = items.value + task.copy(id = id)
        return id
    }
    override suspend fun update(task: Task) {
        items.value = items.value.map { if (it.id == task.id) task else it }
    }
    override suspend fun delete(id: Long) {
        items.value = items.value.filterNot { it.id == id }
    }
    override suspend fun deleteAll() {
        items.value = emptyList()
    }
    override suspend fun insertAll(items: List<Task>) {
        this.items.value = this.items.value + items
    }
}

class FakeTaskRepository(initial: List<Task>) : TaskRepository {
    val flow = MutableStateFlow(initial)
    override val tasks: Flow<List<Task>> = flow
    override suspend fun get(id: Long) = flow.value.firstOrNull { it.id == id }
    override suspend fun save(task: Task): Long {
        flow.value = flow.value.filterNot { it.id == task.id } + task
        return task.id
    }
    override suspend fun delete(id: Long) {
        flow.value = flow.value.filterNot { it.id == id }
    }
    override suspend fun setDone(id: Long, done: Boolean) {
        flow.value = flow.value.map {
            if (it.id == id) it.copy(status = if (done) Status.DONE else Status.ACTIVE, doneAt = if (done) Instant.now() else null) else it
        }
    }
}

package app.organi7er.core.data

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.organi7er.MainActivity
import app.organi7er.R
import app.organi7er.core.data.db.EventDao
import app.organi7er.core.data.db.NoteDao
import app.organi7er.core.model.AppSettings
import app.organi7er.core.model.Event
import app.organi7er.core.model.Note
import app.organi7er.core.model.Status
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

interface ReminderScheduler {
    fun scheduleEvent(event: Event)
    fun cancelEvent(id: Long)
    fun scheduleNote(note: Note)
    fun cancelNote(id: Long)
}

object Notifications {
    const val CHANNEL_REMINDERS = "reminders"
    const val CHANNEL_BRIEFING = "briefing"
    const val CHANNEL_DRAFTS = "drafts"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(CHANNEL_REMINDERS, "Erinnerungen", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Erinnerungen an Termine und Notizen"
                },
                NotificationChannel(CHANNEL_BRIEFING, "Morgen-Briefing", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Tägliche Zusammenfassung am Morgen"
                },
                NotificationChannel(CHANNEL_DRAFTS, "Entwürfe", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Hinweis auf Entwürfe, die noch nicht geplant sind"
                },
            ),
        )
    }

    fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** Zugriff aus Receivern/Workern ohne eigene Injektion. */
    @dagger.hilt.EntryPoint
    @dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
    interface LogEntryPoint {
        fun notifications(): NotificationRepository

        @ApplicationScope fun scope(): CoroutineScope
    }

    fun post(context: Context, channel: String, id: Int, title: String, text: String) {
        // Mitschreiben auch ohne Systemberechtigung: die Seite „Benachrichtigungen“ zeigt alles, was fällig war.
        runCatching {
            val ep = dagger.hilt.android.EntryPointAccessors.fromApplication(context.applicationContext, LogEntryPoint::class.java)
            ep.scope().launch { ep.notifications().log(channel, title, text) }
        }
        if (!canPost(context)) return
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_organi7er)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setColor(0xFF061B3A.toInt())
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
            // Berechtigung wurde zwischenzeitlich entzogen – Erinnerung still verwerfen.
        }
    }
}

@Singleton
class AlarmReminderScheduler @Inject constructor(@ApplicationContext private val context: Context) : ReminderScheduler {
    private val alarms get() = context.getSystemService(AlarmManager::class.java)

    override fun scheduleEvent(event: Event) {
        cancelEvent(event.id)
        val start = event.start ?: return
        val minutes = event.reminderMinutes ?: return
        if (event.status != Status.ACTIVE) return
        val time = if (event.allDay) start.toLocalDate().atTime(8, 0).minusMinutes(minutes.toLong()) else start.minusMinutes(minutes.toLong())
        val text = if (event.allDay) "Heute ganztägig" else "${TIME.format(start)} · ${event.location}".trimEnd(' ', '·')
        set(eventCode(event.id), time, "event", event.title.ifBlank { "Termin" }, text)
    }

    override fun cancelEvent(id: Long) = cancel(eventCode(id))

    override fun scheduleNote(note: Note) {
        cancelNote(note.id)
        val at = note.reminderAt ?: return
        if (note.status == Status.DONE) return
        set(noteCode(note.id), at, "note", note.title.ifBlank { "Notiz" }, note.body.take(120))
    }

    override fun cancelNote(id: Long) = cancel(noteCode(id))

    private fun set(code: Int, at: LocalDateTime, kind: String, title: String, text: String) {
        val millis = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (millis <= System.currentTimeMillis()) return
        val pi = pending(code, kind, title, text)
        val manager = alarms
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !manager.canScheduleExactAlarms()) {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
        } else {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
        }
    }

    private fun cancel(code: Int) = alarms.cancel(pending(code, "", "", ""))

    private fun pending(code: Int, kind: String, title: String, text: String): PendingIntent = PendingIntent.getBroadcast(
        context,
        code,
        Intent(context, ReminderReceiver::class.java)
            .setAction("app.organi7er.REMINDER.$code")
            .putExtra(EXTRA_KIND, kind)
            .putExtra(EXTRA_TITLE, title)
            .putExtra(EXTRA_TEXT, text)
            .putExtra(EXTRA_CODE, code),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun eventCode(id: Long) = (id * 2).toInt()
    private fun noteCode(id: Long) = (id * 2 + 1).toInt()

    companion object {
        const val EXTRA_KIND = "kind"
        const val EXTRA_TITLE = "title"
        const val EXTRA_TEXT = "text"
        const val EXTRA_CODE = "code"
        private val TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.GERMANY)
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Notifications.post(
            context,
            Notifications.CHANNEL_REMINDERS,
            intent.getIntExtra(AlarmReminderScheduler.EXTRA_CODE, 0),
            intent.getStringExtra(AlarmReminderScheduler.EXTRA_TITLE).orEmpty(),
            intent.getStringExtra(AlarmReminderScheduler.EXTRA_TEXT).orEmpty(),
        )
    }
}

/** Nach Neustart oder Zeitänderung alle künftigen Erinnerungen neu stellen. */
@AndroidEntryPoint
class RescheduleReceiver : BroadcastReceiver() {
    @Inject lateinit var eventDao: EventDao

    @Inject lateinit var noteDao: NoteDao

    @Inject lateinit var scheduler: ReminderScheduler

    @Inject @ApplicationScope
    lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in ALLOWED) return
        val pending = goAsync()
        scope.launch {
            try {
                val now = LocalDateTime.now()
                eventDao.withUpcomingReminders(now).forEach(scheduler::scheduleEvent)
                noteDao.withUpcomingReminders(now).forEach(scheduler::scheduleNote)
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        val ALLOWED = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED",
        )
    }
}

@HiltWorker
class MorningBriefingWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val briefing: BriefingRepository,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val today = briefing.briefings.first().firstOrNull() ?: return Result.success()
        val text = buildString {
            append("${today.events} Termine, ${today.tasks} Aufgaben")
            if (today.notes > 0) append(", ${today.notes} Notizen mit Erinnerung")
            if (today.caption.isNotBlank()) append(". Als Nächstes: ${today.caption}")
        }
        Notifications.post(applicationContext, Notifications.CHANNEL_BRIEFING, 9_001, "Guten Morgen · ${today.big}", text)
        return Result.success()
    }
}

@HiltWorker
class DraftReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val tasks: TaskRepository,
    private val events: EventRepository,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val limit = java.time.Instant.now().minus(Duration.ofDays(7))
        val stale = tasks.tasks.first().count { it.status == Status.DRAFT && it.createdAt.isBefore(limit) } +
            events.events.first().count { it.status == Status.DRAFT && it.createdAt.isBefore(limit) }
        if (stale > 0) {
            Notifications.post(
                applicationContext,
                Notifications.CHANNEL_DRAFTS,
                9_002,
                "Entwürfe warten",
                if (stale == 1) "Ein Entwurf ist seit über einer Woche ungeplant." else "$stale Entwürfe sind seit über einer Woche ungeplant.",
            )
        }
        return Result.success()
    }
}

/** Plant oder entfernt die täglichen Hintergrundaufgaben passend zu den Einstellungen. */
@Singleton
class BackgroundScheduler @Inject constructor(@ApplicationContext private val context: Context) {
    fun apply(settings: AppSettings) {
        val wm = WorkManager.getInstance(context)
        if (settings.morningBriefing) {
            val request = PeriodicWorkRequestBuilder<MorningBriefingWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delayUntil(settings.morningBriefingTime), TimeUnit.MILLISECONDS)
                .build()
            wm.enqueueUniquePeriodicWork(BRIEFING, ExistingPeriodicWorkPolicy.UPDATE, request)
        } else {
            wm.cancelUniqueWork(BRIEFING)
        }
        if (settings.draftReminders) {
            val request = PeriodicWorkRequestBuilder<DraftReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delayUntil(10 * 60), TimeUnit.MILLISECONDS)
                .build()
            wm.enqueueUniquePeriodicWork(DRAFTS, ExistingPeriodicWorkPolicy.UPDATE, request)
        } else {
            wm.cancelUniqueWork(DRAFTS)
        }
    }

    private fun delayUntil(minuteOfDay: Int): Long {
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atStartOfDay().plusMinutes(minuteOfDay.toLong())
        if (!next.isAfter(now)) next = next.plusDays(1)
        return Duration.between(now, next).toMillis()
    }

    private companion object {
        const val BRIEFING = "morning-briefing"
        const val DRAFTS = "draft-reminders"
    }
}

package app.organi7er.core.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import app.organi7er.core.model.AppSettings
import app.organi7er.core.model.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.inject.Inject
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.withContext

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

/** Zeitquelle. In Tests ersetzbar; [minuteTicks] treibt alle „in X Min“-Anzeigen. */
interface TimeProvider {
    fun now(): LocalDateTime
    val minuteTicks: Flow<LocalDateTime>
}

@Singleton
class SystemTimeProvider @Inject constructor(@ApplicationScope scope: CoroutineScope) : TimeProvider {
    override fun now(): LocalDateTime = LocalDateTime.now()

    /** Ein geteilter Takt für die ganze App, ausgerichtet auf den Minutenwechsel. */
    override val minuteTicks: Flow<LocalDateTime> = flow {
        while (true) {
            val now = LocalDateTime.now()
            emit(now.truncatedTo(ChronoUnit.MINUTES))
            val next = now.truncatedTo(ChronoUnit.MINUTES).plusMinutes(1)
            delay(ChronoUnit.MILLIS.between(now, next).coerceAtLeast(250))
        }
    }.shareIn(scope, SharingStarted.WhileSubscribed(5_000), replay = 1)
}

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun update(transform: (AppSettings) -> AppSettings)
}

@Singleton
class DataStoreSettingsRepository @Inject constructor(private val store: DataStore<Preferences>) : SettingsRepository {
    private object Keys {
        val theme = stringPreferencesKey("theme")
        val tomorrow = booleanPreferencesKey("show_tomorrow")
        val weekStart = intPreferencesKey("week_start")
        val lead = intPreferencesKey("reminder_lead")
        val briefing = booleanPreferencesKey("briefing")
        val briefingTime = intPreferencesKey("briefing_time")
        val drafts = booleanPreferencesKey("draft_reminders")
        val seeded = booleanPreferencesKey("seeded")
        val profileName = stringPreferencesKey("profile_name")
        val profileImage = stringPreferencesKey("profile_image")
    }

    override val settings: Flow<AppSettings> = store.data.map { p ->
        val d = AppSettings()
        AppSettings(
            themeMode = p[Keys.theme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: d.themeMode,
            showTomorrowTile = p[Keys.tomorrow] ?: d.showTomorrowTile,
            weekStart = p[Keys.weekStart] ?: d.weekStart,
            reminderLeadMinutes = p[Keys.lead] ?: d.reminderLeadMinutes,
            morningBriefing = p[Keys.briefing] ?: d.morningBriefing,
            morningBriefingTime = p[Keys.briefingTime] ?: d.morningBriefingTime,
            draftReminders = p[Keys.drafts] ?: d.draftReminders,
            sampleDataSeeded = p[Keys.seeded] ?: d.sampleDataSeeded,
            profileName = p[Keys.profileName] ?: d.profileName,
            profileImage = p[Keys.profileImage],
        )
    }.distinctUntilChanged()

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        store.edit { p ->
            val current = AppSettings(
                themeMode = p[Keys.theme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
                showTomorrowTile = p[Keys.tomorrow] ?: true,
                weekStart = p[Keys.weekStart] ?: 1,
                reminderLeadMinutes = p[Keys.lead] ?: 15,
                morningBriefing = p[Keys.briefing] ?: true,
                morningBriefingTime = p[Keys.briefingTime] ?: (7 * 60 + 30),
                draftReminders = p[Keys.drafts] ?: false,
                sampleDataSeeded = p[Keys.seeded] ?: false,
                profileName = p[Keys.profileName] ?: "",
                profileImage = p[Keys.profileImage],
            )
            val n = transform(current)
            p[Keys.theme] = n.themeMode.name
            p[Keys.tomorrow] = n.showTomorrowTile
            p[Keys.weekStart] = n.weekStart
            p[Keys.lead] = n.reminderLeadMinutes
            p[Keys.briefing] = n.morningBriefing
            p[Keys.briefingTime] = n.morningBriefingTime
            p[Keys.drafts] = n.draftReminders
            p[Keys.seeded] = n.sampleDataSeeded
            p[Keys.profileName] = n.profileName
            if (n.profileImage != null) p[Keys.profileImage] = n.profileImage else p.remove(Keys.profileImage)
        }
    }
}

/** Bilder liegen im App-Speicher (files/images), nie nur als fremde Content-URI. */
@Singleton
class ImageStore @Inject constructor(@ApplicationContext private val context: Context) {
    private val dir: File get() = File(context.filesDir, "images").apply { mkdirs() }

    suspend fun importUri(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val target = File(dir, "${UUID.randomUUID()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input -> target.outputStream().use { input.copyTo(it) } }
                ?: return@runCatching null
            target.absolutePath
        }.getOrNull()
    }

    /** Ziel für die Kamera (TakePicture) samt FileProvider-URI. */
    fun newCameraTarget(): Pair<File, Uri> {
        val file = File(dir, "${UUID.randomUUID()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        return file to uri
    }

    suspend fun delete(path: String) = withContext(Dispatchers.IO) {
        if (path.startsWith(dir.absolutePath)) File(path).delete()
    }

    suspend fun deleteAll() = withContext(Dispatchers.IO) { dir.listFiles()?.forEach { it.delete() } }

    /** Kopiert ein mitgeliefertes Bild (res/raw) in den App-Speicher (nur für Beispieldaten). */
    suspend fun copyRaw(resId: Int, name: String): String = withContext(Dispatchers.IO) {
        val file = File(dir, name)
        context.resources.openRawResource(resId).use { input -> file.outputStream().use { input.copyTo(it) } }
        file.absolutePath
    }

    /** Erzeugt ein weiches Verlaufsbild als Beispielfoto (nur für Beispieldaten). */
    suspend fun createSampleImage(variant: Int): String = withContext(Dispatchers.IO) {
        val w = 600
        val h = 780
        val bmp = createBitmap(w, h)
        val canvas = Canvas(bmp)
        val palettes = listOf(
            intArrayOf(0xFF3B82F6.toInt(), 0xFF0F2A55.toInt(), 0xFF93BBFD.toInt()),
            intArrayOf(0xFF93BBFD.toInt(), 0xFF1A3665.toInt(), 0xFFFFEC80.toInt()),
            intArrayOf(0xFF2563EB.toInt(), 0xFF061B3A.toInt(), 0xFFFFD500.toInt()),
            intArrayOf(0xFF60A5FA.toInt(), 0xFF0A2248.toInt(), 0xFFDBE8FE.toInt()),
        )
        val p = palettes[variant % palettes.size]
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), Paint().apply { shader = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(), p[0], p[1], Shader.TileMode.CLAMP) })
        canvas.drawCircle(w * 0.3f, h * 0.3f, w * 0.7f, Paint().apply { shader = RadialGradient(w * 0.3f, h * 0.3f, w * 0.7f, p[2], 0x00000000, Shader.TileMode.CLAMP) })
        val file = File(dir, "sample_$variant.jpg")
        file.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        bmp.recycle()
        file.absolutePath
    }
}

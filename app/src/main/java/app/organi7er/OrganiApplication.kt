package app.organi7er

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import app.organi7er.core.data.ApplicationScope
import app.organi7er.core.data.BackgroundScheduler
import app.organi7er.core.data.Notifications
import app.organi7er.core.data.SampleData
import app.organi7er.core.data.SettingsRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@HiltAndroidApp
class OrganiApplication : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var sampleData: SampleData

    @Inject lateinit var settings: SettingsRepository

    @Inject lateinit var background: BackgroundScheduler

    @Inject @ApplicationScope
    lateinit var scope: CoroutineScope

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        Notifications.createChannels(this)
        scope.launch { sampleData.seedIfNeeded() }
        scope.launch {
            settings.settings
                .map { Triple(it.morningBriefing, it.morningBriefingTime, it.draftReminders) }
                .distinctUntilChanged()
                .collect { (briefing, time, drafts) ->
                    background.apply(app.organi7er.core.model.AppSettings(morningBriefing = briefing, morningBriefingTime = time, draftReminders = drafts))
                }
        }
    }
}

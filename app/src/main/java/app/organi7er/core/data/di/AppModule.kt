package app.organi7er.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import app.organi7er.core.data.AlarmReminderScheduler
import app.organi7er.core.data.ApplicationScope
import app.organi7er.core.data.DataStoreSettingsRepository
import app.organi7er.core.data.EventRepository
import app.organi7er.core.data.NoteRepository
import app.organi7er.core.data.NotificationRepository
import app.organi7er.core.data.ReminderScheduler
import app.organi7er.core.data.RoomEventRepository
import app.organi7er.core.data.RoomNoteRepository
import app.organi7er.core.data.RoomNotificationRepository
import app.organi7er.core.data.RoomTaskRepository
import app.organi7er.core.data.SettingsRepository
import app.organi7er.core.data.SystemTimeProvider
import app.organi7er.core.data.TaskRepository
import app.organi7er.core.data.TimeProvider
import app.organi7er.core.data.db.MIGRATION_1_2
import app.organi7er.core.data.db.MIGRATION_2_3
import app.organi7er.core.data.db.OrganiDatabase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): OrganiDatabase =
        Room.databaseBuilder(context, OrganiDatabase::class.java, "organi7er.db").addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()

    @Provides fun calendarDao(db: OrganiDatabase) = db.calendarDao()

    @Provides fun eventDao(db: OrganiDatabase) = db.eventDao()

    @Provides fun taskDao(db: OrganiDatabase) = db.taskDao()

    @Provides fun noteDao(db: OrganiDatabase) = db.noteDao()

    @Provides fun notificationDao(db: OrganiDatabase) = db.notificationDao()

    @Provides
    @Singleton
    fun dataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(produceFile = { context.preferencesDataStoreFile("settings") })

    @Provides
    @Singleton
    @ApplicationScope
    fun applicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class BindingsModule {
    @Binds abstract fun events(impl: RoomEventRepository): EventRepository

    @Binds abstract fun tasks(impl: RoomTaskRepository): TaskRepository

    @Binds abstract fun notes(impl: RoomNoteRepository): NoteRepository

    @Binds abstract fun notifications(impl: RoomNotificationRepository): NotificationRepository

    @Binds abstract fun settings(impl: DataStoreSettingsRepository): SettingsRepository

    @Binds abstract fun time(impl: SystemTimeProvider): TimeProvider

    @Binds abstract fun reminders(impl: AlarmReminderScheduler): ReminderScheduler
}

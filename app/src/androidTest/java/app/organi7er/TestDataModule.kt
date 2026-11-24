package app.organi7er

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import app.organi7er.core.data.ApplicationScope
import app.organi7er.core.data.db.OrganiDatabase
import app.organi7er.core.data.di.DataModule
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import java.util.UUID
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Pro Test eine eigene Datenbank (im Speicher) und eine eigene DataStore-Datei. */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [DataModule::class])
object TestDataModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): OrganiDatabase = Room.inMemoryDatabaseBuilder(context, OrganiDatabase::class.java).build()

    @Provides fun calendarDao(db: OrganiDatabase) = db.calendarDao()

    @Provides fun eventDao(db: OrganiDatabase) = db.eventDao()

    @Provides fun taskDao(db: OrganiDatabase) = db.taskDao()

    @Provides fun noteDao(db: OrganiDatabase) = db.noteDao()

    @Provides fun notificationDao(db: OrganiDatabase) = db.notificationDao()

    @Provides
    @Singleton
    fun dataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(produceFile = { context.preferencesDataStoreFile("test-${UUID.randomUUID()}") })

    @Provides
    @Singleton
    @ApplicationScope
    fun applicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}

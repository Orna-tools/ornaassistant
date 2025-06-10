package com.lloir.ornaassistant.di

import android.content.Context
import androidx.room.Room
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.lloir.ornaassistant.data.database.OrnaDatabase
import com.lloir.ornaassistant.data.database.dao.*
import com.lloir.ornaassistant.data.network.api.OrnaGuideApi
import com.lloir.ornaassistant.data.preferences.SettingsDataStore
import com.lloir.ornaassistant.data.repository.*
import com.lloir.ornaassistant.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideOrnaDatabase(@ApplicationContext context: Context): OrnaDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            OrnaDatabase::class.java,
            OrnaDatabase.DATABASE_NAME
        )
            .addMigrations(OrnaDatabase.MIGRATION_LEGACY_TO_1)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideDungeonVisitDao(database: OrnaDatabase): DungeonVisitDao {
        return database.dungeonVisitDao()
    }

    @Provides
    fun provideWayvesselSessionDao(database: OrnaDatabase): WayvesselSessionDao {
        return database.wayvesselSessionDao()
    }

    @Provides
    fun provideKingdomMemberDao(database: OrnaDatabase): KingdomMemberDao {
        return database.kingdomMemberDao()
    }

    @Provides
    fun provideItemAssessmentDao(database: OrnaDatabase): ItemAssessmentDao {
        return database.itemAssessmentDao()
    }
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
            .create()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
            )
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        gson: Gson
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(OrnaGuideApi.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideOrnaGuideApi(retrofit: Retrofit): OrnaGuideApi {
        return retrofit.create(OrnaGuideApi::class.java)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): SettingsDataStore {
        return SettingsDataStore(context)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindDungeonRepository(
        dungeonRepositoryImpl: DungeonRepositoryImpl
    ): DungeonRepository

    @Binds
    abstract fun bindWayvesselRepository(
        wayvesselRepositoryImpl: WayvesselRepositoryImpl
    ): WayvesselRepository

    @Binds
    abstract fun bindItemAssessmentRepository(
        itemAssessmentRepositoryImpl: ItemAssessmentRepositoryImpl
    ): ItemAssessmentRepository

    @Binds
    abstract fun bindSettingsRepository(
        settingsRepositoryImpl: SettingsRepositoryImpl
    ): SettingsRepository

    @Binds
    abstract fun bindNotificationRepository(
        notificationRepositoryImpl: NotificationRepositoryImpl
    ): NotificationRepository
}

// Settings DataStore Implementation
package com.lloir.ornaassistant.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.lloir.ornaassistant.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

@Singleton
class SettingsDataStore @Inject constructor(
    private val context: Context
) {
    private val dataStore = context.dataStore

    private object PreferencesKeys {
        val SHOW_SESSION_OVERLAY = booleanPreferencesKey("show_session_overlay")
        val SHOW_INVITES_OVERLAY = booleanPreferencesKey("show_invites_overlay")
        val SHOW_ASSESS_OVERLAY = booleanPreferencesKey("show_assess_overlay")
        val WAYVESSEL_NOTIFICATIONS = booleanPreferencesKey("wayvessel_notifications")
        val NOTIFICATION_SOUNDS = booleanPreferencesKey("notification_sounds")
        val OVERLAY_TRANSPARENCY = floatPreferencesKey("overlay_transparency")
        val AUTO_HIDE_OVERLAYS = booleanPreferencesKey("auto_hide_overlays")
    }

    val settingsFlow: Flow<AppSettings> = dataStore.data.map { preferences ->
        AppSettings(
            showSessionOverlay = preferences[PreferencesKeys.SHOW_SESSION_OVERLAY] ?: true,
            showInvitesOverlay = preferences[PreferencesKeys.SHOW_INVITES_OVERLAY] ?: true,
            showAssessOverlay = preferences[PreferencesKeys.SHOW_ASSESS_OVERLAY] ?: true,
            wayvesselNotifications = preferences[PreferencesKeys.WAYVESSEL_NOTIFICATIONS] ?: true,
            notificationSounds = preferences[PreferencesKeys.NOTIFICATION_SOUNDS] ?: true,
            overlayTransparency = preferences[PreferencesKeys.OVERLAY_TRANSPARENCY] ?: 0.8f,
            autoHideOverlays = preferences[PreferencesKeys.AUTO_HIDE_OVERLAYS] ?: false
        )
    }

    suspend fun getSettings(): AppSettings {
        return settingsFlow.first()
    }

    suspend fun updateSettings(settings: AppSettings) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_SESSION_OVERLAY] = settings.showSessionOverlay
            preferences[PreferencesKeys.SHOW_INVITES_OVERLAY] = settings.showInvitesOverlay
            preferences[PreferencesKeys.SHOW_ASSESS_OVERLAY] = settings.showAssessOverlay
            preferences[PreferencesKeys.WAYVESSEL_NOTIFICATIONS] = settings.wayvesselNotifications
            preferences[PreferencesKeys.NOTIFICATION_SOUNDS] = settings.notificationSounds
            preferences[PreferencesKeys.OVERLAY_TRANSPARENCY] = settings.overlayTransparency
            preferences[PreferencesKeys.AUTO_HIDE_OVERLAYS] = settings.autoHideOverlays
        }
    }

    suspend fun updateSessionOverlay(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_SESSION_OVERLAY] = enabled
        }
    }

    suspend fun updateInvitesOverlay(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_INVITES_OVERLAY] = enabled
        }
    }

    suspend fun updateAssessOverlay(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_ASSESS_OVERLAY] = enabled
        }
    }

    suspend fun updateWayvesselNotifications(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.WAYVESSEL_NOTIFICATIONS] = enabled
        }
    }

    suspend fun updateOverlayTransparency(transparency: Float) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.OVERLAY_TRANSPARENCY] = transparency
        }
    }
}
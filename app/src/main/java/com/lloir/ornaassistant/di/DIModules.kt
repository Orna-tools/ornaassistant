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


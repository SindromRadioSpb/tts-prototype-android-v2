package com.sindromradiospb.ttsprototypev2

import android.app.Application
import androidx.room.Room
import com.sindromradiospb.ttsprototypev2.data.audio.AudioStorageRepository
import com.sindromradiospb.ttsprototypev2.data.audio.AndroidAudioPlaybackController
import com.sindromradiospb.ttsprototypev2.data.db.AppDatabase
import com.sindromradiospb.ttsprototypev2.data.export.LibraryZipExportRepository
import com.sindromradiospb.ttsprototypev2.data.export.LibraryZipImportRepository
import com.sindromradiospb.ttsprototypev2.data.provider.translation.createAndroidTranslationProviderRegistry
import com.sindromradiospb.ttsprototypev2.data.provider.tts.createAndroidTtsProviderRegistry
import com.sindromradiospb.ttsprototypev2.data.repository.RoomLibraryRepository
import com.sindromradiospb.ttsprototypev2.data.settings.AndroidKeystoreSecureKeyValueStore
import com.sindromradiospb.ttsprototypev2.data.settings.ProviderSettingsRepository

class TtsPrototypeApplication : Application() {
    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "tts-prototype-v2.db",
        ).addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
            .build()
    }

    val libraryRepository: RoomLibraryRepository by lazy {
        RoomLibraryRepository(database)
    }

    val providerSettingsRepository: ProviderSettingsRepository by lazy {
        ProviderSettingsRepository(
            AndroidKeystoreSecureKeyValueStore(this),
        )
    }

    val translationProviderRegistry by lazy {
        createAndroidTranslationProviderRegistry(providerSettingsRepository)
    }

    val ttsProviderRegistry by lazy {
        createAndroidTtsProviderRegistry(
            context = this,
            providerSettingsRepository = providerSettingsRepository,
        )
    }

    val audioStorageRepository: AudioStorageRepository by lazy {
        AudioStorageRepository(
            database = database,
            appFilesDir = filesDir,
        )
    }

    val audioPlaybackController: AndroidAudioPlaybackController by lazy {
        AndroidAudioPlaybackController()
    }

    val libraryZipExportRepository: LibraryZipExportRepository by lazy {
        LibraryZipExportRepository(
            database = database,
            appFilesDir = filesDir,
        )
    }

    val libraryZipImportRepository: LibraryZipImportRepository by lazy {
        LibraryZipImportRepository(
            database = database,
            appFilesDir = filesDir,
        )
    }
}

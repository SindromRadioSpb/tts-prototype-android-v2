package com.sindromradiospb.ttsprototypev2

import android.app.Application
import androidx.room.Room
import com.sindromradiospb.ttsprototypev2.data.audio.AudioStorageRepository
import com.sindromradiospb.ttsprototypev2.data.db.AppDatabase
import com.sindromradiospb.ttsprototypev2.data.export.LibraryZipExportRepository
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
        ).build()
    }

    val libraryRepository: RoomLibraryRepository by lazy {
        RoomLibraryRepository(database)
    }

    val providerSettingsRepository: ProviderSettingsRepository by lazy {
        ProviderSettingsRepository(
            AndroidKeystoreSecureKeyValueStore(this),
        )
    }

    val ttsProviderRegistry by lazy {
        createAndroidTtsProviderRegistry(this)
    }

    val audioStorageRepository: AudioStorageRepository by lazy {
        AudioStorageRepository(
            database = database,
            appFilesDir = filesDir,
        )
    }

    val libraryZipExportRepository: LibraryZipExportRepository by lazy {
        LibraryZipExportRepository(
            database = database,
            appFilesDir = filesDir,
        )
    }
}

package com.sindromradiospb.ttsprototypev2

import android.app.Application
import androidx.room.Room
import com.sindromradiospb.ttsprototypev2.data.audio.AudioStorageRepository
import com.sindromradiospb.ttsprototypev2.data.db.AppDatabase
import com.sindromradiospb.ttsprototypev2.data.provider.tts.createAndroidTtsProviderRegistry
import com.sindromradiospb.ttsprototypev2.data.repository.RoomLibraryRepository

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

    val ttsProviderRegistry by lazy {
        createAndroidTtsProviderRegistry(this)
    }

    val audioStorageRepository: AudioStorageRepository by lazy {
        AudioStorageRepository(
            database = database,
            appFilesDir = filesDir,
        )
    }
}

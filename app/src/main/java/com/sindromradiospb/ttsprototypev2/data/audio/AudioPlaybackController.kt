package com.sindromradiospb.ttsprototypev2.data.audio

import android.media.MediaPlayer
import java.io.File

sealed class AudioPlaybackResult {
    data object Started : AudioPlaybackResult()
    data class Failed(val message: String) : AudioPlaybackResult()
}

interface AudioPlaybackController {
    fun play(file: File): AudioPlaybackResult
    fun stop()
    fun release()
}

class AndroidAudioPlaybackController : AudioPlaybackController {
    private var mediaPlayer: MediaPlayer? = null

    override fun play(file: File): AudioPlaybackResult {
        if (!file.exists() || file.length() <= 0L) {
            return AudioPlaybackResult.Failed("Audio file is missing.")
        }
        stop()
        return try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnCompletionListener {
                    this@AndroidAudioPlaybackController.stop()
                }
                prepare()
                start()
            }
            AudioPlaybackResult.Started
        } catch (error: RuntimeException) {
            stop()
            AudioPlaybackResult.Failed(error.message ?: "Audio playback failed.")
        }
    }

    override fun stop() {
        mediaPlayer?.runCatching {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null
    }

    override fun release() {
        stop()
    }
}

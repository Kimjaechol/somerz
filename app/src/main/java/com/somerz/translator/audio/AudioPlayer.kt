package com.somerz.translator.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Audio player for playing PCM audio output from Gemini Live API
 */
class AudioPlayer {

    companion object {
        const val SAMPLE_RATE = 24000 // Gemini outputs at 24kHz
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private var audioTrack: AudioTrack? = null
    private var isPlaying = false

    private val bufferSize = AudioTrack.getMinBufferSize(
        SAMPLE_RATE,
        CHANNEL_CONFIG,
        AUDIO_FORMAT
    ).coerceAtLeast(4096)

    /**
     * Initialize the audio player
     */
    fun initialize() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()

        val audioFormat = AudioFormat.Builder()
            .setSampleRate(SAMPLE_RATE)
            .setEncoding(AUDIO_FORMAT)
            .setChannelMask(CHANNEL_CONFIG)
            .build()

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(audioAttributes)
            .setAudioFormat(audioFormat)
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
    }

    /**
     * Play audio data
     */
    suspend fun play(audioData: ByteArray) = withContext(Dispatchers.IO) {
        if (audioTrack == null) {
            initialize()
        }

        if (audioTrack?.state != AudioTrack.STATE_INITIALIZED) {
            return@withContext
        }

        if (!isPlaying) {
            audioTrack?.play()
            isPlaying = true
        }

        audioTrack?.write(audioData, 0, audioData.size)
    }

    /**
     * Stop playback
     */
    fun stop() {
        isPlaying = false
        audioTrack?.stop()
    }

    /**
     * Release resources
     */
    fun release() {
        stop()
        audioTrack?.release()
        audioTrack = null
    }

    /**
     * Check if currently playing
     */
    fun isPlaying(): Boolean = isPlaying
}

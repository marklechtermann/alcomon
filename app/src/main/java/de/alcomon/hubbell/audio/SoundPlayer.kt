package de.alcomon.hubbell.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/** Spielt die im Code erzeugten Sounds über je einen statischen AudioTrack ab. */
class SoundPlayer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val tracks = ConcurrentHashMap<Sfx, AudioTrack>()

    var enabled: Boolean = true
        set(value) {
            field = value
            if (!value) stopAll()
        }

    init {
        // Die Samples werden einmalig im Hintergrund berechnet.
        scope.launch {
            for (sfx in Sfx.entries) {
                runCatching { tracks[sfx] = createTrack(Synth.render(sfx)) }
            }
        }
    }

    fun play(sfx: Sfx) {
        if (!enabled) return
        val track = tracks[sfx] ?: return
        scope.launch {
            runCatching {
                if (track.playState == AudioTrack.PLAYSTATE_PLAYING) track.stop()
                track.reloadStaticData()
                track.play()
            }
        }
    }

    fun stopAll() {
        tracks.values.forEach { runCatching { if (it.playState == AudioTrack.PLAYSTATE_PLAYING) it.stop() } }
    }

    fun release() {
        scope.cancel()
        tracks.values.forEach { runCatching { it.release() } }
        tracks.clear()
    }

    private fun createTrack(pcm: ShortArray): AudioTrack {
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(Synth.SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(pcm.size * 2)
            .build()
        track.write(pcm, 0, pcm.size)
        return track
    }
}

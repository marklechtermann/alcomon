package de.alcomon.hubbell.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import de.alcomon.hubbell.R

/** Die Hintergrundmusik „Galloping Riffs“ läuft in Schleife leise unter den Effekten. */
class MusicPlayer(private val context: Context) {
    private var player: MediaPlayer? = null
    private var foreground = false

    var enabled: Boolean = true
        set(value) {
            field = value
            update()
        }

    /** App ist sichtbar: Musik läuft (wenn der Ton an ist). */
    fun resume() {
        foreground = true
        update()
    }

    /** App im Hintergrund: Musik pausiert. */
    fun pause() {
        foreground = false
        update()
    }

    private fun update() {
        runCatching {
            if (enabled && foreground) {
                val p = player ?: createPlayer()?.also { player = it }
                if (p != null && !p.isPlaying) p.start()
            } else {
                player?.let { if (it.isPlaying) it.pause() }
            }
        }
    }

    // MediaPlayer.create(…, audioSessionId = 0) liefert auf vielen Geräten null, daher selbst vorbereiten
    private fun createPlayer(): MediaPlayer? = runCatching {
        context.resources.openRawResourceFd(R.raw.background_music).use { afd ->
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                )
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                isLooping = true
                setVolume(VOLUME, VOLUME)
                prepare()
            }
        }
    }.onFailure { Log.w("Hubbell", "Musik konnte nicht starten", it) }.getOrNull()

    fun release() {
        runCatching { player?.release() }
        player = null
    }

    companion object {
        private const val VOLUME = 0.6f
    }
}

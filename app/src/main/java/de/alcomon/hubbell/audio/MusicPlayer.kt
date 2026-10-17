package de.alcomon.hubbell.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
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
                val p = player ?: MediaPlayer.create(
                    context,
                    R.raw.background_music,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                    0,
                )?.apply {
                    isLooping = true
                    setVolume(VOLUME, VOLUME)
                }.also { player = it }
                if (p != null && !p.isPlaying) p.start()
            } else {
                player?.let { if (it.isPlaying) it.pause() }
            }
        }
    }

    fun release() {
        runCatching { player?.release() }
        player = null
    }

    companion object {
        private const val VOLUME = 0.35f
    }
}

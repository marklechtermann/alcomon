package de.alcomon.hubbell.audio

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tanh
import kotlin.random.Random

/** Die Soundeffekte des Spiels. Alles wird im Code erzeugt, es gibt keine Audiodateien. */
enum class Sfx { PROST, KASSE, LEVEL_UP, ALARM, GAME_OVER, RIFF_1, RIFF_2, RIFF_3, SCHNURR, FANG }

/**
 * Ein kleiner Synthesizer für verzerrte Gitarrenriffs, Schlagzeug und Effekte.
 * Liefert 16-Bit-Mono-Samples.
 */
object Synth {
    const val SAMPLE_RATE = 22050

    /** Tiefes E auf der Gitarre (Drop-Stimmung) in Hertz. */
    private const val LOW_E = 82.41f

    private fun noteFreq(semitones: Int): Float = LOW_E * 2f.pow(semitones / 12f)

    fun render(sfx: Sfx): ShortArray = when (sfx) {
        Sfx.PROST -> toPcm(clink())
        Sfx.KASSE -> toPcm(coin())
        Sfx.LEVEL_UP -> toPcm(riff(LevelUpRiff, bpm = 160, drums = true))
        Sfx.ALARM -> toPcm(alarm())
        Sfx.GAME_OVER -> toPcm(riff(GameOverRiff, bpm = 70, drums = false))
        Sfx.RIFF_1 -> toPcm(riff(Riff1, bpm = 150, drums = true))
        Sfx.RIFF_2 -> toPcm(riff(Riff2, bpm = 120, drums = true))
        Sfx.RIFF_3 -> toPcm(riff(Riff3, bpm = 180, drums = true))
        Sfx.SCHNURR -> toPcm(purr())
        Sfx.FANG -> toPcm(pop())
    }

    /** Ein Riff-Ton: Halbtöne über dem tiefen E, Länge in Achteln, abgedämpft oder offen. */
    data class Note(val semitones: Int?, val eighths: Int = 1, val palmMute: Boolean = true)

    private fun n(s: Int?, e: Int = 1, mute: Boolean = true) = Note(s, e, mute)

    // Galopp-Riff im Stil des klassischen Heavy Metal
    private val Riff1 = listOf(
        n(0), n(0), n(0), n(0), n(3, 2, false), n(0), n(0),
        n(5, 2, false), n(0), n(0), n(7, 2, false), n(6, 2, false), n(5, 2, false),
    )

    // schweres, langsames Doom-Riff
    private val Riff2 = listOf(
        n(0, 3, false), n(0), n(1, 2, false), n(0, 2), n(-1, 4, false), n(null, 1), n(0, 3, false),
    )

    // schnelles Thrash-Riff
    private val Riff3 = listOf(
        n(0), n(0), n(0), n(0), n(0), n(0), n(1), n(0), n(0), n(0), n(0), n(0), n(3), n(0), n(5, 2, false),
        n(0), n(0), n(0), n(0), n(6, 2, false), n(5, 2, false), n(3, 2, false), n(0, 2, false),
    )

    private val LevelUpRiff = listOf(
        n(0), n(0), n(3), n(5), n(7, 2, false), n(12, 6, false),
    )

    private val GameOverRiff = listOf(
        n(7, 2, false), n(6, 2, false), n(5, 2, false), n(4, 6, false),
    )

    private fun riff(notes: List<Note>, bpm: Int, drums: Boolean): FloatArray {
        val eighth = 60f / bpm / 2f
        val totalEighths = notes.sumOf { it.eighths }
        val tail = 0.6f
        val out = FloatArray(((totalEighths * eighth + tail) * SAMPLE_RATE).toInt())
        var pos = 0
        for (note in notes) {
            val length = note.eighths * eighth
            val semis = note.semitones
            if (semis != null) {
                val ring = if (note.palmMute) length * 0.9f else length + 0.35f
                addInto(out, pos, powerChord(noteFreq(semis), ring, note.palmMute))
            }
            pos += (length * SAMPLE_RATE).toInt()
        }
        if (drums) {
            val random = Random(42)
            var beat = 0
            var t = 0
            val quarter = (2 * eighth * SAMPLE_RATE).toInt()
            while (t < pos) {
                addInto(out, t, kick(), 0.9f)
                if (beat % 2 == 1) addInto(out, t, snare(random), 0.5f)
                addInto(out, t + quarter / 2, hihat(random), 0.18f)
                addInto(out, t, hihat(random), 0.18f)
                t += quarter
                beat++
            }
        }
        normalize(out, 0.85f)
        return out
    }

    /** Grundton, Quinte und Oktave als Sägezahn, durch eine Röhren-artige Verzerrung gejagt. */
    private fun powerChord(freq: Float, seconds: Float, palmMute: Boolean): FloatArray {
        val samples = (seconds * SAMPLE_RATE).toInt()
        val out = FloatArray(samples)
        val freqs = floatArrayOf(freq, freq * 1.4983f, freq * 2f, freq * 1.003f)
        val gains = floatArrayOf(1f, 0.8f, 0.5f, 0.6f)
        val decay = if (palmMute) 18f else 2.2f
        val phases = FloatArray(freqs.size)
        var low = 0f
        for (i in 0 until samples) {
            val t = i.toFloat() / SAMPLE_RATE
            var v = 0f
            for (k in freqs.indices) {
                phases[k] += freqs[k] / SAMPLE_RATE
                if (phases[k] >= 1f) phases[k] -= 1f
                v += (phases[k] * 2f - 1f) * gains[k]
            }
            val attack = (t / 0.004f).coerceAtMost(1f)
            val release = ((seconds - t) / 0.02f).coerceIn(0f, 1f)
            val env = attack * release * exp(-decay * t)
            var x = tanh(v * 3.5f) * env
            // abgedämpfte Töne klingen dumpfer
            val cutoff = if (palmMute) 0.25f else 0.55f
            low += cutoff * (x - low)
            x = low
            out[i] = x
        }
        return out
    }

    private fun kick(): FloatArray {
        val samples = (0.25f * SAMPLE_RATE).toInt()
        var phase = 0f
        return FloatArray(samples) { i ->
            val t = i.toFloat() / SAMPLE_RATE
            val f = 50f + 110f * exp(-t * 30f)
            phase += f / SAMPLE_RATE
            sin(2f * PI.toFloat() * phase) * exp(-t * 14f)
        }
    }

    private fun snare(random: Random): FloatArray {
        val samples = (0.18f * SAMPLE_RATE).toInt()
        return FloatArray(samples) { i ->
            val t = i.toFloat() / SAMPLE_RATE
            val noise = random.nextFloat() * 2f - 1f
            (noise * 0.8f + sin(2f * PI.toFloat() * 190f * t) * 0.4f) * exp(-t * 22f)
        }
    }

    private fun hihat(random: Random): FloatArray {
        val samples = (0.05f * SAMPLE_RATE).toInt()
        var last = 0f
        return FloatArray(samples) { i ->
            val t = i.toFloat() / SAMPLE_RATE
            val noise = random.nextFloat() * 2f - 1f
            val high = noise - last
            last = noise
            high * exp(-t * 70f)
        }
    }

    /** Zwei Gläser stoßen an. */
    private fun clink(): FloatArray {
        val samples = (0.6f * SAMPLE_RATE).toInt()
        val partials = floatArrayOf(2093f, 3136f, 4186f, 5274f)
        val out = FloatArray(samples) { i ->
            val t = i.toFloat() / SAMPLE_RATE
            var v = 0f
            partials.forEachIndexed { k, f -> v += sin(2f * PI.toFloat() * f * t) * exp(-t * (6f + k * 3f)) / (k + 1) }
            // zweiter, leicht versetzter Anschlag
            val t2 = t - 0.06f
            if (t2 > 0f) partials.forEachIndexed { k, f -> v += 0.6f * sin(2f * PI.toFloat() * f * 1.02f * t2) * exp(-t2 * (7f + k * 3f)) / (k + 1) }
            v
        }
        normalize(out, 0.5f)
        return out
    }

    /** Münz-Ding beim Einkaufen. */
    private fun coin(): FloatArray {
        val samples = (0.35f * SAMPLE_RATE).toInt()
        val out = FloatArray(samples) { i ->
            val t = i.toFloat() / SAMPLE_RATE
            val f = if (t < 0.08f) 988f else 1319f
            val square = if (sin(2f * PI.toFloat() * f * t) >= 0f) 1f else -1f
            square * exp(-t * 6f)
        }
        normalize(out, 0.35f)
        return out
    }

    /** Zufriedenes Brummen, wenn Hubbell gestreichelt wird. */
    private fun purr(): FloatArray {
        val samples = (0.6f * SAMPLE_RATE).toInt()
        var phase = 0f
        val out = FloatArray(samples) { i ->
            val t = i.toFloat() / SAMPLE_RATE
            val f = 140f + 60f * (t / 0.6f)
            phase += f / SAMPLE_RATE
            val tremolo = 0.6f + 0.4f * sin(2f * PI.toFloat() * 22f * t)
            val env = (t / 0.05f).coerceAtMost(1f) * ((0.6f - t) / 0.15f).coerceIn(0f, 1f)
            tanh(sin(2f * PI.toFloat() * phase) * 2f) * tremolo * env
        }
        normalize(out, 0.45f)
        return out
    }

    /** Kurzes Plopp mit Glitzer, wenn man etwas Herabfallendes fängt. */
    private fun pop(): FloatArray {
        val samples = (0.4f * SAMPLE_RATE).toInt()
        var phase = 0f
        val out = FloatArray(samples) { i ->
            val t = i.toFloat() / SAMPLE_RATE
            val f = 400f + 1400f * (t / 0.4f)
            phase += f / SAMPLE_RATE
            sin(2f * PI.toFloat() * phase) * exp(-t * 7f) +
                0.3f * sin(2f * PI.toFloat() * 2637f * t) * exp(-((t - 0.12f) * 20f).let { it * it })
        }
        normalize(out, 0.45f)
        return out
    }

    /** Warnton, wenn Hubbell nüchtern wird. */
    private fun alarm(): FloatArray {
        val samples = (0.45f * SAMPLE_RATE).toInt()
        val out = FloatArray(samples) { i ->
            val t = i.toFloat() / SAMPLE_RATE
            val on = t < 0.15f || (t > 0.22f && t < 0.37f)
            if (!on) 0f else tanh(sin(2f * PI.toFloat() * (if (t < 0.2f) 880f else 660f) * t) * 3f)
        }
        normalize(out, 0.4f)
        return out
    }

    private fun addInto(target: FloatArray, offset: Int, source: FloatArray, gain: Float = 1f) {
        for (i in source.indices) {
            val j = offset + i
            if (j >= target.size) return
            if (j >= 0) target[j] += source[i] * gain
        }
    }

    private fun normalize(data: FloatArray, peak: Float) {
        var max = 0f
        for (v in data) if (kotlin.math.abs(v) > max) max = kotlin.math.abs(v)
        if (max <= 0f) return
        val scale = peak / max
        for (i in data.indices) data[i] *= scale
    }

    private fun toPcm(data: FloatArray): ShortArray =
        ShortArray(data.size) { i -> (data[i].coerceIn(-1f, 1f) * Short.MAX_VALUE).roundToInt().toShort() }
}

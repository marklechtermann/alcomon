package de.alcomon.hubbell.game

import kotlin.math.max
import kotlin.random.Random

/**
 * Erkennt grob eine Trinkbewegung mit dem Handy: hochnehmen (Handy steht ziemlich aufrecht)
 * und dann wie ein Glas nach hinten kippen, bis es etwa waagerecht oder weiter gekippt ist.
 * Solange es so gekippt bleibt, leert sich das Bier ([fill]); ist es leer, ist der Schluck geschafft.
 *
 * Gefüttert wird mit dem Beschleunigungssensor (m/s², Gerätekoordinaten: x nach rechts,
 * y zur Oberkante, z aus dem Display heraus). Absichtlich großzügig.
 */
class DrinkGesture {
    private var time = 0f
    private val recent = ArrayDeque<Pair<Float, Float>>()
    private var drinking = false
    private var smoothX = Float.NaN
    private var smoothY = Float.NaN
    private var smoothZ = Float.NaN

    /** 0..1: wie weit die aktuelle Bewegung schon an einen Schluck herankommt (für die Anzeige). */
    var progress = 0f
        private set

    /** Wie voll das Bierglas auf dem Display noch ist (1 = voll, 0 = ex). */
    var fill = 1f
        private set

    /** Richtung „unten“ auf dem Display (x nach rechts, y nach unten), damit das Bier waagerecht steht. */
    var downX = 0f
        private set
    var downY = 1f
        private set

    fun reset() {
        time = 0f
        recent.clear()
        drinking = false
        smoothX = Float.NaN
        smoothY = Float.NaN
        smoothZ = Float.NaN
        progress = 0f
        fill = 1f
        downX = 0f
        downY = 1f
    }

    /** Liefert true, sobald das Bier ausgetrunken ist. */
    fun update(x: Float, y: Float, z: Float, dt: Float): Boolean {
        time += dt
        // Sensorrauschen glätten
        val k = (dt * 12f).coerceAtMost(1f)
        smoothX = if (smoothX.isNaN()) x else smoothX + (x - smoothX) * k
        smoothY = if (smoothY.isNaN()) y else smoothY + (y - smoothY) * k
        smoothZ = if (smoothZ.isNaN()) z else smoothZ + (z - smoothZ) * k
        val nx = smoothX / GRAVITY
        val ny = smoothY / GRAVITY
        val nz = smoothZ / GRAVITY

        // Phase 1: wie aufrecht war das Handy in den letzten Sekunden?
        recent.addLast(time to ny)
        while (recent.first().first < time - WINDOW) recent.removeFirst()
        val uprightY = recent.maxOf { it.second }
        val lifted = uprightY > UPRIGHT
        val drop = if (lifted) uprightY - ny else 0f

        // Phase 2: wie ein Glas angesetzt, also Unterkante hoch und Display Richtung Boden
        // (nicht einfach flach auf den Tisch gelegt) oder gleich ganz über Kopf gekippt.
        // Wer schon trinkt, darf so lange weitertrinken, wie das Handy gekippt bleibt.
        drinking = ny < TILTED && (nz < FACE_UP || ny < -0.3f) && (drinking || (lifted && drop > MIN_DROP))
        if (drinking) fill = (fill - dt / DRAIN_SECONDS).coerceAtLeast(0f)

        // Das Bier im Glas bleibt waagerecht. Liegt das Handy fast flach, fließt es beim Trinken
        // zum Rand (oben), sonst bleibt die letzte Richtung stehen.
        val len = kotlin.math.sqrt(nx * nx + ny * ny)
        val (tx, ty) = when {
            len > 0.25f -> -nx / len to ny / len
            drinking -> 0f to -1f
            else -> downX to downY
        }
        val m = (dt * 8f).coerceAtMost(1f)
        val bx = downX + (tx - downX) * m
        val by = downY + (ty - downY) * m
        val bl = kotlin.math.sqrt(bx * bx + by * by).coerceAtLeast(0.001f)
        downX = bx / bl
        downY = by / bl

        val sip = 1f - fill
        progress = when {
            drinking -> 0.5f + 0.5f * sip
            lifted -> (drop / MIN_DROP * 0.45f).coerceIn(0f, 0.45f).coerceAtLeast(sip * 0.5f)
            else -> sip * 0.5f
        }
        if (fill <= 0f) {
            reset()
            return true
        }
        return false
    }

    companion object {
        const val GRAVITY = 9.81f
        const val DRAIN_SECONDS = 1.2f
        private const val UPRIGHT = 0.8f
        private const val TILTED = 0.35f
        private const val MIN_DROP = 0.5f
        private const val FACE_UP = 0.3f
        private const val WINDOW = 2f
    }
}

/** Füllt ein Rechteck (0..1 × 0..1) mit Flüssigkeit, deren Oberfläche senkrecht zu „unten“ steht. */
object Liquid {
    /** Polygon der Flüssigkeit für [fill] (0..1) und die Richtung (dx, dy); [extra] verschiebt die Oberfläche (Schaum). */
    fun polygon(fill: Float, dx: Float, dy: Float, extra: Float = 0f): List<Pair<Float, Float>> {
        if (fill <= 0f) return emptyList()
        val corners = listOf(0f to 0f, 1f to 0f, 1f to 1f, 0f to 1f)
        val proj = corners.map { it.first * dx + it.second * dy }
        var lo = proj.min()
        var hi = proj.max()
        // Grenze s so suchen, dass der Teil mit p·d >= s genau [fill] der Fläche ausmacht
        repeat(30) {
            val mid = (lo + hi) / 2f
            if (area(clip(corners, dx, dy, mid)) > fill) lo = mid else hi = mid
        }
        return clip(corners, dx, dy, (lo + hi) / 2f - extra)
    }

    private fun clip(poly: List<Pair<Float, Float>>, dx: Float, dy: Float, s: Float): List<Pair<Float, Float>> {
        val out = mutableListOf<Pair<Float, Float>>()
        for (i in poly.indices) {
            val a = poly[i]
            val b = poly[(i + 1) % poly.size]
            val da = a.first * dx + a.second * dy - s
            val db = b.first * dx + b.second * dy - s
            if (da >= 0f) out += a
            if ((da >= 0f) != (db >= 0f)) {
                val t = da / (da - db)
                out += (a.first + (b.first - a.first) * t) to (a.second + (b.second - a.second) * t)
            }
        }
        return out
    }

    fun area(poly: List<Pair<Float, Float>>): Float {
        var sum = 0f
        for (i in poly.indices) {
            val a = poly[i]
            val b = poly[(i + 1) % poly.size]
            sum += a.first * b.second - b.first * a.second
        }
        return kotlin.math.abs(sum) / 2f
    }
}

/** Das Special Event „Handy hoch und ex!“. */
object SpecialEvent {
    const val WINDOW_SECONDS = 10f

    fun firstDelay(random: Random = Random): Float = 40f + random.nextFloat() * 25f
    fun nextDelay(random: Random = Random): Float = 70f + random.nextFloat() * 50f

    fun bonusCoins(level: Int): Int = max(60, 20 * level * level.coerceAtMost(10))

    /** Der dicke Bonus: Pegel randvoll, Rettung beendet, viele Taler und eine Menge Erfahrung. */
    fun reward(s: GameState): GameState {
        val filled = s.copy(
            pegel = GameEngine.MAX_PEGEL,
            rescueSecondsLeft = null,
            coins = s.coins + bonusCoins(s.level),
        )
        return GameEngine.gainXpPublic(filled, GameEngine.xpForNextLevel(s.level) * 0.6f)
    }
}

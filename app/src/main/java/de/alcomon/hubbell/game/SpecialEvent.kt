package de.alcomon.hubbell.game

import kotlin.math.max
import kotlin.random.Random

/**
 * Erkennt grob eine Trinkbewegung mit dem Handy: hochnehmen (Handy steht ziemlich aufrecht)
 * und dann wie ein Glas nach hinten kippen, bis es etwa waagerecht oder weiter gekippt ist.
 *
 * Gefüttert wird mit dem Beschleunigungssensor (m/s², Gerätekoordinaten: y zeigt zur Oberkante,
 * z aus dem Display heraus).
 * Absichtlich großzügig: es reicht, das Handy aus einer aufrechten Haltung kräftig zu kippen
 * und kurz so zu halten.
 */
class DrinkGesture {
    private var time = 0f
    private val recent = ArrayDeque<Pair<Float, Float>>()
    private var tiltedFor = 0f
    private var smoothY = Float.NaN
    private var smoothZ = Float.NaN

    /** 0..1: wie weit die aktuelle Bewegung schon an einen Schluck herankommt (für die Anzeige). */
    var progress = 0f
        private set

    fun reset() {
        time = 0f
        recent.clear()
        tiltedFor = 0f
        smoothY = Float.NaN
        smoothZ = Float.NaN
        progress = 0f
    }

    /** Liefert true, sobald eine Trinkbewegung erkannt wurde. */
    fun update(x: Float, y: Float, z: Float, dt: Float): Boolean {
        time += dt
        // Sensorrauschen glätten
        val k = (dt * 12f).coerceAtMost(1f)
        smoothY = if (smoothY.isNaN()) y else smoothY + (y - smoothY) * k
        smoothZ = if (smoothZ.isNaN()) z else smoothZ + (z - smoothZ) * k
        val ny = smoothY / GRAVITY
        val nz = smoothZ / GRAVITY

        // Phase 1: wie aufrecht war das Handy in den letzten Sekunden?
        recent.addLast(time to ny)
        while (recent.first().first < time - WINDOW) recent.removeFirst()
        val uprightY = recent.maxOf { it.second }
        val lifted = uprightY > UPRIGHT
        val drop = if (lifted) uprightY - ny else 0f

        // Phase 2: wie ein Glas angesetzt, also Unterkante hoch und Display Richtung Boden
        // (nicht einfach flach auf den Tisch gelegt) oder gleich ganz über Kopf gekippt
        val tilted = lifted && ny < TILTED && drop > MIN_DROP && (nz < FACE_UP || ny < -0.3f)
        tiltedFor = if (tilted) tiltedFor + dt else 0f

        progress = when {
            tilted -> (0.75f + 0.25f * tiltedFor / HOLD).coerceAtMost(1f)
            lifted -> (drop / MIN_DROP * 0.7f).coerceIn(0f, 0.7f)
            else -> 0f
        }
        if (tiltedFor >= HOLD) {
            reset()
            return true
        }
        return false
    }

    companion object {
        const val GRAVITY = 9.81f
        private const val UPRIGHT = 0.8f
        private const val TILTED = 0.35f
        private const val MIN_DROP = 0.5f
        private const val FACE_UP = 0.3f
        private const val WINDOW = 2f
        private const val HOLD = 0.25f
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

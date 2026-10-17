package de.alcomon.hubbell.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class SpecialEventTest {
    private val g = DrinkGesture.GRAVITY

    /** Simuliert eine Drehung um die Querachse: 0° = flach mit Display nach oben, 90° = aufrecht, 180° = Display nach unten. */
    private fun DrinkGesture.feed(degrees: Float, seconds: Float, dt: Float = 0.02f): Boolean {
        var hit = false
        var t = 0f
        while (t < seconds) {
            val rad = Math.toRadians(degrees.toDouble())
            if (update(0f, (g * sin(rad)).toFloat(), (g * cos(rad)).toFloat(), dt)) hit = true
            t += dt
        }
        return hit
    }

    @Test
    fun liftAndTiltBackIsDetected() {
        val d = DrinkGesture()
        assertFalse(d.feed(45f, 1f)) // normale Spielhaltung
        assertFalse(d.feed(80f, 0.4f)) // hochnehmen
        assertFalse(d.feed(130f, 0.2f)) // ansetzen
        assertFalse(d.feed(170f, 0.6f)) // kippen: das Bier läuft …
        assertTrue(d.fill < 0.8f)
        assertTrue(d.feed(170f, 1f)) // … bis es leer ist
    }

    @Test
    fun lyingOnTheTableIsNoDrink() {
        val d = DrinkGesture()
        assertFalse(d.feed(0f, 5f))
    }

    @Test
    fun normalHoldingIsNoDrink() {
        val d = DrinkGesture()
        assertFalse(d.feed(50f, 3f))
        assertFalse(d.feed(30f, 3f))
    }

    @Test
    fun slowPutDownLongAfterLiftingIsNoDrink() {
        val d = DrinkGesture()
        assertFalse(d.feed(80f, 0.5f))
        assertFalse(d.feed(35f, 1f))
        assertFalse(d.feed(0f, 3f)) // hinlegen
    }

    @Test
    fun upsideDownWithScreenUpAlsoCounts() {
        val d = DrinkGesture()
        assertFalse(d.feed(90f, 0.5f))
        assertTrue(d.feed(-40f, 1.5f))
    }

    @Test
    fun liquidKeepsItsAreaAndStaysLevel() {
        val upright = Liquid.polygon(0.6f, 0f, 1f)
        assertEquals(0.6f, Liquid.area(upright), 0.01f)
        assertTrue(upright.all { it.second >= 0.39f }) // unten im Glas
        val toRim = Liquid.polygon(0.3f, 0f, -1f)
        assertTrue(toRim.all { it.second <= 0.31f }) // zum Rand geflossen
        assertEquals(0.3f, Liquid.area(Liquid.polygon(0.3f, 0.7f, 0.7f)), 0.01f)
        assertTrue(Liquid.polygon(0f, 0f, 1f).isEmpty())
    }

    @Test
    fun rewardFillsPegelAndPaysBig() {
        val s = GameState(pegel = 0f, coins = 5, rescueSecondsLeft = 3f)
        val r = SpecialEvent.reward(s)
        assertEquals(GameEngine.MAX_PEGEL, r.pegel)
        assertNull(r.rescueSecondsLeft)
        assertEquals(5 + SpecialEvent.bonusCoins(1), r.coins)
        assertTrue(r.xp > 0f || r.level > 1)
    }
}

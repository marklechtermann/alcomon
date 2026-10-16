package de.alcomon.hubbell.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEngineTest {

    private fun run(s: GameState, seconds: Float, step: Float = 0.05f): GameState {
        var state = s
        var t = 0f
        while (t < seconds - 1e-4f) {
            state = GameEngine.tick(state, step)
            t += step
        }
        return state
    }

    @Test
    fun pegelSinktMitDerZeit() {
        val s = run(GameState(pegel = 50f), 2f)
        assertEquals(47f, s.pegel, 0.05f)
    }

    @Test
    fun servierenGibtTalerUndStartetTrinken() {
        val s = GameEngine.serve(GameState(pegel = 50f))
        assertEquals(Drink.HELLES, s.drinking)
        assertEquals(1, s.coins)
        assertEquals(1, s.drinksServed)
    }

    @Test
    fun keinZweitesGetraenkWaehrendHubbellTrinkt() {
        val s = GameEngine.serve(GameState())
        assertSame(s, GameEngine.serve(s))
    }

    @Test
    fun trinkenErhoehtDenPegel() {
        val start = GameState(pegel = 50f)
        val s = run(GameEngine.serve(start), Drink.HELLES.drinkSeconds + 0.1f)
        assertNull(s.drinking)
        val expected = 50f + Drink.HELLES.boost - GameEngine.decayPerSecond(start) * (Drink.HELLES.drinkSeconds + 0.1f)
        assertEquals(expected, s.pegel, 0.1f)
    }

    @Test
    fun beiNullStartetZehnSekundenRettung() {
        val s = run(GameState(pegel = 0.5f), 1f)
        assertNotNull(s.rescueSecondsLeft)
        assertFalse(s.gameOver)
    }

    @Test
    fun ohneRettungIstDasSpielVorbei() {
        val s = run(GameState(pegel = 0.5f), 11f)
        assertTrue(s.gameOver)
    }

    @Test
    fun getraenkRettetHubbell() {
        val rescue = run(GameState(pegel = 0.5f), 5f)
        assertTrue(rescue.inRescue)
        val saved = run(GameEngine.serve(rescue), 0.5f)
        assertFalse(saved.inRescue)
        assertTrue(saved.pegel > 0f)
    }

    @Test
    fun levelAufstiegLegtBelohnungAn() {
        var s = GameState(pegel = 50f)
        repeat(6) { s = run(GameEngine.serve(s), 1.3f) }
        assertEquals(2, s.level)
        assertEquals(listOf(2), s.pendingRewards)
    }

    @Test
    fun getraenkKaufenBrauchtLevelUndTaler() {
        val poor = GameState(level = 2, coins = 10)
        assertSame(poor, GameEngine.buyDrink(poor, Drink.WEIZEN))
        val lowLevel = GameState(level = 1, coins = 100)
        assertSame(lowLevel, GameEngine.buyDrink(lowLevel, Drink.WEIZEN))
        val ok = GameEngine.buyDrink(GameState(level = 2, coins = 100), Drink.WEIZEN)
        assertTrue(Drink.WEIZEN in ok.unlocked)
        assertEquals(Drink.WEIZEN, ok.selected)
        assertEquals(75, ok.coins)
    }

    @Test
    fun upgradeStufenWerdenTeurer() {
        var s = GameState(level = 2, coins = 1000)
        s = GameEngine.buyUpgrade(s, Upgrade.ZAPFHAHN)
        s = GameEngine.buyUpgrade(s, Upgrade.ZAPFHAHN)
        assertEquals(2, s.upgradeLevel(Upgrade.ZAPFHAHN))
        assertEquals(1000 - 40 - 88, s.coins)
    }

    @Test
    fun kellnerinServiertAutomatisch() {
        val s = run(GameState(pegel = 50f, upgrades = mapOf(Upgrade.KELLNERIN to 1)), 6.1f)
        assertEquals(1, s.drinksServed)
    }

    @Test
    fun belohnungenJeLevel() {
        assertEquals(RewardKind.FEUERWERK, Rewards.kindFor(5))
        assertEquals(RewardKind.GROSSES_FEST, Rewards.kindFor(10))
        assertEquals(RewardKind.PARTY, Rewards.kindFor(3))
        assertEquals(RewardKind.DISCO, Rewards.kindFor(7))
        assertEquals("Stammgast", Rewards.titleFor(6))
    }
}

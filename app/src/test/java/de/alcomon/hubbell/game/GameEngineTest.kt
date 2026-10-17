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
        repeat(6) { s = run(GameEngine.serve(s), 1.6f) }
        assertEquals(2, s.level)
        assertEquals(listOf(2), s.pendingRewards)
        assertTrue(s.coins >= GameEngine.levelUpCoins(2))
    }

    @Test
    fun jedesGetraenkHatSeineEigeneWartezeit() {
        var s = GameState(pegel = 30f, unlocked = setOf(Drink.HELLES, Drink.STOUT))
        s = GameEngine.serve(s, Drink.STOUT)
        assertFalse(GameEngine.canServe(s, Drink.STOUT))
        assertTrue(GameEngine.canServe(s, Drink.HELLES))
        s = GameEngine.serve(s, Drink.HELLES)
        assertEquals(2, s.drinksServed)
        // Stout war beim zweiten Bestellen noch nicht leer und wurde in einem Zug geleert
        assertTrue(s.pegel >= 30f + Drink.STOUT.boost - 0.1f)
        s = run(s, 2f)
        assertTrue(GameEngine.canServe(s, Drink.HELLES))
        assertFalse(GameEngine.canServe(s, Drink.STOUT))
        s = run(s, 2f)
        assertTrue(GameEngine.canServe(s, Drink.STOUT))
        assertTrue(Drink.DRACHENBLUT.cooldown > Drink.HELLES.cooldown)
    }

    @Test
    fun ohneMitspielenGehtAuchMitVollemPersonalDerPegelAufNull() {
        val maxed = GameState(
            pegel = 100f,
            level = 20,
            unlocked = Drink.entries.toSet(),
            upgrades = Upgrade.entries.associateWith { it.maxLevel },
        )
        assertTrue(GameEngine.catchUp(maxed, 10f * 60f).gameOver)
        // Solange man mitspielt, hilft das Personal dagegen
        val helped = run(maxed, 5f)
        assertTrue(helped.drinksServed > 0)
    }

    @Test
    fun untenStehenNurDieVierBestenGetraenke() {
        val all = GameState(unlocked = Drink.entries.toSet())
        assertEquals(listOf(Drink.DRACHENBLUT, Drink.SPEZIAL, Drink.ABSINTH, Drink.RUM), GameEngine.barDrinks(all))
        assertEquals(listOf(Drink.HELLES), GameEngine.barDrinks(GameState()))
    }

    @Test
    fun level30IstGewonnen() {
        val s = GameState(level = 29, xp = GameEngine.xpForNextLevel(29) - 1f, pegel = 50f, upgrades = mapOf(Upgrade.ZAPFHAHN to 1))
        val won = GameEngine.serve(s)
        assertTrue(won.won)
        assertTrue(won.gameOver)
        assertEquals(GameEngine.WIN_LEVEL, won.level)
        assertTrue(won.pendingRewards.isEmpty())
        val day = GameEngine.newDay(won)
        assertEquals(1, day.level)
        assertFalse(day.won)
        assertEquals(1, day.upgradeLevel(Upgrade.ZAPFHAHN))
    }

    @Test
    fun langeAbwesenheitBeendetDieRunde() {
        val s = GameEngine.catchUp(GameState(pegel = 60f), 30f * 60f)
        assertTrue(s.gameOver)
    }

    @Test
    fun kurzeAbwesenheitLaesstPegelSinken() {
        val s = GameEngine.catchUp(GameState(pegel = 60f), 10f)
        assertFalse(s.gameOver)
        assertEquals(45f, s.pegel, 0.1f)
    }

    @Test
    fun hoeheresLevelZahltMehrTaler() {
        val low = GameEngine.coinsFor(GameState(pegel = 50f, level = 1), Drink.WHISKY)
        val high = GameEngine.coinsFor(GameState(pegel = 50f, level = 11), Drink.WHISKY)
        assertEquals(25, low)
        assertEquals(45, high)
        assertTrue(GameEngine.xpForNextLevel(10) < 1000f)
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
        assertEquals(1000 - 40 - 76, s.coins)
    }

    @Test
    fun kellnerinServiertAutomatisch() {
        val s = run(GameState(pegel = 50f, upgrades = mapOf(Upgrade.KELLNERIN to 1)), 6.1f)
        assertEquals(1, s.drinksServed)
    }

    @Test
    fun streichelnGibtBonusMitAbklingzeit() {
        val s = GameEngine.pet(GameState(pegel = 50f, level = 2))
        assertEquals(2, s.coins)
        assertTrue(s.pegel > 50f)
        assertTrue(s.petGlow > 0f)
        assertSame(s, GameEngine.pet(s))
        val later = run(s, 1f)
        assertTrue(GameEngine.pet(later).coins > later.coins)
    }

    @Test
    fun wirkungenSummierenSichUeberUpgrades() {
        val s = GameState(upgrades = mapOf(Upgrade.ZAPFHAHN to 2, Upgrade.GROSSE_KEHLE to 1))
        assertEquals(0.17f, s.effect(Effect.SPEED), 0.001f)
    }

    @Test
    fun nebeneinnahmenBringenTalerProSekunde() {
        val s = run(GameState(pegel = 100f, upgrades = mapOf(Upgrade.FLIPPER to 5)), 10f)
        assertTrue(s.coins in 14..15)
    }

    @Test
    fun gefangeneBierflascheRettetHubbell() {
        val rescue = run(GameState(pegel = 0.5f), 3f)
        val caught = Falling.catch(rescue, FallingKind.BIERFLASCHE).state
        assertFalse(caught.inRescue)
        assertEquals(15f, caught.pegel, 0.01f)
    }

    @Test
    fun es_gibt_50_upgrades_im_shop() {
        assertEquals(50, Drink.entries.size + Upgrade.entries.size)
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

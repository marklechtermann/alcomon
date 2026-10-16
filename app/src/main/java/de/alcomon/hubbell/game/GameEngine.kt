package de.alcomon.hubbell.game

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/** Reine Spiellogik ohne Android-Abhängigkeiten. Alle Funktionen liefern einen neuen Zustand. */
object GameEngine {
    const val MAX_PEGEL = 100f
    const val START_PEGEL = 60f
    const val RESCUE_SECONDS = 10f
    const val HAPPY_PEGEL = 80f

    fun decayPerSecond(s: GameState): Float {
        val base = 1.5f + 0.55f * (s.level - 1)
        return base * (1f - 0.08f * s.upgradeLevel(Upgrade.EISERNE_LEBER))
    }

    fun drinkDuration(s: GameState, drink: Drink): Float =
        drink.drinkSeconds * (1f - 0.1f * s.upgradeLevel(Upgrade.ZAPFHAHN))

    /** Taler pro Getränk. Ist Hubbell bestens gelaunt, gibt es doppelt. */
    fun coinsFor(s: GameState, drink: Drink): Int {
        var coins = drink.coins * (1f + 0.25f * s.upgradeLevel(Upgrade.SPENDIERHOSEN))
        if (s.pegel >= HAPPY_PEGEL) coins *= 2f
        return max(1, coins.roundToInt())
    }

    fun xpForNextLevel(level: Int): Float = (40.0 * level.toDouble().pow(1.6)).toFloat()

    fun waitressInterval(level: Int): Float = 6f - 1.5f * (level - 1)

    fun canServe(s: GameState): Boolean = !s.gameOver && s.drinking == null

    fun serve(s: GameState, drink: Drink = s.selected): GameState {
        if (!canServe(s) || drink !in s.unlocked) return s
        val served = s.copy(
            drinking = drink,
            drinkRemaining = drinkDuration(s, drink),
            coins = s.coins + coinsFor(s, drink),
            drinksServed = s.drinksServed + 1,
        )
        return gainXp(served, drink.boost)
    }

    private fun gainXp(s: GameState, amount: Float): GameState {
        var level = s.level
        var xp = s.xp + amount
        val rewards = s.pendingRewards.toMutableList()
        while (xp >= xpForNextLevel(level)) {
            xp -= xpForNextLevel(level)
            level++
            rewards += level
        }
        return s.copy(level = level, xp = xp, pendingRewards = rewards)
    }

    fun tick(s: GameState, dt: Float): GameState {
        if (s.gameOver || dt <= 0f) return s

        var pegel = s.pegel - decayPerSecond(s) * dt
        var drinking = s.drinking
        var remaining = s.drinkRemaining
        if (drinking != null) {
            // Der Pegel steigt gleichmäßig, während Hubbell trinkt.
            val step = min(dt, remaining)
            pegel += drinking.boost * step / drinkDuration(s, drinking)
            remaining -= dt
            if (remaining <= 0f) {
                drinking = null
                remaining = 0f
            }
        }
        pegel = pegel.coerceIn(0f, MAX_PEGEL)

        var rescue = s.rescueSecondsLeft
        var over = false
        if (pegel <= 0f) {
            rescue = if (rescue == null) RESCUE_SECONDS else rescue - dt
            if (rescue <= 0f) {
                rescue = 0f
                over = true
            }
        } else {
            rescue = null
        }

        var next = s.copy(
            pegel = pegel,
            drinking = drinking,
            drinkRemaining = remaining,
            rescueSecondsLeft = rescue,
            gameOver = over,
            elapsedSeconds = s.elapsedSeconds + dt,
        )

        val waitress = s.upgradeLevel(Upgrade.KELLNERIN)
        if (waitress > 0 && !over) {
            val interval = waitressInterval(waitress)
            val timer = next.waitressTimer + dt
            next = if (timer >= interval && next.drinking == null) {
                serve(next.copy(waitressTimer = 0f))
            } else {
                next.copy(waitressTimer = min(timer, interval))
            }
        }
        return next
    }

    fun select(s: GameState, drink: Drink): GameState =
        if (drink in s.unlocked) s.copy(selected = drink) else s

    fun canBuy(s: GameState, drink: Drink): Boolean =
        drink !in s.unlocked && s.level >= drink.requiredLevel && s.coins >= drink.unlockCost

    fun buyDrink(s: GameState, drink: Drink): GameState {
        if (!canBuy(s, drink)) return s
        return s.copy(coins = s.coins - drink.unlockCost, unlocked = s.unlocked + drink, selected = drink)
    }

    fun canBuy(s: GameState, upgrade: Upgrade): Boolean {
        val current = s.upgradeLevel(upgrade)
        return current < upgrade.maxLevel && s.level >= upgrade.requiredLevel && s.coins >= upgrade.cost(current)
    }

    fun buyUpgrade(s: GameState, upgrade: Upgrade): GameState {
        if (!canBuy(s, upgrade)) return s
        val current = s.upgradeLevel(upgrade)
        return s.copy(
            coins = s.coins - upgrade.cost(current),
            upgrades = s.upgrades + (upgrade to current + 1),
        )
    }

    fun anythingAffordable(s: GameState): Boolean =
        Drink.entries.any { canBuy(s, it) } || Upgrade.entries.any { canBuy(s, it) }
}

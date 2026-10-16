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

    const val PET_COOLDOWN = 0.8f
    const val PET_GLOW_SECONDS = 1.5f
    const val PET_PEGEL = 1.5f

    fun decayPerSecond(s: GameState): Float {
        val base = 1.5f + 0.55f * (s.level - 1)
        return base * (1f - min(0.75f, s.effect(Effect.LIVER)))
    }

    fun drinkDuration(s: GameState, drink: Drink): Float =
        drink.drinkSeconds * (1f - min(0.6f, s.effect(Effect.SPEED)))

    /** Pegelzuwachs eines Getränks inklusive Upgrades. */
    fun boostFor(s: GameState, drink: Drink): Float = drink.boost * (1f + s.effect(Effect.BOOST))

    fun rescueSeconds(s: GameState): Float = RESCUE_SECONDS + s.effect(Effect.RESCUE)

    /** Taler pro Getränk. Ist Hubbell bestens gelaunt, gibt es mindestens doppelt. */
    fun coinsFor(s: GameState, drink: Drink): Int {
        var coins = drink.coins * (1f + s.effect(Effect.COINS))
        if (s.pegel >= HAPPY_PEGEL) coins *= 2f + s.effect(Effect.MOOD)
        return max(1, coins.roundToInt())
    }

    /** Taler fürs Kopfkraulen. */
    fun petCoins(s: GameState): Int =
        max(1, ((1f + s.level * 0.5f) * (1f + s.effect(Effect.PET))).roundToInt())

    fun canPet(s: GameState): Boolean = !s.gameOver && s.petCooldown <= 0f

    /** Hubbell wird über den Kopf gestreichelt: ein paar Taler, etwas Pegel und gute Laune. */
    fun pet(s: GameState): GameState {
        if (!canPet(s)) return s
        val petted = s.copy(
            coins = s.coins + petCoins(s),
            pegel = (s.pegel + PET_PEGEL * (1f + s.effect(Effect.PET) * 0.5f)).coerceAtMost(MAX_PEGEL),
            rescueSecondsLeft = null,
            petCooldown = PET_COOLDOWN,
            petGlow = PET_GLOW_SECONDS,
            pets = s.pets + 1,
        )
        return gainXp(petted, 2f)
    }

    fun xpForNextLevel(level: Int): Float = (40.0 * level.toDouble().pow(1.6)).toFloat()

    /** Abstand, in dem das Personal automatisch serviert, oder null ohne Personal. */
    fun waiterInterval(s: GameState): Float? {
        val rate = s.effect(Effect.WAITER)
        return if (rate <= 0f) null else 6f / (0.5f + 0.5f * rate)
    }

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

    fun gainXpPublic(s: GameState, amount: Float): GameState = gainXp(s, amount)

    private fun gainXp(s: GameState, amount: Float): GameState {
        var level = s.level
        var xp = s.xp + amount * (1f + s.effect(Effect.XP))
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
            pegel += boostFor(s, drinking) * step / drinkDuration(s, drinking)
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
            rescue = if (rescue == null) rescueSeconds(s) else rescue - dt
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
            petCooldown = max(0f, s.petCooldown - dt),
            petGlow = max(0f, s.petGlow - dt),
        )

        val passive = s.effect(Effect.PASSIVE)
        if (passive > 0f && !over) {
            val earned = s.coinFraction + passive * dt
            val whole = earned.toInt()
            next = next.copy(coins = next.coins + whole, coinFraction = earned - whole)
        }

        val interval = waiterInterval(s)
        if (interval != null && !over) {
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

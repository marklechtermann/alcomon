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
        min(drink.drinkSeconds, drink.cooldown) * (1f - min(0.6f, s.effect(Effect.SPEED)))

    /** Wartezeit, bis das Getränk wieder bestellt werden kann; Tempo-Upgrades verkürzen sie. */
    fun cooldownFor(s: GameState, drink: Drink): Float =
        drink.cooldown * (1f - min(0.6f, s.effect(Effect.SPEED)))

    /** 0..1: wie weit die Wartezeit des Getränks schon abgelaufen ist (1 = bereit). */
    fun readiness(s: GameState, drink: Drink): Float {
        val left = s.cooldownLeft(drink)
        if (left <= 0f) return 1f
        return (1f - left / cooldownFor(s, drink)).coerceIn(0f, 1f)
    }

    /** Pegelzuwachs eines Getränks inklusive Upgrades. */
    fun boostFor(s: GameState, drink: Drink): Float = drink.boost * (1f + s.effect(Effect.BOOST))

    fun rescueSeconds(s: GameState): Float = RESCUE_SECONDS + s.effect(Effect.RESCUE)

    /** Ruhm-Bonus: Mit jedem Level zahlen die Gäste 8 % mehr. */
    fun fameMultiplier(level: Int): Float = 1f + 0.08f * (level - 1)

    /** Taler, die es beim Erreichen von [level] obendrauf gibt. */
    fun levelUpCoins(level: Int): Int = 25 * level

    /** Taler pro Getränk. Ist Hubbell bestens gelaunt, gibt es mindestens doppelt. */
    fun coinsFor(s: GameState, drink: Drink): Int {
        var coins = drink.coins * (1f + s.effect(Effect.COINS)) * fameMultiplier(s.level)
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

    /** Flachere Kurve als früher (Exponent 1,6), damit es ab Level 10 spürbar schneller weitergeht. */
    fun xpForNextLevel(level: Int): Float = (40.0 * level.toDouble().pow(1.35)).toFloat()

    /** Abstand, in dem das Personal automatisch serviert, oder null ohne Personal. */
    fun waiterInterval(s: GameState): Float? {
        val rate = s.effect(Effect.WAITER)
        return if (rate <= 0f) null else 6f / (0.5f + 0.5f * rate)
    }

    fun canServe(s: GameState, drink: Drink): Boolean =
        !s.gameOver && drink in s.unlocked && s.cooldownLeft(drink) <= 0f

    /** Das beste Getränk, das gerade bereit ist, oder null. */
    fun bestReady(s: GameState): Drink? = Drink.entries.lastOrNull { canServe(s, it) }

    /**
     * Ein Getränk bestellen. Jedes Getränk hat seine eigene Wartezeit, mehrere verschiedene
     * dürfen also kurz nacheinander kommen; ein noch laufendes wird dann in einem Zug geleert.
     */
    fun serve(s: GameState, drink: Drink = s.selected): GameState {
        if (!canServe(s, drink)) return s
        var pegel = s.pegel
        val current = s.drinking
        if (current != null && s.drinkRemaining > 0f) {
            pegel += boostFor(s, current) * s.drinkRemaining / drinkDuration(s, current)
        }
        val served = s.copy(
            pegel = pegel.coerceAtMost(MAX_PEGEL),
            rescueSecondsLeft = if (pegel > 0f) null else s.rescueSecondsLeft,
            drinking = drink,
            drinkRemaining = drinkDuration(s, drink),
            coins = s.coins + coinsFor(s, drink),
            drinksServed = s.drinksServed + 1,
            selected = drink,
            cooldowns = s.cooldowns + (drink to cooldownFor(s, drink)),
        )
        return gainXp(served, drink.boost)
    }

    fun gainXpPublic(s: GameState, amount: Float): GameState = gainXp(s, amount)

    private fun gainXp(s: GameState, amount: Float): GameState {
        var level = s.level
        var xp = s.xp + amount * (1f + s.effect(Effect.XP))
        var coins = s.coins
        val rewards = s.pendingRewards.toMutableList()
        while (xp >= xpForNextLevel(level)) {
            xp -= xpForNextLevel(level)
            level++
            coins += levelUpCoins(level)
            rewards += level
        }
        return s.copy(level = level, xp = xp, coins = coins, pendingRewards = rewards)
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
            cooldowns = if (s.cooldowns.isEmpty()) s.cooldowns else s.cooldowns
                .mapValues { (_, left) -> left - dt }
                .filterValues { it > 0f },
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
            val ready = bestReady(next)
            next = if (timer >= interval && next.drinking == null && ready != null) {
                serve(next.copy(waitressTimer = 0f), ready)
            } else {
                next.copy(waitressTimer = min(timer, interval))
            }
        }
        return next
    }

    /** Höchstens so viel Abwesenheit wird nachgerechnet (danach ist Hubbell ohnehin längst nüchtern). */
    const val MAX_CATCH_UP_SECONDS = 24f * 3600f

    /**
     * Die Zeit läuft weiter, während das Spiel im Hintergrund oder geschlossen ist: Die verpassten
     * [seconds] werden in kleinen Schritten nachgerechnet. Fällt der Pegel dabei auf null und läuft
     * die Rettungsfrist ab, ist die Runde vorbei.
     */
    fun catchUp(s: GameState, seconds: Float): GameState {
        var left = seconds.coerceIn(0f, MAX_CATCH_UP_SECONDS)
        var state = s
        while (left > 0f && !state.gameOver) {
            val step = min(CATCH_UP_STEP, left)
            state = tick(state, step)
            left -= step
        }
        return state
    }

    private const val CATCH_UP_STEP = 0.25f

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

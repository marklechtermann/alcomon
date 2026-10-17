package de.alcomon.hubbell.game

data class GameState(
    val pegel: Float = GameEngine.START_PEGEL,
    val level: Int = 1,
    val xp: Float = 0f,
    val coins: Int = 0,
    val drinksServed: Int = 0,
    val elapsedSeconds: Float = 0f,
    val unlocked: Set<Drink> = setOf(Drink.HELLES),
    val selected: Drink = Drink.HELLES,
    val upgrades: Map<Upgrade, Int> = emptyMap(),
    val drinking: Drink? = null,
    val drinkRemaining: Float = 0f,
    /** Restzeit der Rettungsfrist, `null` solange der Pegel über null ist. */
    val rescueSecondsLeft: Float? = null,
    val gameOver: Boolean = false,
    val waitressTimer: Float = 0f,
    /** Erreichte Level, deren Belohnungs-Animation noch abgespielt werden muss. */
    val pendingRewards: List<Int> = emptyList(),
    /** Angesparte Bruchteile der Nebeneinnahmen. */
    val coinFraction: Float = 0f,
    val petCooldown: Float = 0f,
    /** Wie lange Hubbell das Streicheln noch sichtbar genießt. */
    val petGlow: Float = 0f,
    val pets: Int = 0,
    /** Restzeit pro Getränk, bis man es wieder bestellen kann. */
    val cooldowns: Map<Drink, Float> = emptyMap(),
    /** Sekunden seit der letzten Aktion des Spielers; das Personal hilft nur, solange er mitspielt. */
    val idleSeconds: Float = 0f,
    /** Level [GameEngine.WIN_LEVEL] erreicht: Hubbell hat genug und geht nach Hause, das Spiel ist gewonnen. */
    val won: Boolean = false,
) {
    fun cooldownLeft(drink: Drink): Float = cooldowns[drink] ?: 0f

    fun upgradeLevel(upgrade: Upgrade): Int = upgrades[upgrade] ?: 0

    /** Summe einer Wirkung über alle gekauften Upgrade-Stufen. */
    fun effect(effect: Effect): Float {
        var total = 0f
        for ((upgrade, level) in upgrades) if (upgrade.effect == effect) total += upgrade.perStage * level
        return total
    }

    val inRescue: Boolean get() = rescueSecondsLeft != null

    val drinkProgress: Float
        get() {
            val drink = drinking ?: return 0f
            return (1f - drinkRemaining / GameEngine.drinkDuration(this, drink)).coerceIn(0f, 1f)
        }

    val levelProgress: Float get() = (xp / GameEngine.xpForNextLevel(level)).coerceIn(0f, 1f)

    val mood: Mood
        get() = when {
            inRescue || pegel < 20f -> Mood.NUECHTERN
            pegel < 50f -> Mood.MAESSIG
            pegel < 80f -> Mood.FROH
            else -> Mood.SELIG
        }
}

enum class Mood { NUECHTERN, MAESSIG, FROH, SELIG }

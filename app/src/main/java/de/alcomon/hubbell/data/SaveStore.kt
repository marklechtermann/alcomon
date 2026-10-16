package de.alcomon.hubbell.data

import android.content.Context
import de.alcomon.hubbell.game.Drink
import de.alcomon.hubbell.game.GameState
import de.alcomon.hubbell.game.Stats
import de.alcomon.hubbell.game.Upgrade

/** Speichert die laufende Runde und die Rekorde lokal in den SharedPreferences. */
class SaveStore(context: Context) {
    private val prefs = context.getSharedPreferences("hubbell", Context.MODE_PRIVATE)

    fun saveRun(s: GameState) {
        prefs.edit()
            .putBoolean(KEY_RUN, true)
            .putFloat("run.pegel", s.pegel)
            .putInt("run.level", s.level)
            .putFloat("run.xp", s.xp)
            .putInt("run.coins", s.coins)
            .putInt("run.drinks", s.drinksServed)
            .putFloat("run.elapsed", s.elapsedSeconds)
            .putString("run.unlocked", s.unlocked.joinToString(",") { it.name })
            .putString("run.selected", s.selected.name)
            .putString("run.upgrades", s.upgrades.entries.joinToString(",") { "${it.key.name}:${it.value}" })
            .putFloat("run.rescue", s.rescueSecondsLeft ?: -1f)
            .apply()
    }

    fun loadRun(): GameState? {
        if (!prefs.getBoolean(KEY_RUN, false)) return null
        val unlocked = prefs.getString("run.unlocked", "").orEmpty()
            .split(",").mapNotNull { name -> Drink.entries.firstOrNull { it.name == name } }
            .toSet() + Drink.HELLES
        val selected = Drink.entries.firstOrNull { it.name == prefs.getString("run.selected", null) }
            ?.takeIf { it in unlocked } ?: Drink.HELLES
        val upgrades = prefs.getString("run.upgrades", "").orEmpty()
            .split(",").mapNotNull { entry ->
                val parts = entry.split(":")
                val upgrade = Upgrade.entries.firstOrNull { it.name == parts.getOrNull(0) }
                val level = parts.getOrNull(1)?.toIntOrNull()
                if (upgrade != null && level != null) upgrade to level.coerceIn(0, upgrade.maxLevel) else null
            }.toMap()
        val rescue = prefs.getFloat("run.rescue", -1f)
        return GameState(
            pegel = prefs.getFloat("run.pegel", 60f),
            level = prefs.getInt("run.level", 1),
            xp = prefs.getFloat("run.xp", 0f),
            coins = prefs.getInt("run.coins", 0),
            drinksServed = prefs.getInt("run.drinks", 0),
            elapsedSeconds = prefs.getFloat("run.elapsed", 0f),
            unlocked = unlocked,
            selected = selected,
            upgrades = upgrades,
            rescueSecondsLeft = rescue.takeIf { it >= 0f },
        )
    }

    fun clearRun() {
        prefs.edit().putBoolean(KEY_RUN, false).apply()
    }

    fun loadStats(): Stats = Stats(
        bestLevel = prefs.getInt("stats.level", 0),
        bestDrinks = prefs.getInt("stats.drinks", 0),
        bestSeconds = prefs.getFloat("stats.seconds", 0f),
        gamesPlayed = prefs.getInt("stats.games", 0),
    )

    fun saveStats(stats: Stats) {
        prefs.edit()
            .putInt("stats.level", stats.bestLevel)
            .putInt("stats.drinks", stats.bestDrinks)
            .putFloat("stats.seconds", stats.bestSeconds)
            .putInt("stats.games", stats.gamesPlayed)
            .apply()
    }

    var soundOn: Boolean
        get() = prefs.getBoolean("sound.on", true)
        set(value) {
            prefs.edit().putBoolean("sound.on", value).apply()
        }

    private companion object {
        const val KEY_RUN = "run.exists"
    }
}

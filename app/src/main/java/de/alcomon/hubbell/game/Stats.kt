package de.alcomon.hubbell.game

data class Stats(
    val bestLevel: Int = 0,
    val bestDrinks: Int = 0,
    val bestSeconds: Float = 0f,
    val gamesPlayed: Int = 0,
) {
    fun record(run: GameState): Pair<Stats, RunResult> {
        val result = RunResult(
            level = run.level,
            drinks = run.drinksServed,
            seconds = run.elapsedSeconds,
            newBestLevel = run.level > bestLevel,
            newBestDrinks = run.drinksServed > bestDrinks,
            newBestTime = run.elapsedSeconds > bestSeconds,
        )
        val updated = Stats(
            bestLevel = maxOf(bestLevel, run.level),
            bestDrinks = maxOf(bestDrinks, run.drinksServed),
            bestSeconds = maxOf(bestSeconds, run.elapsedSeconds),
            gamesPlayed = gamesPlayed + 1,
        )
        return updated to result
    }
}

data class RunResult(
    val level: Int,
    val drinks: Int,
    val seconds: Float,
    val newBestLevel: Boolean,
    val newBestDrinks: Boolean,
    val newBestTime: Boolean,
)

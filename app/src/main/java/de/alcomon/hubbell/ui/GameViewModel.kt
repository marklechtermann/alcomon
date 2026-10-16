package de.alcomon.hubbell.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import de.alcomon.hubbell.data.SaveStore
import de.alcomon.hubbell.game.Drink
import de.alcomon.hubbell.game.GameEngine
import de.alcomon.hubbell.game.GameState
import de.alcomon.hubbell.game.RunResult
import de.alcomon.hubbell.game.Upgrade

enum class Screen { TITLE, GAME }

/** Kurz aufsteigender Text über Hubbell, z. B. „+3 Taler“. */
data class Floater(val id: Long, val text: String, val age: Float = 0f)

class GameViewModel(app: Application) : AndroidViewModel(app) {
    private val store = SaveStore(app)

    var state by mutableStateOf(store.loadRun() ?: GameState())
        private set
    var stats by mutableStateOf(store.loadStats())
        private set
    var screen by mutableStateOf(Screen.TITLE)
        private set
    var shopOpen by mutableStateOf(false)
        private set
    var floaters by mutableStateOf(emptyList<Floater>())
        private set
    var lastResult by mutableStateOf<RunResult?>(null)
        private set

    private var nextFloaterId = 0L
    private var sinceSave = 0f

    val canContinue: Boolean
        get() = !state.gameOver && (state.drinksServed > 0 || state.elapsedSeconds > 1f)

    fun tick(dt: Float) {
        if (floaters.isNotEmpty()) {
            floaters = floaters.map { it.copy(age = it.age + dt) }.filter { it.age < FLOATER_LIFETIME }
        }
        if (screen != Screen.GAME || shopOpen || state.gameOver) return

        val before = state
        val after = GameEngine.tick(before, dt)
        if (after.drinksServed > before.drinksServed) {
            addFloater("Gerda: +${after.coins - before.coins} Taler")
        }
        state = after

        if (after.gameOver) {
            onGameOver(after)
        } else {
            sinceSave += dt
            if (sinceSave > 5f) save()
        }
    }

    fun serve() {
        if (screen != Screen.GAME || shopOpen) return
        val before = state
        val after = GameEngine.serve(before)
        if (after === before) return
        state = after
        addFloater("+${after.coins - before.coins} Taler")
    }

    fun select(drink: Drink) {
        state = GameEngine.select(state, drink)
    }

    fun buyDrink(drink: Drink) {
        state = GameEngine.buyDrink(state, drink)
        save()
    }

    fun buyUpgrade(upgrade: Upgrade) {
        state = GameEngine.buyUpgrade(state, upgrade)
        save()
    }

    fun openShop() {
        shopOpen = true
    }

    fun closeShop() {
        shopOpen = false
    }

    fun consumeReward() {
        if (state.pendingRewards.isNotEmpty()) {
            state = state.copy(pendingRewards = state.pendingRewards.drop(1))
        }
    }

    fun startNewGame() {
        state = GameState()
        floaters = emptyList()
        lastResult = null
        shopOpen = false
        screen = Screen.GAME
        save()
    }

    fun continueGame() {
        screen = Screen.GAME
    }

    fun toTitle() {
        save()
        shopOpen = false
        screen = Screen.TITLE
    }

    fun onBack() {
        if (shopOpen) closeShop() else toTitle()
    }

    fun save() {
        sinceSave = 0f
        if (!state.gameOver) store.saveRun(state)
    }

    private fun onGameOver(final: GameState) {
        val (updated, result) = stats.record(final)
        stats = updated
        lastResult = result
        store.saveStats(updated)
        store.clearRun()
    }

    private fun addFloater(text: String) {
        floaters = floaters + Floater(nextFloaterId++, text)
    }

    companion object {
        const val FLOATER_LIFETIME = 1.2f
    }
}

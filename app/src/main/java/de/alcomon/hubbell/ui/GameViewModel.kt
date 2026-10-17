package de.alcomon.hubbell.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import de.alcomon.hubbell.audio.Sfx
import de.alcomon.hubbell.audio.SoundPlayer
import de.alcomon.hubbell.data.SaveStore
import de.alcomon.hubbell.game.Drink
import de.alcomon.hubbell.game.DrinkGesture
import de.alcomon.hubbell.game.Falling
import de.alcomon.hubbell.game.FallingItem
import de.alcomon.hubbell.game.FallingKind
import de.alcomon.hubbell.game.GameEngine
import de.alcomon.hubbell.game.GameState
import de.alcomon.hubbell.game.RunResult
import de.alcomon.hubbell.game.SpecialEvent
import de.alcomon.hubbell.game.Upgrade
import kotlin.math.ceil
import kotlin.random.Random

enum class Screen { TITLE, GAME }

/** Kurz aufsteigender Text über Hubbell, z. B. „+3 Taler“. */
data class Floater(val id: Long, val text: String, val age: Float = 0f)

/** Das laufende Special Event „Handy hoch und ex!“: Restzeit und wie weit die Bewegung schon ist. */
data class SpecialChallenge(
    val secondsLeft: Float,
    val progress: Float = 0f,
    val useSensor: Boolean = true,
    /** Füllstand und Lage des großen Biers auf dem Display, folgt live der Handybewegung */
    val fill: Float = 1f,
    val downX: Float = 0f,
    val downY: Float = 1f,
)

class GameViewModel(app: Application) : AndroidViewModel(app) {
    private val store = SaveStore(app)

    var state by mutableStateOf(GameState())
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

    private val sound = SoundPlayer().also { it.enabled = store.soundOn }
    var soundOn by mutableStateOf(store.soundOn)
        private set
    private var riffTimer = nextRiffDelay()

    var falling by mutableStateOf(emptyList<FallingItem>())
        private set
    private var fallingSpawn = Falling.nextSpawnDelay()
    private var nextFallingId = 0L

    var special by mutableStateOf<SpecialChallenge?>(null)
        private set
    /** Sekunden, die der „EX!“-Jubel nach einem geschafften Special Event noch zu sehen ist. */
    var specialCheer by mutableStateOf(0f)
        private set
    private var specialTimer = SpecialEvent.firstDelay()
    private val gesture = DrinkGesture()
    private val motion = MotionSensor(app) { x, y, z, dt -> onMotion(x, y, z, dt) }

    private var nextFloaterId = 0L
    private var sinceSave = 0f

    /** Wann die App in den Hintergrund ging (Systemzeit in ms), solange sie dort ist. */
    private var pausedAt: Long? = null

    init {
        val saved = store.loadRun()
        if (saved != null) {
            state = saved
            val savedAt = store.runSavedAt
            if (savedAt > 0L) catchUpSince(savedAt)
        }
    }

    val canContinue: Boolean
        get() = !state.gameOver && (state.drinksServed > 0 || state.elapsedSeconds > 1f)

    fun tick(dt: Float) {
        if (floaters.isNotEmpty()) {
            floaters = floaters.map { it.copy(age = it.age + dt) }.filter { it.age < FLOATER_LIFETIME }
        }
        if (specialCheer > 0f) specialCheer = (specialCheer - dt).coerceAtLeast(0f)
        if (screen != Screen.GAME || shopOpen || state.gameOver) {
            motion.stop()
            return
        }
        // Während des Special Events steht Hubbells Pegel still, damit niemand beim Trinken verliert
        if (special != null) {
            updateSpecial(dt)
            return
        }

        val before = state
        val after = GameEngine.tick(before, dt)
        if (after.drinksServed > before.drinksServed) {
            addFloater("Gerda: +${after.coins - before.coins} Taler")
        }
        state = after
        playEventSounds(before, after, dt)
        updateFalling(dt)
        updateSpecial(dt)

        if (state.gameOver) {
            onGameOver(state)
        } else {
            sinceSave += dt
            if (sinceSave > 5f) save()
        }
    }

    fun serve(drink: Drink) {
        if (screen != Screen.GAME || shopOpen) return
        val before = state
        val after = GameEngine.serve(before, drink)
        if (after === before) return
        state = after
        addFloater("+${after.coins - before.coins} Taler")
        sound.play(if (after.level > before.level) Sfx.LEVEL_UP else Sfx.PROST)
    }

    /** Ein Tipp in die Szene: erst schauen, ob etwas Herabfallendes getroffen wurde, sonst servieren. */
    fun tapScene(x: Float, y: Float, width: Float, height: Float) {
        if (screen != Screen.GAME || shopOpen || state.gameOver) return
        val radius = minOf(width, height) * 0.11f
        val hit = falling.firstOrNull { item ->
            val dx = item.x * width - x
            val dy = item.y * height - y
            dx * dx + dy * dy < radius * radius * (if (item.kind == FallingKind.DEINE_MUDDA) 1.8f else 1f)
        }
        // Getränke gibt es nur noch über die Knöpfe unten
        if (hit == null) return
        falling = falling - hit
        val before = state
        val caught = Falling.catch(before, hit.kind)
        state = caught.state
        addFloater(caught.text)
        sound.play(if (state.level > before.level) Sfx.LEVEL_UP else Sfx.FANG)
    }

    /** Ein Wisch über Hubbells Kopf. */
    fun pet() {
        if (screen != Screen.GAME || shopOpen) return
        val before = state
        val after = GameEngine.pet(before)
        if (after === before) return
        state = after
        val coins = after.coins - before.coins
        addFloater(if (after.pets % 5 == 1) "${PetLines.random()} +$coins" else "+$coins Taler ♥")
        sound.play(if (after.level > before.level) Sfx.LEVEL_UP else Sfx.SCHNURR)
    }

    private fun updateFalling(dt: Float) {
        falling = Falling.fall(falling, dt)
        fallingSpawn -= dt
        if (fallingSpawn <= 0f) {
            fallingSpawn = Falling.nextSpawnDelay()
            falling = falling + Falling.spawn(nextFallingId++)
        }
    }

    private fun updateSpecial(dt: Float) {
        val current = special
        if (current == null) {
            // Nicht mitten in eine Belohnungsanimation hinein ankündigen
            if (state.pendingRewards.isNotEmpty()) return
            specialTimer -= dt
            if (specialTimer <= 0f) {
                specialTimer = SpecialEvent.nextDelay()
                gesture.reset()
                special = SpecialChallenge(SpecialEvent.WINDOW_SECONDS, useSensor = motion.available)
                sound.play(Sfx.RIFF_3)
            }
            return
        }
        motion.start()
        val left = current.secondsLeft - dt
        if (left <= 0f) {
            endSpecial()
            addFloater("Verpasst! Nächstes Mal ex!")
        } else {
            special = current.copy(
                secondsLeft = left,
                progress = gesture.progress,
                fill = gesture.fill,
                downX = gesture.downX,
                downY = gesture.downY,
            )
        }
    }

    private fun onMotion(x: Float, y: Float, z: Float, dt: Float) {
        if (special == null || shopOpen) return
        if (gesture.update(x, y, z, dt)) completeSpecial()
    }

    /** Ohne Bewegungssensor reicht ein Tipp auf die Ansage. */
    fun tapSpecial() {
        if (special?.useSensor == false) completeSpecial()
    }

    private fun completeSpecial() {
        if (special == null || state.gameOver) return
        endSpecial()
        val before = state
        state = SpecialEvent.reward(before)
        specialCheer = 2.6f
        addFloater("EX! +${state.coins - before.coins} Taler, Pegel voll!")
        sound.play(Sfx.LEVEL_UP)
    }

    private fun endSpecial() {
        special = null
        motion.stop()
        gesture.reset()
    }

    fun toggleSound() {
        soundOn = !soundOn
        store.soundOn = soundOn
        sound.enabled = soundOn
    }

    private fun playEventSounds(before: GameState, after: GameState, dt: Float) {
        when {
            after.gameOver -> sound.play(Sfx.GAME_OVER)
            after.level > before.level -> sound.play(Sfx.LEVEL_UP)
            after.drinksServed > before.drinksServed -> sound.play(Sfx.PROST)
        }
        val rescueBefore = before.rescueSecondsLeft
        val rescueAfter = after.rescueSecondsLeft
        if (rescueAfter != null && !after.gameOver &&
            (rescueBefore == null || ceil(rescueAfter) < ceil(rescueBefore))
        ) {
            sound.play(Sfx.ALARM)
        }
        // Ab und zu ein Gitarrenriff, solange Hubbell gut drauf ist
        riffTimer -= dt
        if (riffTimer <= 0f) {
            riffTimer = nextRiffDelay()
            if (!after.inRescue && after.drinking == null) {
                sound.play(listOf(Sfx.RIFF_1, Sfx.RIFF_2, Sfx.RIFF_3).random())
            }
        }
    }

    private fun nextRiffDelay(): Float = 20f + Random.nextFloat() * 20f

    fun select(drink: Drink) {
        state = GameEngine.select(state, drink)
    }

    fun buyDrink(drink: Drink) {
        val before = state
        state = GameEngine.buyDrink(state, drink)
        if (state !== before) sound.play(Sfx.KASSE)
        save()
    }

    fun buyUpgrade(upgrade: Upgrade) {
        val before = state
        state = GameEngine.buyUpgrade(state, upgrade)
        if (state !== before) sound.play(Sfx.KASSE)
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
        falling = emptyList()
        fallingSpawn = Falling.nextSpawnDelay()
        endSpecial()
        specialCheer = 0f
        specialTimer = SpecialEvent.firstDelay()
        lastResult = null
        shopOpen = false
        screen = Screen.GAME
        riffTimer = nextRiffDelay()
        sound.play(Sfx.RIFF_1)
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

    fun onPause() {
        motion.stop()
        save()
        pausedAt = System.currentTimeMillis()
    }

    fun onResume() {
        val since = pausedAt ?: return
        pausedAt = null
        catchUpSince(since)
    }

    /** Die Zeit lief weiter, während man weg war: Pegel nachrechnen, notfalls ist die Runde vorbei. */
    private fun catchUpSince(since: Long) {
        if (state.gameOver) return
        val away = (System.currentTimeMillis() - since) / 1000f
        if (away < 1f) return
        val before = state
        val after = GameEngine.catchUp(before, away)
        // Belohnungen fürs Leveln im Hintergrund nicht alle nachträglich abspielen
        state = after.copy(pendingRewards = after.pendingRewards.takeLast(1))
        if (after.gameOver) {
            endSpecial()
            onGameOver(state)
            // Damit man das Ergebnis sieht, auch wenn man vom Titelbild kommt
            screen = Screen.GAME
        } else if (away >= 5f) {
            val minutes = (away / 60f).toInt()
            addFloater(if (minutes >= 1) "$minutes Min weg: Pegel ${after.pegel.toInt()} %" else "Pegel ${after.pegel.toInt()} %")
            save()
        }
    }

    fun save() {
        sinceSave = 0f
        if (!state.gameOver) store.saveRun(state)
    }

    private fun onGameOver(final: GameState) {
        endSpecial()
        val (updated, result) = stats.record(final)
        stats = updated
        lastResult = result
        store.saveStats(updated)
        store.clearRun()
    }

    override fun onCleared() {
        motion.stop()
        sound.release()
    }

    private fun addFloater(text: String) {
        floaters = floaters + Floater(nextFloaterId++, text)
    }

    companion object {
        const val FLOATER_LIFETIME = 1.4f
        private val PetLines = listOf("Hach, schön!", "Mehr davon!", "Das tut gut!", "Schnurr …", "Genau da!")
    }
}

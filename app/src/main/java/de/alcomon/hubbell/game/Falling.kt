package de.alcomon.hubbell.game

import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.random.Random

/** Dinge, die ab und zu vom Himmel fallen und angetippt werden können. */
enum class FallingKind { BIERFLASCHE, TALERSACK, DEINE_MUDDA, PLEKTRUM }

data class FallingItem(
    val id: Long,
    val kind: FallingKind,
    /** Waagerechte Position, 0 = links, 1 = rechts. */
    val x: Float,
    /** Senkrechte Position, 0 = oben, 1 = unten. */
    val y: Float,
    val speed: Float,
    val spin: Float,
)

/** Ergebnis eines gefangenen Gegenstands: neuer Zustand und ein kurzer Text zum Anzeigen. */
data class Catch(val state: GameState, val text: String)

object Falling {
    val MuddaJokes = listOf(
        "Deine Mudda trinkt Alster!",
        "Deine Mudda hört Schlager!",
        "Deine Mudda headbangt im Sitzen!",
        "Deine Mudda bestellt Wasser!",
        "Deine Mudda nennt Bier Gerstensaft!",
        "Deine Mudda klatscht auf eins und drei!",
    )

    fun nextSpawnDelay(random: Random = Random): Float = 10f + random.nextFloat() * 14f

    fun spawn(id: Long, random: Random = Random): FallingItem {
        val roll = random.nextFloat()
        val kind = when {
            roll < 0.35f -> FallingKind.BIERFLASCHE
            roll < 0.6f -> FallingKind.TALERSACK
            roll < 0.85f -> FallingKind.DEINE_MUDDA
            else -> FallingKind.PLEKTRUM
        }
        return FallingItem(
            id = id,
            kind = kind,
            x = 0.12f + random.nextFloat() * 0.76f,
            y = -0.1f,
            speed = 0.16f + random.nextFloat() * 0.08f,
            spin = (random.nextFloat() - 0.5f) * 120f,
        )
    }

    /** Bewegt die Gegenstände nach unten und entfernt alles, was unten rausgefallen ist. */
    fun fall(items: List<FallingItem>, dt: Float): List<FallingItem> =
        items.map { it.copy(y = it.y + it.speed * dt) }.filter { it.y < 1.15f }

    fun catch(s: GameState, kind: FallingKind, random: Random = Random): Catch = when (kind) {
        FallingKind.BIERFLASCHE -> Catch(
            s.copy(pegel = (s.pegel + 15f).coerceAtMost(GameEngine.MAX_PEGEL), rescueSecondsLeft = null),
            "Freibier! +15 Pegel",
        )
        FallingKind.TALERSACK -> {
            val coins = max(10, (8f * s.level * s.level.coerceAtMost(10) / 2f).roundToInt())
            Catch(s.copy(coins = s.coins + coins), "+$coins Taler!")
        }
        FallingKind.DEINE_MUDDA -> {
            val coins = 5 * s.level
            Catch(GameEngine.gainXpPublic(s.copy(coins = s.coins + coins), 25f), MuddaJokes.random(random) + " +$coins")
        }
        FallingKind.PLEKTRUM -> Catch(
            GameEngine.gainXpPublic(s, GameEngine.xpForNextLevel(s.level) * 0.3f),
            "Plektrum! Riesen-Erfahrung",
        )
    }
}

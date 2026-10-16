package de.alcomon.hubbell.game

enum class RewardKind(val headline: String, val seconds: Float) {
    KONFETTI("Konfetti für Hubbell!", 3.5f),
    FEUERWERK("Pyro-Show über der Bühne!", 5f),
    PARTY("Die ganze Bar feiert dich im Moshpit!", 5f),
    DISCO("Lightshow wie auf Wacken!", 5f),
    GROSSES_FEST("Das große Hubbell-Fest!", 6.5f),
}

data class Reward(val level: Int, val kind: RewardKind, val title: String)

object Rewards {
    private val titles = listOf(
        1 to "Durstiger Neuling",
        3 to "Gelegenheitszecher",
        5 to "Stammgast",
        8 to "Moshpit-Philosoph",
        10 to "Kneipenkönig",
        15 to "Legende der Metal-Bar",
        20 to "Bierbaron des Metal",
        30 to "Unsterblicher Metalgott",
    )

    fun titleFor(level: Int): String = titles.last { level >= it.first }.second

    fun kindFor(level: Int): RewardKind = when {
        level % 10 == 0 -> RewardKind.GROSSES_FEST
        level % 5 == 0 -> RewardKind.FEUERWERK
        level % 10 == 3 -> RewardKind.PARTY
        level % 10 == 7 -> RewardKind.DISCO
        else -> RewardKind.KONFETTI
    }

    fun forLevel(level: Int): Reward = Reward(level, kindFor(level), titleFor(level))
}

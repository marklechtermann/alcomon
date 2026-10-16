package de.alcomon.hubbell.game

import kotlin.math.pow
import kotlin.math.roundToInt

/** Getränke, die Hubbell serviert bekommen kann. [boost] ist der Pegelzuwachs pro Getränk. */
enum class Drink(
    val displayName: String,
    val description: String,
    val boost: Float,
    val coins: Int,
    val drinkSeconds: Float,
    val unlockCost: Int,
    val requiredLevel: Int,
) {
    HELLES("Helles", "Das leichte Feierabendbier.", 8f, 1, 1.2f, 0, 1),
    WEIZEN("Weizen", "Hefetrüb, süffig, bayerisch.", 13f, 3, 1.4f, 25, 2),
    STOUT("Stout", "Schwarz wie die Nacht, cremig wie ein Traum.", 20f, 6, 1.6f, 120, 4),
    WEIN("Rotwein", "Ein edler Tropfen für feine Gedanken.", 30f, 12, 1.8f, 400, 6),
    WHISKY("Whisky", "Ein schöner Dram aus Schottland.", 45f, 25, 2.0f, 1200, 9),
    SPEZIAL("Hubbell-Spezial", "Geheimrezept des Hauses. Nicht fragen.", 70f, 60, 2.4f, 4000, 13),
}

/** Dauerhafte Verbesserungen aus dem Upgrade-Shop (gelten für die laufende Runde). */
enum class Upgrade(
    val displayName: String,
    val icon: String,
    val description: String,
    val baseCost: Int,
    val maxLevel: Int,
    val requiredLevel: Int,
) {
    ZAPFHAHN("Turbo-Zapfhahn", "⚡", "Hubbell trinkt pro Stufe 10 % schneller.", 40, 5, 2),
    SPENDIERHOSEN("Spendierhosen", "💰", "Pro Stufe 25 % mehr Taler je Getränk.", 60, 5, 3),
    EISERNE_LEBER("Eiserne Leber", "💪", "Der Pegel sinkt pro Stufe 8 % langsamer.", 150, 5, 5),
    KELLNERIN("Kellnerin Gerda", "💁", "Bringt Hubbell automatisch sein Getränk. Jede Stufe schneller.", 500, 3, 7);

    fun cost(currentLevel: Int): Int = (baseCost * 2.2.pow(currentLevel)).roundToInt()
}

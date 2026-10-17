package de.alcomon.hubbell.game

import kotlin.math.pow
import kotlin.math.roundToInt

/** Glasform, in der ein Getränk gezeichnet wird. */
enum class Glass { BEER, PINT, WHEAT, MUG, HORN, WINE, TUMBLER, SHOT, COCKTAIL }

/**
 * Getränke, die Hubbell serviert bekommen kann. [boost] ist der Pegelzuwachs pro Getränk,
 * [liquid] und [foam] sind ARGB-Farben für die Grafik.
 */
enum class Drink(
    val displayName: String,
    val description: String,
    val boost: Float,
    val coins: Int,
    val drinkSeconds: Float,
    val unlockCost: Int,
    val requiredLevel: Int,
    val glass: Glass,
    val liquid: Long,
    val foam: Long? = null,
) {
    HELLES("Helles", "Das leichte Feierabendbier.", 8f, 1, 1.2f, 0, 1, Glass.BEER, 0xFFF2B32A, 0xFFFFFBF0),
    PILS("Pils", "Herb, klar und schnell gezapft.", 10f, 2, 1.2f, 10, 1, Glass.BEER, 0xFFF7D35C, 0xFFFFFBF0),
    WEIZEN("Weizen", "Hefetrüb, süffig, bayerisch.", 13f, 3, 1.4f, 25, 2, Glass.WHEAT, 0xFFE8962C, 0xFFFFFBF0),
    STOUT("Stout", "Schwarz wie die Nacht, cremig wie ein Traum.", 20f, 6, 1.6f, 120, 4, Glass.PINT, 0xFF2B1A12, 0xFFEAD9B8),
    BOCK("Doppelbock", "Stark, malzig, und ein bisschen gefährlich.", 26f, 9, 1.7f, 250, 5, Glass.MUG, 0xFF8B4513, 0xFFF3E2C0),
    WEIN("Rotwein", "Ein edler Tropfen für feine Gedanken.", 30f, 12, 1.8f, 400, 6, Glass.WINE, 0xFF8E1B2E),
    MET("Met", "Honigwein aus dem Wikinger-Trinkhorn.", 36f, 16, 1.9f, 700, 7, Glass.HORN, 0xFFE0A526),
    WHISKY("Whisky", "Ein schöner Dram aus Schottland.", 45f, 25, 2.0f, 1200, 9, Glass.TUMBLER, 0xFFC77B1E),
    RUM("Piratenrum", "Yo-ho-ho und eine Buddel voll Rum.", 52f, 32, 2.0f, 2000, 11, Glass.TUMBLER, 0xFF7A3E12),
    ABSINTH("Grüne Fee", "Absinth. Hubbell sieht kurz Einhörner.", 60f, 42, 2.1f, 3200, 12, Glass.SHOT, 0xFF7CD64B),
    SPEZIAL("Hubbell-Spezial", "Geheimrezept des Hauses. Nicht fragen.", 70f, 60, 2.4f, 4000, 13, Glass.COCKTAIL, 0xFFFF5FA2, 0xFFFFB347),
    DRACHENBLUT("Drachenblut", "Brennt wie Feuer, schmeckt wie ein Gitarrensolo.", 95f, 100, 2.6f, 12000, 17, Glass.COCKTAIL, 0xFFB71C1C, 0xFF1A0505);

    /**
     * Sekunden, bis dieses Getränk wieder bestellt werden kann. Billiges geht schnell,
     * Edles bringt mehr, braucht aber länger.
     */
    val cooldown: Float
        get() = when (this) {
            HELLES -> 1.5f
            PILS -> 2f
            WEIZEN -> 2.5f
            STOUT -> 3.5f
            BOCK -> 4.5f
            WEIN -> 5f
            MET -> 6f
            WHISKY -> 7.5f
            RUM -> 8.5f
            ABSINTH -> 9.5f
            SPEZIAL -> 11f
            DRACHENBLUT -> 14f
        }
}

/** Wirkung eines Upgrades. Werte gleicher Wirkung addieren sich über alle Upgrades. */
enum class Effect(val category: String) {
    SPEED("Trinktempo"),
    COINS("Mehr Taler"),
    LIVER("Kondition"),
    XP("Erfahrung"),
    BOOST("Stärkere Getränke"),
    WAITER("Personal"),
    PET("Streicheln"),
    RESCUE("Rettung"),
    MOOD("Stimmung"),
    PASSIVE("Nebeneinnahmen"),
}

/**
 * Verbesserungen aus dem Upgrade-Shop (gelten für die laufende Runde).
 * [perStage] ist die Stärke der Wirkung pro gekaufter Stufe.
 */
enum class Upgrade(
    val displayName: String,
    val icon: String,
    val description: String,
    val effect: Effect,
    val perStage: Float,
    val baseCost: Int,
    val maxLevel: Int,
    val requiredLevel: Int,
) {
    // Trinktempo: Anteil, um den Hubbell schneller trinkt
    ZAPFHAHN("Turbo-Zapfhahn", "⚡", "Hubbell trinkt pro Stufe 6 % schneller.", Effect.SPEED, 0.06f, 40, 5, 2),
    GROSSE_KEHLE("Große Kehle", "😮", "Pro Stufe 5 % schneller runter damit.", Effect.SPEED, 0.05f, 150, 5, 4),
    STROHHALM("Metal-Strohhalm", "🥤", "Mit Totenkopf. 4 % schneller pro Stufe.", Effect.SPEED, 0.04f, 400, 3, 6),
    TRICHTER("Bier-Trichter", "🔻", "Für Profis. 8 % schneller pro Stufe.", Effect.SPEED, 0.08f, 1500, 3, 10),
    EXPRESS_SPUELE("Express-Spüle", "🧽", "Saubere Gläser in Rekordzeit. 3 % pro Stufe.", Effect.SPEED, 0.03f, 3000, 5, 14),

    // Taler: Aufschlag auf jedes Getränk
    SPENDIERHOSEN("Spendierhosen", "💰", "Pro Stufe 25 % mehr Taler je Getränk.", Effect.COINS, 0.25f, 60, 5, 3),
    TRINKGELDGLAS("Trinkgeldglas", "🫙", "Pro Stufe 15 % mehr Taler.", Effect.COINS, 0.15f, 200, 5, 5),
    MERCH("Merch-Stand", "👕", "Hubbell-Shirts verkaufen sich gut. +20 % pro Stufe.", Effect.COINS, 0.2f, 700, 5, 8),
    VIP("VIP-Bändchen", "🎫", "Nur die Harten kommen rein. +30 % pro Stufe.", Effect.COINS, 0.3f, 2500, 3, 12),
    GOLDENE_ZAPFANLAGE("Goldene Zapfanlage", "🏆", "Protzig, aber lohnend. +50 % pro Stufe.", Effect.COINS, 0.5f, 9000, 3, 18),

    // Kondition: der Pegel sinkt langsamer
    BREZELKORB("Brezelkorb", "🥨", "Eine gute Grundlage. Pegel sinkt 4 % langsamer.", Effect.LIVER, 0.04f, 100, 3, 3),
    EISERNE_LEBER("Eiserne Leber", "💪", "Der Pegel sinkt pro Stufe 6 % langsamer.", Effect.LIVER, 0.06f, 150, 5, 5),
    CURRYWURST("Currywurst mit Pommes", "🌭", "Mit Pommesgabel gegessen. 5 % pro Stufe.", Effect.LIVER, 0.05f, 450, 3, 7),
    ROLLMOPS("Rollmops-Notration", "🐟", "Hilft gegen alles. 5 % pro Stufe.", Effect.LIVER, 0.05f, 1200, 3, 11),
    STAHLMAGEN("Stahlmagen", "🛡️", "Aus reinem Schwermetall. 6 % pro Stufe.", Effect.LIVER, 0.06f, 5000, 3, 16),

    // Erfahrung: schnellere Level-Aufstiege
    STAMMTISCH("Stammtisch-Geschichten", "📖", "Pro Stufe 15 % mehr Erfahrung.", Effect.XP, 0.15f, 120, 5, 3),
    LUFTGITARRE("Luftgitarren-Kurs", "🎸", "Pro Stufe 20 % mehr Erfahrung.", Effect.XP, 0.2f, 600, 3, 7),
    FESTIVALPASS("Festival-Pass", "🎟️", "Drei Tage Schlamm und Riffs. +25 % pro Stufe.", Effect.XP, 0.25f, 3500, 3, 13),

    // Stärkere Getränke: mehr Pegel pro Glas
    MASSKRUG("Maßkrug statt Glas", "🍻", "Pro Stufe 8 % mehr Pegel je Getränk.", Effect.BOOST, 0.08f, 250, 5, 4),
    DOPPELTER_SCHUSS("Doppelter Schuss", "🥃", "Pro Stufe 10 % mehr Pegel.", Effect.BOOST, 0.1f, 1800, 3, 10),
    FASSANSTICH("Fassanstich", "🛢️", "O'zapft is! 12 % mehr Pegel pro Stufe.", Effect.BOOST, 0.12f, 6000, 3, 17),

    // Personal: bringt automatisch Getränke
    KELLNERIN("Kellnerin Gerda", "💁", "Bringt Hubbell Getränke, solange du mitspielst.", Effect.WAITER, 1f, 500, 3, 7),
    ROADIE("Roadie Rudi", "🧔", "Schleppt Biere ran, solange du mitspielst.", Effect.WAITER, 0.8f, 2000, 3, 11),
    ZAPFROBOTER("Zapf-Roboter 3000", "🤖", "Viel mehr Nachschub, solange du mitspielst.", Effect.WAITER, 1.2f, 8000, 3, 16),

    // Streicheln: mehr Bonus beim Kopfkraulen
    KRAULHANDSCHUH("Kraul-Handschuh", "🧤", "Streicheln bringt pro Stufe 30 % mehr Bonus.", Effect.PET, 0.3f, 80, 5, 2),
    MAEHNENKUR("Mähnen-Kur", "💆", "Seidige Mähne, glücklicher Hubbell. +40 % pro Stufe.", Effect.PET, 0.4f, 500, 3, 6),
    BARTOEL("Bartöl", "🧴", "Duftet nach Wald und Whisky. +50 % pro Stufe.", Effect.PET, 0.5f, 2200, 3, 12),

    // Rettung: mehr Sekunden, wenn der Pegel auf null fällt
    RIECHSALZ("Riechsalz", "🧂", "Pro Stufe 1 Sekunde mehr Rettungsfrist.", Effect.RESCUE, 1f, 300, 3, 5),
    NOTFALLBIER("Notfall-Bier unterm Tisch", "🆘", "Pro Stufe 1,5 Sekunden mehr Rettungsfrist.", Effect.RESCUE, 1.5f, 2500, 2, 12),

    // Stimmung: mehr Taler, wenn Hubbell bestens gelaunt ist
    JUKEBOX("Metal-Jukebox", "📻", "Bei bester Laune pro Stufe +25 % extra.", Effect.MOOD, 0.25f, 350, 4, 5),
    NEBELMASCHINE("Nebelmaschine", "🌫️", "Bei bester Laune pro Stufe +30 % extra.", Effect.MOOD, 0.3f, 1400, 3, 9),
    PYROANLAGE("Pyro-Anlage", "🔥", "Bei bester Laune pro Stufe +40 % extra.", Effect.MOOD, 0.4f, 4500, 3, 15),

    // Nebeneinnahmen: Taler pro Sekunde
    FLIPPER("Flipperautomat", "🕹️", "Bringt pro Stufe 0,3 Taler pro Sekunde.", Effect.PASSIVE, 0.3f, 200, 5, 4),
    DARTSCHEIBE("Dartscheibe", "🎯", "Bringt pro Stufe 0,5 Taler pro Sekunde.", Effect.PASSIVE, 0.5f, 800, 5, 8),
    KICKER("Kickertisch", "⚽", "Bringt pro Stufe 0,8 Taler pro Sekunde.", Effect.PASSIVE, 0.8f, 2500, 5, 12),
    HAUSBAND("Hausband", "🥁", "Live-Musik! 1,5 Taler pro Sekunde und Stufe.", Effect.PASSIVE, 1.5f, 7000, 5, 15),
    TOURBUS("Tourbus", "🚌", "Hubbell auf Tour. 3 Taler pro Sekunde und Stufe.", Effect.PASSIVE, 3f, 20000, 5, 20),
    PLATTENVERTRAG("Plattenvertrag", "💿", "Das Album verkauft sich. 6 Taler pro Sekunde und Stufe.", Effect.PASSIVE, 6f, 60000, 5, 25);

    fun cost(currentLevel: Int): Int = (baseCost * 1.9.pow(currentLevel)).roundToInt()
}

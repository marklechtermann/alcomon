package de.alcomon.hubbell.game

import kotlin.random.Random

/** Was Hubbell so durch den Kopf geht – je nach Laune. */
object Thoughts {
    private val byMood = mapOf(
        Mood.NUECHTERN to listOf(
            "Oh nein … ich seh plötzlich alles so klar.",
            "Mir wird so nüchtern … Hilfe!",
            "Ist das etwa Wasser in meinen Adern?",
            "Bitte ein Bierchen! Nur ein ganz kleines!",
            "Ich muss gleich an meine Steuererklärung denken …",
        ),
        Mood.MAESSIG to listOf(
            "Ein Bierchen geht immer.",
            "Heute ist ein guter Tag für einen guten Tag.",
            "Ich hab da so eine Idee … aber erst noch ein Schluck.",
            "Wusstest du, dass Hopfen eigentlich Gemüse ist?",
            "Ich glaub, ich mag heute alle Menschen.",
        ),
        Mood.FROH to listOf(
            "Ich erfinde gleich das Rad neu. Diesmal eckig!",
            "Wenn Wolken aus Bier wären, würd ich Regen lieben.",
            "Der Wirt ist mein bester Freund. Und du auch!",
            "Ich könnte Bäume umarmen. Oder den Tresen.",
            "Noch eins, und ich schreib ein Gedicht.",
        ),
        Mood.SELIG to listOf(
            "Ich liebe euch alle! Wirklich alle!",
            "Ich bin ein Philosoph. Glaub ich.",
            "Das Leben ist schön, und der Dram ist schöner.",
            "Heute sing ich noch! Ihr wurdet gewarnt.",
            "Prost auf die Erfindung des Prostens!",
        ),
    )

    fun pick(mood: Mood, random: Random = Random, avoid: String? = null): String {
        val options = byMood.getValue(mood).filter { it != avoid }
        return options.random(random)
    }
}

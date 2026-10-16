package de.alcomon.hubbell.audio

import org.junit.Assert.assertTrue
import org.junit.Test

class SynthTest {
    @Test
    fun jederSoundIstHoerbarUndNichtUebersteuert() {
        for (sfx in Sfx.entries) {
            val pcm = Synth.render(sfx)
            assertTrue("$sfx ist leer", pcm.size > Synth.SAMPLE_RATE / 10)
            val peak = pcm.maxOf { kotlin.math.abs(it.toInt()) }
            assertTrue("$sfx ist zu leise", peak > 5000)
            assertTrue("$sfx ist zu lang", pcm.size < Synth.SAMPLE_RATE * 8)
        }
    }
}

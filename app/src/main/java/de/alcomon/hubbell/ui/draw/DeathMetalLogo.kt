package de.alcomon.hubbell.ui.draw

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Ein Strich des Logos in Logo-Einheiten (x: -1..1, y: -0.5..0.5), Breite in denselben Einheiten. */
private class LogoStroke(val ax: Float, val ay: Float, val bx: Float, val by: Float, val width: Float)

/**
 * Ein unleserliches Death-Metal-Bandlogo: dicke, krakelige Buchstabenstämme, Dornen nach oben
 * und unten, Wurzeln, die nach unten wuchern, und alles spiegelsymmetrisch. Wird einmal mit festem
 * Zufall erzeugt, damit es immer gleich aussieht.
 */
private val LogoStrokes: List<LogoStroke> by lazy {
    val rnd = Random(666)
    val half = mutableListOf<LogoStroke>()
    fun line(ax: Float, ay: Float, bx: Float, by: Float, w: Float) {
        half += LogoStroke(ax, ay, bx, by, w)
    }
    // Buchstabenstämme, leicht schief, mit Querbalken zum Nachbarn
    val stems = 7
    for (i in 0 until stems) {
        val x = 0.06f + i * (0.88f / (stems - 1))
        val top = -0.22f - rnd.nextFloat() * 0.12f
        val bottom = 0.18f + rnd.nextFloat() * 0.1f
        val lean = (rnd.nextFloat() - 0.5f) * 0.08f
        val mid = (top + bottom) / 2f + (rnd.nextFloat() - 0.5f) * 0.1f
        line(x, top, x + lean, mid, 0.05f)
        line(x + lean, mid, x - lean * 0.5f, bottom, 0.05f)
        if (i < stems - 1) {
            val nx = x + 0.88f / (stems - 1)
            val by = if (rnd.nextBoolean()) top + 0.04f else bottom - 0.04f
            line(x, by, nx, by + (rnd.nextFloat() - 0.5f) * 0.12f, 0.03f)
        }
        // Dornen nach oben
        repeat(1 + rnd.nextInt(2)) {
            val a = -Math.PI.toFloat() / 2f + (rnd.nextFloat() - 0.5f) * 1.2f
            val len = 0.12f + rnd.nextFloat() * 0.18f
            line(x, top, x + cos(a) * len, top + sin(a) * len, 0.018f)
        }
        // Wurzeln, die nach unten wuchern und sich verzweigen
        var rx = x - lean * 0.5f
        var ry = bottom
        var w = 0.03f
        repeat(2 + rnd.nextInt(3)) {
            val nx2 = rx + (rnd.nextFloat() - 0.5f) * 0.12f
            val ny2 = ry + 0.05f + rnd.nextFloat() * 0.07f
            line(rx, ry, nx2, ny2, w)
            if (rnd.nextFloat() < 0.5f) {
                val side = if (rnd.nextBoolean()) 1f else -1f
                line(nx2, ny2, nx2 + side * 0.08f, ny2 + 0.06f, w * 0.6f)
            }
            rx = nx2
            ry = ny2
            w *= 0.7f
        }
    }
    // Ein paar Ranken quer durch alles
    repeat(5) {
        val x1 = rnd.nextFloat() * 0.9f
        val y1 = (rnd.nextFloat() - 0.5f) * 0.5f
        line(x1, y1, x1 + 0.15f + rnd.nextFloat() * 0.2f, y1 + (rnd.nextFloat() - 0.5f) * 0.3f, 0.014f)
    }
    // Lange Dornen ganz außen
    line(0.94f, -0.05f, 1.08f, -0.42f, 0.02f)
    line(0.94f, 0.1f, 1.05f, 0.4f, 0.016f)
    // gespiegelt zu einem symmetrischen Logo
    half + half.map { LogoStroke(-it.ax, it.ay, -it.bx, it.by, it.width) }
}

/** Zeichnet das Logo mittig bei [center]; [width] ist die Gesamtbreite. */
fun DrawScope.drawDeathMetalLogo(center: Offset, width: Float, ink: Color, accent: Color) {
    val s = width / 2f
    // ein umgedrehtes Dreieck in der Mitte, wie es sich gehört
    drawLine(accent, center + Offset(-0.12f * s, -0.05f * s), center + Offset(0.12f * s, -0.05f * s), strokeWidth = 0.025f * s)
    drawLine(accent, center + Offset(-0.12f * s, -0.05f * s), center + Offset(0f, 0.18f * s), strokeWidth = 0.025f * s)
    drawLine(accent, center + Offset(0.12f * s, -0.05f * s), center + Offset(0f, 0.18f * s), strokeWidth = 0.025f * s)
    for (st in LogoStrokes) {
        drawLine(
            ink,
            center + Offset(st.ax * s, st.ay * s),
            center + Offset(st.bx * s, st.by * s),
            strokeWidth = (st.width * s).coerceAtLeast(0.8f),
            cap = StrokeCap.Round,
        )
    }
}

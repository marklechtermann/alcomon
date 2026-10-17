package de.alcomon.hubbell.ui.draw

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sin

// Die Döner-Pause liegt zwischen den Würfen (alle 23 s ab Sekunde 9), damit sich nichts überschneidet
private const val DOENER_PERIOD = 46f
private const val DOENER_OFFSET = 37f
private const val DOENER_LENGTH = 11f

private const val SPIT_IN = 1f
private const val CUT_END = 3f
private const val SALAD_END = 4f
private const val SAUCE_END = 4.8f
private const val TO_MOUTH_END = 5.8f
private const val EAT_END = 9.4f
private const val BITES = 4

private val Meat = Color(0xFF9C5A2C)
private val MeatDark = Color(0xFF6D3B17)
private val Bread = Color(0xFFE6B877)
private val BreadDark = Color(0xFFC48A45)
private val Salad = Color(0xFF6CC060)
private val Tomato = Color(0xFFE53935)
private val Onion = Color(0xFFE1BEE7)
private val Sauce = Color(0xFFF7F3EA)

private val SayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    textAlign = Paint.Align.CENTER
    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
}

/** Sekunden seit Beginn der aktuellen Döner-Pause oder null, wenn Hubbell gerade keinen Hunger hat. */
fun doenerTime(time: Float): Float? {
    val shifted = time - DOENER_OFFSET
    if (shifted < 0f) return null
    val t = shifted - floor(shifted / DOENER_PERIOD) * DOENER_PERIOD
    return if (t <= DOENER_LENGTH) t else null
}

private fun ease(x: Float): Float {
    val t = x.coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

/**
 * Hubbell hat Hunger: Ein Dönerspieß fährt auf den Tisch, Fleisch wird abgeschnitten,
 * Salat und Soße kommen drauf, dann wandert der Döner zum Mund und wird in Bissen verdrückt.
 */
fun DrawScope.drawDoenerBreak(time: Float, cx: Float, tableTop: Float, u: Float, skin: Color) {
    val t = doenerTime(time) ?: return
    val mouth = Offset(cx, tableTop - 33.5f * u)

    // Spieß fährt von links rein und am Ende wieder raus
    val slide = when {
        t < SPIT_IN -> 1f - ease(t / SPIT_IN)
        t > DOENER_LENGTH - 1f -> ease((t - (DOENER_LENGTH - 1f)) / 1f)
        else -> 0f
    }
    val spitX = cx - 36f * u - slide * size.width * 0.5f
    drawSpit(Offset(spitX, tableTop), u, time, cutting = t in SPIT_IN..CUT_END, cutPhase = t)

    // Brottasche vor dem Spieß, wird nach und nach gefüllt
    val meat = ease((t - SPIT_IN) / (CUT_END - SPIT_IN))
    val salad = ease((t - CUT_END) / (SALAD_END - CUT_END))
    val sauce = ease((t - SALAD_END) / (SAUCE_END - SALAD_END))
    val onTable = Offset(cx - 24f * u, tableTop - 5f * u)
    if (t >= SPIT_IN && t < EAT_END) {
        val travel = ease((t - SAUCE_END) / (TO_MOUTH_END - SAUCE_END))
        val pos = lerp(onTable, mouth + Offset(-7f * u, 2f * u), travel)
        val bites = if (t < TO_MOUTH_END) 0f else ((t - TO_MOUTH_END) / (EAT_END - TO_MOUTH_END) * BITES).coerceAtMost(BITES.toFloat())
        val munch = if (t >= TO_MOUTH_END) abs(sin(t * 9f)) * 1.2f * u else 0f
        rotate(-35f * travel, pivot = pos) {
            drawDoener(pos + Offset(munch * 0.6f, 0f), u, meat, salad, sauce, bites)
        }
        if (travel > 0.3f) {
            // Hubbells Hand hält den Döner
            drawCircle(skin, 3.2f * u, pos + Offset(1f * u, 6f * u))
        }
        // Krümel beim Abbeißen
        if (t >= TO_MOUTH_END) {
            for (i in 0 until 5) {
                val p = ((t * 1.7f + i * 0.21f) % 1f)
                drawCircle(
                    if (i % 2 == 0) Meat else Salad,
                    0.7f * u,
                    mouth + Offset((i - 2) * 1.6f * u, 3f * u + p * 30f * u),
                )
            }
        }
    }

    // Was Hubbell dazu sagt
    val line = when {
        t < 2.4f -> "Hunger! Döner!!!"
        t < SAUCE_END -> "Mit allem und scharf!"
        t in TO_MOUTH_END..EAT_END -> "Mmmh! Mampf!"
        t > EAT_END -> "Lecker! Jetzt ein Bier!"
        else -> null
    }
    if (line != null) drawSay(line, Offset(cx - 22f * u, tableTop - 58f * u), u)
}

/** Der Dönerspieß mit drehendem Fleischkegel; [cutting] lässt ein Messer daran entlangfahren. */
private fun DrawScope.drawSpit(base: Offset, u: Float, time: Float, cutting: Boolean, cutPhase: Float) {
    val top = base.y - 40f * u
    // Grill-Glühen hinter dem Fleisch
    drawRoundRect(Color(0xFF424242), topLeft = Offset(base.x - 12f * u, top - 2f * u), size = Size(3f * u, 42f * u), cornerRadius = CornerRadius(1f * u))
    for (i in 0 until 4) {
        drawRect(
            Color(0xFFFF7043).copy(alpha = 0.55f + 0.25f * sin(time * 6f + i)),
            topLeft = Offset(base.x - 11.5f * u, top + 4f * u + i * 8f * u),
            size = Size(2f * u, 5f * u),
        )
    }
    // Stange und Fuß
    drawLine(Color(0xFFB0BEC5), Offset(base.x, top - 4f * u), Offset(base.x, base.y), strokeWidth = 1.2f * u)
    drawRoundRect(Color(0xFF9E9E9E), topLeft = Offset(base.x - 8f * u, base.y - 2f * u), size = Size(16f * u, 2.5f * u), cornerRadius = CornerRadius(1f * u))
    // Fleischkegel, oben breiter als unten
    val cone = Path().apply {
        moveTo(base.x - 8f * u, top)
        lineTo(base.x + 8f * u, top)
        lineTo(base.x + 4.5f * u, base.y - 4f * u)
        lineTo(base.x - 4.5f * u, base.y - 4f * u)
        close()
    }
    drawPath(cone, Meat)
    clipPath(cone) {
        // Schichten, die sich mit der Drehung verschieben
        val shift = (time * 6f * u) % (4f * u)
        var y = top - 4f * u + shift
        while (y < base.y) {
            drawLine(MeatDark, Offset(base.x - 9f * u, y), Offset(base.x + 9f * u, y + 1.5f * u), strokeWidth = 0.8f * u)
            y += 4f * u
        }
        drawRect(Color(0x33FFFFFF), topLeft = Offset(base.x - 3f * u, top), size = Size(1.5f * u, 40f * u))
    }
    drawCircle(Color(0xFFFFEB3B), 1.4f * u, Offset(base.x, top - 4f * u)) // Peperoni obendrauf
    if (cutting) {
        // Langes Dönermesser fährt auf und ab
        val k = abs(sin(cutPhase * 5f))
        val ky = top + 6f * u + k * 26f * u
        val kx = base.x + 7f * u - k * 2f * u
        rotate(-12f, pivot = Offset(kx, ky)) {
            drawRoundRect(Color(0xFFCFD8DC), topLeft = Offset(kx - 1f * u, ky - 9f * u), size = Size(2f * u, 14f * u), cornerRadius = CornerRadius(0.8f * u))
            drawRoundRect(Color(0xFF3E2723), topLeft = Offset(kx - 1.2f * u, ky + 5f * u), size = Size(2.4f * u, 5f * u), cornerRadius = CornerRadius(0.8f * u))
        }
        // abfallende Fleischstücke
        for (i in 0 until 3) {
            val p = (cutPhase * 2.4f + i * 0.33f) % 1f
            drawRect(Meat, topLeft = Offset(base.x + 4f * u + i * 1.5f * u + p * 6f * u, ky + p * 18f * u), size = Size(1.6f * u, 0.9f * u))
        }
    }
}

/** Die Brottasche; [bites] (0..4) beißt sie von oben her weg. */
private fun DrawScope.drawDoener(c: Offset, u: Float, meat: Float, salad: Float, sauce: Float, bites: Float) {
    val w = 11f * u
    val h = 13f * u
    val left = c.x - w / 2f
    val bottom = c.y + h / 2f
    val eaten = bites / BITES
    val top = c.y - h / 2f + h * 0.8f * eaten
    if (eaten >= 0.99f) return
    // Rückseite des Brots
    drawRoundRect(BreadDark, topLeft = Offset(left - 0.8f * u, top), size = Size(w + 1.6f * u, bottom - top), cornerRadius = CornerRadius(2f * u))
    // Füllung, die oben rausschaut
    val fillTop = top - 2.5f * u * (1f - eaten)
    if (meat > 0f) {
        for (i in 0 until (8 * meat).toInt().coerceAtLeast(1)) {
            drawRoundRect(Meat, topLeft = Offset(left + (i % 4) * 2.6f * u, fillTop + (i / 4) * 1.6f * u), size = Size(2.8f * u, 1.5f * u), cornerRadius = CornerRadius(0.5f * u))
        }
    }
    if (salad > 0f) {
        for (i in 0 until (5 * salad).toInt()) {
            drawCircle(Salad, 1.4f * u, Offset(left + 1.5f * u + i * 2f * u, fillTop - 0.2f * u))
        }
        if (salad > 0.5f) {
            drawCircle(Tomato, 1.1f * u, Offset(left + 3f * u, fillTop + 0.4f * u))
            drawCircle(Tomato, 1.1f * u, Offset(left + 8f * u, fillTop + 0.2f * u))
            drawCircle(Onion, 1f * u, Offset(left + 5.5f * u, fillTop - 0.8f * u), style = Stroke(0.4f * u))
        }
    }
    if (sauce > 0f) {
        drawLine(
            Sauce, Offset(left + 1f * u, fillTop - 0.6f * u), Offset(left + 1f * u + (w - 2f * u) * sauce, fillTop + 0.3f * u),
            strokeWidth = 1.2f * u, cap = StrokeCap.Round,
        )
    }
    // Vorderseite der Brottasche
    drawRoundRect(Bread, topLeft = Offset(left, top + 1.5f * u), size = Size(w, bottom - top - 1.5f * u), cornerRadius = CornerRadius(2f * u))
    drawLine(BreadDark, Offset(left + 1.5f * u, bottom - 2f * u), Offset(left + w - 1.5f * u, bottom - 2f * u), strokeWidth = 0.4f * u)
    // Bissspuren
    if (bites > 0.2f) {
        for (i in 0 until 3) {
            drawCircle(BreadDark, 1.3f * u, Offset(left + 2f * u + i * 3.5f * u, top + 1.2f * u))
        }
    }
}

private fun DrawScope.drawSay(text: String, anchor: Offset, u: Float) {
    drawIntoCanvas { canvas ->
        SayPaint.textSize = 4.6f * u
        SayPaint.color = Color(0xFF1A1A1A).toArgb()
        val width = SayPaint.measureText(text) + 6f * u
        val height = 8f * u
        val cx = anchor.x.coerceIn(width / 2f + 4f, size.width - width / 2f - 4f)
        val top = anchor.y.coerceAtLeast(4f)
        drawRoundRect(Color.White, topLeft = Offset(cx - width / 2f, top), size = Size(width, height), cornerRadius = CornerRadius(3f * u))
        drawRoundRect(Color(0xFF1A1A1A), topLeft = Offset(cx - width / 2f, top), size = Size(width, height), cornerRadius = CornerRadius(3f * u), style = Stroke(0.6f * u))
        canvas.nativeCanvas.drawText(text, cx, top + 5.6f * u, SayPaint)
    }
}

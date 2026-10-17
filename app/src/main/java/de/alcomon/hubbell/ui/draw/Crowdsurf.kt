package de.alcomon.hubbell.ui.draw

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sin

// Liegt in der Lücke zwischen zwei hüpfenden Gästen (die kommen alle 11 s ab Sekunde 5 für 3,8 s)
private const val SURF_PERIOD = 55f
private const val SURF_OFFSET = 42f
private const val SURF_LENGTH = 6.5f

private val JockelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    textAlign = Paint.Align.CENTER
    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
}

private class Carrier(val skin: Color, val hair: Color, val shirt: Color, val longHair: Boolean)

private val Carriers = listOf(
    Carrier(Color(0xFFE8B98F), Color(0xFF4E342E), Color(0xFF263238), true),
    Carrier(Color(0xFFD9A47A), Color(0xFF111111), Color(0xFF5D1A1A), false),
)

/** Jockel macht Crowdsurfing: zwei Metaller tragen ihn auf erhobenen Händen quer durchs Bild. */
fun DrawScope.drawCrowdsurf(time: Float, u: Float) {
    val shifted = time - SURF_OFFSET
    if (shifted < 0f) return
    val k = floor(shifted / SURF_PERIOD).toInt()
    val t = shifted - k * SURF_PERIOD
    if (t > SURF_LENGTH) return
    val w = size.width
    val h = size.height
    val c = u * 1.1f
    val leftToRight = k % 2 == 0
    val dir = if (leftToRight) 1f else -1f
    val span = w + 120f * c
    val centerX = if (leftToRight) -60f * c + span * (t / SURF_LENGTH) else w + 60f * c - span * (t / SURF_LENGTH)

    // Die beiden Träger, mit Schritt-Wippen
    val gap = 26f * c
    val hands = mutableListOf<Offset>()
    for ((i, carrier) in Carriers.withIndex()) {
        val x = centerX + (i - 0.5f) * gap * 2f
        val step = abs(sin(t * 7f + i * 1.6f)) * 2.5f * c
        val shoulderY = h - 22f * c - step
        drawCarrier(carrier, x, shoulderY, c)
        hands += Offset(x - 5f * c, shoulderY - 20f * c)
        hands += Offset(x + 5f * c, shoulderY - 20f * c)
        for (hand in hands.takeLast(2)) {
            val shoulder = Offset(hand.x + (if (hand.x < x) 4f else -4f) * c, shoulderY + 3f * c)
            drawLine(carrier.skin, shoulder, hand, strokeWidth = 4.5f * c, cap = StrokeCap.Round)
            drawLine(carrier.shirt, shoulder, shoulder + (hand - shoulder) * 0.3f, strokeWidth = 6f * c, cap = StrokeCap.Round)
        }
    }

    // Jockel liegt quer auf den Händen und schaukelt
    val bodyY = hands.minOf { it.y } - 5f * c
    val rock = sin(t * 5f) * 5f
    rotate(rock, pivot = Offset(centerX, bodyY)) {
        drawJockel(Offset(centerX, bodyY), c, dir, t)
    }
    // Hände über ihm, damit er wirklich getragen aussieht
    for ((i, hand) in hands.withIndex()) {
        drawCircle(Carriers[i / 2].skin, 3f * c, hand)
    }
}

private fun DrawScope.drawCarrier(carrier: Carrier, x: Float, shoulderY: Float, c: Float) {
    val headC = Offset(x, shoulderY - 9f * c)
    if (carrier.longHair) {
        drawRoundRect(carrier.hair, topLeft = Offset(x - 11f * c, headC.y - 5f * c), size = Size(22f * c, 22f * c), cornerRadius = CornerRadius(8f * c))
    }
    drawRoundRect(carrier.shirt, topLeft = Offset(x - 15f * c, shoulderY), size = Size(30f * c, 40f * c), cornerRadius = CornerRadius(9f * c))
    drawCircle(carrier.skin, 8.5f * c, headC)
    drawArc(carrier.hair, 180f, 180f, true, topLeft = Offset(x - 9f * c, headC.y - 9.5f * c), size = Size(18f * c, 11f * c))
    // nach oben schauend, Mund offen vor Begeisterung
    drawCircle(Color(0xFF2B1A12), 1f * c, headC + Offset(-3f * c, -1.5f * c))
    drawCircle(Color(0xFF2B1A12), 1f * c, headC + Offset(3f * c, -1.5f * c))
    drawOval(Color(0xFF2B1A12), topLeft = headC + Offset(-2f * c, 2.5f * c), size = Size(4f * c, 3f * c))
}

/** Jockel: schwarze Haare, schwarze Brille, schwarzes Shirt mit großem „JOCKEL“, Jeans und Stiefel. */
private fun DrawScope.drawJockel(c0: Offset, c: Float, dir: Float, t: Float) {
    val skin = Color(0xFFF2C6A0)
    val black = Color(0xFF111111)
    // Kopf zeigt in Laufrichtung
    val head = c0 + Offset(dir * 30f * c, -2f * c)
    val hip = c0 + Offset(-dir * 12f * c, 0f)
    // Beine mit Jeans und Stiefeln, eins strampelt
    val kick = sin(t * 8f) * 4f * c
    for ((i, lift) in listOf(0f, kick).withIndex()) {
        val foot = hip + Offset(-dir * 24f * c, -3f * c + lift + i * 3f * c)
        drawLine(Color(0xFF3B5B8C), hip + Offset(0f, i * 3f * c), foot, strokeWidth = 6f * c, cap = StrokeCap.Round)
        drawRoundRect(Color(0xFF1A1A1A), topLeft = foot + Offset(-dir * 3f * c - 3f * c, -3.5f * c), size = Size(6f * c, 7f * c), cornerRadius = CornerRadius(1.5f * c))
    }
    // Oberkörper im schwarzen Shirt
    val torsoW = abs((head.x - dir * 8f * c) - hip.x) + 6f * c
    val torsoX = minOf(hip.x, head.x - dir * 8f * c) - 3f * c
    drawRoundRect(black, topLeft = Offset(torsoX, c0.y - 8f * c), size = Size(torsoW, 15f * c), cornerRadius = CornerRadius(5f * c))
    drawIntoCanvas { canvas ->
        JockelPaint.color = Color(0xFFF2EDE0).toArgb()
        JockelPaint.textSize = 9f * c
        while (JockelPaint.measureText("JOCKEL") > torsoW * 0.9f && JockelPaint.textSize > 3f) JockelPaint.textSize *= 0.92f
        canvas.nativeCanvas.drawText("JOCKEL", torsoX + torsoW / 2f, c0.y + 3f * c, JockelPaint)
    }
    // Arm reckt die Pommesgabel in die Luft
    val shoulder = head + Offset(-dir * 9f * c, 2f * c)
    val fist = shoulder + Offset(dir * 4f * c, -16f * c + sin(t * 6f) * 2f * c)
    drawLine(black, shoulder, shoulder + (fist - shoulder) * 0.35f, strokeWidth = 6f * c, cap = StrokeCap.Round)
    drawLine(skin, shoulder + (fist - shoulder) * 0.3f, fist, strokeWidth = 4f * c, cap = StrokeCap.Round)
    drawLine(skin, fist + Offset(-1.5f * c, -1f * c), fist + Offset(-2.5f * c, -7f * c), strokeWidth = 1.6f * c, cap = StrokeCap.Round)
    drawLine(skin, fist + Offset(1.5f * c, -1f * c), fist + Offset(2.8f * c, -6f * c), strokeWidth = 1.6f * c, cap = StrokeCap.Round)
    drawCircle(skin, 2.8f * c, fist)
    // Kopf mit schwarzen Haaren und schwarzer Brille
    drawCircle(black, 8.6f * c, head + Offset(dir * 1.5f * c, -1.5f * c))
    drawCircle(skin, 7.5f * c, head)
    drawArc(black, 180f, 180f, true, topLeft = head + Offset(-8f * c, -8.5f * c), size = Size(16f * c, 9f * c))
    drawRoundRect(black, topLeft = head + Offset(-6.5f * c, -2.2f * c), size = Size(5.5f * c, 3.6f * c), cornerRadius = CornerRadius(1f * c))
    drawRoundRect(black, topLeft = head + Offset(1f * c, -2.2f * c), size = Size(5.5f * c, 3.6f * c), cornerRadius = CornerRadius(1f * c))
    drawLine(black, head + Offset(-1f * c, -1f * c), head + Offset(1f * c, -1f * c), strokeWidth = 0.8f * c)
    drawCircle(Color(0x55FFFFFF), 0.7f * c, head + Offset(-4.5f * c, -1.2f * c))
    drawCircle(Color(0x55FFFFFF), 0.7f * c, head + Offset(3f * c, -1.2f * c))
    // breites Grinsen
    drawArc(Color(0xFF2B1A12), 0f, 180f, true, topLeft = head + Offset(-3f * c, 2f * c), size = Size(6f * c, 3.5f * c))
}

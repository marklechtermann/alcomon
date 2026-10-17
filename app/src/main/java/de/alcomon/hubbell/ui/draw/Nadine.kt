package de.alcomon.hubbell.ui.draw

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sin

// Auch Nadine nutzt eine Lücke zwischen den hüpfenden Gästen, aber eine andere als Jockel
private const val RUN_PERIOD = 55f
private const val RUN_OFFSET = 20f
private const val RUN_LENGTH = 3.6f

private val Blonde = Color(0xFFF2D16B)
private val BlondeShade = Color(0xFFD9B44A)
private val NadineSkin = Color(0xFFF5C9A0)

private val NadinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    textAlign = Paint.Align.CENTER
    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
}

/** Nadine rennt ab und zu vorne durchs Bild, die Haare wehen, die Pommesgabel ist oben. */
fun DrawScope.drawNadine(time: Float, u: Float) {
    val shifted = time - RUN_OFFSET
    if (shifted < 0f) return
    val k = floor(shifted / RUN_PERIOD).toInt()
    val t = shifted - k * RUN_PERIOD
    if (t > RUN_LENGTH) return
    val c = u * 1.1f
    val w = size.width
    val dir = if (k % 2 == 0) -1f else 1f
    val q = t / RUN_LENGTH
    val x = if (dir > 0) -30f * c + (w + 60f * c) * q else w + 30f * c - (w + 60f * c) * q
    val bob = abs(sin(t * 14f)) * 3f * c
    val shoulderY = size.height - 24f * c - bob
    val headC = Offset(x + dir * 2f * c, shoulderY - 9f * c)

    // Lange blonde Haare wehen nach hinten
    val flutter = sin(t * 18f) * 2f * c
    drawPath(
        Path().apply {
            moveTo(headC.x + dir * 6f * c, headC.y - 8f * c)
            quadraticBezierTo(headC.x - dir * 14f * c, headC.y - 6f * c + flutter, headC.x - dir * 24f * c, headC.y + 6f * c - flutter)
            quadraticBezierTo(headC.x - dir * 14f * c, headC.y + 12f * c, headC.x - dir * 4f * c, headC.y + 12f * c)
            close()
        },
        Blonde,
    )

    // Schwarzes Metal-Shirt mit „NADINE ROCKT“
    val torsoW = 30f * c
    drawRoundRect(Color(0xFF111111), topLeft = Offset(x - torsoW / 2f, shoulderY), size = Size(torsoW, 40f * c), cornerRadius = CornerRadius(9f * c))
    drawIntoCanvas { canvas ->
        NadinePaint.color = Color(0xFFF2EDE0).toArgb()
        NadinePaint.textSize = 5.6f * c
        canvas.nativeCanvas.drawText("NADINE", x, shoulderY + 10f * c, NadinePaint)
        NadinePaint.color = Color(0xFFD7263D).toArgb()
        canvas.nativeCanvas.drawText("ROCKT", x, shoulderY + 16.5f * c, NadinePaint)
    }

    // Kopf
    drawOval(NadineSkin, topLeft = Offset(headC.x - 8f * c, headC.y - 9f * c), size = Size(16f * c, 18f * c))
    drawArc(Blonde, 180f, 180f, true, topLeft = Offset(headC.x - 8.6f * c, headC.y - 10f * c), size = Size(17.2f * c, 11f * c))
    drawArc(BlondeShade, 200f, 60f, false, topLeft = Offset(headC.x - 7f * c, headC.y - 9f * c), size = Size(14f * c, 9f * c), style = Stroke(0.8f * c))
    // Seitensträhnen
    drawRoundRect(Blonde, topLeft = Offset(headC.x - 9f * c, headC.y - 4f * c), size = Size(3f * c, 13f * c), cornerRadius = CornerRadius(1.5f * c))
    drawRoundRect(Blonde, topLeft = Offset(headC.x + 6f * c, headC.y - 4f * c), size = Size(3f * c, 13f * c), cornerRadius = CornerRadius(1.5f * c))
    // fröhliches Gesicht mit Wimpern und rotem Mund
    for (sx in listOf(-1f, 1f)) {
        val eye = headC + Offset(sx * 3.2f * c, -1f * c)
        drawCircle(Color(0xFF2B1A12), 1.1f * c, eye)
        drawLine(Color(0xFF2B1A12), eye + Offset(sx * 0.8f * c, -0.8f * c), eye + Offset(sx * 2f * c, -1.8f * c), strokeWidth = 0.5f * c)
    }
    drawCircle(Color(0xFFE0565B).copy(alpha = 0.35f), 1.8f * c, headC + Offset(-5f * c, 2.5f * c))
    drawCircle(Color(0xFFE0565B).copy(alpha = 0.35f), 1.8f * c, headC + Offset(5f * c, 2.5f * c))
    drawOval(Color(0xFF8E1B2E), topLeft = headC + Offset(-2.4f * c, 3f * c), size = Size(4.8f * c, 3.4f * c))

    // Ein Arm pumpt beim Rennen, der andere zeigt die Pommesgabel
    val pump = sin(t * 14f) * 5f * c
    val back = Offset(x - dir * torsoW / 2f + dir * 3f * c, shoulderY + 4f * c)
    drawLine(NadineSkin, back, back + Offset(-dir * 6f * c, 10f * c + pump), strokeWidth = 4.5f * c, cap = StrokeCap.Round)
    drawLine(Color(0xFF111111), back, back + Offset(-dir * 2f * c, 3f * c), strokeWidth = 6f * c, cap = StrokeCap.Round)
    val front = Offset(x + dir * torsoW / 2f - dir * 3f * c, shoulderY + 4f * c)
    val hand = front + Offset(dir * 7f * c, -20f * c + sin(t * 8f) * 1.5f * c)
    drawLine(NadineSkin, front, hand, strokeWidth = 4.5f * c, cap = StrokeCap.Round)
    drawLine(Color(0xFF111111), front, front + (hand - front) * 0.3f, strokeWidth = 6f * c, cap = StrokeCap.Round)
    drawLine(NadineSkin, hand + Offset(-1.5f * c, -1f * c), hand + Offset(-2.6f * c, -7.5f * c), strokeWidth = 1.7f * c, cap = StrokeCap.Round)
    drawLine(NadineSkin, hand + Offset(1.6f * c, -1f * c), hand + Offset(3f * c, -6.5f * c), strokeWidth = 1.7f * c, cap = StrokeCap.Round)
    drawCircle(NadineSkin, 3.2f * c, hand)
    drawLine(Color(0x55000000), hand + Offset(-1.6f * c, 0.6f * c), hand + Offset(1.6f * c, 0.6f * c), strokeWidth = 0.6f * c)
}

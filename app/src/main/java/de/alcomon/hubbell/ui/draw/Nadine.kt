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

// Auch Nadin nutzt eine Lücke zwischen den hüpfenden Gästen, aber eine andere als Jockel
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

/** Nadin rennt ab und zu vorne durchs Bild, die Haare wehen, die Pommesgabel ist oben. */
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

    // Schwarzes Metal-Shirt mit „NADIN ROCKT“
    val torsoW = 30f * c
    drawRoundRect(Color(0xFF111111), topLeft = Offset(x - torsoW / 2f, shoulderY), size = Size(torsoW, 40f * c), cornerRadius = CornerRadius(9f * c))
    drawIntoCanvas { canvas ->
        NadinePaint.color = Color(0xFFF2EDE0).toArgb()
        NadinePaint.textSize = 5.6f * c
        canvas.nativeCanvas.drawText("NADIN", x, shoulderY + 10f * c, NadinePaint)
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

// Jürgen schlendert in einer dritten Lücke durchs Bild
private const val WALK_OFFSET = 53.2f
private const val WALK_LENGTH = 5.6f

private val Grey = Color(0xFFB8B8B8)
private val JuergenSkin = Color(0xFFEDC09A)

/** Jürgen: wenige graue Haare, schwarzes Shirt mit „JÜÜÜÜRGEN“, schlendert mit Bier vorbei. */
fun DrawScope.drawJuergen(time: Float, u: Float) {
    val shifted = time - WALK_OFFSET
    if (shifted < 0f) return
    val k = floor(shifted / RUN_PERIOD).toInt()
    val t = shifted - k * RUN_PERIOD
    if (t > WALK_LENGTH) return
    val c = u * 1.1f
    val w = size.width
    val dir = if (k % 2 == 0) 1f else -1f
    val q = t / WALK_LENGTH
    val x = if (dir > 0) -30f * c + (w + 60f * c) * q else w + 30f * c - (w + 60f * c) * q
    val bob = abs(sin(t * 6f)) * 1.5f * c
    val shoulderY = size.height - 24f * c - bob
    val headC = Offset(x, shoulderY - 9.5f * c)

    val torsoW = 36f * c
    drawRoundRect(Color(0xFF111111), topLeft = Offset(x - torsoW / 2f, shoulderY), size = Size(torsoW, 40f * c), cornerRadius = CornerRadius(10f * c))
    drawIntoCanvas { canvas ->
        NadinePaint.color = Color(0xFFF2EDE0).toArgb()
        NadinePaint.textSize = 7f * c
        while (NadinePaint.measureText("JÜÜÜÜRGEN") > torsoW * 0.92f && NadinePaint.textSize > 3f) NadinePaint.textSize *= 0.93f
        canvas.nativeCanvas.drawText("JÜÜÜÜRGEN", x, shoulderY + 12f * c, NadinePaint)
    }

    // Kopf: Glatze mit grauem Haarkranz und ein paar einzelnen grauen Härchen obendrauf
    drawCircle(JuergenSkin, 9f * c, headC)
    for (side in listOf(-1f, 1f)) {
        drawArc(Grey, if (side < 0) 110f else -70f, 140f, false, topLeft = headC + Offset(-9.3f * c, -8f * c), size = Size(18.6f * c, 16f * c), style = Stroke(2.2f * c))
    }
    for (i in -1..1) {
        val root = headC + Offset(i * 2.2f * c, -8.8f * c)
        drawLine(Grey, root, root + Offset(i * 1.2f * c + sin(t * 4f + i) * 0.8f * c, -3.5f * c), strokeWidth = 0.6f * c, cap = StrokeCap.Round)
    }
    drawOval(Color(0x55FFFFFF), topLeft = headC + Offset(-4f * c, -8f * c), size = Size(5f * c, 2.5f * c))
    // gemütliches Gesicht
    drawCircle(Color(0xFF2B1A12), 1f * c, headC + Offset(-3f * c, -1f * c))
    drawCircle(Color(0xFF2B1A12), 1f * c, headC + Offset(3f * c, -1f * c))
    drawLine(Grey, headC + Offset(-4.5f * c, -3.2f * c), headC + Offset(-1.5f * c, -3.4f * c), strokeWidth = 0.8f * c)
    drawLine(Grey, headC + Offset(1.5f * c, -3.4f * c), headC + Offset(4.5f * c, -3.2f * c), strokeWidth = 0.8f * c)
    drawArc(Color(0xFF2B1A12), 20f, 140f, false, topLeft = headC + Offset(-3f * c, 1.5f * c), size = Size(6f * c, 3.5f * c), style = Stroke(0.8f * c))

    // Bier in der einen Hand, die andere schwingt beim Gehen
    val swing = sin(t * 6f) * 4f * c
    val front = Offset(x + dir * (torsoW / 2f - 3f * c), shoulderY + 4f * c)
    val beerHand = front + Offset(dir * 6f * c, 8f * c)
    drawLine(JuergenSkin, front, beerHand, strokeWidth = 4.5f * c, cap = StrokeCap.Round)
    drawLine(Color(0xFF111111), front, front + (beerHand - front) * 0.35f, strokeWidth = 6f * c, cap = StrokeCap.Round)
    drawDrink(de.alcomon.hubbell.game.Drink.HELLES, beerHand + Offset(-4f * c, -12f * c), 8f * c, 12f * c, 1f)
    drawCircle(JuergenSkin, 3f * c, beerHand)
    val back = Offset(x - dir * (torsoW / 2f - 3f * c), shoulderY + 4f * c)
    drawLine(JuergenSkin, back, back + Offset(-dir * 3f * c + swing, 14f * c), strokeWidth = 4.5f * c, cap = StrokeCap.Round)
    drawLine(Color(0xFF111111), back, back + Offset(-dir * 1f * c, 4f * c), strokeWidth = 6f * c, cap = StrokeCap.Round)

    // Er hat gehört, dass jemand nach ihm ruft
    if (q in 0.25f..0.8f) {
        drawIntoCanvas { canvas ->
            NadinePaint.textSize = 4.6f * c
            NadinePaint.color = Color(0xFF1A1A1A).toArgb()
            val text = "Hat wer Jürgen gerufen?"
            val bw = NadinePaint.measureText(text) + 6f * c
            val bx = headC.x.coerceIn(bw / 2f + 4f, size.width - bw / 2f - 4f)
            val top = headC.y - 24f * c
            drawRoundRect(Color.White, topLeft = Offset(bx - bw / 2f, top), size = Size(bw, 8f * c), cornerRadius = CornerRadius(3f * c))
            drawRoundRect(Color(0xFF1A1A1A), topLeft = Offset(bx - bw / 2f, top), size = Size(bw, 8f * c), cornerRadius = CornerRadius(3f * c), style = Stroke(0.6f * c))
            canvas.nativeCanvas.drawText(text, bx, top + 5.6f * c, NadinePaint)
        }
    }
}

// Jule geht in einer vierten Lücke vorbei, wenn gerade kein Gast hüpft
private const val JULE_OFFSET = 31.2f
private const val JULE_LENGTH = 5.6f

private val JuleSkin = Color(0xFFF7CFAA)

/** Jule: blonder Pferdeschwanz, schwarzes Shirt mit großem „JULE“, ein Buch in der Hand und die Pommesgabel oben. */
fun DrawScope.drawJule(time: Float, u: Float) {
    val shifted = time - JULE_OFFSET
    if (shifted < 0f) return
    val k = floor(shifted / RUN_PERIOD).toInt()
    val t = shifted - k * RUN_PERIOD
    if (t > JULE_LENGTH) return
    val c = u * 1.1f
    val w = size.width
    val dir = if (k % 2 == 0) 1f else -1f
    val q = t / JULE_LENGTH
    val x = if (dir > 0) -30f * c + (w + 60f * c) * q else w + 30f * c - (w + 60f * c) * q
    val bob = abs(sin(t * 7f)) * 2f * c
    val shoulderY = size.height - 24f * c - bob
    val headC = Offset(x, shoulderY - 9f * c)

    // Pferdeschwanz schwingt hinter dem Kopf
    val sway = sin(t * 7f) * 2.5f * c
    drawPath(
        Path().apply {
            moveTo(headC.x - dir * 4f * c, headC.y - 7f * c)
            quadraticBezierTo(headC.x - dir * 16f * c, headC.y - 4f * c + sway, headC.x - dir * 14f * c + sway, headC.y + 12f * c)
            quadraticBezierTo(headC.x - dir * 10f * c, headC.y + 2f * c, headC.x - dir * 5f * c, headC.y - 1f * c)
            close()
        },
        Blonde,
    )

    // Schwarzes Metal-Shirt mit „JULE“
    val torsoW = 32f * c
    drawRoundRect(Color(0xFF111111), topLeft = Offset(x - torsoW / 2f, shoulderY), size = Size(torsoW, 40f * c), cornerRadius = CornerRadius(9f * c))
    drawIntoCanvas { canvas ->
        NadinePaint.color = Color(0xFFF2EDE0).toArgb()
        NadinePaint.textSize = 10f * c
        canvas.nativeCanvas.drawText("JULE", x, shoulderY + 14f * c, NadinePaint)
    }

    // Kopf mit blonden Haaren und Pony
    drawOval(JuleSkin, topLeft = Offset(headC.x - 8f * c, headC.y - 9f * c), size = Size(16f * c, 18f * c))
    drawArc(Blonde, 180f, 180f, true, topLeft = Offset(headC.x - 8.6f * c, headC.y - 10.5f * c), size = Size(17.2f * c, 12f * c))
    drawRoundRect(Blonde, topLeft = Offset(headC.x - 7.5f * c, headC.y - 6f * c), size = Size(15f * c, 3f * c), cornerRadius = CornerRadius(1.5f * c))
    drawArc(BlondeShade, 200f, 60f, false, topLeft = Offset(headC.x - 7f * c, headC.y - 9.5f * c), size = Size(14f * c, 9f * c), style = Stroke(0.8f * c))
    drawRoundRect(Blonde, topLeft = Offset(headC.x - 9f * c, headC.y - 4f * c), size = Size(2.6f * c, 11f * c), cornerRadius = CornerRadius(1.3f * c))
    drawRoundRect(Blonde, topLeft = Offset(headC.x + 6.4f * c, headC.y - 4f * c), size = Size(2.6f * c, 11f * c), cornerRadius = CornerRadius(1.3f * c))
    for (sx in listOf(-1f, 1f)) {
        val eye = headC + Offset(sx * 3.2f * c, -0.5f * c)
        drawCircle(Color(0xFF2B1A12), 1.1f * c, eye)
        drawLine(Color(0xFF2B1A12), eye + Offset(sx * 0.8f * c, -0.8f * c), eye + Offset(sx * 2f * c, -1.6f * c), strokeWidth = 0.5f * c)
    }
    drawCircle(Color(0xFFE0565B).copy(alpha = 0.3f), 1.7f * c, headC + Offset(-5f * c, 3f * c))
    drawCircle(Color(0xFFE0565B).copy(alpha = 0.3f), 1.7f * c, headC + Offset(5f * c, 3f * c))
    drawArc(Color(0xFF8E1B2E), 10f, 160f, false, topLeft = headC + Offset(-3f * c, 2f * c), size = Size(6f * c, 3.6f * c), style = Stroke(1f * c))

    // Hintere Hand hält ein Buch
    val back = Offset(x - dir * (torsoW / 2f - 3f * c), shoulderY + 4f * c)
    val bookHand = back + Offset(-dir * 5f * c, 12f * c + sin(t * 7f) * 1.5f * c)
    drawLine(JuleSkin, back, bookHand, strokeWidth = 4.5f * c, cap = StrokeCap.Round)
    drawLine(Color(0xFF111111), back, back + (bookHand - back) * 0.3f, strokeWidth = 6f * c, cap = StrokeCap.Round)
    val book = bookHand + Offset(-dir * 4f * c - 5f * c, -4f * c)
    drawRoundRect(Color(0xFF7A1E2C), topLeft = book, size = Size(10f * c, 13f * c), cornerRadius = CornerRadius(1f * c))
    drawRect(Color(0xFFF2EDE0), topLeft = book + Offset(if (dir > 0) 9f * c else 0f, 0.8f * c), size = Size(1f * c, 11.4f * c))
    drawLine(Color(0xFFD9B44A), book + Offset(2f * c, 4f * c), book + Offset(8f * c, 4f * c), strokeWidth = 0.8f * c)
    drawLine(Color(0xFFD9B44A), book + Offset(2f * c, 6f * c), book + Offset(7f * c, 6f * c), strokeWidth = 0.6f * c)
    drawCircle(JuleSkin, 3f * c, bookHand)

    // Vordere Hand zeigt die Pommesgabel
    val front = Offset(x + dir * (torsoW / 2f - 3f * c), shoulderY + 4f * c)
    val hand = front + Offset(dir * 6f * c, -19f * c + sin(t * 5f) * 1.5f * c)
    drawLine(JuleSkin, front, hand, strokeWidth = 4.5f * c, cap = StrokeCap.Round)
    drawLine(Color(0xFF111111), front, front + (hand - front) * 0.3f, strokeWidth = 6f * c, cap = StrokeCap.Round)
    drawLine(JuleSkin, hand + Offset(-1.5f * c, -1f * c), hand + Offset(-2.6f * c, -7.5f * c), strokeWidth = 1.7f * c, cap = StrokeCap.Round)
    drawLine(JuleSkin, hand + Offset(1.6f * c, -1f * c), hand + Offset(3f * c, -6.5f * c), strokeWidth = 1.7f * c, cap = StrokeCap.Round)
    drawCircle(JuleSkin, 3.2f * c, hand)
    drawLine(Color(0x55000000), hand + Offset(-1.6f * c, 0.6f * c), hand + Offset(1.6f * c, 0.6f * c), strokeWidth = 0.6f * c)
}

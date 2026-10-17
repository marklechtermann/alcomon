package de.alcomon.hubbell.ui.draw

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sin

/** Große Dinge, die aus Sicht des Spielers auf Hubbell geworfen werden. */
enum class ThrownKind(val reaction: String, val knocksOver: Boolean) {
    FISCH("Skål! Grüße aus Norwegen!", true),
    WACKEN("Aua! Wacken ruft!", true),
    BIERFLASCHE("Aua! Aber danke!", true),
    WASSERFLASCHE("Iiih! WASSER!!!", false),
    WHISKY("Aua! Aber Malts of Scotland, Slàinte!", true),
}

/** Was gerade mit Hubbell passiert, weil ihn etwas getroffen hat. */
class Knock(
    val kind: ThrownKind,
    /** Zeit seit dem Wurf in Sekunden. */
    val t: Float,
    /** Wie weit er vom Stuhl gekippt ist (0 = sitzt, 1 = liegt). */
    val fall: Float,
    /** Erschrecken bei Wasser (0 bis 1). */
    val startle: Float,
)

private const val THROW_PERIOD = 23f
private const val THROW_OFFSET = 9f
private const val THROW_LENGTH = 4.2f
const val THROW_FLIGHT = 0.7f

private fun ease(x: Float): Float {
    val t = x.coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

/** Ab und zu fliegt etwas auf Hubbell. Liefert null, wenn gerade nichts passiert. */
fun knockAt(time: Float): Knock? {
    val shifted = time - THROW_OFFSET
    if (shifted < 0f) return null
    val k = floor(shifted / THROW_PERIOD).toInt()
    val t = shifted - k * THROW_PERIOD
    if (t > THROW_LENGTH) return null
    val kind = ThrownKind.entries[k % ThrownKind.entries.size]
    val after = t - THROW_FLIGHT
    val fall: Float
    val startle: Float
    if (kind.knocksOver) {
        startle = 0f
        fall = when {
            after < 0f -> 0f
            after < 0.5f -> ease(after / 0.5f)
            after < 1.8f -> 1f
            after < 3f -> 1f - ease((after - 1.8f) / 1.2f)
            else -> 0f
        }
    } else {
        fall = 0f
        startle = when {
            after < 0f -> 0f
            after < 0.15f -> after / 0.15f
            after < 1.6f -> 1f
            after < 2.2f -> 1f - (after - 1.6f) / 0.6f
            else -> 0f
        }
    }
    return Knock(kind, t, fall, startle)
}

private val SignPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    textAlign = Paint.Align.CENTER
    typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
}
private val BubblePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    textAlign = Paint.Align.CENTER
    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
}

/** Zeichnet den fliegenden Gegenstand: erst riesig vor der Kamera, dann klein am Kopf, dann prallt er ab. */
fun DrawScope.drawThrownObject(knock: Knock, head: Offset, u: Float) {
    val t = knock.t
    val start = Offset(size.width * 0.5f, size.height * 1.2f)
    val pos: Offset
    val scale: Float
    val angle: Float
    var alpha = 1f
    if (t < THROW_FLIGHT) {
        val q = t / THROW_FLIGHT
        pos = lerp(start, head, q)
        scale = 3.6f - 2.6f * q
        angle = q * 300f
    } else {
        val q = t - THROW_FLIGHT
        if (q > 1.6f) return
        pos = head + Offset(q * 30f * u, -12f * u * q + 70f * u * q * q)
        scale = 1f
        angle = 300f + q * 400f
        alpha = (1f - q / 1.6f).coerceIn(0f, 1f)
    }
    val s = u * scale
    rotate(angle, pivot = pos) {
        when (knock.kind) {
            ThrownKind.FISCH -> drawFish(pos, s, alpha)
            ThrownKind.WACKEN -> drawWackenSign(pos, s, alpha)
            ThrownKind.BIERFLASCHE -> drawBottle(pos, s, alpha, Color(0xFF6D3B12), Color(0xFFF2E6C9), "BIER", Color(0xFFD7263D))
            ThrownKind.WASSERFLASCHE -> drawBottle(pos, s, alpha, Color(0xAA9FD3E8), Color(0xFFFFFFFF), "H2O", Color(0xFF1565C0))
            ThrownKind.WHISKY -> drawWhiskyBottle(pos, s, alpha)
        }
    }
    // Aufprall-Sterne
    val since = t - THROW_FLIGHT
    if (since in 0f..0.35f) {
        val burst = since / 0.35f
        for (i in 0 until 8) {
            val a = i * PI.toFloat() / 4f
            val p = head + Offset(cos(a), sin(a)) * (8f * u + burst * 12f * u)
            drawCircle(Color(0xFFFFE08A).copy(alpha = 1f - burst), 1.6f * u, p)
        }
    }
}

/** Sprechblase mit Hubbells Reaktion und Schwindel-Sternchen, solange er am Boden liegt. */
fun DrawScope.drawKnockReaction(knock: Knock, head: Offset, u: Float, time: Float) {
    val since = knock.t - THROW_FLIGHT
    if (since < 0.1f || since > 3.2f) return
    drawIntoCanvas { canvas ->
        BubblePaint.textSize = 5f * u
        BubblePaint.color = Color(0xFF1A1A1A).toArgb()
        val text = knock.kind.reaction
        val width = BubblePaint.measureText(text) + 6f * u
        val height = 8f * u
        val cx = (head.x + 4f * u).coerceIn(width / 2f + 4f, size.width - width / 2f - 4f)
        val top = (head.y - 34f * u).coerceAtLeast(4f)
        drawRoundRect(Color.White, topLeft = Offset(cx - width / 2f, top), size = Size(width, height), cornerRadius = CornerRadius(3f * u))
        drawRoundRect(Color(0xFF1A1A1A), topLeft = Offset(cx - width / 2f, top), size = Size(width, height), cornerRadius = CornerRadius(3f * u), style = Stroke(0.6f * u))
        canvas.nativeCanvas.drawText(text, cx, top + 5.8f * u, BubblePaint)
    }
    if (knock.fall > 0.5f) {
        for (i in 0..2) {
            val a = time * 5f + i * 2.1f
            drawCircle(Color(0xFFF2B32A), 1.4f * u, head + Offset(cos(a) * 10f * u, sin(a) * 3f * u - 12f * u))
        }
    }
}

private fun DrawScope.drawFish(c: Offset, s: Float, alpha: Float) {
    val body = Color(0xFF8FB3C9).copy(alpha = alpha)
    // Norwegen-Flagge an einem Stöckchen in der Flosse
    val pole = c + Offset(-2f * s, -5f * s)
    drawLine(Color(0xFF6D4C41).copy(alpha = alpha), c + Offset(-1f * s, -1f * s), pole + Offset(0f, -6f * s), strokeWidth = 0.6f * s)
    val flagTop = pole + Offset(0f, -6f * s)
    drawRect(Color(0xFFBA0C2F).copy(alpha = alpha), topLeft = flagTop, size = Size(8f * s, 5.5f * s))
    drawRect(Color.White.copy(alpha = alpha), topLeft = flagTop + Offset(2.2f * s, 0f), size = Size(1.6f * s, 5.5f * s))
    drawRect(Color.White.copy(alpha = alpha), topLeft = flagTop + Offset(0f, 2f * s), size = Size(8f * s, 1.6f * s))
    drawRect(Color(0xFF00205B).copy(alpha = alpha), topLeft = flagTop + Offset(2.6f * s, 0f), size = Size(0.8f * s, 5.5f * s))
    drawRect(Color(0xFF00205B).copy(alpha = alpha), topLeft = flagTop + Offset(0f, 2.4f * s), size = Size(8f * s, 0.8f * s))
    // Schwanzflosse
    val tail = Path().apply {
        moveTo(c.x + 7f * s, c.y)
        lineTo(c.x + 12f * s, c.y - 4f * s)
        lineTo(c.x + 12f * s, c.y + 4f * s)
        close()
    }
    drawPath(tail, Color(0xFF6E93AA).copy(alpha = alpha))
    drawOval(body, topLeft = c - Offset(9f * s, 3.8f * s), size = Size(17f * s, 7.6f * s))
    drawOval(Color(0xFFDCE8EF).copy(alpha = alpha), topLeft = c - Offset(7f * s, 0f), size = Size(13f * s, 3.4f * s))
    drawCircle(Color.White.copy(alpha = alpha), 1.3f * s, c + Offset(-6f * s, -1f * s))
    drawCircle(Color.Black.copy(alpha = alpha), 0.6f * s, c + Offset(-6.2f * s, -1f * s))
    drawArc(
        Color(0xFF3B2414).copy(alpha = alpha), startAngle = 0f, sweepAngle = 180f, useCenter = false,
        topLeft = c + Offset(-9f * s, 0.5f * s), size = Size(3f * s, 1.6f * s), style = Stroke(0.4f * s),
    )
}

private fun DrawScope.drawWackenSign(c: Offset, s: Float, alpha: Float) {
    drawRect(Color(0xFF5D4037).copy(alpha = alpha), topLeft = c + Offset(-0.6f * s, 3f * s), size = Size(1.2f * s, 9f * s))
    drawRoundRect(Color(0xFF111111).copy(alpha = alpha), topLeft = c - Offset(10f * s, 5f * s), size = Size(20f * s, 9f * s), cornerRadius = CornerRadius(1f * s))
    drawRoundRect(Color.White.copy(alpha = alpha), topLeft = c - Offset(10f * s, 5f * s), size = Size(20f * s, 9f * s), cornerRadius = CornerRadius(1f * s), style = Stroke(0.5f * s))
    // Kuhschädel mit Hörnern über dem Schriftzug
    drawCircle(Color.White.copy(alpha = alpha), 1.4f * s, c + Offset(0f, -2.4f * s))
    drawLine(Color.White.copy(alpha = alpha), c + Offset(-1f * s, -3f * s), c + Offset(-3.5f * s, -4.4f * s), strokeWidth = 0.5f * s)
    drawLine(Color.White.copy(alpha = alpha), c + Offset(1f * s, -3f * s), c + Offset(3.5f * s, -4.4f * s), strokeWidth = 0.5f * s)
    drawIntoCanvas { canvas ->
        SignPaint.color = Color.White.copy(alpha = alpha).toArgb()
        SignPaint.textSize = 3.6f * s
        canvas.nativeCanvas.drawText("WACKEN", c.x, c.y + 2.6f * s, SignPaint)
    }
}

private fun DrawScope.drawBottle(c: Offset, s: Float, alpha: Float, glass: Color, label: Color, text: String, cap: Color) {
    drawRoundRect(glass.copy(alpha = glass.alpha * alpha), topLeft = c - Offset(3f * s, 4f * s), size = Size(6f * s, 12f * s), cornerRadius = CornerRadius(1.6f * s))
    drawRect(glass.copy(alpha = glass.alpha * alpha), topLeft = c - Offset(1.1f * s, 9f * s), size = Size(2.2f * s, 5.5f * s))
    drawRect(cap.copy(alpha = alpha), topLeft = c - Offset(1.3f * s, 10f * s), size = Size(2.6f * s, 1.4f * s))
    drawRect(label.copy(alpha = alpha), topLeft = c - Offset(3f * s, -0.5f * s), size = Size(6f * s, 4f * s))
    drawIntoCanvas { canvas ->
        SignPaint.color = Color(0xFF3B1010).copy(alpha = alpha).toArgb()
        SignPaint.textSize = 2.2f * min(s, s)
        canvas.nativeCanvas.drawText(text, c.x, c.y + 3.2f * s, SignPaint)
    }
    drawLine(Color.White.copy(alpha = 0.4f * alpha), c + Offset(-1.8f * s, -3f * s), c + Offset(-1.8f * s, 6f * s), strokeWidth = 0.6f * s)
}

/** Eckige Whiskyflasche mit Etikett „Malts of Scotland“. */
private fun DrawScope.drawWhiskyBottle(c: Offset, s: Float, alpha: Float) {
    val amber = Color(0xCCB86B1C).copy(alpha = 0.8f * alpha)
    drawRoundRect(amber, topLeft = c - Offset(4f * s, 5f * s), size = Size(8f * s, 14f * s), cornerRadius = CornerRadius(0.8f * s))
    drawRoundRect(amber, topLeft = c - Offset(1.4f * s, 9.5f * s), size = Size(2.8f * s, 5f * s), cornerRadius = CornerRadius(0.6f * s))
    drawRect(Color(0xFF3E2723).copy(alpha = alpha), topLeft = c - Offset(1.7f * s, 11.2f * s), size = Size(3.4f * s, 2f * s))
    // Etikett
    drawRect(Color(0xFFF4EBD4).copy(alpha = alpha), topLeft = c - Offset(3.4f * s, 1.5f * s), size = Size(6.8f * s, 7.5f * s))
    drawRect(Color(0xFF1B3A5C).copy(alpha = alpha), topLeft = c - Offset(3.4f * s, 1.5f * s), size = Size(6.8f * s, 7.5f * s), style = Stroke(0.35f * s))
    drawIntoCanvas { canvas ->
        SignPaint.color = Color(0xFF1B3A5C).copy(alpha = alpha).toArgb()
        SignPaint.textSize = 1.5f * s
        canvas.nativeCanvas.drawText("MALTS OF", c.x, c.y + 1.4f * s, SignPaint)
        canvas.nativeCanvas.drawText("SCOTLAND", c.x, c.y + 3.2f * s, SignPaint)
        SignPaint.color = Color(0xFF8E1B2E).copy(alpha = alpha).toArgb()
        SignPaint.textSize = 1.1f * s
        canvas.nativeCanvas.drawText("SINGLE MALT", c.x, c.y + 5.2f * s, SignPaint)
    }
    drawLine(Color.White.copy(alpha = 0.35f * alpha), c + Offset(-3f * s, -4f * s), c + Offset(-3f * s, 8f * s), strokeWidth = 0.6f * s)
}

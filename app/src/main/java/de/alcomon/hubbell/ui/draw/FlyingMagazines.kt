package de.alcomon.hubbell.ui.draw

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import kotlin.math.floor
import kotlin.math.sin

private const val MAG_PERIOD = 27f
private const val MAG_OFFSET = 8f
private const val MAG_LENGTH = 3.6f

private class MangaCover(val title: String, val bg: Color, val hair: Color, val suit: Color)

private val MangaCovers = listOf(
    MangaCover("OTAKU SUMMER", Color(0xFFFF8FB1), Color(0xFF7E57C2), Color(0xFFE53935)),
    MangaCover("MANGA MAX", Color(0xFF80DEEA), Color(0xFFFFB74D), Color(0xFF1E88E5)),
    MangaCover("KAWAII BEACH", Color(0xFFFFF59D), Color(0xFF26A69A), Color(0xFFD81B60)),
)

private val MagPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
    textAlign = android.graphics.Paint.Align.CENTER
    typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD_ITALIC)
}

/** Hin und wieder flattern ein paar Manga-Hefte quer durchs Bild. */
fun DrawScope.drawFlyingMagazines(time: Float) {
    val shifted = time - MAG_OFFSET
    if (shifted < 0f) return
    val k = floor(shifted / MAG_PERIOD).toInt()
    val t0 = shifted - k * MAG_PERIOD
    if (t0 > MAG_LENGTH) return
    val w = size.width
    val h = size.height
    val fromLeft = k % 2 == 1
    for (i in 0 until 3) {
        val t = (t0 - i * 0.35f) / (MAG_LENGTH - 0.8f)
        if (t < 0f || t > 1f) continue
        val startX = if (fromLeft) -0.3f * w else 1.3f * w
        val endX = if (fromLeft) 1.3f * w else -0.3f * w
        val x = startX + (endX - startX) * t
        val y = h * (0.2f + 0.22f * i) + sin(t * 9f + i) * h * 0.04f
        val cover = MangaCovers[(k + i) % MangaCovers.size]
        val mw = minOf(w, h) * 0.2f
        // Das Heft flattert: Seiten klappen auf und zu
        val flap = 0.55f + 0.45f * kotlin.math.abs(sin(time * 7f + i * 1.7f))
        rotate((if (fromLeft) 1f else -1f) * (sin(time * 3f + i) * 25f + t * 60f), pivot = Offset(x, y)) {
            drawMagazine(cover, Offset(x, y), mw, flap)
        }
    }
}

private fun DrawScope.drawMagazine(cover: MangaCover, c: Offset, mw: Float, flap: Float) {
    val mh = mw * 1.4f
    val left = c.x - mw / 2f
    val top = c.y - mh / 2f
    // aufgeklappte Rückseite mit Textspalten
    val backW = mw * (1f - flap)
    if (backW > 1f) {
        drawRect(Color(0xFFF5F2EA), topLeft = Offset(left - backW, top + mh * 0.03f), size = Size(backW, mh * 0.94f))
        for (r in 0 until 6) {
            drawRect(Color(0x55333333), topLeft = Offset(left - backW * 0.85f, top + mh * (0.12f + r * 0.13f)), size = Size(backW * 0.7f, mh * 0.03f))
        }
    }
    val cw = mw * flap
    val cl = c.x + mw / 2f - cw
    drawRect(Color(0x44000000), topLeft = Offset(cl + mw * 0.04f, top + mw * 0.05f), size = Size(cw, mh))
    drawRect(cover.bg, topLeft = Offset(cl, top), size = Size(cw, mh))
    val sx = cw / mw
    fun p(fx: Float, fy: Float) = Offset(cl + fx * cw, top + fy * mh)
    val r = mw * 0.13f
    // Anime-Dame: lange Haare, große Augen, Badeanzug
    drawOval(cover.hair, topLeft = p(0.26f, 0.22f), size = Size(cw * 0.48f, mh * 0.5f))
    drawOval(Color(0xFFFFE0CC), topLeft = Offset(cl + cw * 0.5f - r * sx, top + mh * 0.27f), size = Size(r * 2f * sx, r * 2.1f))
    drawRect(Color(0xFFFFE0CC), topLeft = p(0.46f, 0.43f), size = Size(cw * 0.08f, mh * 0.05f))
    drawOval(Color(0xFFFFE0CC), topLeft = p(0.33f, 0.47f), size = Size(cw * 0.34f, mh * 0.42f))
    drawRoundRect(cover.suit, topLeft = p(0.35f, 0.53f), size = Size(cw * 0.3f, mh * 0.1f), cornerRadius = CornerRadius(mw * 0.04f))
    drawRoundRect(cover.suit, topLeft = p(0.37f, 0.74f), size = Size(cw * 0.26f, mh * 0.12f), cornerRadius = CornerRadius(mw * 0.04f))
    drawOval(cover.hair, topLeft = p(0.3f, 0.21f), size = Size(cw * 0.4f, mh * 0.1f)) // Pony
    for (side in listOf(-1f, 1f)) {
        val eye = p(0.5f + side * 0.085f, 0.36f)
        drawOval(Color.White, topLeft = eye - Offset(r * 0.32f * sx, r * 0.38f), size = Size(r * 0.64f * sx, r * 0.76f))
        drawOval(Color(0xFF3949AB), topLeft = eye - Offset(r * 0.22f * sx, r * 0.28f), size = Size(r * 0.44f * sx, r * 0.6f))
        drawCircle(Color.White, r * 0.1f, eye - Offset(r * 0.08f * sx, r * 0.12f))
    }
    drawCircle(Color(0x66FF5C8A), r * 0.18f, p(0.39f, 0.42f))
    drawCircle(Color(0x66FF5C8A), r * 0.18f, p(0.61f, 0.42f))
    // Titel
    drawIntoCanvas { canvas ->
        MagPaint.color = Color.White.toArgb()
        MagPaint.textSize = mh * 0.1f
        MagPaint.textScaleX = sx.coerceAtLeast(0.1f)
        MagPaint.setShadowLayer(2f, 1f, 1f, Color(0xFF880E4F).toArgb())
        canvas.nativeCanvas.drawText(cover.title, cl + cw / 2f, top + mh * 0.13f, MagPaint)
        MagPaint.clearShadowLayer()
        MagPaint.textScaleX = 1f
    }
}

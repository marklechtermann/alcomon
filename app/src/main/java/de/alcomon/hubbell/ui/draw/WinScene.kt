package de.alcomon.hubbell.ui.draw

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import kotlin.math.sin

private const val WIN_LOOP = 9f
private const val WALK_SECONDS = 5f

private val ZzzPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    textAlign = Paint.Align.CENTER
    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
}

/**
 * Die Siegeranimation: Hubbell steht auf, schwankt glücklich zur Kneipentür, winkt mit der
 * Pommesgabel und verschwindet in die Nacht. Danach schweben nur noch ein paar „Zzz“ heraus.
 */
fun DrawScope.drawWinScene(time: Float) {
    drawMetalBar(time)
    val w = size.width
    val h = size.height
    val floor = h * 0.92f
    drawRect(Color(0xFF1A1416), topLeft = Offset(0f, floor), size = Size(w, h - floor))

    // Offene Kneipentür, draußen ist Nacht
    val door = Rect(w * 0.74f, h * 0.30f, w * 0.94f, floor)
    drawRect(Color(0xFF3A2A1E), topLeft = door.topLeft - Offset(5f, 5f), size = Size(door.width + 10f, door.height + 5f))
    drawRect(Color(0xFF0B1230), topLeft = door.topLeft, size = door.size)
    drawCircle(Color(0xFFF4EBC8), door.width * 0.16f, Offset(door.left + door.width * 0.68f, door.top + door.height * 0.2f))
    drawCircle(Color(0xFF0B1230), door.width * 0.14f, Offset(door.left + door.width * 0.6f, door.top + door.height * 0.17f))
    for (i in 0 until 6) {
        val sx = door.left + door.width * ((i * 0.37f + 0.1f) % 1f)
        val sy = door.top + door.height * (0.08f + (i * 0.23f % 0.5f))
        drawCircle(Color.White.copy(alpha = 0.5f + 0.5f * sin(time * 3f + i)), 1.6f, Offset(sx, sy))
    }
    drawRect(Color(0xFF241A14), topLeft = Offset(door.left, floor - 6f), size = Size(door.width, 6f))

    val t = time % WIN_LOOP
    val u = h / 150f
    val startX = w * 0.32f
    val endX = door.center.x
    val q = (t / WALK_SECONDS).coerceIn(0f, 1f)
    val x = startX + (endX - startX) * q
    // Hinter der Türschwelle geht er draußen weiter und wird kleiner
    val outside = ((t - WALK_SECONDS) / 1.4f).coerceIn(0f, 1f)
    val scale = 1f - 0.45f * outside
    val feet = Offset(x + door.width * 0.1f * outside, floor - 4f * outside * u)
    if (outside < 1f) {
        val drawFigure: DrawScope.() -> Unit = {
            rotate(sin(time * 2.6f) * 7f, pivot = feet) {
                drawWalkingHubbell(feet, u * scale, time, walk = t * 6f)
            }
        }
        if (outside > 0f) clipRect(door.left, 0f, door.right, floor, block = drawFigure) else drawFigure()
    }

    // Die Tür schwingt hinter ihm halb zu
    val shut = ((t - WALK_SECONDS - 1f) / 0.8f).coerceIn(0f, 1f)
    val leafW = door.width * (0.25f + 0.75f * shut)
    drawPath(
        Path().apply {
            moveTo(door.right, door.top)
            lineTo(door.right - leafW, door.top + 6f * (1f - shut))
            lineTo(door.right - leafW, door.bottom - 6f * (1f - shut))
            lineTo(door.right, door.bottom)
            close()
        },
        Color(0xFF5A3D27),
    )
    drawCircle(Color(0xFFD9B44A), 3f, Offset(door.right - leafW * 0.85f, door.center.y))

    // Zzz aus der Tür, er ist auf dem Weg ins Bett
    if (t > WALK_SECONDS + 1.2f) {
        drawIntoCanvas { canvas ->
            for (i in 0 until 3) {
                val a = ((t - WALK_SECONDS - 1.2f - i * 0.5f) / 2f).coerceIn(0f, 1f)
                if (a <= 0f) continue
                ZzzPaint.textSize = (10f + i * 4f) * u
                ZzzPaint.color = Color(0xFFF2EDE0).copy(alpha = 1f - a * 0.6f).toArgb()
                canvas.nativeCanvas.drawText("Z", door.center.x + (i * 9f - 4f) * u, door.top + (34f - a * 18f - i * 7f) * u, ZzzPaint)
            }
        }
    }

    // Schild über der Tür
    drawRoundRect(Color(0xFF111111), topLeft = Offset(door.left - 4f, door.top - 20f * u), size = Size(door.width + 8f, 12f * u), cornerRadius = CornerRadius(3f * u))
    drawIntoCanvas { canvas ->
        ZzzPaint.textSize = 7f * u
        ZzzPaint.color = Color(0xFFFF3B3B).toArgb()
        canvas.nativeCanvas.drawText("AUSGANG", door.center.x, door.top - 11.5f * u, ZzzPaint)
    }
}

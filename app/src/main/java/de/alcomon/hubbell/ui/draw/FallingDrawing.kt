package de.alcomon.hubbell.ui.draw

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import de.alcomon.hubbell.game.FallingItem
import de.alcomon.hubbell.game.FallingKind
import kotlin.math.min
import kotlin.math.sin

private val FallingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    textAlign = Paint.Align.CENTER
    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
}

/** Zeichnet einen herabfallenden Gegenstand, leicht pendelnd und mit Glanz, damit man ihn antippt. */
fun DrawScope.drawFallingItem(item: FallingItem, time: Float) {
    val s = min(size.width, size.height) * 0.01f
    val c = Offset(item.x * size.width, item.y * size.height)
    val glow = 0.25f + 0.15f * sin(time * 8f + item.id)
    drawCircle(Color(0xFFFFE08A).copy(alpha = glow), 9f * s, c)
    val swing = if (item.kind == FallingKind.DEINE_MUDDA) sin(time * 3f + item.id) * 14f else item.y * item.spin * 4f
    rotate(swing, pivot = c) {
        when (item.kind) {
            FallingKind.BIERFLASCHE -> {
                drawRoundRect(Color(0xFF6D3B12), topLeft = c - Offset(2.6f * s, 3f * s), size = Size(5.2f * s, 10f * s), cornerRadius = CornerRadius(1.4f * s))
                drawRect(Color(0xFF6D3B12), topLeft = c - Offset(1f * s, 7.5f * s), size = Size(2f * s, 5f * s))
                drawRect(Color(0xFFD7263D), topLeft = c - Offset(1.2f * s, 8.4f * s), size = Size(2.4f * s, 1.2f * s))
                drawRect(Color(0xFFF2E6C9), topLeft = c - Offset(2.6f * s, -0.5f * s), size = Size(5.2f * s, 3.4f * s))
                drawLine(Color(0x66FFFFFF), c + Offset(-1.5f * s, -2.5f * s), c + Offset(-1.5f * s, 6f * s), strokeWidth = 0.5f * s)
            }
            FallingKind.TALERSACK -> {
                drawCircle(Color(0xFF8D6E63), 5.5f * s, c + Offset(0f, 1.5f * s))
                drawRect(Color(0xFF6D4C41), topLeft = c - Offset(2f * s, 5.5f * s), size = Size(4f * s, 2.5f * s))
                drawLine(Color(0xFFF2B32A), c - Offset(2.4f * s, 3.2f * s), c + Offset(2.4f * s, -3.2f * s), strokeWidth = 0.8f * s)
                drawCircle(Color(0xFFF2B32A), 2.2f * s, c + Offset(0f, 2f * s))
                drawCircle(Color(0xFFFFE08A), 1f * s, c + Offset(-0.5f * s, 1.5f * s))
            }
            FallingKind.PLEKTRUM -> {
                val pick = Path().apply {
                    moveTo(c.x - 5f * s, c.y - 4f * s)
                    quadraticBezierTo(c.x, c.y - 6.5f * s, c.x + 5f * s, c.y - 4f * s)
                    quadraticBezierTo(c.x + 4f * s, c.y + 2f * s, c.x, c.y + 6f * s)
                    quadraticBezierTo(c.x - 4f * s, c.y + 2f * s, c.x - 5f * s, c.y - 4f * s)
                    close()
                }
                drawPath(pick, Color(0xFFD7263D))
                drawPath(pick, Color(0xFF7A1010), style = Stroke(0.5f * s))
                drawSkull(c + Offset(0f, -1.5f * s), 1.8f * s, Color(0xFFF2EDE0), Color(0xFFD7263D))
            }
            FallingKind.DEINE_MUDDA -> {
                // kleines Holzschild an zwei Schnüren
                val top = c - Offset(0f, 9f * s)
                drawLine(Color(0xFFBCAAA4), top, c + Offset(-7f * s, -3f * s), strokeWidth = 0.4f * s)
                drawLine(Color(0xFFBCAAA4), top, c + Offset(7f * s, -3f * s), strokeWidth = 0.4f * s)
                drawRoundRect(Color(0xFFA1683A), topLeft = c - Offset(9f * s, 3f * s), size = Size(18f * s, 8f * s), cornerRadius = CornerRadius(1f * s))
                drawRoundRect(Color(0xFF5D3A1A), topLeft = c - Offset(9f * s, 3f * s), size = Size(18f * s, 8f * s), cornerRadius = CornerRadius(1f * s), style = Stroke(0.5f * s))
                drawIntoCanvas { canvas ->
                    FallingPaint.color = Color(0xFFFFF3D6).toArgb()
                    FallingPaint.textSize = 3.2f * s
                    canvas.nativeCanvas.drawText("Deine Mudda", c.x, c.y + 2.2f * s, FallingPaint)
                }
            }
        }
    }
}

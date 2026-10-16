package de.alcomon.hubbell.ui.draw

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import de.alcomon.hubbell.game.Drink

private val GlassFill = Color(0x40FFFFFF)
private val GlassEdge = Color(0xCCFFFFFF)
private val Foam = Color(0xFFFFFBF0)

/**
 * Zeichnet ein Getränk in die Box ab [topLeft] mit Breite [w] und Höhe [h].
 * [fill] ist der Füllstand von 0 (leer) bis 1 (voll).
 */
fun DrawScope.drawDrink(drink: Drink, topLeft: Offset, w: Float, h: Float, fill: Float) {
    val f = fill.coerceIn(0f, 1f)
    translate(topLeft.x, topLeft.y) {
        when (drink) {
            Drink.HELLES -> beerGlass(w, h, f, Color(0xFFF2B32A), Foam, bulge = false)
            Drink.WEIZEN -> wheatGlass(w, h, f)
            Drink.STOUT -> beerGlass(w, h, f, Color(0xFF2B1A12), Color(0xFFEAD9B8), bulge = true)
            Drink.WEIN -> wineGlass(w, h, f)
            Drink.WHISKY -> tumbler(w, h, f)
            Drink.SPEZIAL -> cocktail(w, h, f)
        }
    }
}

private fun DrawScope.glassOutline(path: Path, w: Float) {
    drawPath(path, GlassFill)
    drawPath(path, GlassEdge, style = Stroke(width = w * 0.06f))
}

private fun DrawScope.liquid(path: Path, top: Float, bottom: Float, f: Float, color: Color, w: Float): Float {
    val level = bottom - f * (bottom - top)
    if (f > 0.01f) {
        clipPath(path) {
            drawRect(color, topLeft = Offset(0f, level), size = Size(w, bottom - level + 1f))
        }
    }
    return level
}

private fun DrawScope.beerGlass(w: Float, h: Float, f: Float, beer: Color, foam: Color, bulge: Boolean) {
    val path = Path().apply {
        moveTo(w * 0.1f, 0f)
        lineTo(w * 0.9f, 0f)
        if (bulge) {
            lineTo(w * 0.86f, h * 0.18f)
            quadraticBezierTo(w * 0.95f, h * 0.28f, w * 0.85f, h * 0.38f)
        }
        lineTo(w * 0.78f, h)
        lineTo(w * 0.22f, h)
        if (bulge) {
            lineTo(w * 0.15f, h * 0.38f)
            quadraticBezierTo(w * 0.05f, h * 0.28f, w * 0.14f, h * 0.18f)
        }
        close()
    }
    val level = liquid(path, h * 0.14f, h, f, beer, w)
    if (f > 0.05f) {
        clipPath(path) {
            drawRect(foam, topLeft = Offset(0f, level - h * 0.02f), size = Size(w, h * 0.13f))
        }
        if (f > 0.92f) {
            drawCircle(foam, w * 0.17f, Offset(w * 0.3f, level))
            drawCircle(foam, w * 0.2f, Offset(w * 0.55f, level - h * 0.02f))
            drawCircle(foam, w * 0.15f, Offset(w * 0.78f, level + h * 0.01f))
        }
    }
    glassOutline(path, w)
    drawLine(Color(0x66FFFFFF), Offset(w * 0.25f, h * 0.15f), Offset(w * 0.3f, h * 0.85f), strokeWidth = w * 0.06f, cap = StrokeCap.Round)
}

private fun DrawScope.wheatGlass(w: Float, h: Float, f: Float) {
    val path = Path().apply {
        moveTo(w * 0.32f, h)
        lineTo(w * 0.68f, h)
        cubicTo(w * 0.76f, h * 0.72f, w * 0.6f, h * 0.55f, w * 0.88f, h * 0.15f)
        lineTo(w * 0.86f, 0f)
        lineTo(w * 0.14f, 0f)
        lineTo(w * 0.12f, h * 0.15f)
        cubicTo(w * 0.4f, h * 0.55f, w * 0.24f, h * 0.72f, w * 0.32f, h)
        close()
    }
    val level = liquid(path, h * 0.16f, h, f, Color(0xFFE8962C), w)
    if (f > 0.05f) {
        clipPath(path) {
            drawRect(Foam, topLeft = Offset(0f, level - h * 0.03f), size = Size(w, h * 0.16f))
        }
        if (f > 0.92f) {
            drawCircle(Foam, w * 0.2f, Offset(w * 0.32f, level - h * 0.01f))
            drawCircle(Foam, w * 0.24f, Offset(w * 0.58f, level - h * 0.04f))
            drawCircle(Foam, w * 0.16f, Offset(w * 0.8f, level))
        }
    }
    glassOutline(path, w)
}

private fun DrawScope.wineGlass(w: Float, h: Float, f: Float) {
    val bowlBottom = h * 0.55f
    val bowl = Path().apply {
        moveTo(w * 0.12f, 0f)
        lineTo(w * 0.88f, 0f)
        cubicTo(w * 0.95f, h * 0.42f, w * 0.68f, bowlBottom, w * 0.5f, bowlBottom)
        cubicTo(w * 0.32f, bowlBottom, w * 0.05f, h * 0.42f, w * 0.12f, 0f)
        close()
    }
    liquid(bowl, h * 0.18f, bowlBottom, f, Color(0xFF8E1B2E), w)
    glassOutline(bowl, w)
    drawLine(GlassEdge, Offset(w * 0.5f, bowlBottom), Offset(w * 0.5f, h * 0.94f), strokeWidth = w * 0.07f)
    drawOval(GlassEdge, topLeft = Offset(w * 0.2f, h * 0.9f), size = Size(w * 0.6f, h * 0.1f))
}

private fun DrawScope.tumbler(w: Float, h: Float, f: Float) {
    val top = h * 0.38f
    val path = Path().apply {
        moveTo(w * 0.06f, top)
        lineTo(w * 0.94f, top)
        lineTo(w * 0.88f, h)
        lineTo(w * 0.12f, h)
        close()
    }
    val level = liquid(path, top + h * 0.12f, h * 0.9f, f, Color(0xFFC77B1E), w)
    if (f > 0.3f) {
        rotate(18f, pivot = Offset(w * 0.55f, level + h * 0.04f)) {
            drawRoundRect(
                Color(0xAAE6F4FF),
                topLeft = Offset(w * 0.4f, level - h * 0.08f),
                size = Size(w * 0.3f, w * 0.3f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.05f),
            )
        }
    }
    glassOutline(path, w)
    drawRect(Color(0x66FFFFFF), topLeft = Offset(w * 0.12f, h * 0.9f), size = Size(w * 0.76f, h * 0.1f))
}

private fun DrawScope.cocktail(w: Float, h: Float, f: Float) {
    val cupTop = h * 0.12f
    val cupBottom = h * 0.6f
    val cup = Path().apply {
        moveTo(w * 0.02f, cupTop)
        lineTo(w * 0.98f, cupTop)
        lineTo(w * 0.5f, cupBottom)
        close()
    }
    val level = cupBottom - f * (cupBottom - cupTop - h * 0.04f)
    if (f > 0.01f) {
        clipPath(cup) {
            drawRect(
                Brush.verticalGradient(listOf(Color(0xFFFF5FA2), Color(0xFFFFB347)), startY = cupTop, endY = cupBottom),
                topLeft = Offset(0f, level),
                size = Size(w, cupBottom - level),
            )
        }
    }
    glassOutline(cup, w)
    drawLine(GlassEdge, Offset(w * 0.5f, cupBottom), Offset(w * 0.5f, h * 0.94f), strokeWidth = w * 0.06f)
    drawOval(GlassEdge, topLeft = Offset(w * 0.22f, h * 0.9f), size = Size(w * 0.56f, h * 0.1f))
    // Schirmchen und Kirsche
    drawLine(Color(0xFF8B5A2B), Offset(w * 0.62f, cupTop + h * 0.1f), Offset(w * 0.85f, 0f), strokeWidth = w * 0.04f)
    drawArc(
        Color(0xFF3FA7D6), startAngle = 180f, sweepAngle = 180f, useCenter = true,
        topLeft = Offset(w * 0.62f, -h * 0.06f), size = Size(w * 0.46f, h * 0.16f),
    )
    drawCircle(Color(0xFFD7263D), w * 0.09f, Offset(w * 0.32f, cupTop - w * 0.02f))
}

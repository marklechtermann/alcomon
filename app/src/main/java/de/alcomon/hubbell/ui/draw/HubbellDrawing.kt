package de.alcomon.hubbell.ui.draw

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import de.alcomon.hubbell.game.Drink
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private val Skin = Color(0xFFF5C9A0)
private val SkinShade = Color(0xFFE3AE80)
private val Jacket = Color(0xFF2F6B3A)
private val JacketDark = Color(0xFF245530)
private val Shirt = Color(0xFFFFF8EC)
private val Hair = Color(0xFF6B4423)
private val MustacheColor = Color(0xFF5A3418)
private val HatGreen = Color(0xFF3E5F2F)
private val HatBand = Color(0xFFB8322F)
private val Feather = Color(0xFFD9A441)
private val Ink = Color(0xFF3B2414)

/** Alles, was die Szene zum Zeichnen über Hubbells Zustand wissen muss. */
data class HubbellPose(
    val pegel: Float,
    val drinking: Drink?,
    val drinkProgress: Float,
    val nextDrink: Drink?,
    val time: Float,
    val alarm: Boolean = false,
)

/** Kneipe, Stuhl, Hubbell, Tisch und das nächste Getränk. */
fun DrawScope.drawHubbellScene(pose: HubbellPose) {
    drawPub(pose.time)
    val w = size.width
    val h = size.height
    val u = min(w / 100f, h / 118f)
    val cx = w / 2f
    val tableTop = h * 0.74f

    drawRoundRect(
        Color(0xFF4A2A14),
        topLeft = Offset(cx - 25f * u, tableTop - 52f * u),
        size = Size(50f * u, 54f * u),
        cornerRadius = CornerRadius(8f * u),
    )
    drawRoundRect(
        Color(0xFF3A200F),
        topLeft = Offset(cx - 20f * u, tableTop - 47f * u),
        size = Size(40f * u, 6f * u),
        cornerRadius = CornerRadius(3f * u),
    )

    drawHubbell(cx, tableTop, u, pose)
    drawTable(tableTop, u)

    val next = pose.nextDrink
    if (next != null && pose.drinking == null) {
        drawOval(Color(0xFFE8D2A8), topLeft = Offset(cx + 26f * u, tableTop + 0.5f * u), size = Size(15f * u, 3.5f * u))
        drawDrink(next, Offset(cx + 28.5f * u, tableTop - 15f * u), 10f * u, 16f * u, 1f)
    }

    if (pose.alarm) {
        val pulse = 0.35f + 0.25f * sin(pose.time * 8f)
        drawRect(
            Brush.radialGradient(
                listOf(Color.Transparent, Color(0xFFB8322F).copy(alpha = pulse)),
                center = Offset(w / 2f, h / 2f),
                radius = max(w, h) * 0.75f,
            ),
        )
    }
}

fun DrawScope.drawPub(time: Float) {
    val w = size.width
    val h = size.height
    drawRect(Brush.verticalGradient(listOf(Color(0xFF7A4A26), Color(0xFF4A2A14))))
    val plank = w / 7f
    for (i in 1..6) {
        drawLine(Color(0x33000000), Offset(i * plank, 0f), Offset(i * plank, h * 0.62f), strokeWidth = max(2f, w * 0.004f))
    }
    // Wandvertäfelung
    drawRect(Color(0xFF3A200F), topLeft = Offset(0f, h * 0.6f), size = Size(w, h * 0.4f))
    drawRect(Color(0xFF8A5A30), topLeft = Offset(0f, h * 0.6f), size = Size(w, h * 0.014f))

    // Regal mit Flaschen
    val shelfY = h * 0.34f
    val bottles = listOf(
        0.08f to Color(0xFF2E7D32), 0.14f to Color(0xFF8D5524), 0.2f to Color(0xFFC77B1E),
        0.27f to Color(0xFF1B5E20), 0.72f to Color(0xFF8E1B2E), 0.79f to Color(0xFFC77B1E),
        0.85f to Color(0xFF4E342E), 0.91f to Color(0xFF2E7D32),
    )
    val bw = w * 0.04f
    bottles.forEachIndexed { i, (x, color) ->
        val bh = h * (0.09f + (i % 3) * 0.015f)
        val left = x * w - bw / 2f
        drawRoundRect(color, topLeft = Offset(left, shelfY - bh), size = Size(bw, bh * 0.72f), cornerRadius = CornerRadius(bw * 0.25f))
        drawRect(color, topLeft = Offset(left + bw * 0.32f, shelfY - bh - bh * 0.05f), size = Size(bw * 0.36f, bh * 0.4f))
        drawRect(Color(0xFFFFF3D6).copy(alpha = 0.8f), topLeft = Offset(left + bw * 0.15f, shelfY - bh * 0.6f), size = Size(bw * 0.7f, bh * 0.22f))
        drawLine(Color(0x55FFFFFF), Offset(left + bw * 0.2f, shelfY - bh * 0.9f), Offset(left + bw * 0.2f, shelfY - bh * 0.35f), strokeWidth = bw * 0.12f)
    }
    drawRect(Color(0xFF2E1A0C), topLeft = Offset(w * 0.03f, shelfY), size = Size(w * 0.94f, h * 0.018f))

    // Bild mit Bergen
    val frameTop = Offset(w * 0.07f, h * 0.06f)
    val frameSize = Size(w * 0.2f, h * 0.15f)
    drawRect(Color(0xFFC9963B), topLeft = frameTop, size = frameSize)
    val inner = Offset(frameTop.x + w * 0.012f, frameTop.y + w * 0.012f)
    val innerSize = Size(frameSize.width - w * 0.024f, frameSize.height - w * 0.024f)
    drawRect(Color(0xFF9FD3E8), topLeft = inner, size = innerSize)
    val mountain = Path().apply {
        moveTo(inner.x, inner.y + innerSize.height)
        lineTo(inner.x + innerSize.width * 0.35f, inner.y + innerSize.height * 0.3f)
        lineTo(inner.x + innerSize.width * 0.55f, inner.y + innerSize.height * 0.6f)
        lineTo(inner.x + innerSize.width * 0.75f, inner.y + innerSize.height * 0.2f)
        lineTo(inner.x + innerSize.width, inner.y + innerSize.height)
        close()
    }
    drawPath(mountain, Color(0xFF5B7F4A))

    // Schild "PROST" rechts oben als Bierkrug-Silhouette
    val signC = Offset(w * 0.85f, h * 0.14f)
    drawCircle(Color(0xFF2E1A0C), w * 0.075f, signC)
    drawCircle(Color(0xFFC9963B), w * 0.068f, signC, style = Stroke(w * 0.008f))
    drawRoundRect(Color(0xFFF2B32A), topLeft = Offset(signC.x - w * 0.03f, signC.y - w * 0.025f), size = Size(w * 0.05f, w * 0.06f), cornerRadius = CornerRadius(w * 0.008f))
    drawCircle(Color(0xFFFFFBF0), w * 0.02f, Offset(signC.x - w * 0.012f, signC.y - w * 0.028f))
    drawCircle(Color(0xFFFFFBF0), w * 0.018f, Offset(signC.x + w * 0.012f, signC.y - w * 0.03f))
    drawArc(
        Color(0xFFF2B32A), startAngle = -90f, sweepAngle = 180f, useCenter = false,
        topLeft = Offset(signC.x + w * 0.008f, signC.y - w * 0.01f), size = Size(w * 0.03f, w * 0.03f),
        style = Stroke(w * 0.008f),
    )

    // Lampe mit warmem Licht
    val lamp = Offset(w * 0.5f, h * 0.1f)
    val flicker = 0.38f + 0.03f * sin(time * 7f)
    drawCircle(
        Brush.radialGradient(listOf(Color(0xFFFFD27A).copy(alpha = flicker), Color.Transparent), center = lamp, radius = w * 0.65f),
        radius = w * 0.65f,
        center = lamp,
    )
    drawLine(Color(0xFF1E120A), Offset(lamp.x, 0f), Offset(lamp.x, lamp.y - h * 0.02f), strokeWidth = max(2f, w * 0.006f))
    val shade = Path().apply {
        moveTo(lamp.x - w * 0.03f, lamp.y - h * 0.025f)
        lineTo(lamp.x + w * 0.03f, lamp.y - h * 0.025f)
        lineTo(lamp.x + w * 0.07f, lamp.y + h * 0.02f)
        lineTo(lamp.x - w * 0.07f, lamp.y + h * 0.02f)
        close()
    }
    drawPath(shade, Color(0xFF2F6B3A))
    drawCircle(Color(0xFFFFF1B8), w * 0.02f, Offset(lamp.x, lamp.y + h * 0.022f))
}

private fun DrawScope.drawTable(tableTop: Float, u: Float) {
    val w = size.width
    val h = size.height
    drawRect(Color(0xFF8B5A2B), topLeft = Offset(0f, tableTop), size = Size(w, h - tableTop))
    drawRect(Color(0xFFB07A42), topLeft = Offset(0f, tableTop), size = Size(w, 3f * u))
    drawRect(Color(0xFF6E431E), topLeft = Offset(0f, tableTop + 9f * u), size = Size(w, h - tableTop - 9f * u))
    for (i in 0..4) {
        val y = tableTop + 13f * u + i * 6f * u
        drawLine(Color(0x22000000), Offset(w * (0.05f + 0.1f * (i % 3)), y), Offset(w * (0.6f + 0.08f * i), y), strokeWidth = 0.8f * u)
    }
}

private fun smooth(x: Float): Float {
    val t = x.coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

/** Wie weit Hubbell das Glas zum Mund hebt (0 = auf dem Tisch, 1 = am Mund). */
private fun liftCurve(progress: Float): Float = when {
    progress < 0.2f -> smooth(progress / 0.2f)
    progress > 0.85f -> smooth((1f - progress) / 0.15f)
    else -> 1f
}

private fun DrawScope.drawHubbell(cx: Float, tableTop: Float, u: Float, pose: HubbellPose) {
    val p = (pose.pegel / 100f).coerceIn(0f, 1f)
    val sober = pose.pegel < 20f
    val angle = sin(pose.time * 1.6f) * (1f + p * 7f)

    rotate(angle, pivot = Offset(cx, tableTop + 8f * u)) {
        val headY = tableTop - 41f * u
        val headR = 15f * u

        // Körper mit Trachtenjanker
        drawOval(Jacket, topLeft = Offset(cx - 22f * u, tableTop - 28f * u), size = Size(44f * u, 44f * u))
        val shirt = Path().apply {
            moveTo(cx - 7f * u, tableTop - 27f * u)
            lineTo(cx + 7f * u, tableTop - 27f * u)
            lineTo(cx, tableTop - 11f * u)
            close()
        }
        drawPath(shirt, Shirt)
        val bowLeft = Path().apply {
            moveTo(cx, tableTop - 24f * u)
            lineTo(cx - 4.5f * u, tableTop - 27f * u)
            lineTo(cx - 4.5f * u, tableTop - 21f * u)
            close()
        }
        val bowRight = Path().apply {
            moveTo(cx, tableTop - 24f * u)
            lineTo(cx + 4.5f * u, tableTop - 27f * u)
            lineTo(cx + 4.5f * u, tableTop - 21f * u)
            close()
        }
        drawPath(bowLeft, HatBand)
        drawPath(bowRight, HatBand)
        for (i in 0..1) drawCircle(Color(0xFFC9963B), 1.1f * u, Offset(cx + 10f * u, tableTop - 18f * u + i * 6f * u))

        // Linker Arm liegt auf dem Tisch
        drawLine(JacketDark, Offset(cx - 18f * u, tableTop - 20f * u), Offset(cx - 24f * u, tableTop - 2f * u), strokeWidth = 8f * u, cap = StrokeCap.Round)
        drawCircle(Skin, 3.8f * u, Offset(cx - 24f * u, tableTop - 1.5f * u))

        // Ohren, Kopf, Haare
        drawCircle(Skin, 3.6f * u, Offset(cx - 15f * u, headY + 1f * u))
        drawCircle(Skin, 3.6f * u, Offset(cx + 15f * u, headY + 1f * u))
        drawCircle(Skin, headR, Offset(cx, headY))
        drawCircle(Hair, 4f * u, Offset(cx - 13f * u, headY - 4f * u))
        drawCircle(Hair, 4f * u, Offset(cx + 13f * u, headY - 4f * u))

        // Tirolerhut mit Feder
        val crown = Path().apply {
            moveTo(cx - 11f * u, headY - 11f * u)
            lineTo(cx - 8.5f * u, headY - 25f * u)
            quadraticBezierTo(cx, headY - 29f * u, cx + 8.5f * u, headY - 25f * u)
            lineTo(cx + 11f * u, headY - 11f * u)
            close()
        }
        drawPath(crown, HatGreen)
        drawLine(HatBand, Offset(cx - 10.5f * u, headY - 14f * u), Offset(cx + 10.5f * u, headY - 14f * u), strokeWidth = 3f * u)
        val feather = Path().apply {
            moveTo(cx + 8f * u, headY - 15f * u)
            quadraticBezierTo(cx + 22f * u, headY - 22f * u, cx + 19f * u, headY - 34f * u)
            quadraticBezierTo(cx + 12f * u, headY - 24f * u, cx + 8f * u, headY - 15f * u)
            close()
        }
        drawPath(feather, Feather)
        drawOval(HatGreen, topLeft = Offset(cx - 19f * u, headY - 14f * u), size = Size(38f * u, 6f * u))

        // Augenbrauen
        val eyeY = headY - 2f * u
        val ex = 5.5f * u
        for (sx in listOf(-1f, 1f)) {
            val inner = Offset(cx + sx * 2.8f * u, eyeY - if (sober) 6.5f * u else 5.2f * u)
            val outer = Offset(cx + sx * 8.2f * u, eyeY - if (sober) 4.5f * u else 5.6f * u)
            drawLine(Hair, inner, outer, strokeWidth = 1.4f * u, cap = StrokeCap.Round)
        }

        // Augen
        if (pose.drinking != null || pose.pegel >= 80f) {
            for (sx in listOf(-1f, 1f)) {
                drawArc(
                    Ink, startAngle = 180f, sweepAngle = 180f, useCenter = false,
                    topLeft = Offset(cx + sx * ex - 2.8f * u, eyeY - 1.5f * u), size = Size(5.6f * u, 4f * u),
                    style = Stroke(1.4f * u, cap = StrokeCap.Round),
                )
            }
        } else {
            val pupil = if (sober) 1.0f * u else 1.6f * u
            val wobble = sin(pose.time * 2.3f) * 0.7f * u * p
            for (sx in listOf(-1f, 1f)) {
                drawCircle(Color.White, 2.8f * u, Offset(cx + sx * ex, eyeY))
                drawCircle(Ink, pupil, Offset(cx + sx * ex + wobble, eyeY + 0.3f * u))
                if (pose.pegel >= 50f) {
                    drawArc(
                        SkinShade, startAngle = 180f, sweepAngle = 180f, useCenter = true,
                        topLeft = Offset(cx + sx * ex - 3f * u, eyeY - 3f * u), size = Size(6f * u, 6f * u),
                    )
                }
            }
        }

        // Rote Bäckchen – je höher der Pegel, desto röter
        val cheek = Color(0xFFE0565B).copy(alpha = 0.12f + 0.5f * p)
        drawCircle(cheek, 3.6f * u, Offset(cx - 9.5f * u, headY + 4f * u))
        drawCircle(cheek, 3.6f * u, Offset(cx + 9.5f * u, headY + 4f * u))

        // Knollennase
        drawCircle(lerp(SkinShade, Color(0xFFE0565B), p), 3.9f * u, Offset(cx, headY + 2f * u))
        drawCircle(Color(0x66FFFFFF), 1.1f * u, Offset(cx - 1.2f * u, headY + 0.8f * u))

        // Mund
        val mouthY = headY + 7.5f * u
        when {
            pose.drinking != null -> drawCircle(Ink, 1.8f * u, Offset(cx, mouthY + 1.5f * u))
            sober -> drawArc(
                Ink, startAngle = 180f, sweepAngle = 180f, useCenter = false,
                topLeft = Offset(cx - 4f * u, mouthY + 1.5f * u), size = Size(8f * u, 4f * u),
                style = Stroke(1.5f * u, cap = StrokeCap.Round),
            )
            else -> {
                val mw = 6f * u + 5f * u * p
                drawArc(
                    Ink, startAngle = 0f, sweepAngle = 180f, useCenter = p > 0.6f,
                    topLeft = Offset(cx - mw / 2f, mouthY - 1f * u), size = Size(mw, 3.5f * u + 2.5f * u * p),
                    style = if (p > 0.6f) androidx.compose.ui.graphics.drawscope.Fill else Stroke(1.5f * u, cap = StrokeCap.Round),
                )
            }
        }

        // Schnauzbart
        drawOval(MustacheColor, topLeft = Offset(cx - 8.5f * u, headY + 4.2f * u), size = Size(8.5f * u, 3.4f * u))
        drawOval(MustacheColor, topLeft = Offset(cx, headY + 4.2f * u), size = Size(8.5f * u, 3.4f * u))

        // Angstschweiß, wenn er nüchtern wird
        if (sober) {
            val drop = Offset(cx + 12f * u, headY - 8f * u + (pose.time * 6f % 4f) * u)
            drawCircle(Color(0xFF8FD3FF), 1.6f * u, drop)
        }

        // Rechter Arm mit Glas
        val shoulder = Offset(cx + 18f * u, tableTop - 20f * u)
        val rest = Offset(cx + 23f * u, tableTop - 1.5f * u)
        val mouth = Offset(cx + 7f * u, headY + 10f * u)
        val drink = pose.drinking
        val lift = if (drink != null) liftCurve(pose.drinkProgress) else 0f
        val hand = lerp(rest, mouth, lift)
        drawLine(JacketDark, shoulder, hand, strokeWidth = 8f * u, cap = StrokeCap.Round)
        if (drink != null) {
            val gw = 9f * u
            val gh = 14f * u
            rotate(-lift * 65f, pivot = hand) {
                drawDrink(drink, Offset(hand.x - gw * 0.5f, hand.y - gh * 0.75f), gw, gh, 1f - pose.drinkProgress)
            }
        }
        drawCircle(Skin, 3.8f * u, hand)
    }
}

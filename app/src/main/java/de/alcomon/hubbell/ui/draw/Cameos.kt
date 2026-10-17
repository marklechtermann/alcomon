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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import de.alcomon.hubbell.game.Drink
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/** Was ein Gast im Vordergrund in den Händen hält. */
enum class Prop { HORNS, WHISKY, BEER, DYNAMITE, RUM, DRONE, LAPTOP, COFFEE }

/** Ein Gast, der hin und wieder von unten ins Bild hüpft und etwas reinruft. */
class Cameo(
    val look: MetalGuy,
    val shout: String,
    val shirtText: String?,
    val left: Prop,
    val right: Prop,
    val slimFace: Boolean = false,
    val lady: Boolean = false,
    /** Kapuzenpulli und dicke Nerdbrille */
    val nerd: Boolean = false,
    /** Ruft so laut, dass die Sprechblase riesig wird */
    val loud: Boolean = false,
)

val Cameos = listOf(
    Cameo(
        MetalGuy("Whisky-Walter", Color(0xFFE8B98F), Color(0xFF4E342E), HairStyle.LONG, BeardStyle.FULL, Color(0xFF1C1C1F), belly = true),
        shout = "Ich mag Whisky!", shirtText = "DRAM CLAN", left = Prop.HORNS, right = Prop.WHISKY,
    ),
    Cameo(
        MetalGuy("Peter", Color(0xFFF2C6A0), Color(0xFF111111), HairStyle.HALF_BALD, BeardStyle.FULL, Color(0xFF263238)),
        shout = "Jetzt knallt's!", shirtText = "Ich bin der Peter", left = Prop.DYNAMITE, right = Prop.DYNAMITE,
    ),
    Cameo(
        MetalGuy("Langer Lars", Color(0xFFF5C9A0), Color(0xFF5D4037), HairStyle.LONG, BeardStyle.LONG, Color(0xFF3E2723), height = 1.15f),
        shout = "Rum und Drohnen, Baby!", shirtText = "Ich darf hier stehen", left = Prop.RUM, right = Prop.DRONE, slimFace = true,
    ),
    Cameo(
        MetalGuy("Metal-Mia", Color(0xFFF2C6A0), Color(0xFFB7410E), HairStyle.LONG, BeardStyle.NONE, Color(0xFF111111)),
        shout = "Hubbell, du Legende!", shirtText = "MOSH QUEEN", left = Prop.HORNS, right = Prop.BEER, lady = true,
    ),
    Cameo(
        MetalGuy("Kutten-Klaus", Color(0xFFD9A47A), Color(0xFF2ECC71), HairStyle.MOHAWK, BeardStyle.GOATEE, Color(0xFF111111), kutte = true),
        shout = "LAUTER!!!", shirtText = null, left = Prop.HORNS, right = Prop.HORNS,
    ),
    Cameo(
        MetalGuy("Dörte", Color(0xFFF5C9A0), Color(0xFFF2D16B), HairStyle.LONG, BeardStyle.NONE, Color(0xFF4A148C), shades = true),
        shout = "Noch ein Bier für den Mann!", shirtText = "HEAVY DÖRTE", left = Prop.BEER, right = Prop.HORNS, lady = true,
    ),
    Cameo(
        MetalGuy("Bodo", Color(0xFFF5F5F5), Color(0xFF0A0A0A), HairStyle.LONG, BeardStyle.NONE, Color(0xFF0A0A0A), corpsePaint = true),
        shout = "Evil Prost!", shirtText = "TRVE", left = Prop.HORNS, right = Prop.BEER,
    ),
    Cameo(
        MetalGuy("Oma Hilde", Color(0xFFE3AE80), Color(0xFFCFCFCF), HairStyle.CURLY, BeardStyle.NONE, Color(0xFF8E1B2E), shades = true),
        shout = "Rock on, Kinners!", shirtText = "WACKEN 1990", left = Prop.HORNS, right = Prop.BEER, lady = true,
    ),
    Cameo(
        MetalGuy("Zora", Color(0xFFA9714B), Color(0xFF2A1D16), HairStyle.PONYTAIL, BeardStyle.NONE, Color(0xFF1E3B1E)),
        shout = "Mosh for Hubbell!", shirtText = "\\m/", left = Prop.HORNS, right = Prop.HORNS, lady = true,
    ),
    Cameo(
        MetalGuy("Kevin", Color(0xFFF7D9BE), Color(0xFF6D4C41), HairStyle.CURLY, BeardStyle.NONE, Color(0xFF607D8B)),
        shout = "JÜRGEN!!!", shirtText = "works on my machine", left = Prop.LAPTOP, right = Prop.COFFEE, nerd = true, loud = true,
    ),
    Cameo(
        MetalGuy("Gabi", Color(0xFFF2C6A0), Color(0xFF1A1A1A), HairStyle.LONG, BeardStyle.NONE, Color(0xFF7B1F1F)),
        shout = "PETER!!!", shirtText = "WO IST PETER?", left = Prop.HORNS, right = Prop.BEER, lady = true, loud = true,
    ),
)

private const val CAMEO_PERIOD = 11f
private const val CAMEO_OFFSET = 5f
private const val CAMEO_LENGTH = 3.8f

private val ShirtPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    textAlign = Paint.Align.CENTER
    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
}
private val ShoutPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    textAlign = Paint.Align.CENTER
    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD_ITALIC)
}
private val LabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    textAlign = Paint.Align.CENTER
    typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
}

private fun easeOutBack(x: Float): Float {
    val t = x.coerceIn(0f, 1f) - 1f
    return 1f + t * t * (2.7f * t + 1.7f)
}

/** Ab und zu hüpft ein Gast von unten links oder rechts ins Bild. [u] ist die Szenen-Einheit. */
fun DrawScope.drawCameos(time: Float, u: Float) {
    val shifted = time - CAMEO_OFFSET
    if (shifted < 0f) return
    val k = floor(shifted / CAMEO_PERIOD).toInt()
    val t = shifted - k * CAMEO_PERIOD
    if (t > CAMEO_LENGTH) return
    // 7 ist teilerfremd zur Anzahl der Gäste, so kommt jeder mal dran
    val cameo = Cameos[((k * 7) % Cameos.size + Cameos.size) % Cameos.size]
    val fromLeft = k % 2 == 0
    val x = if (fromLeft) size.width * 0.17f else size.width * 0.83f

    val rise = when {
        t < 0.45f -> easeOutBack(t / 0.45f)
        t > CAMEO_LENGTH - 0.35f -> 1f - (t - (CAMEO_LENGTH - 0.35f)) / 0.35f
        else -> 1f
    }.coerceIn(-0.2f, 1.2f)
    val hop = if (t in 0.45f..(CAMEO_LENGTH - 0.35f)) abs(sin((t - 0.45f) * 7f)) * 3f * u else 0f
    val c = u * 1.1f * cameo.look.height
    val visible = 52f * c
    val base = size.height + visible * (1f - rise) + 10f * c - hop
    drawCameo(cameo, x, base, c, time, if (fromLeft) 1f else -1f, showShout = t > 0.35f && t < CAMEO_LENGTH - 0.3f)
}

private fun DrawScope.drawCameo(cameo: Cameo, x: Float, base: Float, c: Float, time: Float, towardCenter: Float, showShout: Boolean) {
    val look = cameo.look
    val torsoW = if (look.belly) 40f * c else 32f * c
    val shoulderY = base - 26f * c
    val headC = Offset(x, shoulderY - 9f * c)
    val headR = 9f * c
    val headW = if (cameo.slimFace) headR * 0.82f else headR

    // Kapuze hinter dem Kopf
    if (cameo.nerd) {
        drawOval(look.shirt, topLeft = Offset(x - headW * 1.45f, headC.y - headR * 1.3f), size = Size(headW * 2.9f, headR * 2.9f))
        drawOval(Color(0x33000000), topLeft = Offset(x - headW * 1.2f, headC.y - headR * 1.05f), size = Size(headW * 2.4f, headR * 2.4f))
    }
    // Haare hinter dem Kopf
    if (look.hairStyle == HairStyle.LONG || look.hairStyle == HairStyle.CURLY) {
        drawRoundRect(look.hair, topLeft = Offset(x - headW * 1.3f, headC.y - headR * 0.6f), size = Size(headW * 2.6f, headR * 2.6f), cornerRadius = CornerRadius(headR))
    }

    // Oberkörper und Shirt-Aufdruck
    drawRoundRect(look.shirt, topLeft = Offset(x - torsoW / 2f, shoulderY), size = Size(torsoW, 40f * c), cornerRadius = CornerRadius(10f * c))
    if (look.kutte) {
        val vest = Color(0xFF3F5A7A)
        drawRoundRect(vest, topLeft = Offset(x - torsoW / 2f, shoulderY), size = Size(torsoW * 0.3f, 40f * c), cornerRadius = CornerRadius(8f * c))
        drawRoundRect(vest, topLeft = Offset(x + torsoW * 0.2f, shoulderY), size = Size(torsoW * 0.3f, 40f * c), cornerRadius = CornerRadius(8f * c))
        drawCircle(Color(0xFFD7263D), 2f * c, Offset(x - torsoW * 0.35f, shoulderY + 8f * c))
        drawRect(Color(0xFFF2B32A), topLeft = Offset(x + torsoW * 0.28f, shoulderY + 7f * c), size = Size(4f * c, 4f * c))
    }
    if (cameo.nerd) {
        drawLine(Color(0xFFECEFF1), Offset(x - 3f * c, shoulderY + 1f * c), Offset(x - 3.5f * c, shoulderY + 8f * c), strokeWidth = 0.7f * c)
        drawLine(Color(0xFFECEFF1), Offset(x + 3f * c, shoulderY + 1f * c), Offset(x + 3.5f * c, shoulderY + 8f * c), strokeWidth = 0.7f * c)
        drawRoundRect(Color(0x33000000), topLeft = Offset(x - torsoW * 0.3f, shoulderY + 24f * c), size = Size(torsoW * 0.6f, 9f * c), cornerRadius = CornerRadius(3f * c))
    }
    val text = cameo.shirtText
    if (text != null) {
        drawIntoCanvas { canvas ->
            ShirtPaint.color = Color(0xFFF2EDE0).toArgb()
            ShirtPaint.textSize = 4.2f * c
            val lines = wrap(text, ShirtPaint, torsoW * 0.78f)
            lines.forEachIndexed { i, line ->
                canvas.nativeCanvas.drawText(line, x, shoulderY + 10f * c + i * 5f * c, ShirtPaint)
            }
        }
    } else {
        drawSkull(Offset(x, shoulderY + 10f * c), 4f * c, Color(0xFFE8E4DA), look.shirt)
    }

    // Kopf
    drawOval(look.skin, topLeft = Offset(x - headW, headC.y - headR), size = Size(headW * 2f, headR * 2f))
    val eyeY = headC.y - 1.5f * c
    if (look.corpsePaint) {
        drawCircle(Color.Black, 2.6f * c, Offset(x - 3.4f * c, eyeY))
        drawCircle(Color.Black, 2.6f * c, Offset(x + 3.4f * c, eyeY))
    }
    if (look.shades) {
        drawRoundRect(Color(0xFF111111), topLeft = Offset(x - 7f * c, eyeY - 2f * c), size = Size(6f * c, 3.6f * c), cornerRadius = CornerRadius(1f * c))
        drawRoundRect(Color(0xFF111111), topLeft = Offset(x + 1f * c, eyeY - 2f * c), size = Size(6f * c, 3.6f * c), cornerRadius = CornerRadius(1f * c))
        drawLine(Color(0xFF111111), Offset(x - 1f * c, eyeY - 1f * c), Offset(x + 1f * c, eyeY - 1f * c), strokeWidth = 0.8f * c)
    } else if (cameo.nerd) {
        // dicke Nerdbrille, dahinter weit aufgerissene Augen
        for (sx in listOf(-1f, 1f)) {
            val eye = Offset(x + sx * 3.6f * c, eyeY)
            drawCircle(Color.White, 2.6f * c, eye)
            drawCircle(Color(0xFF2B1A12), 1f * c, eye + Offset(-sx * 0.4f * c, 0f))
            drawCircle(Color(0xFF111111), 3f * c, eye, style = Stroke(1f * c))
        }
        drawLine(Color(0xFF111111), Offset(x - 0.6f * c, eyeY), Offset(x + 0.6f * c, eyeY), strokeWidth = 1f * c)
    } else {
        // vor Begeisterung zugekniffene Augen
        for (sx in listOf(-1f, 1f)) {
            drawArc(
                Color(0xFF2B1A12), startAngle = 180f, sweepAngle = 180f, useCenter = false,
                topLeft = Offset(x + sx * 3.4f * c - 2f * c, eyeY - 1f * c), size = Size(4f * c, 3f * c),
                style = Stroke(0.9f * c, cap = StrokeCap.Round),
            )
            if (cameo.lady) {
                drawLine(Color(0xFF2B1A12), Offset(x + sx * 5.2f * c, eyeY - 0.2f * c), Offset(x + sx * 6.4f * c, eyeY - 1.2f * c), strokeWidth = 0.6f * c)
            }
        }
    }
    drawCircle(Color(0xFFE0565B).copy(alpha = 0.35f), 2f * c, Offset(x - 5.5f * c, headC.y + 2f * c))
    drawCircle(Color(0xFFE0565B).copy(alpha = 0.35f), 2f * c, Offset(x + 5.5f * c, headC.y + 2f * c))

    when (look.beard) {
        BeardStyle.NONE -> Unit
        BeardStyle.GOATEE -> drawOval(look.hair, topLeft = Offset(x - 2.6f * c, headC.y + 5f * c), size = Size(5.2f * c, 7f * c))
        BeardStyle.FULL -> drawArc(
            look.hair, startAngle = 0f, sweepAngle = 180f, useCenter = true,
            topLeft = Offset(x - headW * 1.05f, headC.y - 3f * c), size = Size(headW * 2.1f, headR * 2.2f),
        )
        BeardStyle.LONG -> drawPath(
            Path().apply {
                moveTo(x - headW * 0.9f, headC.y + 1f * c)
                lineTo(x + headW * 0.9f, headC.y + 1f * c)
                lineTo(x + 3f * c, headC.y + 19f * c)
                lineTo(x - 3f * c, headC.y + 19f * c)
                close()
            },
            look.hair,
        )
    }
    if (look.beard == BeardStyle.FULL) {
        drawOval(look.hair, topLeft = Offset(x - 5f * c, headC.y + 1.2f * c), size = Size(10f * c, 2.6f * c))
    }
    // Weit aufgerissener Mund beim Reinrufen
    val shoutOpen = 0.7f + 0.3f * abs(sin(time * 12f))
    drawOval(Color(0xFF2B1A12), topLeft = Offset(x - 3f * c, headC.y + 2.5f * c), size = Size(6f * c, 5f * c * shoutOpen))
    drawOval(Color(0xFFD7263D), topLeft = Offset(x - 1.8f * c, headC.y + 2.5f * c + 2.8f * c * shoutOpen), size = Size(3.6f * c, 1.8f * c * shoutOpen))
    if (cameo.lady && !look.corpsePaint) {
        drawOval(Color(0xFF8E1B2E), topLeft = Offset(x - 3.2f * c, headC.y + 2.2f * c), size = Size(6.4f * c, 5.4f * c * shoutOpen), style = Stroke(0.8f * c))
    }

    // Frisur
    when (look.hairStyle) {
        HairStyle.LONG, HairStyle.PONYTAIL -> drawArc(
            look.hair, startAngle = 180f, sweepAngle = 180f, useCenter = true,
            topLeft = Offset(x - headW * 1.05f, headC.y - headR * 1.1f), size = Size(headW * 2.1f, headR * 1.3f),
        )
        HairStyle.CURLY -> for (i in -2..2) {
            drawCircle(look.hair, 3.6f * c, Offset(x + i * 3.6f * c, headC.y - headR * 0.85f + abs(i) * 1.2f * c))
        }
        HairStyle.MOHAWK -> for (i in -2..2) {
            val bx = x + i * 2.4f * c
            val by = headC.y - headR * 0.85f + abs(i) * c
            drawPath(
                Path().apply {
                    moveTo(bx - 1.5f * c, by)
                    lineTo(bx + i * 0.8f * c, by - 9f * c + abs(i) * 1.5f * c)
                    lineTo(bx + 1.5f * c, by)
                    close()
                },
                look.hair,
            )
        }
        HairStyle.BALD -> drawCircle(Color(0x55FFFFFF), 2.2f * c, Offset(x - 3f * c, headC.y - headR * 0.6f))
        HairStyle.HALF_BALD -> {
            // Halbglatze wie bei Friedrich Merz, nur schwarz: hohe Stirn mit Geheimratsecken,
            // kurzes Haar an den Seiten und ein schmaler, zurückgewichener Streifen oben
            val top = headC.y - headR
            for (side in listOf(-1f, 1f)) {
                drawPath(
                    Path().apply {
                        moveTo(x + side * headW * 1.02f, headC.y + 2f * c)
                        lineTo(x + side * headW * 1.08f, headC.y - headR * 0.35f)
                        quadraticBezierTo(x + side * headW * 1.02f, headC.y - headR * 0.85f, x + side * headW * 0.62f, top + headR * 0.22f)
                        lineTo(x + side * headW * 0.5f, top + headR * 0.4f)
                        quadraticBezierTo(x + side * headW * 0.8f, headC.y - headR * 0.5f, x + side * headW * 0.82f, headC.y + 2f * c)
                        close()
                    },
                    look.hair,
                )
            }
            // dünner Haarstreifen ganz oben, der nach hinten gekämmt ist
            drawPath(
                Path().apply {
                    moveTo(x - headW * 0.62f, top + headR * 0.22f)
                    quadraticBezierTo(x, top - headR * 0.12f, x + headW * 0.62f, top + headR * 0.22f)
                    lineTo(x + headW * 0.35f, top + headR * 0.2f)
                    quadraticBezierTo(x, top + headR * 0.32f, x - headW * 0.35f, top + headR * 0.2f)
                    close()
                },
                look.hair,
            )
            // glänzende Stirn
            drawOval(Color(0x66FFFFFF), topLeft = Offset(x - 3.5f * c, top + headR * 0.35f), size = Size(5f * c, 2.6f * c))
        }
    }
    if (look.hairStyle == HairStyle.PONYTAIL) {
        drawOval(look.hair, topLeft = Offset(x + headW * 0.7f, headC.y - 2f * c), size = Size(4f * c, 12f * c))
    }

    // Arme hoch und die Hände voll
    val wave = sin(time * 8f) * 2f * c
    val leftShoulder = Offset(x - torsoW / 2f + 3f * c, shoulderY + 4f * c)
    val rightShoulder = Offset(x + torsoW / 2f - 3f * c, shoulderY + 4f * c)
    val leftHand = Offset(x - torsoW / 2f - 6f * c + wave, shoulderY - 14f * c)
    val rightHand = Offset(x + torsoW / 2f + 6f * c - wave, shoulderY - 14f * c)
    val sleeve = if (look.kutte) Color(0xFF3F5A7A) else look.shirt
    for ((shoulder, hand) in listOf(leftShoulder to leftHand, rightShoulder to rightHand)) {
        drawLine(look.skin, shoulder, hand, strokeWidth = 5.5f * c, cap = StrokeCap.Round)
        drawLine(sleeve, shoulder, shoulder + (hand - shoulder) * 0.3f, strokeWidth = 7f * c, cap = StrokeCap.Round)
    }
    drawProp(cameo.left, leftHand, c, time, look.skin, -1f)
    drawProp(cameo.right, rightHand, c, time, look.skin, 1f)

    if (showShout) {
        if (cameo.loud) {
            // So laut, dass die Blase bebt
            val shake = sin(time * 60f) * 0.8f * c
            drawShout(cameo.shout, Offset(x + towardCenter * 26f * c + shake, headC.y - 22f * c), c * 1.9f, towardCenter, Color(0xFFD7263D))
        } else {
            drawShout(cameo.shout, Offset(x + towardCenter * 22f * c, headC.y - 20f * c), c, towardCenter)
        }
    }
}

private fun wrap(text: String, paint: Paint, maxWidth: Float): List<String> {
    if (paint.measureText(text) <= maxWidth) return listOf(text)
    val words = text.split(" ")
    val lines = mutableListOf<String>()
    var current = ""
    for (word in words) {
        val candidate = if (current.isEmpty()) word else "$current $word"
        if (paint.measureText(candidate) > maxWidth && current.isNotEmpty()) {
            lines += current
            current = word
        } else {
            current = candidate
        }
    }
    if (current.isNotEmpty()) lines += current
    return lines
}

private fun DrawScope.drawShout(text: String, anchor: Offset, c: Float, towardCenter: Float, ink: Color = Color(0xFF1A1A1A)) {
    drawIntoCanvas { canvas ->
        ShoutPaint.textSize = 4.6f * c
        ShoutPaint.color = ink.toArgb()
        val lines = wrap(text, ShoutPaint, 44f * c)
        val width = lines.maxOf { ShoutPaint.measureText(it) } + 6f * c
        val height = lines.size * 5.6f * c + 4f * c
        // Sprechblase so verschieben, dass sie nicht aus dem Bild ragt
        val cx = (anchor.x).coerceIn(width / 2f + 4f, size.width - width / 2f - 4f)
        val top = anchor.y - height
        val tail = Path().apply {
            moveTo(cx - towardCenter * width * 0.25f, top + height - 1f)
            lineTo(cx - towardCenter * width * 0.4f, top + height + 5f * c)
            lineTo(cx - towardCenter * width * 0.05f, top + height - 1f)
            close()
        }
        drawPath(tail, Color.White)
        drawRoundRect(Color.White, topLeft = Offset(cx - width / 2f, top), size = Size(width, height), cornerRadius = CornerRadius(3f * c))
        drawRoundRect(Color(0xFF1A1A1A), topLeft = Offset(cx - width / 2f, top), size = Size(width, height), cornerRadius = CornerRadius(3f * c), style = Stroke(0.6f * c))
        lines.forEachIndexed { i, line ->
            canvas.nativeCanvas.drawText(line, cx, top + 5.6f * c + i * 5.6f * c, ShoutPaint)
        }
    }
}

private fun DrawScope.drawProp(prop: Prop, hand: Offset, c: Float, time: Float, skin: Color, side: Float) {
    when (prop) {
        Prop.HORNS -> {
            drawLine(skin, hand + Offset(-1.6f * c, -1f * c), hand + Offset(-2.8f * c, -8f * c), strokeWidth = 1.8f * c, cap = StrokeCap.Round)
            drawLine(skin, hand + Offset(1.8f * c, -1f * c), hand + Offset(3.2f * c, -7f * c), strokeWidth = 1.8f * c, cap = StrokeCap.Round)
            drawCircle(skin, 3.4f * c, hand)
            drawLine(Color(0x55000000), hand + Offset(-1.8f * c, 0.6f * c), hand + Offset(1.8f * c, 0.6f * c), strokeWidth = 0.7f * c)
        }
        Prop.WHISKY -> {
            drawDrink(Drink.WHISKY, hand + Offset(-4f * c, -9f * c), 8f * c, 10f * c, 1f)
            drawCircle(skin, 3.2f * c, hand)
        }
        Prop.BEER -> {
            drawDrink(Drink.HELLES, hand + Offset(-4f * c, -13f * c), 8f * c, 13f * c, 1f)
            drawCircle(skin, 3.2f * c, hand)
        }
        Prop.DYNAMITE -> {
            val tilt = side * (12f + sin(time * 6f) * 6f)
            rotate(tilt, pivot = hand) {
                drawRoundRect(Color(0xFFC62828), topLeft = Offset(hand.x - 1.8f * c, hand.y - 10f * c), size = Size(3.6f * c, 13f * c), cornerRadius = CornerRadius(0.8f * c))
                drawRect(Color(0xFF8E1B2E), topLeft = Offset(hand.x - 1.8f * c, hand.y - 6.5f * c), size = Size(3.6f * c, 1f * c))
                val fuseTop = Offset(hand.x + 1.5f * c, hand.y - 13.5f * c)
                drawLine(Color(0xFF5D4037), Offset(hand.x, hand.y - 10f * c), fuseTop, strokeWidth = 0.5f * c)
                // knisternder Funke an der Lunte
                for (i in 0..4) {
                    val a = time * 25f + i * 1.3f
                    val r = (0.6f + abs(sin(a)) * 1.4f) * c
                    drawLine(
                        if (i % 2 == 0) Color(0xFFFFD27A) else Color(0xFFFF7043),
                        fuseTop,
                        fuseTop + Offset(cos(a) * r * 1.5f, sin(a) * r * 1.5f),
                        strokeWidth = 0.4f * c,
                    )
                }
                drawCircle(Color(0xFFFFF3B0), 0.7f * c, fuseTop)
            }
            drawCircle(skin, 3.2f * c, hand)
        }
        Prop.RUM -> {
            rotate(side * -15f, pivot = hand) {
                drawRoundRect(Color(0xFF5D3A1A), topLeft = Offset(hand.x - 2.6f * c, hand.y - 12f * c), size = Size(5.2f * c, 10f * c), cornerRadius = CornerRadius(1.5f * c))
                drawRect(Color(0xFF5D3A1A), topLeft = Offset(hand.x - 0.9f * c, hand.y - 16f * c), size = Size(1.8f * c, 4.5f * c))
                drawRect(Color(0xFFB71C1C), topLeft = Offset(hand.x - 1f * c, hand.y - 16.8f * c), size = Size(2f * c, 1.2f * c))
                drawRect(Color(0xFFF2E6C9), topLeft = Offset(hand.x - 2.2f * c, hand.y - 9.5f * c), size = Size(4.4f * c, 4f * c))
                drawIntoCanvas { canvas ->
                    LabelPaint.color = Color(0xFF3B1010).toArgb()
                    LabelPaint.textSize = 2.4f * c
                    canvas.nativeCanvas.drawText("RUM", hand.x, hand.y - 6.6f * c, LabelPaint)
                }
            }
            drawCircle(skin, 3.2f * c, hand)
        }
        Prop.LAPTOP -> {
            // Aufgeklappter Laptop mit rotem Fehlerbildschirm
            rotate(side * 8f + sin(time * 5f) * 4f, pivot = hand) {
                val screen = Offset(hand.x - 8f * c, hand.y - 13f * c)
                drawRoundRect(Color(0xFF37474F), topLeft = screen, size = Size(16f * c, 11f * c), cornerRadius = CornerRadius(1f * c))
                val blink = if (sin(time * 9f) > 0f) Color(0xFFD32F2F) else Color(0xFF8E1B1B)
                drawRect(blink, topLeft = screen + Offset(1f * c, 1f * c), size = Size(14f * c, 9f * c))
                drawRect(Color(0xFF263238), topLeft = Offset(hand.x - 9f * c, hand.y - 2f * c), size = Size(18f * c, 2f * c))
                drawIntoCanvas { canvas ->
                    LabelPaint.color = Color.White.toArgb()
                    LabelPaint.textSize = 2.6f * c
                    canvas.nativeCanvas.drawText("ERROR", hand.x, hand.y - 8.5f * c, LabelPaint)
                    LabelPaint.textSize = 1.8f * c
                    canvas.nativeCanvas.drawText("NullPointer", hand.x, hand.y - 5.6f * c, LabelPaint)
                }
            }
            drawCircle(skin, 3.2f * c, hand)
        }
        Prop.COFFEE -> {
            val mug = Offset(hand.x - 3.2f * c, hand.y - 9f * c)
            drawRoundRect(Color(0xFFF5F5F5), topLeft = mug, size = Size(6.4f * c, 8f * c), cornerRadius = CornerRadius(1f * c))
            drawRect(Color(0xFF4E342E), topLeft = mug + Offset(0.6f * c, 0.6f * c), size = Size(5.2f * c, 1.2f * c))
            drawCircle(Color(0xFFF5F5F5), 2f * c, mug + Offset(7f * c, 4f * c), style = Stroke(1f * c))
            for (i in 0..1) {
                val p = (time * 0.8f + i * 0.5f) % 1f
                drawCircle(Color(0x88FFFFFF), (0.8f + p) * c, mug + Offset(2f * c + i * 2.4f * c + sin(time * 4f + i) * c, -1f * c - p * 6f * c))
            }
            drawCircle(skin, 3.2f * c, hand)
        }
        Prop.DRONE -> {
            // Fernbedienung in der Hand, die Drohne wirbelt um ihn herum
            drawRoundRect(Color(0xFF424242), topLeft = Offset(hand.x - 3f * c, hand.y - 3.5f * c), size = Size(6f * c, 3.5f * c), cornerRadius = CornerRadius(1f * c))
            drawLine(Color(0xFF9E9E9E), hand + Offset(2f * c, -3.5f * c), hand + Offset(3f * c, -7f * c), strokeWidth = 0.5f * c)
            drawCircle(skin, 3f * c, hand)
            val a = time * 3.2f
            val drone = hand + Offset(cos(a) * 14f * c - side * 6f * c, sin(a) * 6f * c - 14f * c)
            drawRoundRect(Color(0xFF263238), topLeft = drone - Offset(3f * c, 1.2f * c), size = Size(6f * c, 2.4f * c), cornerRadius = CornerRadius(1f * c))
            drawCircle(Color(0xFFFF5252).copy(alpha = if (sin(time * 10f) > 0f) 1f else 0.3f), 0.6f * c, drone)
            for (sx in listOf(-1f, 1f)) {
                val rotor = drone + Offset(sx * 4.5f * c, -1.6f * c)
                drawLine(Color(0xFF263238), drone + Offset(sx * 2.5f * c, 0f), rotor, strokeWidth = 0.6f * c)
                val spin = abs(sin(time * 40f + sx))
                drawOval(Color(0x99B0BEC5), topLeft = rotor - Offset(3.2f * c * spin + 0.5f * c, 0.5f * c), size = Size(6.4f * c * spin + 1f * c, 1f * c))
            }
        }
    }
}

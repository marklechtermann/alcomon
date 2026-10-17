package de.alcomon.hubbell.ui.draw

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import de.alcomon.hubbell.game.Drink
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private val Skin = Color(0xFFF5C9A0)
private val SkinShade = Color(0xFFE3AE80)
private val Shirt = Color(0xFF1C1C1F)
private val ShirtPrint = Color(0xFFE8E4DA)
private val Hair = Color(0xFF151515)
private val HairLight = Color(0xFF353535)
private val Beard = Color(0xFF1B1B1B)
private val Wristband = Color(0xFF111111)
private val Stud = Color(0xFFB0B4BA)
private val Ink = Color(0xFF2B1A12)
private val DevilRed = Color(0xFFD7263D)

/** Alles, was die Szene zum Zeichnen über Hubbells Zustand wissen muss. */
data class HubbellPose(
    val pegel: Float,
    val drinking: Drink?,
    val drinkProgress: Float,
    val nextDrink: Drink?,
    val time: Float,
    val alarm: Boolean = false,
    /** Wie lange die aktuelle Runde schon läuft; Hubbells Gesicht verändert sich damit. */
    val runSeconds: Float = 0f,
    /** Wie lange er das Streicheln noch genießt. */
    val petGlow: Float = 0f,
    val startled: Boolean = false,
    val dizzy: Boolean = false,
    /** Peters Foto für das Poster an der Wand */
    val photo: ImageBitmap? = null,
)

/** Wo Hubbells Kopf sitzt (für Streicheln und Würfe). */
fun hubbellHead(width: Float, height: Float): Pair<Offset, Float> {
    val u = min(width / 100f, height / 118f)
    return Offset(width / 2f, height * 0.74f - 41f * u) to u
}

private fun rotateAround(p: Offset, pivot: Offset, degrees: Float): Offset {
    val a = degrees * PI.toFloat() / 180f
    val d = p - pivot
    return pivot + Offset(d.x * kotlin.math.cos(a) - d.y * sin(a), d.x * sin(a) + d.y * kotlin.math.cos(a))
}

/** Kneipe, Stuhl, Hubbell, Tisch und das nächste Getränk. */
fun DrawScope.drawHubbellScene(pose: HubbellPose) {
    drawMetalBar(pose.time, pose.photo)
    val w = size.width
    val h = size.height
    val u = min(w / 100f, h / 118f)
    val cx = w / 2f
    val tableTop = h * 0.74f

    drawRoundRect(
        Color(0xFF2B2B30),
        topLeft = Offset(cx - 25f * u, tableTop - 52f * u),
        size = Size(50f * u, 54f * u),
        cornerRadius = CornerRadius(8f * u),
    )
    drawRoundRect(
        Color(0xFF1C1C20),
        topLeft = Offset(cx - 20f * u, tableTop - 47f * u),
        size = Size(40f * u, 6f * u),
        cornerRadius = CornerRadius(3f * u),
    )

    // Ab und zu fliegt etwas auf ihn: dann kippt er vom Stuhl oder erschrickt
    val knock = knockAt(pose.time)
    val fall = knock?.fall ?: 0f
    val startle = knock?.startle ?: 0f
    val fallPivot = Offset(cx - 24f * u, tableTop + 10f * u)
    val fallAngle = -78f * fall
    val lift = -7f * u * startle + if (startle > 0f) sin(pose.time * 70f) * 0.7f * u * startle else 0f
    val drop = 16f * u * fall
    translate(0f, drop + lift) {
        rotate(fallAngle, pivot = fallPivot) {
            drawHubbell(cx, tableTop, u, pose.copy(startled = startle > 0.3f, dizzy = fall > 0.6f))
        }
    }
    drawTable(tableTop, u)

    val next = pose.nextDrink
    if (next != null && pose.drinking == null) {
        drawOval(Color(0xFFE8D2A8), topLeft = Offset(cx + 26f * u, tableTop + 0.5f * u), size = Size(15f * u, 3.5f * u))
        drawDrink(next, Offset(cx + 28.5f * u, tableTop - 15f * u), 10f * u, 16f * u, 1f)
    }

    // Ab und zu hat Hubbell Hunger und baut sich einen Döner
    if (knock == null) drawDoenerBreak(pose.time, cx, tableTop, u, Skin)

    // Gäste, die ab und zu von unten ins Bild hüpfen
    drawCameos(pose.time, u)
    // Jockel surft ab und zu über die Menge
    drawCrowdsurf(pose.time, u)

    if (knock != null) {
        val seated = Offset(cx, tableTop - 41f * u)
        val now = rotateAround(seated, fallPivot, fallAngle) + Offset(0f, drop + lift)
        drawThrownObject(knock, seated, u)
        drawKnockReaction(knock, now, u, pose.time)
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

/** Dunkle Metal-Bar mit Backsteinwand, schiefen Bandpostern und rotem Neonlicht. */
fun DrawScope.drawMetalBar(time: Float, photo: ImageBitmap? = null) {
    val w = size.width
    val h = size.height
    drawRect(Brush.verticalGradient(listOf(Color(0xFF2A2326), Color(0xFF151214))))

    // Backsteine
    val brickH = h * 0.045f
    val brickW = w * 0.14f
    val mortar = Color(0x33000000)
    var row = 0
    var y = 0f
    while (y < h * 0.62f) {
        drawLine(mortar, Offset(0f, y), Offset(w, y), strokeWidth = max(1.5f, h * 0.003f))
        var x = if (row % 2 == 0) 0f else -brickW / 2f
        while (x < w) {
            drawLine(mortar, Offset(x, y), Offset(x, y + brickH), strokeWidth = max(1.5f, h * 0.003f))
            if ((row * 7 + (x / brickW).toInt() * 3) % 5 == 0) {
                drawRect(Color(0x14FFFFFF), topLeft = Offset(x + 2f, y + 2f), size = Size(brickW - 4f, brickH - 4f))
            }
            x += brickW
        }
        y += brickH
        row++
    }

    // Rotes Neonlicht von oben
    val glow = 0.3f + 0.04f * sin(time * 9f) + if (sin(time * 1.3f) > 0.97f) -0.15f else 0f
    drawCircle(
        Brush.radialGradient(listOf(Color(0xFFFF3B3B).copy(alpha = glow), Color.Transparent), center = Offset(w * 0.5f, 0f), radius = w * 0.8f),
        radius = w * 0.8f,
        center = Offset(w * 0.5f, 0f),
    )


    // Regal mit Flaschen
    val shelfY = h * 0.34f
    val bottles = listOf(
        0.08f to Color(0xFF2E7D32), 0.14f to Color(0xFF8D5524), 0.2f to Color(0xFFC77B1E),
        0.8f to Color(0xFF4E342E), 0.86f to Color(0xFFC77B1E), 0.92f to Color(0xFF1B5E20),
    )
    val bw = w * 0.04f
    bottles.forEachIndexed { i, (x, color) ->
        val bh = h * (0.09f + (i % 3) * 0.015f)
        val left = x * w - bw / 2f
        drawRoundRect(color, topLeft = Offset(left, shelfY - bh), size = Size(bw, bh * 0.72f), cornerRadius = CornerRadius(bw * 0.25f))
        drawRect(color, topLeft = Offset(left + bw * 0.32f, shelfY - bh - bh * 0.05f), size = Size(bw * 0.36f, bh * 0.4f))
        drawRect(Color(0xFF111111), topLeft = Offset(left + bw * 0.15f, shelfY - bh * 0.6f), size = Size(bw * 0.7f, bh * 0.22f))
        drawLine(Color(0x55FFFFFF), Offset(left + bw * 0.2f, shelfY - bh * 0.9f), Offset(left + bw * 0.2f, shelfY - bh * 0.35f), strokeWidth = bw * 0.12f)
    }
    drawRect(Color(0xFF3A3A40), topLeft = Offset(w * 0.03f, shelfY), size = Size(w * 0.94f, h * 0.018f))

    // Poster, die ständig von wechselnden Metallern ausgetauscht werden
    drawPosterWall(time, floorY = h * 0.6f, photo = photo)

    // Theke unten
    drawRect(Color(0xFF1A1416), topLeft = Offset(0f, h * 0.6f), size = Size(w, h * 0.4f))
    drawRect(Color(0xFF5C5F66), topLeft = Offset(0f, h * 0.6f), size = Size(w, h * 0.012f))

    // Neon-Schild
    drawIntoCanvas { canvas ->
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD_ITALIC)
            textSize = w * 0.07f
            color = Color(0xFFFF6B6B).toArgb()
            setShadowLayer(w * 0.025f, 0f, 0f, Color(0xFFFF2020).toArgb())
        }
        canvas.nativeCanvas.drawText("METAL BAR", w * 0.5f, h * 0.17f, paint)
    }
}

internal fun DrawScope.drawSkull(c: Offset, r: Float, bone: Color, holes: Color) {
    drawCircle(bone, r, c)
    drawRoundRect(bone, topLeft = Offset(c.x - r * 0.55f, c.y + r * 0.5f), size = Size(r * 1.1f, r * 0.75f), cornerRadius = CornerRadius(r * 0.2f))
    drawCircle(holes, r * 0.28f, Offset(c.x - r * 0.4f, c.y + r * 0.05f))
    drawCircle(holes, r * 0.28f, Offset(c.x + r * 0.4f, c.y + r * 0.05f))
    val nose = Path().apply {
        moveTo(c.x, c.y + r * 0.3f)
        lineTo(c.x - r * 0.12f, c.y + r * 0.55f)
        lineTo(c.x + r * 0.12f, c.y + r * 0.55f)
        close()
    }
    drawPath(nose, holes)
    for (i in -1..1) {
        drawLine(holes, Offset(c.x + i * r * 0.25f, c.y + r * 0.8f), Offset(c.x + i * r * 0.25f, c.y + r * 1.2f), strokeWidth = r * 0.08f)
    }
}

private fun DrawScope.drawTable(tableTop: Float, u: Float) {
    val w = size.width
    val h = size.height
    drawRect(Color(0xFF4A2E1A), topLeft = Offset(0f, tableTop), size = Size(w, h - tableTop))
    drawRect(Color(0xFF6E4728), topLeft = Offset(0f, tableTop), size = Size(w, 3f * u))
    drawRect(Color(0xFF3A2312), topLeft = Offset(0f, tableTop + 9f * u), size = Size(w, h - tableTop - 9f * u))
    for (i in 0..4) {
        val y = tableTop + 13f * u + i * 6f * u
        drawLine(Color(0x22000000), Offset(w * (0.05f + 0.1f * (i % 3)), y), Offset(w * (0.6f + 0.08f * i), y), strokeWidth = 0.8f * u)
    }
    // eingeritzte Pommesgabel im Tisch
    val m = Offset(w * 0.15f, tableTop + 6f * u)
    drawLine(Color(0x44000000), m, m + Offset(-1.5f * u, -3f * u), strokeWidth = 0.7f * u)
    drawLine(Color(0x44000000), m + Offset(3f * u, 0f), m + Offset(4.5f * u, -3f * u), strokeWidth = 0.7f * u)
    drawLine(Color(0x44000000), m + Offset(-1f * u, 0.5f * u), m + Offset(4f * u, 0.5f * u), strokeWidth = 0.7f * u)
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

/** Ein- und Ausblenden einer Aktion, die von [start] bis [start] + [length] im Zyklus läuft. */
private fun envelope(phase: Float, start: Float, length: Float): Float {
    val local = phase - start
    if (local < 0f || local > length) return 0f
    val edge = 0.25f
    return min(smooth(local / edge), smooth((length - local) / edge))
}

private const val CYCLE = 10f
private const val HEADBANG_START = 3f
private const val HEADBANG_LENGTH = 1.8f
private const val HORNS_START = 6.5f
private const val HORNS_LENGTH = 2.2f

private fun DrawScope.drawHubbell(cx: Float, tableTop: Float, u: Float, pose: HubbellPose) {
    val p = (pose.pegel / 100f).coerceIn(0f, 1f)
    val sober = pose.pegel < 20f
    val idle = pose.drinking == null && !sober
    val phase = pose.time % CYCLE
    val bang = if (idle) envelope(phase, HEADBANG_START, HEADBANG_LENGTH) else 0f
    val horns = if (idle) envelope(phase, HORNS_START, HORNS_LENGTH) else 0f
    val rocking = bang > 0.05f || horns > 0.3f
    val angle = sin(pose.time * 1.6f) * (1f + p * 7f)

    rotate(angle, pivot = Offset(cx, tableTop + 8f * u)) {
        val headY = tableTop - 41f * u

        // Körper mit Metal-Shirt
        drawOval(Shirt, topLeft = Offset(cx - 22f * u, tableTop - 28f * u), size = Size(44f * u, 44f * u))
        drawArc(
            Color(0xFF3A3A40), startAngle = 0f, sweepAngle = 180f, useCenter = false,
            topLeft = Offset(cx - 6f * u, tableTop - 30f * u), size = Size(12f * u, 6f * u),
            style = Stroke(1.2f * u),
        )
        // Verschlungenes, völlig unleserliches Death-Metal-Bandlogo auf dem Shirt
        drawDeathMetalLogo(Offset(cx, tableTop - 18f * u), 30f * u, ShirtPrint, DevilRed)

        // Kopf (beim Headbangen nickt er kräftig)
        val bob = abs(sin(pose.time * 14f)) * 4f * u * bang
        val headAngle = sin(pose.time * 14f) * 12f * bang
        val neck = Offset(cx, headY + 14f * u)
        translate(0f, bob) {
            rotate(headAngle, pivot = neck) {
                drawHead(cx, headY, u, pose, p, sober, rocking, bang)
            }
        }

        // Linker Arm: liegt auf dem Tisch oder zeigt die Pommesgabel
        val leftShoulder = Offset(cx - 18f * u, tableTop - 20f * u)
        val leftRest = Offset(cx - 24f * u, tableTop - 1.5f * u)
        val wave = sin(pose.time * 6f) * 2f * u * horns
        val leftUp = Offset(cx - 31f * u + wave, headY - 10f * u)
        val leftHand = lerp(leftRest, leftUp, horns)
        drawArm(leftShoulder, leftHand, u)
        if (horns > 0.5f) drawHornsHand(leftHand, u) else drawCircle(Skin, 3.8f * u, leftHand)

        // Rechter Arm mit Glas
        val shoulder = Offset(cx + 18f * u, tableTop - 20f * u)
        val rest = Offset(cx + 23f * u, tableTop - 1.5f * u)
        val mouth = Offset(cx + 7f * u, headY + 10f * u)
        val drink = pose.drinking
        val lift = if (drink != null) liftCurve(pose.drinkProgress) else 0f
        val hand = lerp(rest, mouth, lift)
        drawArm(shoulder, hand, u)
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

private fun DrawScope.drawHeart(c: Offset, r: Float, color: Color) {
    drawCircle(color, r * 0.55f, c + Offset(-r * 0.45f, 0f))
    drawCircle(color, r * 0.55f, c + Offset(r * 0.45f, 0f))
    val tip = Path().apply {
        moveTo(c.x - r * 0.98f, c.y + r * 0.15f)
        lineTo(c.x + r * 0.98f, c.y + r * 0.15f)
        lineTo(c.x, c.y + r * 1.1f)
        close()
    }
    drawPath(tip, color)
}

/** Kurzer schwarzer Ärmel, nackter Unterarm und Nietenarmband. */
private fun DrawScope.drawArm(shoulder: Offset, hand: Offset, u: Float) {
    val sleeveEnd = lerp(shoulder, hand, 0.3f)
    drawLine(Skin, shoulder, hand, strokeWidth = 6.5f * u, cap = StrokeCap.Round)
    drawLine(Shirt, shoulder, sleeveEnd, strokeWidth = 8.5f * u, cap = StrokeCap.Round)
    val bandStart = lerp(shoulder, hand, 0.78f)
    val bandEnd = lerp(shoulder, hand, 0.9f)
    drawLine(Wristband, bandStart, bandEnd, strokeWidth = 7f * u, cap = StrokeCap.Butt)
    val mid = lerp(bandStart, bandEnd, 0.5f)
    drawCircle(Stud, 0.9f * u, mid + Offset(-1.8f * u, 0f))
    drawCircle(Stud, 0.9f * u, mid + Offset(1.8f * u, 0f))
}

/** Die Pommesgabel: Faust mit ausgestrecktem Zeige- und kleinem Finger. */
private fun DrawScope.drawHornsHand(hand: Offset, u: Float) {
    val finger = 1.9f * u
    drawLine(Skin, hand + Offset(-1.8f * u, -1f * u), hand + Offset(-3f * u, -9f * u), strokeWidth = finger, cap = StrokeCap.Round)
    drawLine(Skin, hand + Offset(2f * u, -1f * u), hand + Offset(3.6f * u, -8f * u), strokeWidth = finger, cap = StrokeCap.Round)
    drawCircle(Skin, 3.8f * u, hand)
    drawLine(SkinShade, hand + Offset(-2f * u, 0.5f * u), hand + Offset(2f * u, 0.5f * u), strokeWidth = 1.4f * u, cap = StrokeCap.Round)
    drawLine(SkinShade, hand + Offset(-1.2f * u, -1.5f * u), hand + Offset(1.6f * u, -1.5f * u), strokeWidth = 0.8f * u, cap = StrokeCap.Round)
}

private fun DrawScope.drawHead(
    cx: Float, headY: Float, u: Float, pose: HubbellPose,
    p: Float, sober: Boolean, rocking: Boolean, bang: Float,
) {
    val headR = 15f * u
    // Lange Mähne: beim Headbangen fliegen die Strähnen
    val flick = sin(pose.time * 14f + 1f) * 3f * u * bang
    drawRoundRect(Hair, topLeft = Offset(cx - 18.5f * u, headY - 12f * u), size = Size(37f * u, 22f * u), cornerRadius = CornerRadius(10f * u))
    for (side in listOf(-1f, 1f)) {
        val strand = Path().apply {
            val inner = cx + side * 9f * u
            val outer = cx + side * 19.5f * u
            moveTo(inner, headY)
            lineTo(outer, headY - 4f * u)
            quadraticBezierTo(outer + side * 2f * u + flick, headY + 18f * u, outer + side * 1f * u + flick * 1.5f, headY + 31f * u)
            lineTo(outer - side * 3f * u + flick * 1.5f, headY + 28f * u)
            lineTo(outer - side * 6f * u + flick, headY + 32f * u)
            quadraticBezierTo(inner + side * 1f * u, headY + 20f * u, inner, headY)
            close()
        }
        drawPath(strand, Hair)
        drawLine(HairLight, Offset(cx + side * 15f * u, headY + 2f * u), Offset(cx + side * 16.5f * u + flick, headY + 24f * u), strokeWidth = 0.8f * u)
    }

    drawCircle(Skin, headR, Offset(cx, headY))

    // Haaransatz mit Pony-Zacken
    drawArc(
        Hair, startAngle = 180f, sweepAngle = 180f, useCenter = true,
        topLeft = Offset(cx - 16f * u, headY - 17f * u), size = Size(32f * u, 18f * u),
    )
    val fringe = Path().apply {
        moveTo(cx - 14f * u, headY - 8.5f * u)
        lineTo(cx - 11f * u, headY - 5.5f * u)
        lineTo(cx - 8f * u, headY - 8.5f * u)
        lineTo(cx - 4f * u, headY - 6.5f * u)
        lineTo(cx, headY - 8.5f * u)
        lineTo(cx + 5f * u, headY - 6f * u)
        lineTo(cx + 9f * u, headY - 8.5f * u)
        lineTo(cx + 12f * u, headY - 6f * u)
        lineTo(cx + 14.5f * u, headY - 8.5f * u)
        close()
    }
    drawPath(fringe, Hair)

    // Augenbrauen
    val eyeY = headY - 2f * u
    val ex = 5.5f * u
    for (sx in listOf(-1f, 1f)) {
        val inner = Offset(cx + sx * 2.8f * u, eyeY - if (sober) 5.5f * u else 4.3f * u)
        val outer = Offset(cx + sx * 8.2f * u, eyeY - if (sober) 3.8f * u else 4.8f * u)
        drawLine(Hair, inner, outer, strokeWidth = 1.5f * u, cap = StrokeCap.Round)
    }

    // Augen
    if (pose.dizzy) {
        for (sx in listOf(-1f, 1f)) {
            val e = Offset(cx + sx * ex, eyeY)
            drawLine(Ink, e + Offset(-2f * u, -2f * u), e + Offset(2f * u, 2f * u), strokeWidth = 1.3f * u, cap = StrokeCap.Round)
            drawLine(Ink, e + Offset(-2f * u, 2f * u), e + Offset(2f * u, -2f * u), strokeWidth = 1.3f * u, cap = StrokeCap.Round)
        }
    } else if (pose.startled) {
        for (sx in listOf(-1f, 1f)) {
            drawCircle(Color.White, 3.6f * u, Offset(cx + sx * ex, eyeY - 0.5f * u))
            drawCircle(Ink, 0.8f * u, Offset(cx + sx * ex, eyeY - 0.5f * u))
        }
    } else if (pose.drinking != null || pose.pegel >= 80f || rocking || pose.petGlow > 0f) {
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

    // Je länger die Runde läuft, desto mehr sieht man sie ihm an
    val age = (pose.runSeconds / 600f).coerceIn(0f, 1f)
    if (pose.runSeconds > 120f) {
        for (sx in listOf(-1f, 1f)) {
            drawArc(
                Color(0x557A4E2D), startAngle = 20f, sweepAngle = 140f, useCenter = false,
                topLeft = Offset(cx + sx * ex - 2.6f * u, eyeY + 1.2f * u), size = Size(5.2f * u, 2.6f * u),
                style = Stroke(0.7f * u, cap = StrokeCap.Round),
            )
        }
    }
    if (pose.runSeconds > 60f) {
        // Bartstoppeln wandern die Wangen hoch
        val stubble = ((pose.runSeconds - 60f) / 300f).coerceIn(0f, 1f)
        for (i in 0 until (6 + 14 * stubble).toInt()) {
            val sx = if (i % 2 == 0) -1f else 1f
            val row = i / 2
            drawCircle(Beard.copy(alpha = 0.55f), 0.35f * u, Offset(cx + sx * (8f + (row % 3) * 1.6f) * u, headY + (6.5f - row * 0.5f) * u))
        }
    }

    // Rote Bäckchen – je höher der Pegel, desto röter
    val cheek = Color(0xFFE0565B).copy(alpha = 0.12f + 0.5f * p + if (pose.petGlow > 0f) 0.25f else 0f)
    drawCircle(cheek, 3.4f * u, Offset(cx - 9.5f * u, headY + 3.5f * u))
    drawCircle(cheek, 3.4f * u, Offset(cx + 9.5f * u, headY + 3.5f * u))

    // Fusseliger Kinnbart
    val chin = listOf(-7f to 9.5f, -4.5f to 12f, -1.5f to 13.5f, 1.5f to 13.5f, 4.5f to 12f, 7f to 9.5f)
    chin.forEach { (dx, dy) -> drawCircle(Beard, 3.1f * u * (1f + 0.35f * age), Offset(cx + dx * u, headY + (dy + 1.5f * age) * u)) }
    val goatee = Path().apply {
        moveTo(cx - 4f * u, headY + 14f * u)
        lineTo(cx + 4f * u, headY + 14f * u)
        lineTo(cx + 1f * u, headY + (21f + 7f * age) * u)
        lineTo(cx - 1.5f * u, headY + (20f + 7f * age) * u)
        close()
    }
    drawPath(goatee, Beard)
    val fuzz = listOf(-9f to 9f, -7f to 13f, -3f to 16f, 3f to 16f, 7f to 13f, 9f to 9f, 0f to 21f)
    fuzz.forEachIndexed { i, (dx, dy) ->
        val base = Offset(cx + dx * u, headY + dy * u)
        val dir = Offset(dx * 0.12f * u + (if (i % 2 == 0) 0.6f else -0.6f) * u, 1.8f * u)
        drawLine(Beard, base, base + dir, strokeWidth = 0.7f * u, cap = StrokeCap.Round)
    }

    // Knollennase
    drawCircle(lerp(SkinShade, Color(0xFFE0565B), max(p, age * 0.8f)), 3.9f * u * (1f + 0.25f * age), Offset(cx, headY + 2f * u))
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
            val open = rocking || p > 0.6f
            val mw = 6f * u + 5f * u * p + if (rocking) 2f * u else 0f
            val mh = 3.5f * u + 2.5f * u * p + if (rocking) 2f * u else 0f
            drawArc(
                Ink, startAngle = 0f, sweepAngle = 180f, useCenter = open,
                topLeft = Offset(cx - mw / 2f, mouthY - 1f * u), size = Size(mw, mh),
                style = if (open) Fill else Stroke(1.5f * u, cap = StrokeCap.Round),
            )
            if (open) {
                drawArc(
                    DevilRed.copy(alpha = 0.8f), startAngle = 20f, sweepAngle = 140f, useCenter = true,
                    topLeft = Offset(cx - mw * 0.3f, mouthY + mh * 0.25f), size = Size(mw * 0.6f, mh * 0.6f),
                )
            }
        }
    }

    // Schnauzer, etwas zerzaust
    drawOval(Beard, topLeft = Offset(cx - 8f * u, headY + 4.4f * u), size = Size(8f * u, 3f * u))
    drawOval(Beard, topLeft = Offset(cx, headY + 4.4f * u), size = Size(8f * u, 3f * u))
    drawLine(Beard, Offset(cx - 7.5f * u, headY + 6.5f * u), Offset(cx - 9f * u, headY + 8.5f * u), strokeWidth = 1f * u, cap = StrokeCap.Round)
    drawLine(Beard, Offset(cx + 7.5f * u, headY + 6.5f * u), Offset(cx + 9f * u, headY + 8.5f * u), strokeWidth = 1f * u, cap = StrokeCap.Round)

    // Nach einer Weile steht die Mähne wild ab, später klebt ein Pflaster auf der Stirn
    if (pose.runSeconds > 240f) {
        val wild = ((pose.runSeconds - 240f) / 240f).coerceIn(0f, 1f)
        for (i in -2..2) {
            val root = Offset(cx + i * 4f * u, headY - 15f * u + abs(i) * 1.2f * u)
            drawLine(Hair, root, root + Offset(i * 1.5f * u, -(2f + 3f * wild) * u), strokeWidth = 1.2f * u, cap = StrokeCap.Round)
        }
    }
    if (pose.runSeconds > 480f) {
        rotate(-25f, pivot = Offset(cx + 6f * u, headY - 7f * u)) {
            drawRoundRect(Color(0xFFE8C9A0), topLeft = Offset(cx + 2f * u, headY - 8f * u), size = Size(8f * u, 2.4f * u), cornerRadius = CornerRadius(1f * u))
            drawRect(Color(0xFFF5E6D0), topLeft = Offset(cx + 5f * u, headY - 8f * u), size = Size(2f * u, 2.4f * u))
        }
    }

    // Herzchen, wenn er gestreichelt wird
    if (pose.petGlow > 0f) {
        val rise = 1f - (pose.petGlow / 1.5f).coerceIn(0f, 1f)
        for (i in 0..2) {
            val hx = cx + (i - 1) * 9f * u + sin(pose.time * 3f + i) * 1.5f * u
            val hy = headY - 22f * u - rise * 12f * u - i * 2f * u
            drawHeart(Offset(hx, hy), 2.4f * u, Color(0xFFFF4D6D).copy(alpha = (pose.petGlow / 0.5f).coerceIn(0f, 1f)))
        }
    }

    // Angstschweiß, wenn er nüchtern wird
    if (sober) {
        val drop = Offset(cx + 12f * u, headY - 8f * u + (pose.time * 6f % 4f) * u)
        drawCircle(Color(0xFF8FD3FF), 1.6f * u, drop)
    }

    // Sternchen beim Headbangen
    if (bang > 0.3f) {
        val spin = pose.time * 4f
        for (i in 0..2) {
            val a = spin + i * 2f * PI.toFloat() / 3f
            val star = Offset(cx + kotlin.math.cos(a) * 20f * u, headY - 18f * u + sin(a) * 4f * u)
            drawCircle(Color(0xFFF2B32A).copy(alpha = bang), 1.3f * u, star)
        }
    }
}

package de.alcomon.hubbell.ui.draw

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

// ---------------------------------------------------------------------------------------------
// Die 50 Poster
// ---------------------------------------------------------------------------------------------

private val BandNames = listOf(
    "BIERZERKER", "IRON LEBER", "HOPFENTOD", "THRASH TRESEN", "MALZFEUER",
    "DOOM DOPPELBOCK", "SCHAUMKRONE", "STOUT RITUAL", "KORKENKRIEGER", "HEFEHÖLLE",
    "WEIZENWOLF", "GRIMM & GERSTE", "PILSPANZER", "KRACHKELLER", "FASSBRECHER",
    "DONNERDRAM", "RIFFKÖNIG", "SÄGEZAHN", "NIETENGNOM", "KAPUTTE KUTTE",
    "LUFTGITARRE", "MOSHPIT MÖNCHE", "VERSTÄRKERHÖLLE", "BIERDECKEL OF DOOM", "TRESENTROLLE",
    "GROWL & GRÖHL", "METWALL", "SCHÄDELBRAUEREI", "KREISSÄGE", "BOCKBIER BATAILLON",
    "ROSTIGE RIFFS", "DRACHENDURST", "HAMMERFASS", "STAHLKRUG", "MITTERNACHTSMALZ",
    "FUSSELBART", "DUNKELWEIZEN DÄMON", "HEADBANG HANNES", "BLITZBIER", "ORKAN OKTOBER",
    "KNOCHENBRECHER", "ZWERGENZORN", "WÜTENDE WIKINGER", "SCHWERMETALL SCHORSCH", "FRITTENGABEL",
    "PECHSCHWARZ", "GEISTERBRAU", "HÖLLENHOPFEN", "MAMMUTMOSH", "DER LETZTE SCHLUCK",
)

private val Taglines = listOf(
    "LIVE!", "WORLD TOUR", "NEUES ALBUM", "AUSVERKAUFT", "HEUTE ABEND",
    "OPEN AIR", "ABSCHIEDSTOUR", "LAUT!", "EINTRITT FREI", "FREIBIER*",
)

private val PosterBackgrounds = listOf(
    Color(0xFF1B1B1B), Color(0xFF7A1010), Color(0xFF2C3E50), Color(0xFF3B2A12),
    Color(0xFF1E3B1E), Color(0xFF4A148C), Color(0xFFD9C9A3), Color(0xFF0D2B45),
)
private val PosterInks = listOf(
    Color(0xFFE0E0E0), Color(0xFFFFD27A), Color(0xFF9FD3E8), Color(0xFFF2B32A),
    Color(0xFFFF5252), Color(0xFFB9F6CA),
)

class PosterDesign(
    val band: String,
    val tagline: String,
    val background: Color,
    val ink: Color,
    val motif: Int,
    val font: Int,
)

/** 50 erfundene Metal-Bands, jede mit eigenem Motiv, Farben und Schrift. */
val PosterDesigns: List<PosterDesign> = BandNames.mapIndexed { i, name ->
    var bg = PosterBackgrounds[(i * 3) % PosterBackgrounds.size]
    var ink = PosterInks[(i * 5 + i / 8) % PosterInks.size]
    // helles Papier braucht dunkle Schrift
    if (bg == Color(0xFFD9C9A3)) ink = Color(0xFF3B1010)
    if (bg == ink) bg = Color(0xFF1B1B1B)
    PosterDesign(
        band = name,
        tagline = Taglines[(i * 7) % Taglines.size],
        background = bg,
        ink = ink,
        motif = (i + i / 8) % 8,
        font = i % 3,
    )
}

// ---------------------------------------------------------------------------------------------
// Die 10 Metaller, die die Poster tauschen
// ---------------------------------------------------------------------------------------------

enum class HairStyle { LONG, MOHAWK, BALD, HALF_BALD, PONYTAIL, CURLY }
enum class BeardStyle { NONE, LONG, GOATEE, FULL }

class MetalGuy(
    val name: String,
    val skin: Color,
    val hair: Color,
    val hairStyle: HairStyle,
    val beard: BeardStyle,
    val shirt: Color,
    val kutte: Boolean = false,
    val shades: Boolean = false,
    val helmet: Boolean = false,
    val bandana: Color? = null,
    val corpsePaint: Boolean = false,
    val belly: Boolean = false,
    val height: Float = 1f,
)

val MetalGuys = listOf(
    MetalGuy("Lemmy-Lutz", Color(0xFFE8B98F), Color(0xFF1A1A1A), HairStyle.LONG, BeardStyle.FULL, Color(0xFF111111), kutte = true),
    MetalGuy("Wikinger-Waldemar", Color(0xFFF2C6A0), Color(0xFFD9822B), HairStyle.LONG, BeardStyle.LONG, Color(0xFF5B3A1E), helmet = true, belly = true, height = 1.05f),
    MetalGuy("Iro-Ingo", Color(0xFFF5C9A0), Color(0xFF2ECC71), HairStyle.MOHAWK, BeardStyle.NONE, Color(0xFF8E1B2E), shades = true),
    MetalGuy("Glatzen-Günther", Color(0xFFD9A47A), Color(0xFF3B2414), HairStyle.BALD, BeardStyle.GOATEE, Color(0xFF263238), kutte = true, belly = true),
    MetalGuy("Corpsepaint-Kalle", Color(0xFFF5F5F5), Color(0xFF0A0A0A), HairStyle.LONG, BeardStyle.NONE, Color(0xFF0A0A0A), corpsePaint = true, height = 1.08f),
    MetalGuy("Zopf-Zora", Color(0xFFA9714B), Color(0xFF2A1D16), HairStyle.PONYTAIL, BeardStyle.NONE, Color(0xFF4A148C), shades = true, height = 0.95f),
    MetalGuy("Locken-Lothar", Color(0xFFF5C9A0), Color(0xFF8D5524), HairStyle.CURLY, BeardStyle.FULL, Color(0xFF1E3B1E), bandana = Color(0xFFD7263D)),
    MetalGuy("Bandana-Bernd", Color(0xFF7A4E2D), Color(0xFF111111), HairStyle.LONG, BeardStyle.GOATEE, Color(0xFF37474F), bandana = Color(0xFF1565C0), shades = true),
    MetalGuy("Rote-Mähne-Rita", Color(0xFFF2C6A0), Color(0xFFB7410E), HairStyle.LONG, BeardStyle.NONE, Color(0xFF111111), kutte = true, height = 0.97f),
    MetalGuy("Opa-Otto", Color(0xFFE3AE80), Color(0xFFBDBDBD), HairStyle.LONG, BeardStyle.LONG, Color(0xFF5D4037), belly = true, shades = true),
)

// ---------------------------------------------------------------------------------------------
// Wand mit vier Plätzen und dem Tausch-Ablauf
// ---------------------------------------------------------------------------------------------

private class Slot(val x: Float, val y: Float, val w: Float, val h: Float, val tilt: Float)

private val Slots = listOf(
    Slot(0.04f, 0.05f, 0.24f, 0.2f, -9f),
    Slot(0.7f, 0.04f, 0.26f, 0.18f, 7f),
    Slot(0.02f, 0.41f, 0.2f, 0.14f, 12f),
    Slot(0.77f, 0.42f, 0.21f, 0.13f, -6f),
)

private const val SWAP_PERIOD = 8f
private const val WALK_IN_END = 2.6f
private const val TAKE_DOWN_END = 3.4f
private const val HANG_END = 4.6f
private const val WALK_OUT_END = 7.4f

private fun floorMod(a: Int, b: Int): Int = ((a % b) + b) % b

/** Welches Poster beim Tausch Nummer [k] neu aufgehängt wird. 13 ist teilerfremd zu 50,
 *  dadurch hängen nie zwei gleiche Poster gleichzeitig und alle 50 kommen dran. */
private fun posterForSwap(k: Int): PosterDesign = PosterDesigns[floorMod(k * 13 + 7, PosterDesigns.size)]

private fun guyForSwap(k: Int): MetalGuy = MetalGuys[floorMod(k * 3, MetalGuys.size)]

private fun slotForSwap(k: Int): Int = floorMod(k, Slots.size)

private fun smoothStep(x: Float): Float {
    val t = x.coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

/**
 * Zeichnet die Posterwand: Alle paar Sekunden läuft ein anderer Metaller herein,
 * nimmt ein Poster ab, hängt ein neues auf und verschwindet mit dem alten unterm Arm.
 * [floorY] ist die Höhe der Theke, hinter der die Beine verschwinden.
 */
fun DrawScope.drawPosterWall(time: Float, floorY: Float) {
    val k = floor(time / SWAP_PERIOD).toInt()
    val phase = time - k * SWAP_PERIOD
    val activeSlot = slotForSwap(k)

    for (s in Slots.indices) {
        // letzter abgeschlossener Tausch an diesem Platz vor dem aktuellen Tausch
        val previous = k - floorMod(k - s, Slots.size).let { if (it == 0) Slots.size else it }
        if (s == activeSlot) {
            val old = posterForSwap(k - Slots.size)
            val new = posterForSwap(k)
            when {
                phase < WALK_IN_END -> drawPoster(old, Slots[s], time, 1f)
                phase < TAKE_DOWN_END -> Unit // hängt gerade in den Händen
                phase < HANG_END -> drawPoster(new, Slots[s], time, smoothStep((phase - TAKE_DOWN_END) / (HANG_END - TAKE_DOWN_END)))
                else -> drawPoster(new, Slots[s], time, 1f)
            }
        } else {
            drawPoster(posterForSwap(previous), Slots[s], time, 1f)
        }
    }

    if (phase < WALK_OUT_END) drawSwap(k, phase, time, floorY)
}

private fun DrawScope.slotRect(slot: Slot): Pair<Offset, Size> =
    Offset(slot.x * size.width, slot.y * size.height) to Size(slot.w * size.width, slot.h * size.height)

private fun DrawScope.drawSwap(k: Int, phase: Float, time: Float, floorY: Float) {
    val w = size.width
    val h = size.height
    val slot = Slots[slotForSwap(k)]
    val guy = guyForSwap(k)
    val (topLeft, posterSize) = slotRect(slot)
    val targetX = topLeft.x + posterSize.width / 2f
    val fromLeft = targetX < w / 2f
    val startX = if (fromLeft) -0.15f * w else 1.15f * w
    val endX = if (fromLeft) 1.15f * w else -0.15f * w

    val walking: Boolean
    val x = when {
        phase < WALK_IN_END -> {
            walking = true
            startX + (targetX - startX) * smoothStep(phase / WALK_IN_END * 1.15f)
        }
        phase < HANG_END -> {
            walking = false
            targetX
        }
        else -> {
            walking = true
            targetX + (endX - targetX) * smoothStep((phase - HANG_END) / (WALK_OUT_END - HANG_END))
        }
    }

    val posterBottom = topLeft.y + posterSize.height
    val reachY = when {
        phase < WALK_IN_END -> null
        phase < TAKE_DOWN_END -> {
            val t = (phase - WALK_IN_END) / (TAKE_DOWN_END - WALK_IN_END)
            // erst greifen, dann das alte Poster runterziehen
            if (t < 0.4f) posterBottom else posterBottom + (t - 0.4f) / 0.6f * h * 0.08f
        }
        phase < HANG_END -> {
            val t = smoothStep((phase - TAKE_DOWN_END) / (HANG_END - TAKE_DOWN_END))
            topLeft.y + posterSize.height * t
        }
        else -> null
    }

    val carrying = when {
        phase < WALK_IN_END -> posterForSwap(k)
        phase < TAKE_DOWN_END -> posterForSwap(k)
        else -> posterForSwap(k - Slots.size)
    }

    drawMetalGuy(guy, x, floorY, time, walking, reachY, carrying)

    // Das alte Poster zwischen Wand und Händen
    if (phase >= WALK_IN_END && phase < TAKE_DOWN_END) {
        val t = (phase - WALK_IN_END) / (TAKE_DOWN_END - WALK_IN_END)
        val drop = if (t < 0.4f) 0f else (t - 0.4f) / 0.6f
        val old = posterForSwap(k - Slots.size)
        val shifted = Slot(slot.x, slot.y + drop * 0.08f, slot.w, slot.h, slot.tilt * (1f - drop))
        drawPoster(old, shifted, time, 1f - drop * 0.6f, fromBottom = drop > 0f)
    }
}

/** Ein Metaller von vorn. [reachY] hebt beide Hände auf diese Höhe; [carrying] ist die Rolle unterm Arm. */
private fun DrawScope.drawMetalGuy(
    guy: MetalGuy,
    x: Float,
    floorY: Float,
    time: Float,
    walking: Boolean,
    reachY: Float?,
    carrying: PosterDesign?,
) {
    val g = size.height * 0.0105f * guy.height
    val step = if (walking) sin(time * 9f) else 0f
    val bob = if (walking) abs(step) * 0.8f * g else 0f
    // auf Zehenspitzen, wenn es hoch hinaus geht
    val tiptoe = if (reachY != null && reachY < floorY - 36f * g) min(4f * g, (floorY - 36f * g - reachY) * 0.3f) else 0f
    val feet = floorY + 2f * g - bob - tiptoe
    val hip = feet - 13f * g
    val shoulderY = feet - 25f * g
    val headC = Offset(x, feet - 30.5f * g)
    val headR = 4.6f * g
    val torsoW = if (guy.belly) 11f * g else 9f * g

    // Beine (verschwinden größtenteils hinter der Theke)
    drawLine(Color(0xFF263238), Offset(x - 2f * g, hip), Offset(x - 2f * g + step * 3f * g, feet), strokeWidth = 3.4f * g, cap = StrokeCap.Round)
    drawLine(Color(0xFF263238), Offset(x + 2f * g, hip), Offset(x + 2f * g - step * 3f * g, feet), strokeWidth = 3.4f * g, cap = StrokeCap.Round)

    // Lange Haare hinter dem Rücken
    if (guy.hairStyle == HairStyle.LONG || guy.hairStyle == HairStyle.CURLY) {
        val swing = step * 0.8f * g
        drawRoundRect(
            guy.hair,
            topLeft = Offset(headC.x - headR * 1.25f + swing, headC.y - headR * 0.6f),
            size = Size(headR * 2.5f, headR * 2.6f),
            cornerRadius = CornerRadius(headR),
        )
    }

    // Oberkörper
    drawRoundRect(guy.shirt, topLeft = Offset(x - torsoW / 2f, shoulderY - 1f * g), size = Size(torsoW, hip - shoulderY + 2f * g), cornerRadius = CornerRadius(3f * g))
    if (guy.kutte) {
        val vest = Color(0xFF3F5A7A)
        drawRect(vest, topLeft = Offset(x - torsoW / 2f, shoulderY - 1f * g), size = Size(torsoW * 0.3f, hip - shoulderY + 2f * g))
        drawRect(vest, topLeft = Offset(x + torsoW * 0.2f, shoulderY - 1f * g), size = Size(torsoW * 0.3f, hip - shoulderY + 2f * g))
        drawCircle(Color(0xFFD7263D), 0.9f * g, Offset(x - torsoW * 0.36f, shoulderY + 3f * g))
        drawRect(Color(0xFFF2B32A), topLeft = Offset(x + torsoW * 0.27f, shoulderY + 4f * g), size = Size(1.6f * g, 1.6f * g))
        drawCircle(Color(0xFFB0B4BA), 0.5f * g, Offset(x - torsoW * 0.36f, shoulderY + 6f * g))
    }
    // Shirt-Aufdruck
    drawSkull(Offset(x, shoulderY + 5f * g), 1.8f * g, Color(0xFFE8E4DA), guy.shirt)

    // Arme
    val leftShoulder = Offset(x - torsoW / 2f + 0.8f * g, shoulderY + 0.5f * g)
    val rightShoulder = Offset(x + torsoW / 2f - 0.8f * g, shoulderY + 0.5f * g)
    val armLen = 13f * g
    fun reach(shoulder: Offset, target: Offset): Offset {
        val d = target - shoulder
        val len = sqrt(d.x * d.x + d.y * d.y)
        return if (len <= armLen || len == 0f) target else shoulder + d * (armLen / len)
    }
    val leftHand: Offset
    val rightHand: Offset
    if (reachY != null) {
        leftHand = reach(leftShoulder, Offset(x - 5f * g, reachY))
        rightHand = reach(rightShoulder, Offset(x + 5f * g, reachY))
    } else {
        leftHand = Offset(x - torsoW / 2f - 1.5f * g + step * 2f * g, hip - 1f * g)
        rightHand = Offset(x + torsoW / 2f + 1f * g, hip - 3f * g)
    }
    val sleeve = if (guy.kutte) Color(0xFF3F5A7A) else guy.shirt
    for ((shoulder, hand) in listOf(leftShoulder to leftHand, rightShoulder to rightHand)) {
        drawLine(guy.skin, shoulder, hand, strokeWidth = 2.6f * g, cap = StrokeCap.Round)
        drawLine(sleeve, shoulder, lerp(shoulder, hand, 0.3f), strokeWidth = 3.2f * g, cap = StrokeCap.Round)
        drawCircle(guy.skin, 1.5f * g, hand)
    }
    // Zusammengerollte Poster unterm Arm
    if (carrying != null && reachY == null) {
        rotate(-20f, pivot = rightHand) {
            drawRoundRect(carrying.background, topLeft = Offset(rightHand.x - 1.2f * g, rightHand.y - 7f * g), size = Size(2.4f * g, 10f * g), cornerRadius = CornerRadius(1.2f * g))
            drawLine(carrying.ink, Offset(rightHand.x, rightHand.y - 6f * g), Offset(rightHand.x, rightHand.y + 2f * g), strokeWidth = 0.4f * g)
        }
        drawCircle(guy.skin, 1.5f * g, rightHand)
    }

    // Kopf
    drawCircle(guy.skin, headR, headC)
    val eyeY = headC.y - 0.6f * g
    if (guy.corpsePaint) {
        drawCircle(Color.Black, 1.4f * g, Offset(headC.x - 1.8f * g, eyeY))
        drawCircle(Color.Black, 1.4f * g, Offset(headC.x + 1.8f * g, eyeY))
        drawLine(Color.Black, Offset(headC.x, headC.y + 1.5f * g), Offset(headC.x, headC.y + 4f * g), strokeWidth = 0.7f * g)
    }
    if (guy.shades) {
        drawRoundRect(Color(0xFF111111), topLeft = Offset(headC.x - 3.6f * g, eyeY - 1f * g), size = Size(3.2f * g, 2f * g), cornerRadius = CornerRadius(0.6f * g))
        drawRoundRect(Color(0xFF111111), topLeft = Offset(headC.x + 0.4f * g, eyeY - 1f * g), size = Size(3.2f * g, 2f * g), cornerRadius = CornerRadius(0.6f * g))
        drawLine(Color(0xFF111111), Offset(headC.x - 0.4f * g, eyeY - 0.4f * g), Offset(headC.x + 0.4f * g, eyeY - 0.4f * g), strokeWidth = 0.4f * g)
    } else {
        drawCircle(Color(0xFF2B1A12), 0.6f * g, Offset(headC.x - 1.8f * g, eyeY))
        drawCircle(Color(0xFF2B1A12), 0.6f * g, Offset(headC.x + 1.8f * g, eyeY))
    }
    // Grinsen
    drawArc(
        Color(0xFF2B1A12), startAngle = 10f, sweepAngle = 160f, useCenter = false,
        topLeft = Offset(headC.x - 1.8f * g, headC.y + 0.8f * g), size = Size(3.6f * g, 2.2f * g),
        style = Stroke(0.5f * g, cap = StrokeCap.Round),
    )

    when (guy.beard) {
        BeardStyle.NONE -> Unit
        BeardStyle.GOATEE -> drawOval(guy.hair, topLeft = Offset(headC.x - 1.3f * g, headC.y + 2.8f * g), size = Size(2.6f * g, 3.4f * g))
        BeardStyle.FULL -> drawArc(
            guy.hair, startAngle = 0f, sweepAngle = 180f, useCenter = true,
            topLeft = Offset(headC.x - headR, headC.y - 1f * g), size = Size(headR * 2f, headR * 1.9f),
        )
        BeardStyle.LONG -> {
            val beard = Path().apply {
                moveTo(headC.x - headR * 0.9f, headC.y + 1f * g)
                lineTo(headC.x + headR * 0.9f, headC.y + 1f * g)
                lineTo(headC.x + 1.5f * g, headC.y + 11f * g)
                lineTo(headC.x - 1.5f * g, headC.y + 11f * g)
                close()
            }
            drawPath(beard, guy.hair)
        }
    }
    if (guy.beard == BeardStyle.FULL || guy.beard == BeardStyle.LONG) {
        // Mund wieder freilegen
        drawArc(
            Color(0xFF2B1A12), startAngle = 10f, sweepAngle = 160f, useCenter = true,
            topLeft = Offset(headC.x - 1.6f * g, headC.y + 1f * g), size = Size(3.2f * g, 1.8f * g),
        )
    }

    // Frisur und Kopfbedeckung
    when (guy.hairStyle) {
        HairStyle.LONG, HairStyle.PONYTAIL -> drawArc(
            guy.hair, startAngle = 180f, sweepAngle = 180f, useCenter = true,
            topLeft = Offset(headC.x - headR * 1.05f, headC.y - headR * 1.08f), size = Size(headR * 2.1f, headR * 1.3f),
        )
        HairStyle.CURLY -> for (i in -2..2) {
            drawCircle(guy.hair, 1.9f * g, Offset(headC.x + i * 1.9f * g, headC.y - headR * 0.85f + abs(i) * 0.6f * g))
        }
        HairStyle.MOHAWK -> for (i in -2..2) {
            val spike = Path().apply {
                val bx = headC.x + i * 1.3f * g
                val by = headC.y - headR * 0.85f + abs(i) * 0.5f * g
                moveTo(bx - 0.8f * g, by)
                lineTo(bx + i * 0.4f * g, by - 4.5f * g + abs(i) * 0.8f * g)
                lineTo(bx + 0.8f * g, by)
                close()
            }
            drawPath(spike, guy.hair)
        }
        HairStyle.BALD, HairStyle.HALF_BALD -> drawCircle(Color(0x55FFFFFF), 1.2f * g, Offset(headC.x - 1.5f * g, headC.y - headR * 0.6f))
    }
    if (guy.hairStyle == HairStyle.PONYTAIL) {
        val tail = Offset(headC.x + headR * 0.9f + step * 0.8f * g, headC.y + 1f * g)
        drawOval(guy.hair, topLeft = Offset(tail.x - 1.2f * g, tail.y - 1f * g), size = Size(2.6f * g, 7f * g))
    }
    guy.bandana?.let { band ->
        drawArc(
            band, startAngle = 180f, sweepAngle = 180f, useCenter = true,
            topLeft = Offset(headC.x - headR * 1.05f, headC.y - headR * 1.1f), size = Size(headR * 2.1f, headR * 1.1f),
        )
        drawCircle(Color.White, 0.4f * g, Offset(headC.x - 1f * g, headC.y - headR * 0.8f))
        drawCircle(Color.White, 0.4f * g, Offset(headC.x + 1.5f * g, headC.y - headR * 0.75f))
    }
    if (guy.helmet) {
        drawArc(
            Color(0xFF9E9E9E), startAngle = 180f, sweepAngle = 180f, useCenter = true,
            topLeft = Offset(headC.x - headR * 1.1f, headC.y - headR * 1.2f), size = Size(headR * 2.2f, headR * 1.5f),
        )
        drawRect(Color(0xFF757575), topLeft = Offset(headC.x - headR * 1.1f, headC.y - headR * 0.5f), size = Size(headR * 2.2f, 1f * g))
        for (side in listOf(-1f, 1f)) {
            val horn = Path().apply {
                moveTo(headC.x + side * headR * 0.8f, headC.y - headR * 0.6f)
                quadraticBezierTo(
                    headC.x + side * headR * 1.9f, headC.y - headR * 0.9f,
                    headC.x + side * headR * 1.7f, headC.y - headR * 2f,
                )
                lineTo(headC.x + side * headR * 0.9f, headC.y - headR * 1f)
                close()
            }
            drawPath(horn, Color(0xFFFFF3D6))
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Ein einzelnes Poster
// ---------------------------------------------------------------------------------------------

private val TitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    textAlign = Paint.Align.CENTER
    textScaleX = 0.85f
}
private val TagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    textAlign = Paint.Align.CENTER
    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
}
private val Fonts = listOf(
    Typeface.create(Typeface.SERIF, Typeface.BOLD),
    Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD_ITALIC),
    Typeface.create(Typeface.MONOSPACE, Typeface.BOLD),
)

/** [reveal] blendet das Poster von oben nach unten ein (Aufrollen); mit [fromBottom] hängt es unten an den Händen. */
private fun DrawScope.drawPoster(p: PosterDesign, slot: Slot, time: Float, reveal: Float, fromBottom: Boolean = false) {
    if (reveal <= 0.01f) return
    val (topLeft, posterSize) = slotRect(slot)
    val left = topLeft.x
    val top = topLeft.y
    val pw = posterSize.width
    val ph = posterSize.height
    val tilt = slot.tilt + sin(time * 0.8f + slot.x * 10f) * 0.6f
    rotate(tilt, pivot = Offset(left + pw / 2f, top)) {
        val visibleTop = if (fromBottom) top + ph * (1f - reveal) else top
        clipRect(left - pw * 0.1f, visibleTop, left + pw * 1.1f, visibleTop + ph * reveal + ph * 0.06f) {
            drawRect(Color(0x55000000), topLeft = Offset(left + pw * 0.04f, top + ph * 0.05f), size = Size(pw, ph))
            drawRect(p.background, topLeft = Offset(left, top), size = Size(pw, ph))
            drawRect(p.ink.copy(alpha = 0.6f), topLeft = Offset(left, top), size = Size(pw, ph), style = Stroke(pw * 0.02f))
            drawMotif(p, Offset(left + pw / 2f, top + ph * 0.56f), min(pw, ph) * 0.2f)
            drawIntoCanvas { canvas ->
                TitlePaint.typeface = Fonts[p.font]
                TitlePaint.color = p.ink.toArgb()
                TitlePaint.textSize = ph * 0.19f
                while (TitlePaint.measureText(p.band) > pw * 0.9f && TitlePaint.textSize > 4f) TitlePaint.textSize *= 0.92f
                canvas.nativeCanvas.drawText(p.band, left + pw / 2f, top + ph * 0.24f, TitlePaint)
                TagPaint.color = p.ink.copy(alpha = 0.85f).toArgb()
                TagPaint.textSize = ph * 0.09f
                canvas.nativeCanvas.drawText(p.tagline, left + pw / 2f, top + ph * 0.93f, TagPaint)
            }
        }
        if (reveal < 1f && !fromBottom) {
            // die Rolle, die gerade abgerollt wird
            drawRoundRect(
                p.background,
                topLeft = Offset(left - pw * 0.02f, top + ph * reveal - ph * 0.04f),
                size = Size(pw * 1.04f, ph * 0.08f),
                cornerRadius = CornerRadius(ph * 0.04f),
            )
        }
        if (!fromBottom) {
            drawRect(Color(0x99E8E0C8), topLeft = Offset(left + pw * 0.4f, top - ph * 0.04f), size = Size(pw * 0.2f, ph * 0.08f))
        }
    }
}

private fun DrawScope.drawMotif(p: PosterDesign, c: Offset, r: Float) {
    val ink = p.ink
    when (p.motif) {
        0 -> drawSkull(c, r, ink, p.background)
        1 -> { // Blitz
            val bolt = Path().apply {
                moveTo(c.x + r * 0.3f, c.y - r)
                lineTo(c.x - r * 0.5f, c.y + r * 0.1f)
                lineTo(c.x, c.y + r * 0.1f)
                lineTo(c.x - r * 0.3f, c.y + r)
                lineTo(c.x + r * 0.5f, c.y - r * 0.15f)
                lineTo(c.x, c.y - r * 0.15f)
                close()
            }
            drawPath(bolt, ink)
        }
        2 -> for (i in -2..2) { // Flammen
            val fx = c.x + i * r * 0.45f
            val flame = Path().apply {
                moveTo(fx - r * 0.25f, c.y + r * 0.8f)
                quadraticBezierTo(fx - r * 0.35f, c.y - r * 0.1f, fx, c.y - r * (0.6f + 0.2f * abs(i % 2)))
                quadraticBezierTo(fx + r * 0.35f, c.y - r * 0.1f, fx + r * 0.25f, c.y + r * 0.8f)
                close()
            }
            drawPath(flame, ink.copy(alpha = 0.85f))
        }
        3 -> { // Bierkrug
            drawRoundRect(ink, topLeft = Offset(c.x - r * 0.55f, c.y - r * 0.6f), size = Size(r * 1.1f, r * 1.4f), cornerRadius = CornerRadius(r * 0.15f))
            drawArc(ink, startAngle = -90f, sweepAngle = 180f, useCenter = false, topLeft = Offset(c.x + r * 0.3f, c.y - r * 0.3f), size = Size(r * 0.7f, r * 0.8f), style = Stroke(r * 0.18f))
            drawCircle(ink, r * 0.35f, Offset(c.x - r * 0.3f, c.y - r * 0.65f))
            drawCircle(ink, r * 0.4f, Offset(c.x + r * 0.15f, c.y - r * 0.72f))
        }
        4 -> { // Pommesgabel
            drawCircle(ink, r * 0.5f, Offset(c.x, c.y + r * 0.35f))
            drawLine(ink, Offset(c.x - r * 0.3f, c.y), Offset(c.x - r * 0.5f, c.y - r), strokeWidth = r * 0.25f, cap = StrokeCap.Round)
            drawLine(ink, Offset(c.x + r * 0.35f, c.y), Offset(c.x + r * 0.55f, c.y - r * 0.85f), strokeWidth = r * 0.25f, cap = StrokeCap.Round)
        }
        5 -> { // Flying-V-Gitarre
            val body = Path().apply {
                moveTo(c.x, c.y + r)
                lineTo(c.x - r * 0.9f, c.y - r * 0.2f)
                lineTo(c.x - r * 0.55f, c.y - r * 0.35f)
                lineTo(c.x, c.y + r * 0.35f)
                lineTo(c.x + r * 0.55f, c.y - r * 0.35f)
                lineTo(c.x + r * 0.9f, c.y - r * 0.2f)
                close()
            }
            drawPath(body, ink)
            drawLine(ink, Offset(c.x, c.y + r * 0.4f), Offset(c.x, c.y - r * 1.1f), strokeWidth = r * 0.16f)
        }
        6 -> { // Mond über Bergen
            drawCircle(ink, r * 0.45f, Offset(c.x + r * 0.45f, c.y - r * 0.5f))
            drawCircle(p.background, r * 0.4f, Offset(c.x + r * 0.62f, c.y - r * 0.6f))
            val mountains = Path().apply {
                moveTo(c.x - r * 1.1f, c.y + r)
                lineTo(c.x - r * 0.4f, c.y - r * 0.2f)
                lineTo(c.x, c.y + r * 0.4f)
                lineTo(c.x + r * 0.5f, c.y - r * 0.1f)
                lineTo(c.x + r * 1.1f, c.y + r)
                close()
            }
            drawPath(mountains, ink)
        }
        else -> { // gekreuzte Äxte
            for (side in listOf(-1f, 1f)) {
                drawLine(ink, Offset(c.x - side * r * 0.8f, c.y + r), Offset(c.x + side * r * 0.7f, c.y - r * 0.8f), strokeWidth = r * 0.14f, cap = StrokeCap.Round)
                drawArc(
                    ink, startAngle = if (side < 0) 90f else -90f, sweepAngle = 180f, useCenter = true,
                    topLeft = Offset(c.x + side * r * 0.45f - r * 0.35f, c.y - r * 1.05f), size = Size(r * 0.7f, r * 0.7f),
                )
            }
        }
    }
}

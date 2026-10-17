package de.alcomon.hubbell.ui.draw

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin

private const val PHOTO_PERIOD = 31f
private const val PHOTO_OFFSET = 17f
private const val PHOTO_LENGTH = 2.8f

/** Hin und wieder segelt Peters Foto als Polaroid quer durchs Bild und ist dann wieder weg. */
fun DrawScope.drawFlyingPhoto(photo: ImageBitmap, time: Float) {
    val shifted = time - PHOTO_OFFSET
    if (shifted < 0f) return
    val k = floor(shifted / PHOTO_PERIOD).toInt()
    val t = (shifted - k * PHOTO_PERIOD) / PHOTO_LENGTH
    if (t > 1f) return

    val w = size.width
    val h = size.height
    val fromLeft = k % 2 == 0
    val startX = if (fromLeft) -0.35f * w else 1.35f * w
    val endX = if (fromLeft) 1.35f * w else -0.35f * w
    val startY = h * (0.75f - 0.1f * (k % 3))
    val x = startX + (endX - startX) * t
    // flacher Bogen nach oben, wie geworfen
    val y = startY - sin(t * Math.PI.toFloat()) * h * 0.35f
    val spin = (if (fromLeft) 1f else -1f) * (t * 540f - 20f)

    val photoW = minOf(w, h) * 0.34f
    val photoH = photoW * photo.height / photo.width
    val border = photoW * 0.06f
    val frameW = photoW + border * 2f
    val frameH = photoH + border * 4.5f
    val topLeft = Offset(x - frameW / 2f, y - frameH / 2f)

    rotate(spin, pivot = Offset(x, y)) {
        drawRect(Color(0x55000000), topLeft = topLeft + Offset(border * 0.6f, border * 0.8f), size = Size(frameW, frameH))
        drawRect(Color(0xFFF8F6F0), topLeft = topLeft, size = Size(frameW, frameH))
        drawImage(
            photo,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(photo.width, photo.height),
            dstOffset = IntOffset((topLeft.x + border).roundToInt(), (topLeft.y + border).roundToInt()),
            dstSize = IntSize(photoW.roundToInt(), photoH.roundToInt()),
        )
    }
}

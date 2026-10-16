package de.alcomon.hubbell.ui.draw

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import de.alcomon.hubbell.game.RewardKind
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

private val Palette = listOf(
    Color(0xFFF2B32A), Color(0xFFE0565B), Color(0xFF6CC060), Color(0xFF3FA7D6),
    Color(0xFFFF5FA2), Color(0xFFB388FF), Color(0xFFFFFBF0),
)
private val Skins = listOf(Color(0xFFF5C9A0), Color(0xFFD9A47A), Color(0xFFA9714B), Color(0xFF7A4E2D))

class ConfettiPiece(
    val x: Float, val delay: Float, val speed: Float, val color: Color,
    val phase: Float, val spin: Float, val sway: Float,
)

class Burst(val x: Float, val y: Float, val t0: Float, val color: Color, val count: Int, val radius: Float)

class Partygoer(val x: Float, val color: Color, val skin: Color, val scale: Float, val phase: Float, val hasBeer: Boolean)

/** Vorberechnete Zufallsteilchen, damit die Animation über alle Frames stabil bleibt. */
class RewardParticles(
    val confetti: List<ConfettiPiece>,
    val bursts: List<Burst>,
    val crowd: List<Partygoer>,
) {
    companion object {
        fun create(kind: RewardKind, seed: Int): RewardParticles {
            val r = Random(seed * 31 + kind.ordinal)
            val confettiCount = when (kind) {
                RewardKind.KONFETTI -> 130
                RewardKind.GROSSES_FEST -> 150
                RewardKind.PARTY, RewardKind.DISCO -> 80
                RewardKind.FEUERWERK -> 30
            }
            val confetti = List(confettiCount) {
                ConfettiPiece(
                    x = r.nextFloat(),
                    delay = r.nextFloat() * 1.6f,
                    speed = 0.22f + r.nextFloat() * 0.3f,
                    color = Palette.random(r),
                    phase = r.nextFloat() * 6.28f,
                    spin = (r.nextFloat() - 0.5f) * 720f,
                    sway = 0.02f + r.nextFloat() * 0.04f,
                )
            }
            val burstCount = when (kind) {
                RewardKind.FEUERWERK -> 10
                RewardKind.GROSSES_FEST -> 14
                else -> 0
            }
            val bursts = List(burstCount) { i ->
                Burst(
                    x = 0.15f + r.nextFloat() * 0.7f,
                    y = 0.1f + r.nextFloat() * 0.35f,
                    t0 = 0.5f + i * (if (kind == RewardKind.GROSSES_FEST) 0.38f else 0.4f) + r.nextFloat() * 0.2f,
                    color = Palette.random(r),
                    count = 22 + r.nextInt(14),
                    radius = 0.14f + r.nextFloat() * 0.1f,
                )
            }
            val crowd = if (kind == RewardKind.PARTY || kind == RewardKind.GROSSES_FEST) {
                List(9) { i ->
                    Partygoer(
                        x = (i + 0.5f) / 9f + (r.nextFloat() - 0.5f) * 0.04f,
                        color = Palette.random(r),
                        skin = Skins.random(r),
                        scale = 0.85f + r.nextFloat() * 0.3f,
                        phase = r.nextFloat() * 6.28f,
                        hasBeer = r.nextBoolean(),
                    )
                }
            } else {
                emptyList()
            }
            return RewardParticles(confetti, bursts, crowd)
        }
    }
}

/** Zeichnet die Belohnungs-Animation zum Zeitpunkt [t] (Sekunden) einer Animation der Länge [duration]. */
fun DrawScope.drawReward(kind: RewardKind, particles: RewardParticles, t: Float, duration: Float) {
    val fade = ((duration - t) / 0.6f).coerceIn(0f, 1f) * (t / 0.3f).coerceIn(0f, 1f)
    when (kind) {
        RewardKind.DISCO -> drawDisco(t, fade)
        RewardKind.FEUERWERK, RewardKind.GROSSES_FEST ->
            drawRect(Color(0xFF0B0B2A).copy(alpha = 0.5f * fade))
        else -> Unit
    }
    if (kind == RewardKind.GROSSES_FEST) drawDisco(t, fade * 0.6f)
    particles.bursts.forEach { drawBurst(it, t, fade) }
    if (particles.crowd.isNotEmpty()) drawCrowd(particles.crowd, t, fade)
    drawConfetti(particles.confetti, t, fade)
}

private fun DrawScope.drawConfetti(pieces: List<ConfettiPiece>, t: Float, fade: Float) {
    val w = size.width
    val h = size.height
    val minD = min(w, h)
    val pw = minD * 0.02f
    val ph = minD * 0.011f
    for (c in pieces) {
        val tt = t - c.delay
        if (tt < 0f) continue
        val y = -0.05f * h + tt * c.speed * h
        if (y > h * 1.05f) continue
        val x = c.x * w + sin(tt * 3f + c.phase) * c.sway * w
        rotate(c.phase * 57f + tt * c.spin, pivot = Offset(x, y)) {
            drawRect(c.color.copy(alpha = fade), topLeft = Offset(x - pw / 2f, y - ph / 2f), size = Size(pw, ph))
        }
    }
}

private fun DrawScope.drawBurst(b: Burst, t: Float, fade: Float) {
    val w = size.width
    val h = size.height
    val minD = min(w, h)
    val launch = 0.6f
    val dt = t - b.t0
    if (dt < -launch) return
    val target = Offset(b.x * w, b.y * h)
    if (dt < 0f) {
        val k = 1f + dt / launch
        val pos = Offset(target.x + sin(dt * 20f) * minD * 0.004f, h - (h - target.y) * k)
        drawLine(Color(0xFFFFD27A).copy(alpha = 0.5f * fade), pos, pos + Offset(0f, minD * 0.05f), strokeWidth = minD * 0.006f, cap = StrokeCap.Round)
        drawCircle(Color(0xFFFFF3D6).copy(alpha = fade), minD * 0.007f, pos)
        return
    }
    val life = 1.7f
    if (dt > life) return
    val alpha = (1f - dt / life) * fade
    val spread = (1f - exp(-3.5f * dt)) * b.radius * minD
    val drop = 0.05f * h * dt * dt
    for (i in 0 until b.count) {
        val angle = 2.0 * PI * i / b.count
        val dir = Offset(cos(angle).toFloat(), sin(angle).toFloat())
        val p = target + dir * spread + Offset(0f, drop)
        val tail = target + dir * (spread * 0.75f) + Offset(0f, drop * 0.8f)
        drawLine(b.color.copy(alpha = alpha * 0.6f), tail, p, strokeWidth = minD * 0.005f, cap = StrokeCap.Round)
        drawCircle(b.color.copy(alpha = alpha), minD * 0.007f, p)
        if (i % 3 == 0 && dt > 0.5f) {
            drawCircle(Color.White.copy(alpha = alpha * (0.5f + 0.5f * sin(dt * 30f + i))), minD * 0.004f, p + dir * (minD * 0.01f))
        }
    }
    if (dt < 0.15f) {
        drawCircle(Color.White.copy(alpha = (1f - dt / 0.15f) * fade), minD * 0.05f, target)
    }
}

private fun DrawScope.drawCrowd(crowd: List<Partygoer>, t: Float, fade: Float) {
    val w = size.width
    val h = size.height
    val enter = (1f - (t / 0.6f).coerceIn(0f, 1f)) * h * 0.25f
    for (p in crowd) {
        val jump = abs(sin(t * 7f + p.phase)) * 0.035f * h
        val bodyW = w * 0.09f * p.scale
        val bodyH = h * 0.12f * p.scale
        val baseX = p.x * w
        val bottom = h + 0.02f * h - jump + enter
        val top = bottom - bodyH
        val color = p.color.copy(alpha = fade)
        val wave = sin(t * 9f + p.phase) * 0.3f
        val leftHand = Offset(baseX - bodyW * (0.75f + wave * 0.3f), top - bodyW * 0.9f)
        val rightHand = Offset(baseX + bodyW * (0.75f - wave * 0.3f), top - bodyW * 0.9f)
        drawLine(color, Offset(baseX - bodyW * 0.35f, top + bodyW * 0.2f), leftHand, strokeWidth = bodyW * 0.2f, cap = StrokeCap.Round)
        drawLine(color, Offset(baseX + bodyW * 0.35f, top + bodyW * 0.2f), rightHand, strokeWidth = bodyW * 0.2f, cap = StrokeCap.Round)
        if (p.hasBeer) {
            drawRoundRect(
                Color(0xFFF2B32A).copy(alpha = fade),
                topLeft = Offset(rightHand.x - bodyW * 0.15f, rightHand.y - bodyW * 0.45f),
                size = Size(bodyW * 0.3f, bodyW * 0.4f),
                cornerRadius = CornerRadius(bodyW * 0.05f),
            )
            drawCircle(Color(0xFFFFFBF0).copy(alpha = fade), bodyW * 0.14f, Offset(rightHand.x, rightHand.y - bodyW * 0.45f))
        }
        drawRoundRect(color, topLeft = Offset(baseX - bodyW / 2f, top), size = Size(bodyW, bodyH + h * 0.05f), cornerRadius = CornerRadius(bodyW * 0.4f))
        val head = Offset(baseX, top - bodyW * 0.42f)
        drawCircle(p.skin.copy(alpha = fade), bodyW * 0.42f, head)
        drawCircle(Color(0xFF3B2414).copy(alpha = fade), bodyW * 0.12f, head + Offset(0f, bodyW * 0.16f))
        drawCircle(Color(0xFF3B2414).copy(alpha = fade), bodyW * 0.05f, head + Offset(-bodyW * 0.15f, -bodyW * 0.08f))
        drawCircle(Color(0xFF3B2414).copy(alpha = fade), bodyW * 0.05f, head + Offset(bodyW * 0.15f, -bodyW * 0.08f))
    }
}

private fun DrawScope.drawDisco(t: Float, fade: Float) {
    val w = size.width
    val h = size.height
    val minD = min(w, h)
    drawRect(Color(0xFF120624).copy(alpha = 0.4f * fade))
    val ball = Offset(w / 2f, h * 0.12f)
    val beamColors = listOf(Color(0xFFFF5FA2), Color(0xFF3FA7D6), Color(0xFF6CC060), Color(0xFFF2B32A), Color(0xFFB388FF), Color(0xFFE0565B))
    val length = h * 1.3f
    for (i in beamColors.indices) {
        // Strahlen schwenken im unteren Halbkreis hin und her
        val a = (PI / 2).toFloat() + sin(t * 1.5f + i * 1.1f) * 1.1f
        val spread = 0.12f
        val path = Path().apply {
            moveTo(ball.x, ball.y)
            lineTo(ball.x + cos(a - spread) * length, ball.y + sin(a - spread) * length)
            lineTo(ball.x + cos(a + spread) * length, ball.y + sin(a + spread) * length)
            close()
        }
        drawPath(path, beamColors[i].copy(alpha = 0.22f * fade))
    }
    drawLine(Color(0xFF9E9E9E).copy(alpha = fade), Offset(ball.x, 0f), ball, strokeWidth = minD * 0.004f)
    val r = minD * 0.06f
    drawCircle(Color(0xFFCFD3E0).copy(alpha = fade), r, ball)
    for (row in -2..2) {
        for (col in -2..2) {
            val tile = Offset(ball.x + col * r * 0.38f, ball.y + row * r * 0.38f)
            if ((tile - ball).getDistance() < r * 0.9f) {
                val shine = 0.5f + 0.5f * sin(t * 6f + row * 1.7f + col * 2.3f)
                drawRect(
                    Color.White.copy(alpha = fade * (0.3f + 0.7f * shine)),
                    topLeft = tile - Offset(r * 0.15f, r * 0.15f),
                    size = Size(r * 0.3f, r * 0.3f),
                )
            }
        }
    }
}

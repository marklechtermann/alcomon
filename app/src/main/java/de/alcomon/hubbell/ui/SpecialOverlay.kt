package de.alcomon.hubbell.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.alcomon.hubbell.ui.theme.PubColors
import kotlin.math.ceil
import kotlin.math.sin

/** Ansage des Special Events mit einer kleinen Vorführung der Trinkbewegung. */
@Composable
fun SpecialBanner(challenge: SpecialChallenge, time: () -> Float, onTap: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .padding(horizontal = 24.dp)
            .graphicsLayer {
                val pulse = 1f + 0.03f * sin(time() * 8f)
                scaleX = pulse
                scaleY = pulse
            }
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xF0151515))
            .border(3.dp, PubColors.Gold, RoundedCornerShape(24.dp))
            .clickable(enabled = !challenge.useSensor, onClick = onTap)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("🤘 SPECIAL EVENT 🤘", color = PubColors.Gold, fontWeight = FontWeight.Black, fontSize = 22.sp)
        Text(
            if (challenge.useSensor) "Nimm dein Handy hoch und trink daraus wie aus einem Bier!"
            else "Dein Handy hat keinen Bewegungssensor: tippe hier, um ex zu trinken!",
            color = PubColors.Cream,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
        )
        Canvas(Modifier.padding(vertical = 6.dp).size(width = 180.dp, height = 130.dp)) {
            drawDrinkDemo(time())
        }
        Text(
            "Hochheben, ansetzen, nach hinten kippen. Fertig!",
            color = PubColors.Cream.copy(alpha = 0.8f),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { challenge.progress },
            modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
            color = PubColors.Gold,
            trackColor = PubColors.PanelLight,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Noch ${ceil(challenge.secondsLeft).toInt()} s · Riesenbonus!",
            color = PubColors.Gold,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
        )
    }
}

/** Der große Jubel nach einem geschafften Special Event. */
@Composable
fun SpecialCheer(secondsLeft: Float, modifier: Modifier = Modifier) {
    val t = 2.6f - secondsLeft
    Text(
        "EX! 🍺🤘",
        modifier = modifier.graphicsLayer {
            val pop = (t / 0.25f).coerceAtMost(1f)
            scaleX = 0.4f + 1.2f * pop
            scaleY = 0.4f + 1.2f * pop
            rotationZ = -8f + 4f * sin(t * 12f)
            alpha = (secondsLeft / 0.6f).coerceIn(0f, 1f)
        },
        color = PubColors.Gold,
        fontWeight = FontWeight.Black,
        fontSize = 64.sp,
    )
}

/**
 * Kleine Endlos-Vorführung: Ein Kopf im Profil, das Handy wird hochgenommen,
 * an den Mund gesetzt und wie ein Glas nach hinten gekippt.
 */
private fun DrawScope.drawDrinkDemo(time: Float) {
    val w = size.width
    val h = size.height
    val cycle = (time % 2.6f) / 2.6f
    fun ease(x: Float) = x * x * (3f - 2f * x)
    val lift = ease((cycle / 0.35f).coerceIn(0f, 1f))
    val tilt = ease(((cycle - 0.4f) / 0.3f).coerceIn(0f, 1f))

    // Kopf im Profil, Blick nach rechts
    val head = Offset(w * 0.28f, h * 0.42f)
    val r = h * 0.26f
    drawCircle(Color(0xFF111111), r * 1.12f, head + Offset(-r * 0.15f, -r * 0.05f)) // Haare
    drawCircle(Color(0xFFF1C27D), r, head)
    drawRect(Color(0xFF111111), Offset(head.x - r * 1.25f, head.y - r * 0.2f), Size(r * 0.7f, r * 1.9f)) // lange Haare hinten
    drawCircle(Color(0xFF222222), r * 0.12f, head + Offset(r * 0.45f, -r * 0.25f)) // Auge
    drawArc(
        Color(0xFF111111), 0f, 180f, true,
        topLeft = head + Offset(r * 0.1f, r * 0.15f), size = Size(r * 0.95f, r * 0.9f),
    ) // Bart
    val mouth = head + Offset(r * 0.95f, r * 0.25f)
    drawCircle(Color(0xFF6B1A1A), r * (0.1f + 0.08f * tilt), mouth)

    // Handy: vom Tisch zum Mund, dann kippen (Oberkante am Mund, Unterkante geht hoch)
    val phoneW = w * 0.13f
    val phoneH = h * 0.5f
    val restTop = Offset(w * 0.72f, h * 0.55f)
    val mouthTop = mouth + Offset(phoneW * 0.15f, 0f)
    val top = Offset(restTop.x + (mouthTop.x - restTop.x) * lift, restTop.y + (mouthTop.y - restTop.y) * lift)
    translate(top.x, top.y) {
        rotate(-125f * tilt, pivot = Offset.Zero) {
            drawRoundRect(Color(0xFF2B2B2B), Offset(-phoneW / 2f, 0f), Size(phoneW, phoneH), CornerRadius(phoneW * 0.25f))
            drawRoundRect(Color(0xFF4A90C8), Offset(-phoneW * 0.38f, phoneH * 0.06f), Size(phoneW * 0.76f, phoneH * 0.88f), CornerRadius(phoneW * 0.12f))
            // Ein Bier auf dem Display, das beim Kippen leer wird
            val beerH = phoneH * 0.7f * (1f - 0.8f * tilt)
            drawRect(PubColors.Gold, Offset(-phoneW * 0.3f, phoneH * 0.9f - beerH), Size(phoneW * 0.6f, beerH))
            drawRect(PubColors.Cream, Offset(-phoneW * 0.3f, phoneH * 0.9f - beerH - phoneH * 0.06f), Size(phoneW * 0.6f, phoneH * 0.06f))
        }
    }

    // Bewegungspfeil, solange noch gekippt werden muss
    if (cycle < 0.4f) {
        drawArc(
            PubColors.Gold.copy(alpha = 0.8f), 200f, 110f, false,
            topLeft = Offset(w * 0.6f, h * 0.05f), size = Size(w * 0.32f, h * 0.7f),
            style = Stroke(width = 5f, cap = StrokeCap.Round),
        )
    }
    // Glucker-Blasen beim Trinken
    if (tilt > 0.5f) {
        repeat(3) { i ->
            val p = ((time * 2.2f + i * 0.33f) % 1f)
            drawCircle(
                PubColors.Cream.copy(alpha = 1f - p),
                3f + 3f * i, mouth + Offset(-r * 0.3f - p * r * 0.5f, -r * (0.5f + p * 1.6f)),
            )
        }
    }
}

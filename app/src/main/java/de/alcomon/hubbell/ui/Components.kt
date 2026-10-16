package de.alcomon.hubbell.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.alcomon.hubbell.game.Drink
import de.alcomon.hubbell.game.Mood
import de.alcomon.hubbell.game.Thoughts
import de.alcomon.hubbell.ui.draw.drawDrink
import de.alcomon.hubbell.ui.theme.PubColors
import kotlinx.coroutines.delay
import kotlin.math.sin

@Composable
fun DrinkIcon(drink: Drink, iconSize: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(iconSize)) {
        val w = size.width * 0.62f
        drawDrink(drink, Offset((size.width - w) / 2f, size.height * 0.04f), w, size.height * 0.92f, 1f)
    }
}

@Composable
fun CoinIcon(iconSize: Dp = 18.dp) {
    Canvas(Modifier.size(iconSize)) {
        val r = size.minDimension / 2f
        drawCircle(Color(0xFFC9963B), r)
        drawCircle(PubColors.Gold, r * 0.82f)
        drawCircle(Color(0xFFFFE08A), r * 0.35f, center + Offset(-r * 0.25f, -r * 0.25f))
    }
}

/** Der Alkoholpegel oben im Bild. */
@Composable
fun PegelBar(pegel: Float, time: () -> Float, modifier: Modifier = Modifier) {
    val fraction = (pegel / 100f).coerceIn(0f, 1f)
    val low = pegel < 20f
    Box(
        modifier
            .fillMaxWidth()
            .height(30.dp)
            .graphicsLayer {
                if (low) {
                    val s = 1f + 0.02f * sin(time() * 10f)
                    scaleX = s
                    scaleY = s
                }
            }
            .clip(RoundedCornerShape(15.dp))
            .background(Color(0xFF1E120A))
            .border(2.dp, if (low) PubColors.Red else Color(0xFF8A5A30), RoundedCornerShape(15.dp)),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction)
                .background(
                    Brush.horizontalGradient(
                        if (low) listOf(PubColors.DeepRed, PubColors.Red)
                        else listOf(Color(0xFFE8962C), PubColors.Gold, Color(0xFFFFE08A)),
                    ),
                ),
        )
        Text(
            "Alkoholpegel  ${promille(pegel)}",
            modifier = Modifier.align(Alignment.Center),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
        )
    }
}

/** Gedankenblase mit Hubbells schönen Ideen, wechselt alle paar Sekunden. */
@Composable
fun ThoughtBubble(mood: Mood, modifier: Modifier = Modifier) {
    var text by remember { mutableStateOf(Thoughts.pick(mood)) }
    LaunchedEffect(mood) {
        text = Thoughts.pick(mood, avoid = text)
        while (true) {
            delay(6500)
            text = Thoughts.pick(mood, avoid = text)
        }
    }
    Crossfade(targetState = text, modifier = modifier, label = "thought") { thought ->
        Box(
            Modifier
                .widthIn(max = 220.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(PubColors.Cream)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(
                thought,
                color = Color(0xFF3B2414),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
            )
        }
    }
}

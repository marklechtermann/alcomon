package de.alcomon.hubbell.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.alcomon.hubbell.game.Reward
import de.alcomon.hubbell.game.RewardKind
import de.alcomon.hubbell.ui.draw.RewardParticles
import de.alcomon.hubbell.ui.draw.drawReward
import de.alcomon.hubbell.ui.theme.PubColors
import kotlin.math.sin

/** Spielt die Belohnung für ein erreichtes Level ab. Berührungen gehen durch zum Spiel. */
@Composable
fun RewardOverlay(reward: Reward, onFinished: () -> Unit) {
    val duration = reward.kind.seconds
    val progress = remember(reward.level) { Animatable(0f) }
    val particles = remember(reward.level) { RewardParticles.create(reward.kind, reward.level) }
    val finished = rememberUpdatedState(onFinished)

    LaunchedEffect(reward.level) {
        progress.animateTo(1f, tween(durationMillis = (duration * 1000).toInt(), easing = LinearEasing))
        finished.value()
    }

    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            drawReward(reward.kind, particles, progress.value * duration, duration)
        }
        val shadow = Shadow(Color.Black, Offset(3f, 4f), 8f)
        Column(
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = 170.dp, start = 16.dp, end = 16.dp)
                .graphicsLayer {
                    val t = progress.value * duration
                    val pop = (t / 0.35f).coerceIn(0f, 1f)
                    val bounce = 1f + 0.25f * sin(pop * Math.PI.toFloat()) + 0.03f * sin(t * 6f)
                    scaleX = pop * bounce
                    scaleY = pop * bounce
                    alpha = ((duration - t) / 0.6f).coerceIn(0f, 1f)
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "LEVEL ${reward.level}!",
                style = TextStyle(shadow = shadow),
                color = PubColors.Gold,
                fontWeight = FontWeight.Black,
                fontSize = 46.sp,
            )
            Text(
                reward.kind.headline,
                style = TextStyle(shadow = shadow),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                textAlign = TextAlign.Center,
            )
            Text(
                "Neuer Titel: ${reward.title}",
                style = TextStyle(shadow = shadow),
                color = PubColors.Cream,
                fontSize = 16.sp,
            )
            if (reward.kind == RewardKind.PARTY || reward.kind == RewardKind.GROSSES_FEST) {
                Text(
                    "Hub-bell! Hub-bell! Hub-bell!",
                    style = TextStyle(shadow = shadow),
                    color = PubColors.Green,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

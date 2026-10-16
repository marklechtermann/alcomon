package de.alcomon.hubbell.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.alcomon.hubbell.game.Drink
import de.alcomon.hubbell.game.Rewards
import de.alcomon.hubbell.ui.draw.HubbellPose
import de.alcomon.hubbell.ui.draw.drawHubbellScene
import de.alcomon.hubbell.ui.theme.PubColors

@Composable
fun HubbellApp(vm: GameViewModel) {
    BackHandler(enabled = vm.screen == Screen.GAME) { vm.onBack() }
    when (vm.screen) {
        Screen.TITLE -> TitleScreen(vm)
        Screen.GAME -> GameScreen(vm)
    }
}

@Composable
private fun TitleScreen(vm: GameViewModel) {
    val time = rememberGameClock { vm.tick(it) }
    val stats = vm.stats
    Box(Modifier.fillMaxSize().background(PubColors.WoodDark)) {
        Canvas(Modifier.fillMaxSize()) {
            drawHubbellScene(
                HubbellPose(pegel = 70f, drinking = null, drinkProgress = 0f, nextDrink = Drink.HELLES, time = time()),
            )
        }
        Column(
            Modifier.fillMaxSize().systemBarsPadding().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))
            Text(
                "Hubbell",
                style = TextStyle(shadow = Shadow(Color.Black, Offset(4f, 6f), 10f)),
                color = PubColors.Gold,
                fontWeight = FontWeight.Black,
                fontSize = 64.sp,
            )
            Text(
                "Halt ihn bei Laune!",
                style = TextStyle(shadow = Shadow(Color.Black, Offset(2f, 3f), 6f)),
                color = PubColors.Cream,
                fontSize = 20.sp,
            )
            Spacer(Modifier.weight(1f))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(PubColors.Panel.copy(alpha = 0.94f))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "Hubbell, der sympathischste Metaller der Stadt, sitzt in seiner Metal-Bar und will auf keinen Fall nüchtern werden. " +
                        "Tippe, um ihm ein Getränk zu bringen, und kauf im Shop bessere Tropfen. " +
                        "Fällt sein Pegel auf null, hast du 10 Sekunden, um ihn zu retten!",
                    color = PubColors.Cream,
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp,
                )
                if (vm.canContinue) {
                    Button(onClick = vm::continueGame, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)) {
                        Text("Weiterspielen (Level ${vm.state.level})", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(onClick = vm::startNewGame, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Text("Neues Spiel", color = PubColors.Cream)
                    }
                } else {
                    Button(onClick = vm::startNewGame, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)) {
                        Text("Prost, los geht's!", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
                if (stats.gamesPlayed > 0) {
                    Text(
                        "Rekord: Level ${stats.bestLevel} (${Rewards.titleFor(maxOf(1, stats.bestLevel))}) · " +
                            "${stats.bestDrinks} Getränke · ${formatDuration(stats.bestSeconds)} durchgehalten",
                        color = PubColors.Gold,
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp,
                    )
                }
            }
        }
    }
}

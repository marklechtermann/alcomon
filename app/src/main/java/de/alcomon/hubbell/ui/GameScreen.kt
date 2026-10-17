package de.alcomon.hubbell.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.alcomon.hubbell.R
import de.alcomon.hubbell.game.Drink
import de.alcomon.hubbell.game.GameEngine
import de.alcomon.hubbell.game.GameState
import de.alcomon.hubbell.game.Rewards
import de.alcomon.hubbell.game.RunResult
import de.alcomon.hubbell.ui.draw.HubbellPose
import de.alcomon.hubbell.ui.draw.drawFallingItem
import de.alcomon.hubbell.ui.draw.drawFlyingMagazines
import de.alcomon.hubbell.ui.draw.drawHubbellScene
import de.alcomon.hubbell.ui.draw.hubbellHead
import de.alcomon.hubbell.ui.theme.PubColors
import kotlin.math.ceil
import kotlin.math.sin

/** Treibt das Spiel pro Frame an und liefert eine laufende Animationszeit. */
@Composable
fun rememberGameClock(onFrame: (Float) -> Unit): () -> Float {
    var time by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val dt = ((now - last) / 1_000_000_000f).coerceIn(0f, 0.1f)
            last = now
            time += dt
            onFrame(dt)
        }
    }
    return { time }
}

@Composable
fun GameScreen(vm: GameViewModel) {
    val time = rememberGameClock { vm.tick(it) }
    val s = vm.state

    Box(Modifier.fillMaxSize().background(PubColors.WoodDark)) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            TopHud(s, time, vm.soundOn, vm::toggleSound)
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { pos -> vm.tapScene(pos.x, pos.y, size.width.toFloat(), size.height.toFloat()) })
                    }
                    .pointerInput(Unit) {
                        // Über den Kopf wischen = streicheln
                        var stroke = 0f
                        detectDragGestures(onDragStart = { stroke = 0f }) { change, drag ->
                            val (head, u) = hubbellHead(size.width.toFloat(), size.height.toFloat())
                            if ((change.position - head).getDistance() < 26f * u) {
                                stroke += drag.getDistance()
                                if (stroke > 22f * u) {
                                    stroke = 0f
                                    vm.pet()
                                }
                            }
                            change.consume()
                        }
                    },
            ) {
                val photo = ImageBitmap.imageResource(R.drawable.flying_photo)
                Canvas(Modifier.fillMaxSize()) {
                    drawHubbellScene(
                        HubbellPose(
                            pegel = s.pegel,
                            drinking = s.drinking,
                            drinkProgress = s.drinkProgress,
                            nextDrink = s.selected,
                            time = time(),
                            alarm = s.inRescue,
                            runSeconds = s.elapsedSeconds,
                            petGlow = s.petGlow,
                            photo = photo,
                        ),
                    )
                }
                Canvas(Modifier.fillMaxSize()) {
                    vm.falling.forEach { drawFallingItem(it, time()) }
                    drawFlyingMagazines(time())
                }
                Column(
                    Modifier.align(Alignment.TopEnd).padding(top = 12.dp, end = 12.dp),
                    horizontalAlignment = Alignment.End,
                ) {
                    ThoughtBubble(s.mood)
                    Box(Modifier.padding(end = 70.dp, top = 4.dp).size(12.dp).clip(CircleShape).background(PubColors.Cream))
                    Box(Modifier.padding(end = 90.dp, top = 3.dp).size(7.dp).clip(CircleShape).background(PubColors.Cream))
                }
                vm.floaters.forEach { f ->
                    Text(
                        f.text,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(y = (-60 - f.age * 80).dp)
                            .graphicsLayer { alpha = (1f - f.age / GameViewModel.FLOATER_LIFETIME).coerceIn(0f, 1f) },
                        color = PubColors.Gold,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                    )
                }
                val rescue = s.rescueSecondsLeft
                if (rescue != null && !s.gameOver && vm.special == null) {
                    RescueBanner(rescue, time, Modifier.align(Alignment.Center))
                }
                vm.special?.let { challenge ->
                    SpecialBanner(challenge, time, onTap = vm::tapSpecial, modifier = Modifier.align(Alignment.Center))
                }
                AppVersion(Modifier.align(Alignment.BottomStart).padding(start = 8.dp, bottom = 6.dp))
                if (vm.specialCheer > 0f) {
                    SpecialCheer(vm.specialCheer, Modifier.align(Alignment.Center))
                }
                Text(
                    "Tippen = Getränk bringen · über den Kopf wischen = streicheln",
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp),
                    color = PubColors.Cream.copy(alpha = if (s.drinksServed < 6) 0.85f else 0f),
                    fontSize = 12.sp,
                )
            }
            BottomBar(s, vm)
        }

        s.pendingRewards.firstOrNull()?.let { level ->
            RewardOverlay(Rewards.forLevel(level), onFinished = vm::consumeReward)
        }

        if (vm.shopOpen) {
            ShopPanel(
                state = s,
                onBuyDrink = vm::buyDrink,
                onSelectDrink = vm::select,
                onBuyUpgrade = vm::buyUpgrade,
                onClose = vm::closeShop,
            )
        }

        val result = vm.lastResult
        if (s.gameOver && result != null) {
            GameOverOverlay(result, time, onAgain = vm::startNewGame, onTitle = vm::toTitle)
        }
    }
}

@Composable
private fun TopHud(s: GameState, time: () -> Float, soundOn: Boolean, onToggleSound: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(PubColors.Panel).padding(horizontal = 14.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Level ${s.level}", color = PubColors.Gold, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                Text(Rewards.titleFor(s.level), color = PubColors.Cream, fontSize = 13.sp)
            }
            Text(
                if (soundOn) "🔊" else "🔇",
                fontSize = 22.sp,
                modifier = Modifier.clip(CircleShape).clickable(onClick = onToggleSound).padding(8.dp),
            )
            Spacer(Modifier.width(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CoinIcon(20.dp)
                Spacer(Modifier.width(6.dp))
                Text("${s.coins} Taler", color = PubColors.Cream, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
        PegelBar(s.pegel, time)
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { s.levelProgress },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = PubColors.Green,
            trackColor = Color(0xFF1E120A),
        )
        if (s.pegel >= GameEngine.HAPPY_PEGEL) {
            Text(
                "Beste Laune: doppelte Taler!",
                color = PubColors.Green,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun BottomBar(s: GameState, vm: GameViewModel) {
    Column(Modifier.fillMaxWidth().background(PubColors.Panel).padding(12.dp)) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Drink.entries.filter { it in s.unlocked }.forEach { drink ->
                DrinkChip(drink, selected = drink == s.selected, onClick = { vm.select(drink) })
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            val drinking = s.drinking
            Button(
                onClick = vm::serve,
                enabled = drinking == null,
                modifier = Modifier.weight(1f).height(60.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                if (drinking != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Gluck, gluck …", fontSize = 16.sp)
                        LinearProgressIndicator(
                            progress = { s.drinkProgress },
                            modifier = Modifier.padding(top = 4.dp).width(120.dp),
                        )
                    }
                } else {
                    Text("Prost! ${s.selected.displayName}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(10.dp))
            Box {
                OutlinedButton(onClick = vm::openShop, modifier = Modifier.height(60.dp), shape = RoundedCornerShape(16.dp)) {
                    Text("Shop", fontSize = 16.sp, color = PubColors.Gold, fontWeight = FontWeight.Bold)
                }
                if (GameEngine.anythingAffordable(s)) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-4).dp)
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(PubColors.Red),
                    )
                }
            }
        }
    }
}

@Composable
private fun DrinkChip(drink: Drink, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier
            .clip(shape)
            .background(if (selected) PubColors.Gold.copy(alpha = 0.22f) else PubColors.PanelLight)
            .border(2.dp, if (selected) PubColors.Gold else Color.Transparent, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        DrinkIcon(drink, 40.dp)
        Text(drink.displayName, color = PubColors.Cream, fontSize = 12.sp)
    }
}

@Composable
private fun RescueBanner(secondsLeft: Float, time: () -> Float, modifier: Modifier = Modifier) {
    Column(
        modifier
            .graphicsLayer {
                val pulse = 1f + 0.06f * sin(time() * 10f)
                scaleX = pulse
                scaleY = pulse
            }
            .clip(RoundedCornerShape(24.dp))
            .background(PubColors.DeepRed.copy(alpha = 0.9f))
            .padding(horizontal = 28.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Hubbell wird nüchtern!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text("${ceil(secondsLeft).toInt()}", color = Color.White, fontWeight = FontWeight.Black, fontSize = 64.sp)
        Text("Schnell, ein Getränk!", color = Color.White, fontSize = 16.sp)
    }
}

@Composable
private fun GameOverOverlay(result: RunResult, time: () -> Float, onAgain: () -> Unit, onTitle: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f))
            .pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .padding(24.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(PubColors.Panel)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Canvas(Modifier.size(180.dp).clip(RoundedCornerShape(16.dp))) {
                drawHubbellScene(HubbellPose(pegel = 0f, drinking = null, drinkProgress = 0f, nextDrink = null, time = time()))
            }
            Spacer(Modifier.height(16.dp))
            Text("Spiel vorbei!", color = PubColors.Gold, fontWeight = FontWeight.Black, fontSize = 30.sp)
            Text(
                "Hubbell ist stocknüchtern geworden und geht schmollend nach Hause.",
                color = PubColors.Cream,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp),
            )
            Spacer(Modifier.height(14.dp))
            StatLine("Erreichtes Level", "${result.level}", result.newBestLevel)
            StatLine("Getränke serviert", "${result.drinks}", result.newBestDrinks)
            StatLine("Durchgehalten", formatDuration(result.seconds), result.newBestTime)
            Spacer(Modifier.height(18.dp))
            Button(onClick = onAgain, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)) {
                Text("Nochmal!", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onTitle, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Text("Zum Titel", color = PubColors.Cream)
            }
        }
    }
}

@Composable
private fun StatLine(label: String, value: String, record: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = PubColors.Cream, modifier = Modifier.weight(1f))
        if (record) {
            Text("Rekord! ", color = PubColors.Green, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        Text(value, color = PubColors.Gold, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}

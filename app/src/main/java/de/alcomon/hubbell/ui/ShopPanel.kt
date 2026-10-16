package de.alcomon.hubbell.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.alcomon.hubbell.game.Drink
import de.alcomon.hubbell.game.GameEngine
import de.alcomon.hubbell.game.GameState
import de.alcomon.hubbell.game.Upgrade
import de.alcomon.hubbell.ui.theme.PubColors

@Composable
fun ShopPanel(
    state: GameState,
    onBuyDrink: (Drink) -> Unit,
    onSelectDrink: (Drink) -> Unit,
    onBuyUpgrade: (Upgrade) -> Unit,
    onClose: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .pointerInput(Unit) { detectTapGestures { onClose() } },
    ) {
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.82f)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(PubColors.Panel)
                .pointerInput(Unit) { detectTapGestures { } }
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Upgrade-Shop (${Drink.entries.size + Upgrade.entries.size})", color = PubColors.Gold, fontWeight = FontWeight.Black, fontSize = 24.sp)
                    Text("Die Zeit steht still, solange du einkaufst.", color = PubColors.Cream.copy(alpha = 0.8f), fontSize = 12.sp)
                }
                CoinIcon(20.dp)
                Spacer(Modifier.width(6.dp))
                Text("${state.coins}", color = PubColors.Cream, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                TextButton(onClick = onClose) { Text("Schließen", color = PubColors.Gold) }
            }
            LazyColumn(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { SectionHeader("Getränke") }
                items(Drink.entries) { drink -> DrinkRow(state, drink, onBuyDrink, onSelectDrink) }
                Upgrade.entries.groupBy { it.effect }.forEach { (effect, upgrades) ->
                    item { SectionHeader(effect.category) }
                    items(upgrades) { upgrade -> UpgradeRow(state, upgrade, onBuyUpgrade) }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        color = PubColors.Gold,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
    )
}

@Composable
private fun ShopRow(
    icon: @Composable () -> Unit,
    title: String,
    description: String,
    detail: String,
    action: @Composable () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(PubColors.PanelLight)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) { icon() }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = PubColors.Cream, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(description, color = PubColors.Cream.copy(alpha = 0.8f), fontSize = 12.sp)
            Text(detail, color = PubColors.Gold, fontSize = 12.sp)
        }
        Spacer(Modifier.width(8.dp))
        Box(Modifier.widthIn(min = 96.dp), contentAlignment = Alignment.Center) { action() }
    }
}

@Composable
private fun DrinkRow(state: GameState, drink: Drink, onBuy: (Drink) -> Unit, onSelect: (Drink) -> Unit) {
    ShopRow(
        icon = { DrinkIcon(drink, 50.dp) },
        title = drink.displayName,
        description = drink.description,
        detail = "+${drink.boost.toInt()} Pegel · ${drink.coins} Taler je Glas",
    ) {
        when {
            drink in state.unlocked && drink == state.selected ->
                Text("Ausgewählt", color = PubColors.Green, fontWeight = FontWeight.Bold)
            drink in state.unlocked ->
                OutlinedButton(onClick = { onSelect(drink) }) { Text("Wählen", color = PubColors.Cream) }
            state.level < drink.requiredLevel ->
                Text("ab Level ${drink.requiredLevel}", color = PubColors.Cream.copy(alpha = 0.6f), fontSize = 13.sp)
            else -> Button(onClick = { onBuy(drink) }, enabled = GameEngine.canBuy(state, drink)) {
                Text("${drink.unlockCost} T")
            }
        }
    }
}

@Composable
private fun UpgradeRow(state: GameState, upgrade: Upgrade, onBuy: (Upgrade) -> Unit) {
    val current = state.upgradeLevel(upgrade)
    ShopRow(
        icon = { Text(upgrade.icon, fontSize = 32.sp) },
        title = upgrade.displayName,
        description = upgrade.description,
        detail = "Stufe $current von ${upgrade.maxLevel}",
    ) {
        when {
            current >= upgrade.maxLevel ->
                Text("Maximal", color = PubColors.Green, fontWeight = FontWeight.Bold)
            state.level < upgrade.requiredLevel ->
                Text("ab Level ${upgrade.requiredLevel}", color = PubColors.Cream.copy(alpha = 0.6f), fontSize = 13.sp)
            else -> Button(onClick = { onBuy(upgrade) }, enabled = GameEngine.canBuy(state, upgrade)) {
                Text("${upgrade.cost(current)} T")
            }
        }
    }
}

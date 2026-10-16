package de.alcomon.hubbell.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object PubColors {
    val Wood = Color(0xFF5A3418)
    val WoodDark = Color(0xFF2E1A0C)
    val Panel = Color(0xFF3B2414)
    val PanelLight = Color(0xFF55331C)
    val Gold = Color(0xFFF2B32A)
    val Cream = Color(0xFFFFF3D6)
    val Red = Color(0xFFE0565B)
    val DeepRed = Color(0xFFB8322F)
    val Green = Color(0xFF6CC060)
}

@Composable
fun HubbellTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = PubColors.Gold,
            onPrimary = PubColors.WoodDark,
            secondary = PubColors.Green,
            onSecondary = PubColors.WoodDark,
            background = PubColors.WoodDark,
            onBackground = PubColors.Cream,
            surface = PubColors.Panel,
            onSurface = PubColors.Cream,
            surfaceVariant = PubColors.PanelLight,
            onSurfaceVariant = PubColors.Cream,
            error = PubColors.Red,
        ),
        content = content,
    )
}

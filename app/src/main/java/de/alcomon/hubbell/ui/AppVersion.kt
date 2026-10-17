package de.alcomon.hubbell.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import de.alcomon.hubbell.ui.theme.PubColors

/** Kleine Versionsanzeige, damit man sieht, ob die aktuelle APK installiert ist. */
@Composable
fun AppVersion(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "?"
    }
    Text("v$version", modifier = modifier, color = PubColors.Cream.copy(alpha = 0.55f), fontSize = 11.sp)
}

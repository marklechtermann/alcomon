package de.alcomon.hubbell.ui

import java.util.Locale

/** Der Pegel (0–100) als augenzwinkernde Promille-Anzeige (0,0–3,0 ‰). */
fun promille(pegel: Float): String = String.format(Locale.GERMANY, "%.1f ‰", pegel / 100f * 3f)

fun formatDuration(seconds: Float): String {
    val total = seconds.toInt()
    return String.format(Locale.GERMANY, "%d:%02d", total / 60, total % 60)
}

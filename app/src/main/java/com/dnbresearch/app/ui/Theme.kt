package com.dnbresearch.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val DnbRed = Color(0xFFFF2D55)

private val colors = darkColorScheme(
    primary = DnbRed,
    onPrimary = Color.White,
    secondary = Color(0xFF7C4DFF),
    background = Color(0xFF0E0E12),
    surface = Color(0xFF17171D),
    surfaceVariant = Color(0xFF22222A),
    onBackground = Color(0xFFECECF1),
    onSurface = Color(0xFFECECF1),
    onSurfaceVariant = Color(0xFFA0A0AE),
)

@Composable
fun DnbTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, content = content)
}

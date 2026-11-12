package com.abretesesamo.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Colors = darkColorScheme(
    primary = Color(0xFFE0A458),
    onPrimary = Color(0xFF2A1A08),
    secondary = Color(0xFF8FB8DE),
    background = Color(0xFF15110E),
    surface = Color(0xFF221B16),
    surfaceVariant = Color(0xFF30271F),
    onBackground = Color(0xFFF2E8DC),
    onSurface = Color(0xFFF2E8DC),
)

@Composable
fun PuertaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, content = content)
}

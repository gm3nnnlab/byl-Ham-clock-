package com.hamdash.chornobyl.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import com.hamdash.chornobyl.domain.RadioMode

private val DarkColors = darkColorScheme(
    primary = Color(0xFF4DE8A0),
    onPrimary = Color(0xFF00210F),
    primaryContainer = Color(0xFF0F3A27),
    onPrimaryContainer = Color(0xFFB6F5D4),
    secondary = Color(0xFF5CC8FF),
    onSecondary = Color(0xFF00212E),
    tertiary = Color(0xFFFFC145),
    onTertiary = Color(0xFF2B1D00),
    background = Color(0xFF0A0E12),
    onBackground = Color(0xFFE8EEF2),
    surface = Color(0xFF111820),
    onSurface = Color(0xFFE8EEF2),
    surfaceVariant = Color(0xFF1B2530),
    onSurfaceVariant = Color(0xFFB4C0CB),
    outline = Color(0xFF3B4A59),
    error = Color(0xFFFF6B6B),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF006C45),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB6F5D4),
    onPrimaryContainer = Color(0xFF00210F),
    secondary = Color(0xFF00608A),
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFF8A5A00),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF4F7F9),
    onBackground = Color(0xFF0E1419),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0E1419),
    surfaceVariant = Color(0xFFE3EAF0),
    onSurfaceVariant = Color(0xFF3A4752),
    outline = Color(0xFF72808C),
    error = Color(0xFFB3261E),
)

@Composable
fun HamDashTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, content = content)
}

val NumberFont: FontFamily = FontFamily.Monospace

object ModeColors {
    fun of(mode: RadioMode): Color = when (mode) {
        RadioMode.DMR -> Color(0xFFFF9800)
        RadioMode.FM -> Color(0xFF4CAF50)
        RadioMode.DSTAR -> Color(0xFF42A5F5)
        RadioMode.C4FM -> Color(0xFFAB47BC)
        RadioMode.OTHER -> Color(0xFF90A4AE)
    }
}

val OnlineColor = Color(0xFF4DE8A0)
val OfflineColor = Color(0xFFFFC145)

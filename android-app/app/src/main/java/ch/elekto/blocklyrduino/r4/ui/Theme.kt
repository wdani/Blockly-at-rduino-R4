package ch.elekto.blocklyrduino.r4.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF3B66D9),
    secondary = Color(0xFF5B5FC7),
    tertiary = Color(0xFF00897B),
    surface = Color(0xFFF9FAFD),
    surfaceVariant = Color(0xFFE9EDF5)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFAFC6FF),
    secondary = Color(0xFFC4C6FF),
    tertiary = Color(0xFF80D5C8)
)

@Composable
fun ElektoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}

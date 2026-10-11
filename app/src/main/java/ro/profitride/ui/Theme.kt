package ro.profitride.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object PrColors {
    val Bg = Color(0xFF0E1116)
    val Surface = Color(0xFF171C24)
    val Surface2 = Color(0xFF1F2631)
    val Text = Color(0xFFEEF2F6)
    val Muted = Color(0xFF9AA6B2)
    val Faint = Color(0xFF7C8896)
    val Accent = Color(0xFF2EE6A6)
    val Amber = Color(0xFFFFB020)
    val Red = Color(0xFFFF5A66)
    val Blue = Color(0xFF6AA9FF)
    val Purple = Color(0xFFC7B3FF)
    val Outline = Color(0x24FFFFFF)
}

private val scheme = darkColorScheme(
    primary = PrColors.Accent,
    onPrimary = PrColors.Bg,
    secondary = PrColors.Blue,
    background = PrColors.Bg,
    onBackground = PrColors.Text,
    surface = PrColors.Surface,
    onSurface = PrColors.Text,
    surfaceVariant = PrColors.Surface2,
    onSurfaceVariant = PrColors.Muted,
    outline = PrColors.Outline,
    outlineVariant = PrColors.Outline,
    error = PrColors.Red,
)

/** Tema este mereu închisă: șoferii lucrează mult noaptea, iar bula overlay e și ea închisă la culoare. */
@Composable
fun ProfitRideTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, content = content)
}

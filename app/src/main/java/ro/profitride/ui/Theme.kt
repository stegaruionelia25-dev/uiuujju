package ro.profitride.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import ro.profitride.core.ThemeMode

class PrPalette(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val text: Color,
    val muted: Color,
    val faint: Color,
    val accent: Color,
    val amber: Color,
    val red: Color,
    val blue: Color,
    val purple: Color,
    val outline: Color,
    val tile: Color,
    val dark: Boolean,
)

private val NightPalette = PrPalette(
    bg = Color(0xFF0E1116),
    surface = Color(0xFF171C24),
    surface2 = Color(0xFF1F2631),
    text = Color(0xFFEEF2F6),
    muted = Color(0xFF9AA6B2),
    faint = Color(0xFF7C8896),
    accent = Color(0xFF2EE6A6),
    amber = Color(0xFFFFB020),
    red = Color(0xFFFF5A66),
    blue = Color(0xFF6AA9FF),
    purple = Color(0xFFC7B3FF),
    outline = Color(0x24FFFFFF),
    tile = Color(0x0DFFFFFF),
    dark = true,
)

private val DayPalette = PrPalette(
    bg = Color(0xFFF4F7FB),
    surface = Color(0xFFFFFFFF),
    surface2 = Color(0xFFE8EEF6),
    text = Color(0xFF0E1A2B),
    muted = Color(0xFF52627A),
    faint = Color(0xFF66758B),
    accent = Color(0xFF0A8F66),
    amber = Color(0xFFB36A00),
    red = Color(0xFFD42B3A),
    blue = Color(0xFF2B6AD8),
    purple = Color(0xFF7452D6),
    outline = Color(0x240E1A2B),
    tile = Color(0x0A0E1A2B),
    dark = false,
)

private val LocalPalette = staticCompositionLocalOf { NightPalette }

/** Culorile aplicației. Se schimbă automat între zi și noapte; se citesc doar în funcții @Composable. */
object PrColors {
    val Bg: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.bg
    val Surface: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.surface
    val Surface2: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.surface2
    val Text: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.text
    val Muted: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.muted
    val Faint: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.faint
    val Accent: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.accent
    val Amber: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.amber
    val Red: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.red
    val Blue: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.blue
    val Purple: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.purple
    val Outline: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.outline
    val Tile: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.tile
}

/** Dacă tema curentă e întunecată, după setarea șoferului și, la „Automat”, după telefon. */
@Composable
fun isNightTheme(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.AUTO -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun ProfitRideTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val palette = if (isNightTheme(mode)) NightPalette else DayPalette
    val scheme = if (palette.dark) {
        darkColorScheme(
            primary = palette.accent, onPrimary = palette.bg, secondary = palette.blue,
            background = palette.bg, onBackground = palette.text,
            surface = palette.surface, onSurface = palette.text,
            surfaceVariant = palette.surface2, onSurfaceVariant = palette.muted,
            outline = palette.outline, outlineVariant = palette.outline, error = palette.red,
        )
    } else {
        lightColorScheme(
            primary = palette.accent, onPrimary = Color.White, secondary = palette.blue,
            background = palette.bg, onBackground = palette.text,
            surface = palette.surface, onSurface = palette.text,
            surfaceVariant = palette.surface2, onSurfaceVariant = palette.muted,
            outline = palette.outline, outlineVariant = palette.outline, error = palette.red,
        )
    }
    CompositionLocalProvider(LocalPalette provides palette) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

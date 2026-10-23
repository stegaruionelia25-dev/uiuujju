package ro.profitride.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import ro.profitride.R
import ro.profitride.core.AppTheme
import ro.profitride.core.ThemeSpec
import ro.profitride.core.Themes

/** Fontul Netomax: Nunito, rotunjit. Nu se schimbă între teme. */
val NunitoFamily = FontFamily(
    Font(R.font.nunito_regular, FontWeight.Normal),
    Font(R.font.nunito_semibold, FontWeight.SemiBold),
    Font(R.font.nunito_bold, FontWeight.Bold),
    Font(R.font.nunito_extrabold, FontWeight.ExtraBold),
    Font(R.font.nunito_black, FontWeight.Black),
)

private fun Typography.withFont(f: FontFamily) = Typography(
    displayLarge = displayLarge.copy(fontFamily = f),
    displayMedium = displayMedium.copy(fontFamily = f),
    displaySmall = displaySmall.copy(fontFamily = f),
    headlineLarge = headlineLarge.copy(fontFamily = f),
    headlineMedium = headlineMedium.copy(fontFamily = f),
    headlineSmall = headlineSmall.copy(fontFamily = f),
    titleLarge = titleLarge.copy(fontFamily = f),
    titleMedium = titleMedium.copy(fontFamily = f),
    titleSmall = titleSmall.copy(fontFamily = f),
    bodyLarge = bodyLarge.copy(fontFamily = f),
    bodyMedium = bodyMedium.copy(fontFamily = f),
    bodySmall = bodySmall.copy(fontFamily = f),
    labelLarge = labelLarge.copy(fontFamily = f),
    labelMedium = labelMedium.copy(fontFamily = f),
    labelSmall = labelSmall.copy(fontFamily = f),
)

class PrPalette(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val text: Color,
    val muted: Color,
    val faint: Color,
    /** „bine / activ”: verde sau echivalentul lui în temă */
    val accent: Color,
    /** culoarea butoanelor principale */
    val brand: Color,
    val onBrand: Color,
    /** evidențieri: cifre mari, link-uri, fila activă */
    val hi: Color,
    val amber: Color,
    val red: Color,
    val blue: Color,
    val purple: Color,
    val outline: Color,
    val tile: Color,
    val dark: Boolean,
    val glow: Boolean,
)

private fun ThemeSpec.palette() = PrPalette(
    bg = Color(bg), surface = Color(surface), surface2 = Color(surface2),
    text = Color(text), muted = Color(muted), faint = Color(faint),
    accent = Color(good), brand = Color(brand), onBrand = Color(onBrand), hi = Color(hi),
    amber = Color(amber), red = Color(red), blue = Color(alt1), purple = Color(alt2),
    outline = Color(outline), tile = Color(tile), dark = dark, glow = glow,
)

private val LocalPalette = staticCompositionLocalOf { Themes.spec(AppTheme.BLUE).palette() }

/** Culorile aplicației. Se schimbă odată cu tema; se citesc doar în funcții @Composable. */
object PrColors {
    val Bg: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.bg
    val Surface: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.surface
    val Surface2: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.surface2
    val Text: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.text
    val Muted: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.muted
    val Faint: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.faint
    val Accent: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.accent
    val Brand: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.brand
    val OnBrand: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.onBrand
    val Hi: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.hi
    val Amber: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.amber
    val Red: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.red
    val Blue: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.blue
    val Purple: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.purple
    val Outline: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.outline
    val Tile: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.tile
}

/** Tema aplicată acum, după alegerea șoferului și, la „Automat”, după modul telefonului. */
@Composable
fun effectiveTheme(chosen: AppTheme, auto: Boolean): AppTheme =
    Themes.effective(chosen, auto, isSystemInDarkTheme())

/** Fundalul aplicației; la Ember adaugă lumina portocalie din colț. */
fun Modifier.themedBackground(spec: ThemeSpec): Modifier {
    val base = this.fillMaxSize().background(Color(spec.bg))
    if (!spec.glow) return base
    return base.drawBehind {
        val r1 = size.maxDimension * 0.75f
        drawRect(
            Brush.radialGradient(
                colors = listOf(Color(0x6BFF5A1F), Color.Transparent),
                center = Offset(size.width * 1.05f, -size.height * 0.04f),
                radius = r1,
            ),
        )
        drawRect(
            Brush.radialGradient(
                colors = listOf(Color(0xFF8C240A).copy(alpha = 0.38f), Color.Transparent),
                center = Offset(-size.width * 0.1f, size.height * 1.04f),
                radius = r1 * 0.8f,
            ),
        )
    }
}

@Composable
fun ProfitRideTheme(theme: AppTheme, content: @Composable () -> Unit) {
    val palette = Themes.spec(theme).palette()
    val scheme = if (palette.dark) {
        darkColorScheme(
            primary = palette.brand, onPrimary = palette.onBrand, secondary = palette.blue,
            background = palette.bg, onBackground = palette.text,
            surface = palette.surface, onSurface = palette.text,
            surfaceVariant = palette.surface2, onSurfaceVariant = palette.muted,
            outline = palette.outline, outlineVariant = palette.outline, error = palette.red,
        )
    } else {
        lightColorScheme(
            primary = palette.brand, onPrimary = palette.onBrand, secondary = palette.blue,
            background = palette.bg, onBackground = palette.text,
            surface = palette.surface, onSurface = palette.text,
            surfaceVariant = palette.surface2, onSurfaceVariant = palette.muted,
            outline = palette.outline, outlineVariant = palette.outline, error = palette.red,
        )
    }
    CompositionLocalProvider(LocalPalette provides palette) {
        MaterialTheme(colorScheme = scheme, typography = MaterialTheme.typography.withFont(NunitoFamily), content = content)
    }
}

/** Culorile foilor (editare, explicații, tutorial): fundal alb, text închis, accent albastru, în toate temele. */
private val SheetPalette = PrPalette(
    bg = Color.White, surface = Color(0xFFEEF2FF), surface2 = Color(0xFFE3E9FB),
    text = Color(0xFF0E1A2B), muted = Color(0xFF34425E), faint = Color(0xFF5B6B8C),
    accent = Color(0xFF08764C), brand = Color(0xFF2F5BFF), onBrand = Color.White, hi = Color(0xFF2F5BFF),
    amber = Color(0xFF8F5200), red = Color(0xFFC62839), blue = Color(0xFF2F5BFF), purple = Color(0xFF7452D6),
    outline = Color(0xFFC9D3F5), tile = Color(0xFFEEF2FF), dark = false, glow = false,
)

/** Pune conținutul unei foi pe fundal alb: componentele obișnuite își iau culorile de aici. */
@Composable
fun SheetTheme(content: @Composable () -> Unit) {
    val p = SheetPalette
    val scheme = lightColorScheme(
        primary = p.brand, onPrimary = p.onBrand, secondary = p.blue,
        background = p.bg, onBackground = p.text,
        surface = p.bg, onSurface = p.text,
        surfaceVariant = p.surface, onSurfaceVariant = p.muted,
        outline = p.outline, outlineVariant = p.outline, error = p.red,
    )
    CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(colorScheme = scheme, typography = MaterialTheme.typography, content = content)
    }
}

/** Culoarea de fundal și de text a foilor, pentru containerele din afara lui [SheetTheme]. */
val SheetContainer: Color = SheetPalette.bg
val SheetText: Color = SheetPalette.text

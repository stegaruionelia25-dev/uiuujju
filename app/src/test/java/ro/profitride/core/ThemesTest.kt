package ro.profitride.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemesTest {

    @Test
    fun everyThemeHasASpec() {
        AppTheme.entries.forEach { Themes.spec(it) }
    }

    @Test
    fun autoThemeMovesLightThemesToNightWhenPhoneIsDark() {
        assertEquals(AppTheme.NOAPTE, Themes.effective(AppTheme.BLUE, auto = true, systemDark = true))
        assertEquals(AppTheme.NOAPTE, Themes.effective(AppTheme.DESCHIS, auto = true, systemDark = true))
    }

    @Test
    fun autoThemeKeepsDarkThemes() {
        assertEquals(AppTheme.EMBER, Themes.effective(AppTheme.EMBER, auto = true, systemDark = true))
        assertEquals(AppTheme.NOAPTE, Themes.effective(AppTheme.NOAPTE, auto = true, systemDark = true))
    }

    @Test
    fun withoutAutoTheChosenThemeAlwaysWins() {
        assertEquals(AppTheme.BLUE, Themes.effective(AppTheme.BLUE, auto = false, systemDark = true))
        assertEquals(AppTheme.DESCHIS, Themes.effective(AppTheme.DESCHIS, auto = true, systemDark = false))
    }

    @Test
    fun bubbleScaleIsClampedToSeventyAndOneThirty() {
        assertEquals(70, BubbleScale.clamp(10))
        assertEquals(130, BubbleScale.clamp(400))
        assertEquals(100, BubbleScale.clamp(100))
    }

    @Test
    fun bubbleScaleNeverExceedsScreenWidth() {
        val s = BubbleScale.effective(130, widthCap = 1.1f, heightCap = 3f)
        assertEquals(1.1f, s, 1e-6f)
    }

    @Test
    fun bubbleScaleStaysAboveHeightLimitOnlyWhenEnlarged() {
        // peste 100%: limitat de înălțime
        assertEquals(1.0f, BubbleScale.effective(130, widthCap = 2f, heightCap = 0.8f), 1e-6f)
        // sub 100%: limita de înălțime nu se aplică
        assertEquals(0.8f, BubbleScale.effective(80, widthCap = 2f, heightCap = 0.5f), 1e-6f)
    }

    @Test
    fun bubbleScaleNeverGoesBelowMinimum() {
        assertTrue(BubbleScale.effective(70, widthCap = 0.3f, heightCap = 0.3f) >= 0.7f)
    }
}

package ro.profitride.core

/** Temele aplicației. Schimbă doar culorile; fontul, calculele și textele rămân la fel. */
enum class AppTheme { BLUE, NOAPTE, DESCHIS, EMBER }

/**
 * Culorile unei teme, ca valori ARGB (Long), ca să poată fi folosite și de Compose, și de bula clasică (View).
 * `accent` = culoarea semantică „bine / activ” (verde), `brand` = culoarea butoanelor, `hi` = evidențieri.
 */
class ThemeSpec(
    val bg: Long,
    val surface: Long,
    val surface2: Long,
    val text: Long,
    val muted: Long,
    val faint: Long,
    val brand: Long,
    val onBrand: Long,
    val hi: Long,
    val good: Long,
    val amber: Long,
    val red: Long,
    val alt1: Long,
    val alt2: Long,
    val outline: Long,
    val tile: Long,
    /** fundal închis: pictogramele barei de stare sunt deschise la culoare */
    val dark: Boolean,
    val bubbleBg: Long,
    val bubbleBorder: Long,
    val bubbleText: Long,
    val bubbleMuted: Long,
    val bubbleTrack: Long,
    /** true: „pastila” verdictului e plină și albă (tema Blue); false: transparentă, în culoarea verdictului */
    val chipSolid: Boolean,
    val pillGood: Long,
    val pillMid: Long,
    val pillBad: Long,
    /** inelul scorului e alb pe toate verdictele (tema Blue) */
    val ringWhite: Boolean,
    /** lumina portocalie din fundal (tema Ember) */
    val glow: Boolean,
    /** culoarea textului „peste medie” din bulă */
    val deltaUp: Long,
    val deltaDown: Long,
)

object Themes {
    private val blue = ThemeSpec(
        bg = 0xFF2F5BFFL, surface = 0xFF2449E0L, surface2 = 0xFF1B3AC2L,
        text = 0xFFFFFFFFL, muted = 0xDBFFFFFFL, faint = 0xB8FFFFFFL,
        brand = 0xFFFFFFFFL, onBrand = 0xFF2F5BFFL, hi = 0xFFFFD83DL,
        good = 0xFF7DF5C0L, amber = 0xFFFFD83DL, red = 0xFFFFA3ADL,
        alt1 = 0xFFFFD83DL, alt2 = 0x73FFFFFFL, outline = 0x33FFFFFFL, tile = 0x1FFFFFFFL,
        dark = true,
        bubbleBg = 0xFF2F5BFFL, bubbleBorder = 0x47FFFFFFL, bubbleText = 0xFFFFFFFFL,
        bubbleMuted = 0xD1FFFFFFL, bubbleTrack = 0x47FFFFFFL,
        chipSolid = true, pillGood = 0xFF08764CL, pillMid = 0xFF8F5200L, pillBad = 0xFFC62839L,
        ringWhite = true, glow = false, deltaUp = 0xFFFFD83DL, deltaDown = 0xFFFFD83DL,
    )
    private val noapte = ThemeSpec(
        bg = 0xFF0C1433L, surface = 0xFF151F4AL, surface2 = 0xFF1D2A5EL,
        text = 0xFFF2F5FFL, muted = 0xE0F2F5FFL, faint = 0xB8F2F5FFL,
        brand = 0xFF6C94FFL, onBrand = 0xFF0C1433L, hi = 0xFF8FB0FFL,
        good = 0xFF3DDC97L, amber = 0xFFFFC84AL, red = 0xFFFF7A88L,
        alt1 = 0xFF6AA9FFL, alt2 = 0xFFC7B3FFL, outline = 0x33FFFFFFL, tile = 0x0DFFFFFFL,
        dark = true,
        bubbleBg = 0xF2151F4AL, bubbleBorder = 0x806C94FFL, bubbleText = 0xFFF2F5FFL,
        bubbleMuted = 0xB3F2F5FFL, bubbleTrack = 0x29FFFFFFL,
        chipSolid = false, pillGood = 0xFF3DDC97L, pillMid = 0xFFFFC84AL, pillBad = 0xFFFF7A88L,
        ringWhite = false, glow = false, deltaUp = 0xFF3DDC97L, deltaDown = 0xFFFF7A88L,
    )
    private val deschis = ThemeSpec(
        bg = 0xFFF3F6FBL, surface = 0xFFFFFFFFL, surface2 = 0xFFE7EDF7L,
        text = 0xFF0E1A2BL, muted = 0xFF4A5B78L, faint = 0xFF66758BL,
        brand = 0xFF2F5BFFL, onBrand = 0xFFFFFFFFL, hi = 0xFF2F5BFFL,
        good = 0xFF0A7F53L, amber = 0xFF9A5B00L, red = 0xFFC62839L,
        alt1 = 0xFF2B6AD8L, alt2 = 0xFF7452D6L, outline = 0x240E1A2BL, tile = 0x0A0E1A2BL,
        dark = false,
        bubbleBg = 0xFFFFFFFFL, bubbleBorder = 0xFFC4D0E6L, bubbleText = 0xFF0E1A2BL,
        bubbleMuted = 0xFF4A5B78L, bubbleTrack = 0x1F0E1A2BL,
        chipSolid = false, pillGood = 0xFF0A7F53L, pillMid = 0xFF9A5B00L, pillBad = 0xFFC62839L,
        ringWhite = false, glow = false, deltaUp = 0xFF0A7F53L, deltaDown = 0xFFC62839L,
    )
    private val ember = ThemeSpec(
        bg = 0xFF08080AL, surface = 0xFF16110FL, surface2 = 0xFF221915L,
        text = 0xFFFFF3ECL, muted = 0xE0FFF3ECL, faint = 0xB8FFF3ECL,
        brand = 0xFFFF5A1FL, onBrand = 0xFF1A0A03L, hi = 0xFFFF8A3DL,
        good = 0xFF34E3B0L, amber = 0xFFFFD23FL, red = 0xFFFF5C98L,
        alt1 = 0xFF8FB0FFL, alt2 = 0xFFC7B3FFL, outline = 0x40FFC8AAL, tile = 0x0DFFFFFFL,
        dark = true,
        bubbleBg = 0xFF101722L, bubbleBorder = 0x4796B4FFL, bubbleText = 0xFFF4F7FFL,
        bubbleMuted = 0xB3F4F7FFL, bubbleTrack = 0x24FFFFFFL,
        chipSolid = false, pillGood = 0xFF34E3B0L, pillMid = 0xFFFFD23FL, pillBad = 0xFFFF5C98L,
        ringWhite = false, glow = true, deltaUp = 0xFF34E3B0L, deltaDown = 0xFFFF5C98L,
    )

    fun spec(theme: AppTheme): ThemeSpec = when (theme) {
        AppTheme.BLUE -> blue
        AppTheme.NOAPTE -> noapte
        AppTheme.DESCHIS -> deschis
        AppTheme.EMBER -> ember
    }

    /**
     * Tema aplicată acum. La „Automat, după lumină”, dacă telefonul e pe modul întunecat, temele luminoase
     * (Blue, Deschis) trec pe Noapte; Noapte și Ember rămân cum sunt.
     */
    fun effective(chosen: AppTheme, auto: Boolean, systemDark: Boolean): AppTheme =
        if (auto && systemDark && (chosen == AppTheme.BLUE || chosen == AppTheme.DESCHIS)) AppTheme.NOAPTE else chosen
}

/** Mărimea bulei de verdict: procent din mărimea normală, cu limite care o țin pe ecran. */
object BubbleScale {
    const val MIN = 70
    const val MAX = 130
    const val DEFAULT = 100

    fun clamp(percent: Int): Int = percent.coerceIn(MIN, MAX)

    /**
     * Factorul real aplicat bulei. Nu depășește lățimea ecranului (`widthCap`), iar peste 100% nici zona
     * de deasupra butoanelor Acceptă / Refuză (`heightCap`). Sub 100% nu se aplică limita de înălțime.
     */
    fun effective(percent: Int, widthCap: Float, heightCap: Float): Float {
        val wanted = clamp(percent) / 100f
        val limit = minOf(widthCap, maxOf(1f, heightCap))
        return minOf(wanted, maxOf(limit, MIN / 100f))
    }
}

package ro.profitride.core

/** Un text vizibil pe ecran, cu poziția lui (pentru a-l citi de sus în jos). */
data class ScreenText(val text: String, val top: Int = 0, val left: Int = 0)

data class ParsedOffer(
    val price: Double,
    val pickupKm: Double,
    val pickupMin: Double,
    val tripKm: Double,
    val tripMin: Double,
    /** ecranul spune că prețul e deja net („NET, taxe incluse”, „Net of Service Fee”) */
    val priceIsNet: Boolean = false,
) {
    fun toOffer(platform: Platform) = RideOffer(platform, price, pickupKm, tripKm, pickupMin, tripMin, priceIsNet)
}

enum class Missing { DISTANCE, TIME }

sealed interface ParseResult {
    /** Nu pare o ofertă de cursă. */
    data object NoOffer : ParseResult

    data class Complete(val offer: ParsedOffer) : ParseResult

    /** Există preț și un început de date, dar nu destule pentru a calcula. */
    data class Incomplete(val missing: Set<Missing>) : ParseResult

    /** Prea multe valori sau valori absurde: mai bine nu afișăm nimic decât o cifră greșită. */
    data object Unsure : ParseResult
}

/**
 * Citește oferta din textele vizibile ale ecranului. Bolt și Uber nu au API public,
 * deci parserul caută tipare generale (preț cu monedă, „x min”, „y km”) și presupune:
 *  - prima pereche timp+distanță este ridicarea, a doua este traseul;
 *  - prețul cursei este cea mai mare sumă care nu începe cu „+” (bonusurile sunt „+x”).
 * Ce nu poate fi citit sigur nu se afișează. Ecranul de calibrare din aplicație arată
 * exact ce a citit parserul pe telefonul șoferului.
 */
object OfferParser {

    private const val CUR =
        "(?:lei|ron|eur|€|£|gbp|\\$|usd|zł|zl|pln|kč|czk|ft|huf|лв|bgn|kr|sek|nok|dkk|chf|₺|try|mdl|rsd)"
    private const val NUM =
        "\\d{1,3}(?:[ .,\\u00A0]\\d{3})+(?:[.,]\\d{1,2})?|\\d+(?:[.,]\\d{1,2})?"

    private val priceNumFirst = Regex("(?<![\\p{L}\\d.,])($NUM)\\s?$CUR(?![\\p{L}])", RegexOption.IGNORE_CASE)
    private val priceCurFirst = Regex("(?<![\\p{L}])$CUR\\s?($NUM)(?!\\d)", RegexOption.IGNORE_CASE)

    private val distance = Regex(
        "(?<![\\p{L}\\d.,])(\\d+(?:[.,]\\d+)?)\\s?(km|mi|m)(?![\\p{L}/²])",
        RegexOption.IGNORE_CASE,
    )

    // 1 = ore, 2 = minute (ore + minute) | 3 = doar minute | 4 = doar ore
    private val time = Regex(
        "(?<![\\p{L}\\d.,])(?:(\\d+)\\s?(?:h|hr|hrs|ore|ora|oră)\\s?(\\d+)\\s?min" +
            "|(\\d+)\\s?min(?:ute?s?|s)?" +
            "|(\\d+)\\s?(?:h|hr|hrs|ore))(?![\\p{L}])",
        RegexOption.IGNORE_CASE,
    )

    /** „NET, taxe incluse” (Bolt), „Net of Service Fee” (Uber): comisionul platformei e deja scăzut din preț. */
    private val netPrice = Regex("(?<![\\p{L}])net(?![\\p{L}])", RegexOption.IGNORE_CASE)

    private const val MAX_PRICE = 1_000_000.0
    private const val MAX_KM = 1_000.0
    private const val MAX_MIN = 1_440.0

    fun parse(nodes: List<ScreenText>): ParseResult {
        if (nodes.isEmpty()) return ParseResult.NoOffer
        val text = nodes
            .sortedWith(compareBy<ScreenText>({ it.top / 16 }, { it.left }))
            .joinToString(" | ") { it.text }

        val prices = findPrices(text)
        if (prices.isEmpty()) return ParseResult.NoOffer

        val dists = findDistancesKm(text)
        val times = findMinutes(text)
        if (dists.isEmpty() && times.isEmpty()) return ParseResult.NoOffer

        val missing = buildSet {
            if (dists.size < 2) add(Missing.DISTANCE)
            if (times.size < 2) add(Missing.TIME)
        }
        if (missing.isNotEmpty()) return ParseResult.Incomplete(missing)
        if (dists.size > 2 || times.size > 2) return ParseResult.Unsure

        val price = prices.max()
        val ok = price in 0.0..MAX_PRICE && price > 0 &&
            dists.all { it in 0.0..MAX_KM } && times.all { it in 0.0..MAX_MIN }
        if (!ok) return ParseResult.Unsure

        return ParseResult.Complete(
            ParsedOffer(
                price = price,
                pickupKm = dists[0],
                pickupMin = times[0],
                tripKm = dists[1],
                tripMin = times[1],
                priceIsNet = netPrice.containsMatchIn(text),
            ),
        )
    }

    private fun findPrices(text: String): List<Double> {
        val out = ArrayList<Double>()
        fun take(m: MatchResult) {
            if (precededByPlus(text, m.range.first)) return
            val v = parseNumber(m.groupValues[1]) ?: return
            if (v > 0 && v <= MAX_PRICE) out.add(v)
        }
        priceNumFirst.findAll(text).forEach(::take)
        priceCurFirst.findAll(text).forEach(::take)
        return out
    }

    private fun precededByPlus(text: String, start: Int): Boolean {
        var i = start - 1
        while (i >= 0 && (text[i] == ' ' || text[i] == ' ')) i--
        return i >= 0 && text[i] == '+'
    }

    private fun findDistancesKm(text: String): List<Double> =
        distance.findAll(text).mapNotNull { m ->
            val v = parseNumber(m.groupValues[1]) ?: return@mapNotNull null
            when (m.groupValues[2].lowercase()) {
                "km" -> v
                "mi" -> v * 1.609344
                else -> v / 1000.0 // metri
            }
        }.toList()

    private fun findMinutes(text: String): List<Double> =
        time.findAll(text).mapNotNull { m ->
            val g = m.groupValues
            when {
                g[1].isNotEmpty() && g[2].isNotEmpty() -> g[1].toDouble() * 60 + g[2].toDouble()
                g[3].isNotEmpty() -> g[3].toDouble()
                g[4].isNotEmpty() -> g[4].toDouble() * 60
                else -> null
            }
        }.toList()

    /**
     * Acceptă „12,50”, „12.50”, „1.234,50”, „1,234.50”, „1 234”.
     * Un singur separator urmat de exact 3 cifre este tratat ca separator de mii.
     */
    internal fun parseNumber(raw: String): Double? {
        val s = raw.replace(' ', ' ').trim()
        if (s.isEmpty()) return null
        val lastDot = s.lastIndexOf('.')
        val lastComma = s.lastIndexOf(',')
        val decimalSep: Char? = when {
            lastDot >= 0 && lastComma >= 0 -> if (lastDot > lastComma) '.' else ','
            lastDot >= 0 || lastComma >= 0 -> {
                val sep = if (lastDot >= 0) '.' else ','
                val digitsAfter = s.length - s.lastIndexOf(sep) - 1
                if (s.count { it == sep } == 1 && digitsAfter in 1..2) sep else null
            }
            else -> null
        }
        val cleaned = buildString {
            for (c in s) {
                when {
                    c.isDigit() -> append(c)
                    c == decimalSep -> append('.')
                }
            }
        }
        return cleaned.toDoubleOrNull()
    }
}

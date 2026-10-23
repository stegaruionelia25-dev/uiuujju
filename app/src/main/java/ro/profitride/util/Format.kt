package ro.profitride.util

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import kotlin.math.abs
import ro.profitride.core.Country
import ro.profitride.core.DistanceUnit

/** Formatare după limba telefonului: „16,9” în română, „16.9” în engleză. */
object Format {

    fun number(value: Double, decimals: Int, locale: Locale = Locale.getDefault()): String {
        val nf = NumberFormat.getNumberInstance(locale)
        nf.minimumFractionDigits = decimals
        nf.maximumFractionDigits = decimals
        nf.isGroupingUsed = false
        return nf.format(value)
    }

    /** Fără zerouri inutile, pentru câmpurile de text: 6,8 / 7,4 / 40000. */
    fun plain(value: Double, locale: Locale = Locale.getDefault()): String {
        val nf = NumberFormat.getNumberInstance(locale)
        nf.minimumFractionDigits = 0
        nf.maximumFractionDigits = 2
        nf.isGroupingUsed = false
        return nf.format(value)
    }

    /** „+16,9” sau „−4,2”; fără „−0,0”. */
    fun signed(value: Double, decimals: Int, locale: Locale = Locale.getDefault()): String {
        val text = number(abs(value), decimals, locale)
        val isZero = text.all { !it.isDigit() || it == '0' }
        return (if (value < 0 && !isZero) "−" else "+") + text
    }

    fun currencySymbol(code: String): String = when (code) {
        "RON" -> "lei"
        else -> runCatching { Currency.getInstance(code).symbol }.getOrDefault(code)
    }

    fun distance(km: Double, unit: DistanceUnit, decimals: Int = 1): String =
        number(km / unit.kmPerUnit, decimals)

    fun hour(h: Int): String = String.format(Locale.ROOT, "%02d:00", h)

    fun countryName(country: Country, locale: Locale = Locale.getDefault()): String =
        if (locale.language == "ro") country.nameRo else country.nameEn

    /** Acceptă „6,8” și „6.8” din câmpurile de text; gol sau invalid => null. */
    fun parseInput(text: String): Double? {
        val t = text.trim().replace(',', '.')
        if (t.isEmpty()) return null
        return t.toDoubleOrNull()
    }
}

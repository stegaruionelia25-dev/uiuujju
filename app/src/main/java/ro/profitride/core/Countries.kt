package ro.profitride.core

/**
 * Țările în care sunt prezente de obicei Bolt și Uber. Lista oferă doar moneda și unitatea
 * de distanță implicite; comisioanele, prețurile și costurile le introduce mereu șoferul.
 * „OTHER” folosește setările telefonului.
 */
data class Country(
    val code: String,
    val nameRo: String,
    val nameEn: String,
    val currency: String,
    val unit: DistanceUnit,
)

object Countries {
    private val km = DistanceUnit.KM

    val all: List<Country> = listOf(
        Country("RO", "România", "Romania", "RON", km),
        Country("BG", "Bulgaria", "Bulgaria", "EUR", km),
        Country("HU", "Ungaria", "Hungary", "HUF", km),
        Country("PL", "Polonia", "Poland", "PLN", km),
        Country("CZ", "Cehia", "Czechia", "CZK", km),
        Country("SK", "Slovacia", "Slovakia", "EUR", km),
        Country("HR", "Croația", "Croatia", "EUR", km),
        Country("SI", "Slovenia", "Slovenia", "EUR", km),
        Country("RS", "Serbia", "Serbia", "RSD", km),
        Country("MD", "Moldova", "Moldova", "MDL", km),
        Country("GR", "Grecia", "Greece", "EUR", km),
        Country("DE", "Germania", "Germany", "EUR", km),
        Country("AT", "Austria", "Austria", "EUR", km),
        Country("FR", "Franța", "France", "EUR", km),
        Country("ES", "Spania", "Spain", "EUR", km),
        Country("PT", "Portugalia", "Portugal", "EUR", km),
        Country("IT", "Italia", "Italy", "EUR", km),
        Country("NL", "Țările de Jos", "Netherlands", "EUR", km),
        Country("BE", "Belgia", "Belgium", "EUR", km),
        Country("IE", "Irlanda", "Ireland", "EUR", km),
        Country("GB", "Regatul Unit", "United Kingdom", "GBP", DistanceUnit.MI),
        Country("SE", "Suedia", "Sweden", "SEK", km),
        Country("NO", "Norvegia", "Norway", "NOK", km),
        Country("DK", "Danemarca", "Denmark", "DKK", km),
        Country("FI", "Finlanda", "Finland", "EUR", km),
        Country("EE", "Estonia", "Estonia", "EUR", km),
        Country("LV", "Letonia", "Latvia", "EUR", km),
        Country("LT", "Lituania", "Lithuania", "EUR", km),
        Country("CH", "Elveția", "Switzerland", "CHF", km),
        Country("OTHER", "Altă țară", "Other country", "EUR", km),
    )

    val default: Country = all.first()

    fun byCode(code: String): Country = all.firstOrNull { it.code == code } ?: default
}

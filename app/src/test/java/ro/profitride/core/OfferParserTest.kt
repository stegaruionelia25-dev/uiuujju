package ro.profitride.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfferParserTest {

    private fun nodes(vararg t: String) = t.map { ScreenText(it) }

    private fun complete(r: ParseResult): ParsedOffer {
        assertTrue("așteptat Complete, primit $r", r is ParseResult.Complete)
        return (r as ParseResult.Complete).offer
    }

    @Test
    fun readsSeparateNodes() {
        val o = complete(OfferParser.parse(nodes("Cursă nouă", "31,50 lei", "5 min", "2,1 km", "19 min", "9,4 km")))
        assertEquals(31.5, o.price, 1e-9)
        assertEquals(5.0, o.pickupMin, 1e-9)
        assertEquals(2.1, o.pickupKm, 1e-9)
        assertEquals(19.0, o.tripMin, 1e-9)
        assertEquals(9.4, o.tripKm, 1e-9)
    }

    @Test
    fun readsCombinedNodesAndCurrencyFirst() {
        val o = complete(OfferParser.parse(nodes("RON 31.50", "5 min (2.1 km) away", "19 min (9.4 km) trip")))
        assertEquals(31.5, o.price, 1e-9)
        assertEquals(2.1, o.pickupKm, 1e-9)
        assertEquals(9.4, o.tripKm, 1e-9)
    }

    @Test
    fun convertsMetersAndMiles() {
        val m = complete(OfferParser.parse(nodes("12 lei", "3 min", "800 m", "10 min", "4 km")))
        assertEquals(0.8, m.pickupKm, 1e-9)

        val mi = complete(OfferParser.parse(nodes("£12.40", "4 min", "0.5 mi", "11 min", "3.2 mi")))
        assertEquals(0.5 * 1.609344, mi.pickupKm, 1e-9)
        assertEquals(3.2 * 1.609344, mi.tripKm, 1e-9)
        assertEquals(12.40, mi.price, 1e-9)
    }

    @Test
    fun parsesNumberFormats() {
        assertEquals(1234.5, OfferParser.parseNumber("1.234,50")!!, 1e-9)
        assertEquals(1234.5, OfferParser.parseNumber("1,234.50")!!, 1e-9)
        assertEquals(12.5, OfferParser.parseNumber("12,5")!!, 1e-9)
        assertEquals(12.5, OfferParser.parseNumber("12.50")!!, 1e-9)
        assertEquals(1234.0, OfferParser.parseNumber("1 234")!!, 1e-9)
        assertEquals(12500.0, OfferParser.parseNumber("12.500")!!, 1e-9)
    }

    @Test
    fun ignoresBonusesStartingWithPlus() {
        val o = complete(OfferParser.parse(nodes("31,50 lei", "+ 2,00 lei bonus", "5 min", "2 km", "19 min", "9 km")))
        assertEquals(31.5, o.price, 1e-9)
    }

    @Test
    fun picksLargestAmountAsFare() {
        val o = complete(OfferParser.parse(nodes("4,00 lei", "31,50 lei", "5 min", "2 km", "19 min", "9 km")))
        assertEquals(31.5, o.price, 1e-9)
    }

    @Test
    fun doesNotTreatSpeedOrMinutesAsDistance() {
        val r = OfferParser.parse(nodes("31,50 lei", "limită 50 km/h", "5 min", "19 min"))
        assertTrue(r is ParseResult.Incomplete)
        assertEquals(setOf(Missing.DISTANCE), (r as ParseResult.Incomplete).missing)
    }

    @Test
    fun readsHoursAndMinutes() {
        val o = complete(OfferParser.parse(nodes("85 lei", "5 min", "2 km", "1 h 5 min", "40 km")))
        assertEquals(65.0, o.tripMin, 1e-9)
    }

    @Test
    fun nonOfferScreensAreIgnored() {
        assertEquals(ParseResult.NoOffer, OfferParser.parse(nodes("Ești online", "Se caută curse")))
        assertEquals(ParseResult.NoOffer, OfferParser.parse(nodes("Câștiguri azi", "125 lei", "12 curse")))
        assertEquals(ParseResult.NoOffer, OfferParser.parse(emptyList()))
    }

    @Test
    fun partialOfferIsIncomplete() {
        val r = OfferParser.parse(nodes("31,50 lei", "5 min", "2 km"))
        assertTrue(r is ParseResult.Incomplete)
        assertEquals(setOf(Missing.DISTANCE, Missing.TIME), (r as ParseResult.Incomplete).missing)
    }

    @Test
    fun tooManyValuesIsUnsure() {
        val r = OfferParser.parse(nodes("31,50 lei", "5 min", "2 km", "19 min", "9 km", "30 min", "20 km"))
        assertEquals(ParseResult.Unsure, r)
    }

    @Test
    fun readsInScreenOrderRegardlessOfNodeOrder() {
        val r = OfferParser.parse(
            listOf(
                ScreenText("9,4 km", top = 600, left = 300),
                ScreenText("2,1 km", top = 400, left = 300),
                ScreenText("31,50 lei", top = 100, left = 40),
                ScreenText("19 min", top = 600, left = 40),
                ScreenText("5 min", top = 400, left = 40),
            ),
        )
        val o = complete(r)
        assertEquals(2.1, o.pickupKm, 1e-9)
        assertEquals(9.4, o.tripKm, 1e-9)
        assertEquals(5.0, o.pickupMin, 1e-9)
    }

    @Test
    fun parsedOfferBecomesRideOffer() {
        val o = complete(OfferParser.parse(nodes("20 lei", "5 min", "2 km", "10 min", "5 km"))).toOffer(Platform.UBER)
        assertEquals(Platform.UBER, o.platform)
        assertEquals(7.0, o.totalKm, 1e-9)
        assertEquals(15.0, o.totalMin, 1e-9)
    }

    @Test
    fun readsRealBoltOfferScreen() {
        val o = complete(
            OfferParser.parse(
                nodes(
                    "Refuză", "Bolt", "Numerar", "În afara razei", "Locație", "Oră",
                    "9,82 lei", "(NET, taxe incluse)", "Respingerea cursei nu va afecta rata de acceptare",
                    "Daniela", "4.6", "6 min • 2.8 km", "Str. Mircea Cel Batran 194, Ploiești",
                    "7 min • 3.5 km", "Str. Panduri 69, Corlătești 107063", "Acceptă",
                ),
            ),
        )
        assertEquals(9.82, o.price, 1e-9)
        assertEquals(6.0, o.pickupMin, 1e-9)
        assertEquals(2.8, o.pickupKm, 1e-9)
        assertEquals(7.0, o.tripMin, 1e-9)
        assertEquals(3.5, o.tripKm, 1e-9)
        assertTrue(o.priceIsNet)
    }

    @Test
    fun readsBoltOfferWhenEveryPieceIsItsOwnNode() {
        val o = complete(
            OfferParser.parse(
                nodes("9,82 lei (NET, taxe incluse)", "Daniela • 4.6 ★", "6 min", "•", "2.8 km", "Str. Mircea Cel Batran 194", "7 min", "•", "3.5 km", "Str. Panduri 69"),
            ),
        )
        assertEquals(9.82, o.price, 1e-9)
        assertEquals(2.8, o.pickupKm, 1e-9)
        assertEquals(3.5, o.tripKm, 1e-9)
    }

    @Test
    fun netPriceIsNotChargedCommissionTwice() {
        val profile = DriverProfile(consumption = 6.8, energyPrice = 7.4, yearlyKm = 40_000.0, commissionBoltPct = 20.0, goodThreshold = 40.0)
        val gross = ProfitCalculator.evaluate(RideOffer(Platform.BOLT, 10.0, 2.8, 3.5, 6.0, 7.0), profile)
        val net = ProfitCalculator.evaluate(RideOffer(Platform.BOLT, 10.0, 2.8, 3.5, 6.0, 7.0, priceIsNet = true), profile)
        assertEquals(2.0, gross.commission, 1e-9)
        assertEquals(0.0, net.commission, 1e-9)
    }

    @Test
    fun plainPriceWithoutNetWordIsNotMarkedNet() {
        val o = complete(OfferParser.parse(nodes("31,50 lei", "5 min", "2 km", "19 min", "9 km", "Internet")))
        assertTrue(!o.priceIsNet)
    }

    // ---- Ecrane reale din UK (poze trimise de șofer). Ordinea nodurilor este presupusă.

    @Test
    fun boltUkOfferIgnoresTollAndHolidayPayBreakdown() {
        val o = complete(
            OfferParser.parse(
                nodes(
                    "Bolt Airport", "Toll Fee • £7.00", "£38.69", "(£36.74 + £1.95 est. holiday pay)",
                    "Hajizadeh • 5.0 ★ (20)", "1 min • 0.3 mi", "58 Streatfeild Avenue, London E6 2LA",
                    "58 min • 45.2 mi", "London Stansted Airport, Departures, London Stansted Airport (STN)", "Accept",
                ),
            ),
        )
        assertEquals(38.69, o.price, 1e-9)
        assertEquals(0.3 * 1.609344, o.pickupKm, 1e-9)
        assertEquals(45.2 * 1.609344, o.tripKm, 1e-9)
        assertEquals(1.0, o.pickupMin, 1e-9)
        assertEquals(58.0, o.tripMin, 1e-9)
        assertTrue(!o.tripMinEstimated)
    }

    @Test
    fun tollBiggerThanFareIsNotThePrice() {
        val o = complete(OfferParser.parse(nodes("Toll Fee • £7.00", "£5.20", "3 min • 1.0 mi", "12 min • 3.0 mi")))
        assertEquals(5.2, o.price, 1e-9)
    }

    @Test
    fun uberUkOfferWithBracketedMiles() {
        val o = complete(
            OfferParser.parse(
                nodes(
                    "UberX", "Exclusive", "£39.57", "4.86", "£36.97 + est. holiday pay of £2.60",
                    "1 min (0 mi)", "John Cabot House, London, E16 2BA", "47 mins (34.2 mi)", "Ryanair, Stansted, CM24 1QW", "Confirm",
                ),
            ),
        )
        assertEquals(39.57, o.price, 1e-9)
        assertEquals(0.0, o.pickupKm, 1e-9)
        assertEquals(34.2 * 1.609344, o.tripKm, 1e-9)
        assertEquals(47.0, o.tripMin, 1e-9)
    }

    @Test
    fun uberUkAndroidWithoutTripDurationIsEstimated() {
        val o = complete(
            OfferParser.parse(
                nodes(
                    "Green", "£5.17", "4.91", "£4.73 + est. holiday pay of £0.44",
                    "5 mins (1.3 mi) away", "WD18, Watford", "1.5 mi trip", "WD17, Watford", "Confirm",
                ),
            ),
        )
        assertEquals(5.17, o.price, 1e-9)
        assertEquals(5.0, o.pickupMin, 1e-9)
        assertEquals(1.3 * 1.609344, o.pickupKm, 1e-9)
        assertEquals(1.5 * 1.609344, o.tripKm, 1e-9)
        assertTrue(o.tripMinEstimated)
        // 1.3 mi în 5 min = 25 km/h; 2.41 km / 25 km/h = 5.8 min
        assertEquals(1.5 * 1.609344 / (1.3 * 1.609344 / 5.0 * 60) * 60, o.tripMin, 1e-6)
    }

    @Test
    fun boltUkEndOfRideAndInTripScreensAreNotOffers() {
        assertEquals(
            ParseResult.NoOffer,
            OfferParser.parse(nodes("Confirm Price", "£ 6.26", "In-app payment, do not accept cash!", "Problem with price? Report", "Confirm price")),
        )
        assertEquals(ParseResult.NoOffer, OfferParser.parse(nodes("Rate passenger", "Add a comment", "Continue")))
        assertEquals(ParseResult.NoOffer, OfferParser.parse(nodes("Bushey Country Club", "Navigate", "1 min", "0.3 mi", "Mohammed Adnan", "COMPLETE GREEN")))
    }

    @Test
    fun boltUkHomeWithRadiusIsNotAnOffer() {
        val r = OfferParser.parse(nodes("Go offline", "Bolt price", "0.6MI", "Auto-Confirm is disabled", "£53.46", "4.70"))
        assertTrue("primit $r", r !is ParseResult.Complete)
    }

    @Test
    fun boltUkAndroidOfferNetPriceAndNoTripDuration() {
        val o = complete(
            OfferParser.parse(
                nodes(
                    "4 min · 1.1 mi", "Units 3 & 4 Watford Trade Park Bushey Mill Lane, Watford WD24 7GP",
                    "WD19 · 2.9 mi", "Bolt · Kurt ★ 5.0 (205 trips)", "£6.82 · Net", "£6.26 + est. £0.56 holiday pay", "Confirm",
                ),
            ),
        )
        assertEquals(6.82, o.price, 1e-9)
        assertTrue(o.priceIsNet)
        assertEquals(4.0, o.pickupMin, 1e-9)
        assertEquals(2.9 * 1.609344, o.tripKm, 1e-9)
        assertTrue(o.tripMinEstimated)
    }
}

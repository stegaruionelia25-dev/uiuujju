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
}

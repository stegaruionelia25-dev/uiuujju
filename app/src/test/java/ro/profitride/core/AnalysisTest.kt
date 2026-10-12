package ro.profitride.core

import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalysisTest {

    private val tz = TimeZone.getTimeZone("Europe/Bucharest")
    private val profile = DriverProfile(
        consumption = 6.8, energyPrice = 7.40, yearlyFixedCosts = 3200.0, yearlyWearCosts = 5600.0,
        yearlyKm = 40_000.0, commissionBoltPct = 0.0, taxReservePct = 10.0, goodThreshold = 40.0,
    )

    private fun at(y: Int, mo: Int, d: Int, h: Int): Long =
        Calendar.getInstance(tz).apply { clear(); set(y, mo - 1, d, h, 0, 0) }.timeInMillis

    @Test
    fun fleetPercentIsSeparateAndReducesNet() {
        val offer = RideOffer(Platform.BOLT, 40.0, 0.0, 10.0, 0.0, 20.0)
        val plain = ProfitCalculator.evaluate(offer, profile)
        val fleet = ProfitCalculator.evaluate(offer, profile.copy(fleetPct = 10.0))
        assertEquals(0.0, plain.fleetFee, 1e-9)
        assertEquals(4.0, fleet.fleetFee, 1e-9)
        assertTrue(fleet.net < plain.net)
    }

    @Test
    fun itemizedFixedCostsAreBroughtToOneYear() {
        val p = DriverProfile(yearlyFixedCosts = 100.0, rentMonthly = 10.0, rcaYearly = 200.0, cascoYearly = 300.0,
            accountingMonthly = 5.0, subscriptionsMonthly = 1.0, taxesYearly = 50.0, fleetFixedMonthly = 4.0)
        assertEquals(100.0 + 200 + 300 + 50 + 12 * (10 + 5 + 1 + 4), p.totalYearlyFixed, 1e-9)
    }

    @Test
    fun weekStartsOnMondayAndDayAt0500() {
        val wed = at(2026, 9, 23, 16)
        assertEquals(at(2026, 9, 21, 0), Analysis.periodStart(Period.WEEK, wed, tz))
        assertEquals(at(2026, 9, 23, 5), Analysis.periodStart(Period.DAY, wed, tz))
        assertEquals(at(2026, 9, 22, 5), Analysis.periodStart(Period.DAY, at(2026, 9, 23, 2), tz))
        assertEquals(at(2026, 9, 1, 0), Analysis.periodStart(Period.MONTH, wed, tz))
        assertEquals(at(2026, 1, 1, 0), Analysis.periodStart(Period.YEAR, wed, tz))
    }

    @Test
    fun summaryAddsIncomeManualAndExpenses() {
        val now = at(2026, 9, 23, 18)
        val rides = listOf(
            AnalysisRide(at(2026, 9, 23, 10), Platform.BOLT, 40.0, 10.0, 20.0),
            AnalysisRide(at(2026, 9, 21, 12), Platform.UBER, 30.0, 8.0, 15.0),
            AnalysisRide(at(2026, 9, 10, 12), Platform.UBER, 99.0, 8.0, 15.0),
        )
        val manual = listOf(ManualEntry(1, at(2026, 9, 23, 11), 10.0, "bacsis"))
        val s = Analysis.summarize(rides, manual, profile, Period.WEEK, now, tz)
        assertEquals(2, s.rides)
        assertEquals(18.0, s.km, 1e-9)
        assertEquals(80.0, s.income, 1e-9)
        val v1 = ProfitCalculator.evaluate(RideOffer(Platform.BOLT, 40.0, 0.0, 10.0, 0.0, 20.0), profile)
        val v2 = ProfitCalculator.evaluate(RideOffer(Platform.UBER, 30.0, 0.0, 8.0, 0.0, 15.0), profile)
        assertEquals(v1.net + v2.net + 10.0, s.profit, 1e-9)
        assertEquals(7, s.buckets.size)
        assertEquals(v1.net + 10.0, s.buckets[2], 1e-9)
        assertEquals(v2.net, s.buckets[0], 1e-9)
    }
}

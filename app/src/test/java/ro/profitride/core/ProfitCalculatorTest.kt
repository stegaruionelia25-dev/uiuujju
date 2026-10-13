package ro.profitride.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfitCalculatorTest {

    private val profile = DriverProfile(
        consumption = 6.8,
        energyPrice = 7.40,
        yearlyFixedCosts = 3200.0,
        yearlyWearCosts = 5600.0,
        yearlyKm = 40_000.0,
        commissionBoltPct = 20.0,
        commissionUberPct = 25.0,
        taxReservePct = 10.0,
        basis = VerdictBasis.PER_HOUR,
        goodThreshold = 40.0,
    )

    private val offer = RideOffer(Platform.BOLT, price = 31.50, pickupKm = 2.1, tripKm = 9.4, pickupMin = 5.0, tripMin = 19.0)

    @Test
    fun costPerKmAddsEnergyFixedAndWear() {
        val c = ProfitCalculator.costPerKm(profile)
        assertEquals(0.5032, c.energy, 1e-9)
        assertEquals(0.08, c.fixed, 1e-9)
        assertEquals(0.14, c.wear, 1e-9)
        assertEquals(0.7232, c.total, 1e-9)
    }

    @Test
    fun netProfitSubtractsCommissionCarCostAndTaxReserve() {
        val v = ProfitCalculator.evaluate(offer, profile)
        assertEquals(6.30, v.commission, 1e-9)
        assertEquals(8.3168, v.carCost, 1e-9)
        assertEquals(1.68832, v.taxReserve, 1e-9)
        assertEquals(15.19488, v.net, 1e-9)
        assertEquals(15.19488 / 11.5, v.perKm, 1e-9)
        assertEquals(15.19488 / 0.4, v.perHour, 1e-9)
        assertEquals(15.19488 / 31.5, v.margin, 1e-9)
    }

    @Test
    fun ratingFollowsThresholdOnChosenBasis() {
        val v = ProfitCalculator.evaluate(offer, profile) // 37,99 lei/h < 40
        assertEquals(Rating.MEDIUM, v.rating)
        assertEquals(67, v.score)

        val easier = profile.copy(goodThreshold = 30.0)
        assertEquals(Rating.GOOD, ProfitCalculator.evaluate(offer, easier).rating)

        val byKm = profile.copy(basis = VerdictBasis.PER_KM, goodThreshold = 1.20)
        assertEquals(Rating.GOOD, ProfitCalculator.evaluate(offer, byKm).rating)
    }

    @Test
    fun commissionDependsOnPlatform() {
        val uber = offer.copy(platform = Platform.UBER)
        val v = ProfitCalculator.evaluate(uber, profile)
        assertEquals(7.875, v.commission, 1e-9)
    }

    @Test
    fun poorOfferHasNegativeOrTinyNetAndZeroScoreWhenLosing() {
        val bad = RideOffer(Platform.BOLT, price = 8.0, pickupKm = 6.8, tripKm = 3.0, pickupMin = 14.0, tripMin = 9.0)
        val v = ProfitCalculator.evaluate(bad, profile)
        assertTrue(v.net < 0)
        assertEquals(Rating.POOR, v.rating)
        assertEquals(0, v.score)
        assertEquals(0.0, v.taxReserve, 1e-9) // nu se pune deoparte taxă din pierdere
    }

    @Test
    fun gradeBoundaries() {
        assertEquals(70 to Rating.GOOD, ProfitCalculator.grade(40.0, 40.0, 0.5))
        assertEquals(100 to Rating.GOOD, ProfitCalculator.grade(80.0, 40.0, 0.5))
        assertEquals(100 to Rating.GOOD, ProfitCalculator.grade(400.0, 40.0, 0.5))
        assertEquals(40 to Rating.MEDIUM, ProfitCalculator.grade(20.0, 40.0, 0.5))
        assertEquals(20 to Rating.POOR, ProfitCalculator.grade(10.0, 40.0, 0.5))
        assertEquals(0 to Rating.POOR, ProfitCalculator.grade(-5.0, 40.0, 0.5))
    }

    @Test
    fun incompleteProfileIsNotUsable() {
        assertTrue(!DriverProfile().isUsable)
        assertTrue(profile.isUsable)
    }

    @Test
    fun electricProfileUsesKwh() {
        val ev = profile.copy(fuelType = FuelType.ELECTRIC, consumption = 16.0, energyPrice = 1.2)
        assertEquals(0.192, ProfitCalculator.costPerKm(ev).energy, 1e-9)
    }
}

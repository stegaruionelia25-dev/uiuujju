package ro.profitride.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CostMathTest {

    @Test
    fun monthToYearIsTwelveTimes() {
        assertEquals(1200.0, CostMath.convert(100.0, CostPeriod.MONTH, CostPeriod.YEAR), 1e-9)
    }

    @Test
    fun yearToMonthIsTwelfth() {
        assertEquals(100.0, CostMath.convert(1200.0, CostPeriod.YEAR, CostPeriod.MONTH), 1e-9)
    }

    @Test
    fun samePeriodStaysTheSame() {
        assertEquals(1400.0, CostMath.convert(1400.0, CostPeriod.YEAR, CostPeriod.YEAR), 1e-9)
    }

    @Test
    fun roundTripKeepsTwoDecimals() {
        val monthly = CostMath.round2(CostMath.convert(1400.0, CostPeriod.YEAR, CostPeriod.MONTH))
        assertEquals(116.67, monthly, 1e-9)
    }

    @Test
    fun noCostPerKmUntilEverythingNeededIsFilled() {
        assertNull(CostMath.totalPerKm(DriverProfile()))
        assertNull(CostMath.totalPerKm(DriverProfile(consumption = 6.2, energyPrice = 8.1)))
        assertNotNull(CostMath.totalPerKm(DriverProfile(consumption = 6.2, energyPrice = 8.1, yearlyKm = 40000.0)))
    }

    @Test
    fun rcaItpAndVignetteAreSharedAcrossKilometres() {
        val base = DriverProfile(consumption = 6.0, energyPrice = 8.0, yearlyKm = 40000.0)
        val without = CostMath.totalPerKm(base)!!
        val with = CostMath.totalPerKm(base.copy(rcaYearly = 1400.0, itpYearly = 150.0, vignetteYearly = 150.0))!!
        assertEquals(1700.0 / 40000.0, with - without, 1e-9)
    }

    @Test
    fun newFixedCostsCountInTheYearlyTotal() {
        val p = DriverProfile(itpYearly = 150.0, vignetteYearly = 28.0, rentMonthly = 10.0)
        assertEquals(150.0 + 28.0 + 120.0, p.totalYearlyFixed, 1e-9)
    }

    @Test
    fun tourHasNineSteps() {
        assertEquals(9, CostMath.TOUR_STEPS)
    }
}

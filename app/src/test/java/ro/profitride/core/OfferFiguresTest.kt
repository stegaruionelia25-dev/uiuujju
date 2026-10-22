package ro.profitride.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfferFiguresTest {
    private val offer = RideOffer(Platform.BOLT, 31.5, 2.1, 9.4, 5.0, 19.0)
    private val verdict = Verdict(offer, 6.3, 8.3, 0.0, 0.0, 16.9, 1.47, 42.0, 0.54, 84, Rating.GOOD)

    @Test
    fun bigFigureFollowsTheVerdictBasis() {
        assertEquals(Metric.PER_KM, OfferFigures.bigMetric(VerdictBasis.PER_KM))
        assertEquals(Metric.PER_HOUR, OfferFigures.bigMetric(VerdictBasis.PER_HOUR))
        assertEquals(Metric.NET, OfferFigures.bigMetric(VerdictBasis.NET))
    }

    @Test
    fun bottomRowNeverRepeatsTheBigFigure() {
        for (basis in VerdictBasis.entries) {
            val stats = OfferFigures.statMetrics(basis)
            assertEquals(3, stats.size)
            assertFalse(OfferFigures.bigMetric(basis) in stats)
            assertTrue(Metric.MARGIN in stats)
        }
    }

    @Test
    fun valuesComeFromTheVerdict() {
        assertEquals(1.47, OfferFigures.value(Metric.PER_KM, verdict, 1.0), 1e-9)
        assertEquals(42.0, OfferFigures.value(Metric.PER_HOUR, verdict, 1.0), 1e-9)
        assertEquals(16.9, OfferFigures.value(Metric.NET, verdict, 1.0), 1e-9)
        assertEquals(54.0, OfferFigures.value(Metric.MARGIN, verdict, 1.0), 1e-9)
    }

    @Test
    fun perDistanceUsesTheCountryUnit() {
        assertEquals(1.47 * 1.609344, OfferFigures.value(Metric.PER_KM, verdict, 1.609344), 1e-9)
    }
}

package ro.profitride.core

import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StatsTest {

    private val utc = TimeZone.getTimeZone("UTC")
    private val hour = 3_600_000L
    private val day = 24 * hour

    // 2026-10-10 00:00 UTC
    private val base = 1_791_590_400_000L

    private fun ride(atMillis: Long, net: Double, minutes: Double) = LoggedRide(atMillis, net, minutes)

    @Test
    fun todayCountsOnlyCurrentWorkDay() {
        val now = base + 22 * hour // 22:00
        val rides = listOf(
            ride(base + 20 * hour, 20.0, 30.0),
            ride(base + 21 * hour, 10.0, 30.0),
            ride(base - 10 * hour, 99.0, 30.0), // ieri seara, altă zi de lucru
        )
        val s = Stats.today(rides, now, utc)
        assertEquals(30.0, s.net, 1e-9)
        assertEquals(2, s.rides)
        assertEquals(30.0, s.perHour!!, 1e-9)
    }

    @Test
    fun nightShiftStaysInOneDayUntilFiveAm() {
        val now = base + day + 2 * hour // 02:00 a doua zi
        val rides = listOf(ride(base + 23 * hour, 15.0, 20.0))
        assertEquals(1, Stats.today(rides, now, utc).rides)
        val after5 = base + day + 6 * hour
        assertEquals(0, Stats.today(rides, after5, utc).rides)
    }

    @Test
    fun averageNeedsEnoughRides() {
        val now = base + 22 * hour
        val two = listOf(ride(now - hour, 10.0, 30.0), ride(now - 2 * hour, 10.0, 30.0))
        assertNull(Stats.averagePerHour(two, now))
        val three = two + ride(now - 3 * hour, 10.0, 30.0)
        assertEquals(20.0, Stats.averagePerHour(three, now)!!, 1e-9)
    }

    @Test
    fun bestWindowNeedsHistoryAndAClearWinner() {
        val few = (0 until 5).map { ride(base + it * day + 23 * hour, 20.0, 30.0) }
        assertNull(Stats.bestWindowStartHour(few, utc))

        val rides = buildList {
            for (d in 0 until 5) {
                add(ride(base + d * day + 23 * hour + 5 * 60_000, 25.0, 30.0)) // 23:xx bun: 50 lei/h
                add(ride(base + d * day + 15 * hour + 5 * 60_000, 10.0, 30.0)) // 15:xx slab: 20 lei/h
            }
            for (d in 0 until 3) add(ride(base + d * day + 10 * hour, 12.0, 30.0))
        }
        assertEquals(23, Stats.bestWindowStartHour(rides, utc))
    }

    @Test
    fun idleMessagesUseOnlyWhatIsKnown() {
        val empty = DriverProfile()
        assertTrue(Stats.idleMessages(empty, DayStats(0.0, 0, null), null).isEmpty())

        val p = DriverProfile(goodThreshold = 40.0, dailyTarget = 250.0, basis = VerdictBasis.PER_HOUR)
        val msgs = Stats.idleMessages(p, DayStats(187.4, 9, 30.0), 23)
        assertEquals(4, msgs.size)
        assertTrue(msgs[0] is IdleMessage.Threshold)
        assertTrue(msgs[1] is IdleMessage.Today)
        assertEquals(62.6, (msgs[2] as IdleMessage.ToTarget).remaining, 1e-9)
        assertEquals(23, (msgs[3] as IdleMessage.BestWindow).startHour)

        val done = Stats.idleMessages(p, DayStats(260.0, 10, 30.0), null)
        assertTrue(done.any { it is IdleMessage.TargetReached })
    }

    @Test
    fun idleDetectorUsesSeedsThenCustomMarkers() {
        assertTrue(IdleDetector.isIdle(listOf("Ești online"), emptySet()))
        assertTrue(!IdleDetector.isIdle(listOf("Conectează-te"), emptySet()))
        assertTrue(IdleDetector.isIdle(listOf("Gata de curse"), setOf("gata de curse")))
        assertTrue(!IdleDetector.isIdle(listOf("Ești online"), setOf("gata de curse")))
        assertNotNull(Countries.byCode("GB"))
        assertEquals(DistanceUnit.MI, Countries.byCode("GB").unit)
        assertEquals("RO", Countries.byCode("??").code)
    }

    @Test
    fun recognizesBoltHomeScreenBeforeGoingOnline() {
        assertTrue(IdleDetector.isOfflineHome(listOf("0,00 lei", "Astăzi", "Intră online", "Oferte", "Câștigă mai mult")))
        assertTrue(!IdleDetector.isOfflineHome(listOf("9,82 lei", "Refuză", "Acceptă")))
    }
}

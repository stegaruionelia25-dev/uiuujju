package ro.profitride.core

import java.util.Calendar
import java.util.TimeZone

enum class Period { DAY, WEEK, MONTH, YEAR }

/** O cursă marcată ca făcută, așa cum e salvată. Profitul se recalculează cu profilul curent. */
data class AnalysisRide(val timestampMillis: Long, val platform: Platform, val price: Double, val km: Double, val minutes: Double, val priceIsNet: Boolean = false)

/** Venit adăugat de șofer (bacșiș, cash). */
data class ManualEntry(val id: Long, val timestampMillis: Long, val amount: Double, val note: String)

data class PeriodSummary(
    val rides: Int,
    val km: Double,
    val income: Double,
    val commissions: Double,
    val energyAndWear: Double,
    val fixed: Double,
    val tax: Double,
    /** profit pe fiecare interval (oră, zi sau lună), pentru grafic */
    val buckets: List<Double>,
) {
    val expenses: Double get() = commissions + energyAndWear + fixed + tax
    val profit: Double get() = income - expenses
}

object Analysis {

    fun bucketCount(period: Period): Int = when (period) {
        Period.DAY -> 24
        Period.WEEK -> 7
        Period.MONTH -> 31
        Period.YEAR -> 12
    }

    /** Începutul perioadei. Ziua de lucru începe la 05:00, ca în restul aplicației. */
    fun periodStart(period: Period, now: Long, tz: TimeZone = TimeZone.getDefault()): Long {
        val cal = Calendar.getInstance(tz).apply { timeInMillis = now; firstDayOfWeek = Calendar.MONDAY }
        when (period) {
            Period.DAY -> {
                cal.add(Calendar.HOUR_OF_DAY, -5)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                zeroBelowHour(cal)
                cal.add(Calendar.HOUR_OF_DAY, 5)
            }
            Period.WEEK -> {
                val back = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
                cal.add(Calendar.DAY_OF_YEAR, -back)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                zeroBelowHour(cal)
            }
            Period.MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                zeroBelowHour(cal)
            }
            Period.YEAR -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                zeroBelowHour(cal)
            }
        }
        return cal.timeInMillis
    }

    private fun zeroBelowHour(cal: Calendar) {
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
    }

    private fun bucketOf(period: Period, ts: Long, tz: TimeZone): Int {
        val cal = Calendar.getInstance(tz).apply { timeInMillis = ts }
        return when (period) {
            Period.DAY -> cal.get(Calendar.HOUR_OF_DAY)
            Period.WEEK -> (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
            Period.MONTH -> cal.get(Calendar.DAY_OF_MONTH) - 1
            Period.YEAR -> cal.get(Calendar.MONTH)
        }
    }

    /**
     * Încasări = prețurile curselor făcute + veniturile manuale. Cheltuieli = comisioane, combustibil și uzură,
     * costurile fixe (părțile lor pe kilometru, ca în calculul din overlay) și rezerva de taxe.
     * Costurile fixe se împart pe kilometri, deci nu se numără de două ori.
     */
    fun summarize(
        rides: List<AnalysisRide>,
        manual: List<ManualEntry>,
        profile: DriverProfile,
        period: Period,
        now: Long,
        tz: TimeZone = TimeZone.getDefault(),
    ): PeriodSummary {
        val start = periodStart(period, now, tz)
        val cpk = ProfitCalculator.costPerKm(profile)
        val buckets = DoubleArray(bucketCount(period))
        var km = 0.0
        var income = 0.0
        var commissions = 0.0
        var energyWear = 0.0
        var fixed = 0.0
        var tax = 0.0
        var count = 0

        for (r in rides) {
            if (r.timestampMillis < start || r.timestampMillis > now) continue
            val v = ProfitCalculator.evaluate(RideOffer(r.platform, r.price, 0.0, r.km, 0.0, r.minutes, r.priceIsNet), profile)
            count++
            km += r.km
            income += r.price
            commissions += v.commission + v.fleetFee
            energyWear += r.km * (cpk.energy + cpk.wear)
            fixed += r.km * cpk.fixed
            tax += v.taxReserve
            buckets[bucketOf(period, r.timestampMillis, tz)] += v.net
        }
        for (m in manual) {
            if (m.timestampMillis < start || m.timestampMillis > now) continue
            income += m.amount
            buckets[bucketOf(period, m.timestampMillis, tz)] += m.amount
        }
        return PeriodSummary(count, km, income, commissions, energyWear, fixed, tax, buckets.toList())
    }
}

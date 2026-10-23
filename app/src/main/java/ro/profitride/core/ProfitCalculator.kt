package ro.profitride.core

data class CostPerKm(val energy: Double, val fixed: Double, val wear: Double) {
    val total: Double get() = energy + fixed + wear
}

data class Verdict(
    val offer: RideOffer,
    val commission: Double,
    val carCost: Double,
    val taxReserve: Double,
    /** comisionul flotei, separat de cel al platformei */
    val fleetFee: Double = 0.0,
    /** profit curat: preț − comision − cost auto − rezervă pentru taxe */
    val net: Double,
    val perKm: Double,
    val perHour: Double,
    /** net / preț, între 0 și 1 (poate fi negativ) */
    val margin: Double,
    val score: Int,
    val rating: Rating,
)

object ProfitCalculator {

    fun costPerKm(profile: DriverProfile): CostPerKm {
        val km = profile.yearlyKm
        return CostPerKm(
            energy = profile.consumption / 100.0 * profile.energyPrice,
            fixed = if (km > 0) profile.totalYearlyFixed / km else 0.0,
            wear = if (km > 0) profile.yearlyWearCosts / km else 0.0,
        )
    }

    /**
     * Costul auto se aplică pe toți kilometrii parcurși pentru cursă (ridicare + traseu),
     * deoarece șoferul îi conduce pe amândoi. Timpul include la fel ridicarea și traseul;
     * așteptarea dintre curse nu este inclusă, deci lei/h este „pe cursă”.
     */
    fun evaluate(offer: RideOffer, profile: DriverProfile): Verdict {
        val cpk = costPerKm(profile)
        val commission = if (offer.priceIsNet) 0.0 else offer.price * profile.commissionFor(offer.platform) / 100.0
        val carCost = offer.totalKm * cpk.total
        val fleetFee = offer.price * profile.fleetPct / 100.0
        val beforeTax = offer.price - commission - fleetFee - carCost
        val tax = if (beforeTax > 0) beforeTax * profile.taxReservePct / 100.0 else 0.0
        val net = beforeTax - tax
        val hours = offer.totalMin / 60.0
        val perHour = if (hours > 0) net / hours else 0.0
        val perKm = if (offer.totalKm > 0) net / offer.totalKm else 0.0
        val margin = if (offer.price > 0) net / offer.price else 0.0

        val basisValue = when (profile.basis) {
            VerdictBasis.PER_KM -> perKm
            VerdictBasis.PER_HOUR -> perHour
            VerdictBasis.NET -> net
        }
        val (score, rating) = grade(basisValue, profile.goodThreshold, profile.mediumFraction)
        return Verdict(offer, commission, carCost, tax, fleetFee, net, perKm, perHour, margin, score, rating)
    }

    /**
     * Scor 0–100 și calificativ. La prag => 70; la dublul pragului => 100;
     * la fracțiunea „medie” => 40; la zero sau sub => 0.
     */
    fun grade(value: Double, good: Double, mediumFraction: Double): Pair<Int, Rating> {
        if (good <= 0 || value <= 0) return 0 to Rating.POOR
        val mf = mediumFraction.coerceIn(0.05, 0.95)
        val r = value / good
        val score = when {
            r >= 1.0 -> 70.0 + 30.0 * ((r - 1.0).coerceAtMost(1.0))
            r >= mf -> 40.0 + 30.0 * ((r - mf) / (1.0 - mf))
            else -> 40.0 * (r / mf)
        }
        val rating = when {
            r >= 1.0 -> Rating.GOOD
            r >= mf -> Rating.MEDIUM
            else -> Rating.POOR
        }
        return Math.round(score).toInt().coerceIn(0, 100) to rating
    }
}

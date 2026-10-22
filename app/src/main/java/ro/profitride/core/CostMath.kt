package ro.profitride.core

/** Perioada în care e scrisă o sumă: pe lună sau pe an. */
enum class CostPeriod { MONTH, YEAR }

/** Calcule mici ale ecranului „Costurile mele”, fără Android, ca să poată fi testate. */
object CostMath {

    /** Trece o sumă dintr-o perioadă în alta: 100 pe lună = 1200 pe an. */
    fun convert(value: Double, from: CostPeriod, to: CostPeriod): Double = when {
        from == to -> value
        from == CostPeriod.MONTH -> value * 12.0
        else -> value / 12.0
    }

    /** Rotunjire la două zecimale, pentru ce se arată în câmpul de text după schimbarea perioadei. */
    fun round2(value: Double): Double = Math.round(value * 100.0) / 100.0

    /** Costul real pe km, sau null când lipsesc consumul, prețul sau kilometrii pe an. */
    fun totalPerKm(profile: DriverProfile): Double? {
        if (profile.consumption <= 0 || profile.energyPrice <= 0 || profile.yearlyKm <= 0) return null
        return ProfitCalculator.costPerKm(profile).total
    }

    /** Pașii turului: numărul lor trebuie să rămână egal cu numărul de texte din resurse. */
    const val TOUR_STEPS = 9
}

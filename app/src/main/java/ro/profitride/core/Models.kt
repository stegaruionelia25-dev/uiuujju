package ro.profitride.core

/** Unitatea de distanță afișată șoferului. Calculele interne se fac mereu în km. */
enum class DistanceUnit(val kmPerUnit: Double, val symbol: String) {
    KM(1.0, "km"),
    MI(1.609344, "mi"),
}

/** Tipul de energie. Pentru GPL/hibrid, șoferul introduce consumul și prețul mediu ponderat. */
enum class FuelType(val perKwh: Boolean) {
    PETROL(false),
    DIESEL(false),
    LPG(false),
    HYBRID(false),
    ELECTRIC(true),
}

/**
 * Platformele suportate. Numele pachetelor sunt cele cunoscute public ale aplicațiilor
 * pentru șoferi; se verifică pe telefon prin ecranul de calibrare.
 */
enum class Platform(val packageName: String) {
    BOLT("ee.mtakso.driver"),
    UBER("com.ubercab.driver"),
    ;

    companion object {
        fun fromPackage(packageName: String?): Platform? = entries.firstOrNull { it.packageName == packageName }
    }
}

/** Pe ce se bazează culoarea verdictului. */
enum class VerdictBasis { PER_KM, PER_HOUR, NET }

enum class Rating { GOOD, MEDIUM, POOR }

/** Tot ce completează șoferul. Nimic nu are valori implicite „inventate”: 0 înseamnă necompletat. */
data class DriverProfile(
    val countryCode: String = "RO",
    val fuelType: FuelType = FuelType.PETROL,
    /** l/100 km sau kWh/100 km */
    val consumption: Double = 0.0,
    /** preț pe litru sau pe kWh */
    val energyPrice: Double = 0.0,
    /** asigurare, ITP, rovinietă etc., pe an */
    val yearlyFixedCosts: Double = 0.0,
    /** service, anvelope, uzură, pe an */
    val yearlyWearCosts: Double = 0.0,
    val yearlyKm: Double = 0.0,
    val commissionBoltPct: Double = 0.0,
    val commissionUberPct: Double = 0.0,
    /** procent din profit pus deoparte pentru taxe */
    val taxReservePct: Double = 0.0,
    val basis: VerdictBasis = VerdictBasis.PER_HOUR,
    /** pragul „BUN”, în unitatea criteriului ales (lei/km, lei/h sau lei) */
    val goodThreshold: Double = 0.0,
    /** sub această fracțiune din prag, verdictul devine „SLAB” */
    val mediumFraction: Double = 0.5,
    val dailyTarget: Double = 0.0,
    val onboardingDone: Boolean = false,
    val makeName: String = "",
    val modelName: String = "",
    val year: Int = 0,
) {
    fun commissionFor(platform: Platform): Double = when (platform) {
        Platform.BOLT -> commissionBoltPct
        Platform.UBER -> commissionUberPct
    }

    /** Are destule date cât să calculeze un profit credibil. */
    val isUsable: Boolean
        get() = consumption > 0 && energyPrice > 0 && yearlyKm > 0 && goodThreshold > 0
}

enum class IdleStyle { RADAR, PULSE }

/** Setări de afișare ale overlay-ului. */
data class OverlaySettings(
    /** poziția bulei pe verticală, ca procent din înălțimea ecranului */
    val yPercent: Int = 34,
    val showIdle: Boolean = true,
    val idleStyle: IdleStyle = IdleStyle.RADAR,
    val compact: Boolean = false,
    /** arată o notă când cursa nu a putut fi citită complet */
    val showUnsure: Boolean = false,
    /** după câte minute fără curse bula trece pe „Fără curse · X min” */
    val pauseAfterMin: Int = 10,
    /** texte care identifică ecranul „ești online, se caută curse”, per aplicație */
    val idleMarkersBolt: Set<String> = emptySet(),
    val idleMarkersUber: Set<String> = emptySet(),
) {
    fun markersFor(platform: Platform): Set<String> = when (platform) {
        Platform.BOLT -> idleMarkersBolt
        Platform.UBER -> idleMarkersUber
    }
}

data class RideOffer(
    val platform: Platform,
    val price: Double,
    val pickupKm: Double,
    val tripKm: Double,
    val pickupMin: Double,
    val tripMin: Double,
) {
    val totalKm: Double get() = pickupKm + tripKm
    val totalMin: Double get() = pickupMin + tripMin
}

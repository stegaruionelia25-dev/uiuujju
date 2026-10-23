package ro.profitride.core

/** Cifrele unei oferte pe care le poate arăta bula. */
enum class Metric { PER_KM, PER_HOUR, NET, MARGIN }

/**
 * Ce se vede în bulă, după baza verdictului aleasă de șofer: cifra mare e baza verdictului,
 * iar rândul de dedesubt arată celelalte cifre (fără să repete cifra mare).
 */
object OfferFigures {

    fun bigMetric(basis: VerdictBasis): Metric = when (basis) {
        VerdictBasis.PER_KM -> Metric.PER_KM
        VerdictBasis.PER_HOUR -> Metric.PER_HOUR
        VerdictBasis.NET -> Metric.NET
    }

    /** Trei cifre pentru rândul de jos: niciuna nu e cifra mare. */
    fun statMetrics(basis: VerdictBasis): List<Metric> = when (basis) {
        VerdictBasis.PER_KM -> listOf(Metric.PER_HOUR, Metric.NET, Metric.MARGIN)
        VerdictBasis.PER_HOUR -> listOf(Metric.PER_KM, Metric.NET, Metric.MARGIN)
        VerdictBasis.NET -> listOf(Metric.PER_KM, Metric.PER_HOUR, Metric.MARGIN)
    }

    /** Valoarea cifrei; `kmPerUnit` convertește km în unitatea țării (mile în UK). Marja e în procente. */
    fun value(metric: Metric, v: Verdict, kmPerUnit: Double): Double = when (metric) {
        Metric.PER_KM -> v.perKm * kmPerUnit
        Metric.PER_HOUR -> v.perHour
        Metric.NET -> v.net
        Metric.MARGIN -> v.margin * 100.0
    }
}

package ro.profitride.core

import java.util.Locale
import java.util.TimeZone

/** O cursă marcată de șofer ca fiind acceptată și făcută. */
data class LoggedRide(val timestampMillis: Long, val net: Double, val minutes: Double)

data class DayStats(val net: Double, val rides: Int, val perHour: Double?)

/** Mesaje scurte pentru bula din așteptare. Sunt construite doar din datele reale ale șoferului. */
sealed interface IdleMessage {
    data class Threshold(val value: Double, val basis: VerdictBasis) : IdleMessage
    data class Today(val net: Double, val rides: Int) : IdleMessage
    data class ToTarget(val remaining: Double) : IdleMessage
    data object TargetReached : IdleMessage
    data class BestWindow(val startHour: Int) : IdleMessage
}

object Stats {
    /** O „zi de lucru” începe la 05:00, ca o tură de noapte să rămână într-o singură zi. */
    const val DAY_START_HOUR = 5
    const val MIN_RIDES_FOR_AVERAGE = 3
    const val MIN_RIDES_FOR_WINDOW = 12
    const val MIN_RIDES_PER_HOUR_BUCKET = 3
    private const val HOUR_MS = 3_600_000L
    private const val DAY_MS = 86_400_000L

    private fun localMillis(ts: Long, tz: TimeZone) = ts + tz.getOffset(ts)

    private fun dayKey(ts: Long, tz: TimeZone): Long =
        Math.floorDiv(localMillis(ts, tz) - DAY_START_HOUR * HOUR_MS, DAY_MS)

    fun today(rides: List<LoggedRide>, nowMillis: Long, tz: TimeZone = TimeZone.getDefault()): DayStats {
        val key = dayKey(nowMillis, tz)
        val todays = rides.filter { dayKey(it.timestampMillis, tz) == key }
        val net = todays.sumOf { it.net }
        val hours = todays.sumOf { it.minutes } / 60.0
        return DayStats(net, todays.size, if (todays.isNotEmpty() && hours > 0) net / hours else null)
    }

    /** Media ta lei/h pe curse, pe ultimele 30 de zile. Null până există destule curse. */
    fun averagePerHour(rides: List<LoggedRide>, nowMillis: Long, days: Int = 30): Double? {
        val since = nowMillis - days * DAY_MS
        val recent = rides.filter { it.timestampMillis >= since }
        if (recent.size < MIN_RIDES_FOR_AVERAGE) return null
        val hours = recent.sumOf { it.minutes } / 60.0
        return if (hours > 0) recent.sumOf { it.net } / hours else null
    }

    /** Ora (0–23) cu cel mai bun lei/h, doar dacă istoricul e suficient și diferența e clară. */
    fun bestWindowStartHour(rides: List<LoggedRide>, tz: TimeZone = TimeZone.getDefault()): Int? {
        if (rides.size < MIN_RIDES_FOR_WINDOW) return null
        val overallHours = rides.sumOf { it.minutes } / 60.0
        if (overallHours <= 0) return null
        val overall = rides.sumOf { it.net } / overallHours

        var bestHour: Int? = null
        var bestValue = Double.NEGATIVE_INFINITY
        rides.groupBy { Math.floorMod(localMillis(it.timestampMillis, tz) / HOUR_MS, 24L).toInt() }
            .forEach { (hour, group) ->
                if (group.size < MIN_RIDES_PER_HOUR_BUCKET) return@forEach
                val hours = group.sumOf { it.minutes } / 60.0
                if (hours <= 0) return@forEach
                val value = group.sumOf { it.net } / hours
                if (value > bestValue) {
                    bestValue = value
                    bestHour = hour
                }
            }
        return if (bestHour != null && overall > 0 && bestValue >= overall * 1.1) bestHour else null
    }

    fun idleMessages(profile: DriverProfile, today: DayStats, bestHour: Int?): List<IdleMessage> = buildList {
        if (profile.goodThreshold > 0) add(IdleMessage.Threshold(profile.goodThreshold, profile.basis))
        if (today.rides > 0) add(IdleMessage.Today(today.net, today.rides))
        if (profile.dailyTarget > 0) {
            val remaining = profile.dailyTarget - today.net
            add(if (remaining > 0) IdleMessage.ToTarget(remaining) else IdleMessage.TargetReached)
        }
        if (bestHour != null) add(IdleMessage.BestWindow(bestHour))
    }
}

/**
 * Recunoaște ecranul „ești online, se caută curse” după texte-marcaj. Marcajele implicite sunt
 * presupuneri; șoferul le poate înlocui din ecranul de calibrare cu textul real din aplicația lui.
 */
object IdleDetector {
    val seedMarkers: List<String> = listOf(
        "ești online", "esti online", "you're online", "you are online",
        "looking for trips", "finding trips", "caut curse",
    )

    private fun norm(s: String) = s.lowercase(Locale.ROOT).replace('’', '\'').trim()

    /** Ecranul de start din aplicație, înainte să intri online („Intră online”, „Go online”). */
    private val offlineMarkers = listOf("intră online", "intra online", "go online", "mergi online")

    fun isOfflineHome(texts: List<String>): Boolean {
        val normalized = texts.map(::norm)
        return offlineMarkers.any { m -> normalized.any { it == m || it.contains(m) } }
    }

    /** Butonul din ecranul „online, fără ofertă” (Bolt: „Deconectează-te”); prefix scurt, ca să țină și la citirea vizuală. */
    private val onlineButtonMarkers = listOf("deconecteaz", "go offline")

    fun isIdle(texts: List<String>, markers: Collection<String>): Boolean {
        val effective = if (markers.isEmpty()) seedMarkers else markers
        val normalized = texts.map(::norm)
        if (onlineButtonMarkers.any { m -> normalized.any { it.contains(m) } }) return true
        return effective.map(::norm).any { m -> m.isNotEmpty() && normalized.any { it.contains(m) } }
    }
}

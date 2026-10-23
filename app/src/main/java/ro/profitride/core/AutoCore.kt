package ro.profitride.core

import java.time.LocalDate

/** Scrierea datelor: șoferul tastează doar cifre, punctele și corectura le face aplicația. */
object DateInput {
    sealed interface Result {
        data object Empty : Result
        data object Incomplete : Result
        data class Valid(val date: LocalDate) : Result
        data class Invalid(val reason: Reason, val suggestion: LocalDate?) : Result
    }

    enum class Reason { MONTH, DAY, DAY_OF_MONTH, YEAR }

    const val MIN_YEAR = 2000
    const val MAX_YEAR = 2100

    /** „12082026” devine „12.08.2026”. Ignoră orice nu e cifră. */
    fun format(raw: String): String {
        val digits = raw.filter { it.isDigit() }.take(8)
        return buildString {
            digits.forEachIndexed { i, c ->
                if (i == 2 || i == 4) append('.')
                append(c)
            }
        }
    }

    fun validate(text: String): Result {
        val digits = text.filter { it.isDigit() }
        if (digits.isEmpty()) return Result.Empty
        val day = if (digits.length >= 2) digits.substring(0, 2).toInt() else null
        val month = if (digits.length >= 4) digits.substring(2, 4).toInt() else null
        if (day != null && (day < 1 || day > 31)) return Result.Invalid(Reason.DAY, null)
        if (month != null && (month < 1 || month > 12)) return Result.Invalid(Reason.MONTH, null)
        if (digits.length < 8) return Result.Incomplete
        val year = digits.substring(4, 8).toInt()
        if (year < MIN_YEAR || year > MAX_YEAR) return Result.Invalid(Reason.YEAR, null)
        val last = LocalDate.of(year, month!!, 1).lengthOfMonth()
        if (day!! > last) return Result.Invalid(Reason.DAY_OF_MONTH, LocalDate.of(year, month, last))
        return Result.Valid(LocalDate.of(year, month, day))
    }

    fun display(date: LocalDate): String = format(
        "%02d%02d%04d".format(date.dayOfMonth, date.monthValue, date.year),
    )
}

enum class DocType(val syncsToProfile: Boolean) {
    RCA(true), ITP(false), ROVINIETA(false), CASCO(true), COPIE_CONFORMA(false), PERMIS(false), ATESTAT(false),
}

/** Un document al mașinii sau al șoferului, cu data expirării (zile de la 1970, ca LocalDate.toEpochDay). */
data class CarDoc(val type: DocType, val expiryDay: Long, val cost: Double, val notifyDays: Set<Int>)

enum class MaintKind(val defaultIntervalKm: Long) {
    ULEI(10_000), FILTRE(15_000), DISTRIBUTIE(60_000), PLACUTE(30_000), ANVELOPE(40_000), BATERIE(60_000), REVIZIE(15_000),
}

data class Maintenance(val kind: MaintKind, val lastKm: Long, val intervalKm: Long, val note: String, val lastDay: Long)

enum class ExpenseType { ALIMENTARE, SERVICE, SPALATORIE, PARCARE, IMPOZIT, ALTUL }

data class Expense(val id: Long, val type: ExpenseType, val amount: Double, val liters: Double, val day: Long, val note: String)

data class AutoData(
    val plate: String = "",
    val odometer: Long = 0,
    val tutorialSeen: Boolean = false,
    val docs: List<CarDoc> = emptyList(),
    val maintenance: List<Maintenance> = emptyList(),
    val expenses: List<Expense> = emptyList(),
)

enum class DueState { EXPIRED, SOON, OK }

data class DueDoc(val doc: CarDoc, val daysLeft: Long, val key: String)
data class DueMaint(val item: Maintenance, val kmLeft: Long, val expired: Boolean, val key: String)

object AutoLogic {
    val defaultNotifyDays: Set<Int> = setOf(30, 10, 3, 0)
    const val SOON_DAYS = 30L
    const val SOON_KM = 1_000L

    fun daysLeft(expiryDay: Long, todayDay: Long): Long = expiryDay - todayDay

    fun docState(daysLeft: Long): DueState = when {
        daysLeft < 0 -> DueState.EXPIRED
        daysLeft <= SOON_DAYS -> DueState.SOON
        else -> DueState.OK
    }

    fun kmLeft(m: Maintenance, odometer: Long): Long = m.lastKm + m.intervalKm - odometer

    fun maintState(kmLeft: Long): DueState = when {
        kmLeft < 0 -> DueState.EXPIRED
        kmLeft <= SOON_KM -> DueState.SOON
        else -> DueState.OK
    }

    /** Cât din interval s-a consumat, între 0 și 1. */
    fun usedFraction(m: Maintenance, odometer: Long): Double {
        if (m.intervalKm <= 0) return 0.0
        return ((odometer - m.lastKm).toDouble() / m.intervalKm).coerceIn(0.0, 1.0)
    }

    /** Documente pentru care trebuie trimisă azi o notificare, fără cele deja trimise. */
    fun dueDocs(docs: List<CarDoc>, todayDay: Long, notified: Set<String>): List<DueDoc> = docs.mapNotNull { d ->
        val left = daysLeft(d.expiryDay, todayDay)
        val hit = when {
            left < 0 -> -1L
            left.toInt() in d.notifyDays -> left
            else -> return@mapNotNull null
        }
        val key = "doc:${d.type.name}:${d.expiryDay}:$hit"
        if (key in notified) null else DueDoc(d, left, key)
    }

    fun dueMaintenance(items: List<Maintenance>, odometer: Long, notified: Set<String>): List<DueMaint> {
        if (odometer <= 0) return emptyList()
        return items.mapNotNull { m ->
            val left = kmLeft(m, odometer)
            val state = maintState(left)
            if (state == DueState.OK) return@mapNotNull null
            val key = "m:${m.kind.name}:${m.lastKm}:${if (state == DueState.EXPIRED) "late" else "soon"}"
            if (key in notified) null else DueMaint(m, left, state == DueState.EXPIRED, key)
        }
    }

    /** 148200 -> „148.200” */
    fun groupThousands(value: Long): String {
        val s = kotlin.math.abs(value).toString()
        val out = StringBuilder()
        s.forEachIndexed { i, c ->
            if (i > 0 && (s.length - i) % 3 == 0) out.append('.')
            out.append(c)
        }
        return (if (value < 0) "-" else "") + out
    }

    fun monthTotal(expenses: List<Expense>, today: LocalDate): Double = expenses
        .filter {
            val d = LocalDate.ofEpochDay(it.day)
            d.year == today.year && d.monthValue == today.monthValue
        }
        .sumOf { it.amount }
}

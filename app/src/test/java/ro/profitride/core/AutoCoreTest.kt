package ro.profitride.core

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoCoreTest {
    @Test
    fun formatsDigitsIntoDate() {
        assertEquals("12.08.2026", DateInput.format("12082026"))
        assertEquals("12.0", DateInput.format("120"))
        assertEquals("12.08.2026", DateInput.format("12.08.2026999"))
        assertEquals("12", DateInput.format("1a2"))
    }

    @Test
    fun validatesAndSuggestsCorrections() {
        assertTrue(DateInput.validate("12.08.2026") is DateInput.Result.Valid)
        val bad = DateInput.validate("31.02.2026") as DateInput.Result.Invalid
        assertEquals(DateInput.Reason.DAY_OF_MONTH, bad.reason)
        assertEquals(LocalDate.of(2026, 2, 28), bad.suggestion)
        assertTrue(DateInput.validate("45") is DateInput.Result.Invalid)
        assertTrue(DateInput.validate("12.13") is DateInput.Result.Invalid)
        assertTrue(DateInput.validate("12.08.20") is DateInput.Result.Incomplete)
        assertTrue(DateInput.validate("") is DateInput.Result.Empty)
        assertTrue(DateInput.validate("12.08.1999") is DateInput.Result.Invalid)
    }

    @Test
    fun documentStatesAndNotifications() {
        val today = LocalDate.of(2026, 10, 13).toEpochDay()
        val rca = CarDoc(DocType.RCA, today + 10, 1400.0, AutoLogic.defaultNotifyDays)
        assertEquals(DueState.SOON, AutoLogic.docState(AutoLogic.daysLeft(rca.expiryDay, today)))
        assertEquals(1, AutoLogic.dueDocs(listOf(rca), today, emptySet()).size)
        assertEquals(0, AutoLogic.dueDocs(listOf(rca), today + 1, emptySet()).size)
        val key = AutoLogic.dueDocs(listOf(rca), today, emptySet()).first().key
        assertEquals(0, AutoLogic.dueDocs(listOf(rca), today, setOf(key)).size)
        val expired = AutoLogic.dueDocs(listOf(rca), today + 20, emptySet())
        assertEquals(1, expired.size)
        assertEquals(DueState.EXPIRED, AutoLogic.docState(expired.first().daysLeft))
    }

    @Test
    fun maintenanceKmLeft() {
        val oil = Maintenance(MaintKind.ULEI, 140_000, 10_000, "Ulei 5W30", 0)
        assertEquals(1_800L, AutoLogic.kmLeft(oil, 148_200))
        assertEquals(DueState.OK, AutoLogic.maintState(1_800))
        assertEquals(DueState.SOON, AutoLogic.maintState(AutoLogic.kmLeft(oil, 149_200)))
        assertEquals(DueState.EXPIRED, AutoLogic.maintState(AutoLogic.kmLeft(oil, 150_100)))
        assertEquals(0, AutoLogic.dueMaintenance(listOf(oil), 148_200, emptySet()).size)
        assertEquals(1, AutoLogic.dueMaintenance(listOf(oil), 149_500, emptySet()).size)
        assertEquals("148.200", AutoLogic.groupThousands(148_200))
        assertEquals("800", AutoLogic.groupThousands(800))
    }

    @Test
    fun monthTotalCountsOnlyThisMonth() {
        val today = LocalDate.of(2026, 10, 13)
        val e = listOf(
            Expense(1, ExpenseType.ALIMENTARE, 300.0, 38.0, LocalDate.of(2026, 10, 3).toEpochDay(), ""),
            Expense(2, ExpenseType.SERVICE, 540.0, 0.0, LocalDate.of(2026, 9, 30).toEpochDay(), ""),
        )
        assertEquals(300.0, AutoLogic.monthTotal(e, today), 0.001)
    }
}

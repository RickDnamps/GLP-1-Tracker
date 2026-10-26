package com.rickdnamps.glp1tracker.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ScheduleTest {
    private val nine = LocalTime.of(9, 0)

    @Test
    fun weeklyDoseIsSevenDaysAfterLastOne() {
        val last = LocalDateTime.of(2026, 10, 19, 20, 30)
        val due = Schedule.nextDue(last, 7, DayOfWeek.MONDAY, nine, LocalDate.of(2026, 10, 22))
        assertEquals(LocalDateTime.of(2026, 10, 26, 9, 0), due)
    }

    @Test
    fun withoutHistoryUsesPreferredWeekday() {
        // 2026-10-22 is a Thursday; next Monday is the 26th.
        val due = Schedule.nextDue(null, 7, DayOfWeek.MONDAY, nine, LocalDate.of(2026, 10, 22))
        assertEquals(LocalDateTime.of(2026, 10, 26, 9, 0), due)
    }

    @Test
    fun overdueReminderNagsNextDailySlot() {
        val due = LocalDateTime.of(2026, 10, 20, 9, 0)
        val now = LocalDateTime.of(2026, 10, 22, 10, 0)
        assertEquals(LocalDateTime.of(2026, 10, 23, 9, 0), Schedule.nextReminder(due, nine, now))
        val early = LocalDateTime.of(2026, 10, 22, 8, 0)
        assertEquals(LocalDateTime.of(2026, 10, 22, 9, 0), Schedule.nextReminder(due, nine, early))
    }

    @Test
    fun suggestsLeastRecentlyUsedSite() {
        val lastUse = InjectionSite.injectable.associateWith { 1000L }.toMutableMap()
        lastUse[InjectionSite.THIGH_RIGHT] = 10L
        assertEquals(InjectionSite.THIGH_RIGHT, InjectionSite.suggest(lastUse))
        lastUse.remove(InjectionSite.ARM_LEFT)
        assertEquals(InjectionSite.ARM_LEFT, InjectionSite.suggest(lastUse))
    }

    @Test
    fun changingDoseDayMovesNextWeeklyDose() {
        // Last dose Monday 19; user switches to Thursday: next is Thursday 22 (3-day gap respected).
        val last = LocalDateTime.of(2026, 10, 19, 9, 0)
        val due = Schedule.nextDue(last, 7, DayOfWeek.THURSDAY, nine, LocalDate.of(2026, 10, 20))
        assertEquals(LocalDateTime.of(2026, 10, 22, 9, 0), due)
        // Switching to Tuesday would be too close (1 day): it goes to the following Tuesday.
        val tue = Schedule.nextDue(last, 7, DayOfWeek.TUESDAY, nine, LocalDate.of(2026, 10, 20))
        assertEquals(LocalDateTime.of(2026, 10, 27, 9, 0), tue)
    }

    @Test
    fun skippedDoseMovesToNextSlot() {
        val last = LocalDateTime.of(2026, 10, 5, 9, 0)
        val due = Schedule.nextDue(last, 7, DayOfWeek.MONDAY, nine, LocalDate.of(2026, 10, 20), skippedThrough = LocalDate.of(2026, 10, 12))
        assertEquals(LocalDateTime.of(2026, 10, 19, 9, 0), due)
    }

    @Test
    fun oralBioavailabilityScalesLevel() {
        val oral = Pharmacokinetics.Params(168.0, 1.0, bioavailability = 0.01)
        val dose = Pharmacokinetics.Dose(0, 14.0, oral)
        val level = Pharmacokinetics.amountAt(dose, 24 * 3_600_000L)
        assert(level < 0.14) { "oral level should be ~1 % of the dose, was $level" }
    }
}

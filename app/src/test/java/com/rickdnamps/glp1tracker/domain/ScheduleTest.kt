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
}

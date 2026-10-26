package com.rickdnamps.glp1tracker.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

object Schedule {

    /**
     * Next planned dose. With a previous dose it is that day + interval at the reminder
     * time; without one it is the next preferred weekday (weekly plans) or today.
     */
    fun nextDue(
        lastDose: LocalDateTime?,
        intervalDays: Int,
        preferredDay: DayOfWeek,
        time: LocalTime,
        today: LocalDate,
    ): LocalDateTime {
        val interval = intervalDays.coerceAtLeast(1).toLong()
        if (lastDose != null) return lastDose.toLocalDate().plusDays(interval).atTime(time)
        return if (interval == 7L) {
            today.with(TemporalAdjusters.nextOrSame(preferredDay)).atTime(time)
        } else {
            today.atTime(time)
        }
    }

    /** When to fire the reminder: the due time, or the next daily nag while overdue. */
    fun nextReminder(due: LocalDateTime, time: LocalTime, now: LocalDateTime): LocalDateTime {
        if (due.isAfter(now)) return due
        val todayAt = now.toLocalDate().atTime(time)
        return if (todayAt.isAfter(now)) todayAt else todayAt.plusDays(1)
    }
}

package com.rickdnamps.glp1tracker.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class ThemeScheduleTest {
    @Test
    fun nightAcrossMidnight() {
        val start = 20 * 60
        val end = 7 * 60
        assertTrue(ThemeSchedule.isNight(LocalTime.of(22, 0), start, end))
        assertTrue(ThemeSchedule.isNight(LocalTime.of(3, 30), start, end))
        assertTrue(ThemeSchedule.isNight(LocalTime.of(20, 0), start, end))
        assertFalse(ThemeSchedule.isNight(LocalTime.of(7, 0), start, end))
        assertFalse(ThemeSchedule.isNight(LocalTime.of(12, 0), start, end))
    }

    @Test
    fun nightWithinTheSameDayAndEmptyWindow() {
        assertTrue(ThemeSchedule.isNight(LocalTime.of(1, 0), 0, 6 * 60))
        assertFalse(ThemeSchedule.isNight(LocalTime.of(7, 0), 0, 6 * 60))
        assertFalse(ThemeSchedule.isNight(LocalTime.of(7, 0), 300, 300))
    }
}

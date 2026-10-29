package com.rickdnamps.glp1tracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TherapyStatsTest {
    private val day = 86_400_000L

    @Test
    fun summarizesDosesIntervalsAndComfort() {
        // Weekly doses on days 0, 7, 14, 23 (one late), comfort 5, 4, 0 (not rated), 3.
        val entries = listOf(
            TherapyStats.Entry(0, 0.5, 5),
            TherapyStats.Entry(7 * day, 0.5, 4),
            TherapyStats.Entry(14 * day, 1.0, 0),
            TherapyStats.Entry(23 * day, 1.0, 3),
        )
        val s = TherapyStats.summarize(entries, fromMillis = 0, toMillis = 30 * day, plannedIntervalDays = 7)
        assertEquals(4, s.doses)
        assertEquals(0, s.previousDoses)
        assertEquals(0.75, s.averageDoseMg!!, 1e-9)
        assertEquals(23.0 / 3, s.averageIntervalDays!!, 1e-9)
        assertEquals(2.0 / 3, s.onTimeShare!!, 1e-9)
        assertEquals(4.0, s.averageComfort!!, 1e-9)
    }

    @Test
    fun previousPeriodAndIntervalCrossingTheBoundary() {
        val entries = listOf(TherapyStats.Entry(0, 1.0), TherapyStats.Entry(7 * day, 1.0), TherapyStats.Entry(14 * day, 1.0))
        val s = TherapyStats.summarize(entries, fromMillis = 10 * day, toMillis = 20 * day, plannedIntervalDays = 7)
        assertEquals(1, s.doses)
        assertEquals(2, s.previousDoses)
        assertEquals(7.0, s.averageIntervalDays!!, 1e-9) // 7 → 14 ends inside the period
        assertNull(TherapyStats.summarize(emptyList(), 0, day, 7).averageDoseMg)
    }

    @Test
    fun peakComesAboutTmaxAfterTheDose() {
        val params = Pharmacokinetics.Params(halfLifeHours = 168.0, tmaxHours = 48.0)
        val dose = Pharmacokinetics.Dose(0, 1.0, params)
        val peak = TherapyStats.peakTime(listOf(dose), 0, 7 * day)
        assertNotNull(peak)
        assertEquals(48.0, peak!! / 3_600_000.0, 1.0)
    }

    @Test
    fun comfortBySiteAndCyclePhases() {
        val bySite = TherapyStats.comfortBySite(listOf("A" to 5, "A" to 3, "B" to 2, "B" to 0))
        assertEquals(4.0, bySite["A"]!!, 1e-9)
        assertEquals(2.0, bySite["B"]!!, 1e-9)
        assertEquals(CyclePhase.INJECTION_DAY, CycleTips.phase(0, 7))
        assertEquals(CyclePhase.EARLY, CycleTips.phase(2, 7))
        assertEquals(CyclePhase.PEAK, CycleTips.phase(4, 7))
        assertEquals(CyclePhase.LATE, CycleTips.phase(6, 7))
        assertNull(CycleTips.phase(3, 1))
        assertTrue(CycleTips.phase(null, 7) == null)
    }
}

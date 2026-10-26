package com.rickdnamps.glp1tracker.report

import com.rickdnamps.glp1tracker.report.ReportMath.DAY_MS
import com.rickdnamps.glp1tracker.report.ReportMath.DoseRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportMathTest {
    private fun day(n: Int): Long = 1_700_000_000_000L + n * DAY_MS
    private fun weekly(fromDay: Int, count: Int): List<Long> = (0 until count).map { day(fromDay + 7 * it) }

    @Test
    fun treatmentStartedInPeriodCountsFromFirstDose() {
        // First dose on day 10, then weekly; the day-38 dose is still within the grace day.
        assertEquals(4, ReportMath.expectedDoses(day(0), day(38), 7, day(10)))
        val adherence = ReportMath.adherence(weekly(10, 4), day(0), day(38), 7)!!
        assertEquals(4, adherence.taken)
        assertEquals(100, adherence.percent)
    }

    @Test
    fun treatmentStartedBeforePeriodCountsWholeIntervals() {
        assertEquals(12, ReportMath.expectedDoses(day(0), day(91), 7, day(-100)))
    }

    @Test
    fun adherenceIsCappedAndReflectsMissedDoses() {
        val history = weekly(-98, 14) + weekly(0, 13) // 13 doses in a 91-day window: more than the 12 expected
        assertEquals(100, ReportMath.adherence(history, day(0), day(91), 7)!!.percent)

        val missed = weekly(-98, 14) + weekly(0, 13).filterIndexed { i, _ -> i != 3 && i != 7 }
        val adherence = ReportMath.adherence(missed, day(0), day(91), 7)!!
        assertEquals(11, adherence.taken)
        assertEquals(12, adherence.expected)
        assertEquals(92, adherence.percent)
    }

    @Test
    fun adherenceUnknownWithoutDosesOrWhenPeriodTooShort() {
        assertNull(ReportMath.adherence(emptyList(), day(0), day(90), 7))
        assertNull(ReportMath.adherence(listOf(day(-30)), day(0), day(5), 7))
    }

    @Test
    fun dailyDosing() {
        val doses = (0 until 30).map { day(it) }
        assertEquals(100, ReportMath.adherence(doses, day(0), day(30), 1)!!.percent)
    }

    @Test
    fun doseStepsFollowEachChange() {
        val doses = (0 until 4).map { DoseRecord(day(7 * it), "OZEMPIC", 0.25) } +
            (4 until 8).map { DoseRecord(day(7 * it), "OZEMPIC", 0.5) } +
            (8 until 10).map { DoseRecord(day(7 * it), "OZEMPIC", 1.0) }
        val steps = ReportMath.doseSteps(doses.shuffled(java.util.Random(1)))
        assertEquals(listOf(0.25, 0.5, 1.0), steps.map { it.doseMg })
        assertEquals(listOf(4, 4, 2), steps.map { it.count })
        assertEquals(listOf(day(0), day(28), day(56)), steps.map { it.startMillis })
        assertTrue(ReportMath.isTitration(steps))
    }

    @Test
    fun decreaseOrMedicationSwitchIsNotATitration() {
        val down = ReportMath.doseSteps(listOf(DoseRecord(day(0), "OZEMPIC", 1.0), DoseRecord(day(7), "OZEMPIC", 0.5)))
        assertFalse(ReportMath.isTitration(down))
        val switched = ReportMath.doseSteps(listOf(DoseRecord(day(0), "OZEMPIC", 1.0), DoseRecord(day(7), "MOUNJARO", 2.5)))
        assertEquals(2, switched.size)
        assertFalse(ReportMath.isTitration(switched))
        val single = ReportMath.doseSteps(listOf(DoseRecord(day(0), "OZEMPIC", 1.0), DoseRecord(day(7), "OZEMPIC", 1.0)))
        assertEquals(1, single.size)
        assertFalse(ReportMath.isTitration(single))
    }

    @Test
    fun symptomStatsSortedByFrequency() {
        val stats = ReportMath.symptomStats(
            listOf("NAUSEA" to 1, "FATIGUE" to 3, "NAUSEA" to 2, "NAUSEA" to 3, "FATIGUE" to 1, "HEADACHE" to 2)
        )
        assertEquals(listOf("NAUSEA", "FATIGUE", "HEADACHE"), stats.map { it.type })
        assertEquals(3, stats[0].count)
        assertEquals(2.0, stats[0].averageSeverity, 1e-9)
        assertEquals(3, stats[0].maxSeverity)
        assertEquals(2.0, stats[1].averageSeverity, 1e-9)
    }

    @Test
    fun averagesIgnoreUnsetValues() {
        assertEquals(3.0, ReportMath.averageOfSet(listOf(0, 2, 4, 0))!!, 1e-9)
        assertNull(ReportMath.averageOfSet(listOf(0, 0)))
        assertEquals(2, ReportMath.countSet(listOf(0, 2, 4, 0)))
    }

    @Test
    fun changePercentIsSigned() {
        assertEquals(-5.0, ReportMath.changePercent(100.0, 95.0)!!, 1e-9)
        assertNull(ReportMath.changePercent(0.0, 95.0))
    }

    @Test
    fun niceTicksCoverTheRange() {
        assertEquals(listOf(70.0, 75.0, 80.0, 85.0, 90.0), ReportMath.niceTicks(72.3, 85.1, 5))
        val flat = ReportMath.niceTicks(80.0, 80.0, 5)
        assertTrue(flat.size >= 2)
        assertTrue(flat.first() <= 79.0 && flat.last() >= 81.0)
        val small = ReportMath.niceTicks(81.2, 82.0, 5)
        assertTrue(small.first() <= 81.2 + 1e-9 && small.last() >= 82.0 - 1e-9)
        assertTrue(small.size in 2..7)
    }

    @Test
    fun timeTicksIncludeBothEnds() {
        assertEquals(listOf(0L, 50L, 100L), ReportMath.timeTicks(0, 100, 3))
        assertEquals(listOf(10L), ReportMath.timeTicks(10, 10, 5))
    }
}

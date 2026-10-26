package com.rickdnamps.glp1tracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrialsTest {
    private val week = 7 * 86_400_000L
    private val t0 = 1_767_225_600_000L // 2026-01-01T00:00Z

    @Test
    fun curvesAreWellFormed() {
        Trial.entries.forEach { trial ->
            assertEquals(trial.name, 0 to 0.0, trial.points.first())
            assertTrue(trial.name, trial.points.zipWithNext().all { (a, b) -> b.first > a.first })
            assertTrue(trial.name, trial.points.all { it.second <= 0.0 })
            // Mean weight never goes back up by more than rounding in these trials.
            assertTrue(trial.name, trial.points.zipWithNext().all { (a, b) -> b.second <= a.second + 1e-9 })
            assertTrue(trial.name, trial.citation.isNotBlank())
        }
    }

    @Test
    fun publishedEndpoints() {
        assertEquals(68, Trial.STEP1_SEMAGLUTIDE_2_4.endWeek)
        assertEquals(-14.9, Trial.STEP1_SEMAGLUTIDE_2_4.endPercent, 1e-9)
        assertEquals(-2.4, Trial.STEP1_SEMAGLUTIDE_2_4.placeboPercent!!, 1e-9)
        assertEquals(-20.9, Trial.SURMOUNT1_TIRZEPATIDE_15.endPercent, 1e-9)
        assertEquals(-19.5, Trial.SURMOUNT1_TIRZEPATIDE_10.endPercent, 1e-9)
        assertEquals(-15.0, Trial.SURMOUNT1_TIRZEPATIDE_5.endPercent, 1e-9)
        assertEquals(72, Trial.SURMOUNT1_TIRZEPATIDE_5.endWeek)
        assertEquals(-8.0, Trial.SCALE_LIRAGLUTIDE_3_0.endPercent, 1e-9)
        assertEquals(56, Trial.SCALE_LIRAGLUTIDE_3_0.endWeek)
        assertTrue(Trial.SUSTAIN7_SEMAGLUTIDE_1_0.diabetes)
        assertFalse(Trial.STEP1_SEMAGLUTIDE_2_4.diabetes)
    }

    @Test
    fun interpolatesLinearly() {
        val t = Trial.STEP1_SEMAGLUTIDE_2_4
        assertEquals(0.0, Trials.expectedAt(t, -3.0), 1e-9)
        assertEquals(0.0, Trials.expectedAt(t, 0), 1e-9)
        assertEquals(-2.5, Trials.expectedAt(t, 4), 1e-9)
        assertEquals(-1.25, Trials.expectedAt(t, 2.0), 1e-9)
        assertEquals(-6.0, Trials.expectedAt(t, 12), 1e-9)
        assertEquals(-4.25, Trials.expectedAt(t, 8.0), 1e-9) // halfway between −2.5 (wk 4) and −6 (wk 12)
        assertEquals(-10.0, Trials.expectedAt(t, 28), 1e-9)
        // Held at the final value after the end of the trial.
        assertEquals(-14.9, Trials.expectedAt(t, 68), 1e-9)
        assertEquals(-14.9, Trials.expectedAt(t, 100.0), 1e-9)
    }

    @Test
    fun picksTheReferenceTrial() {
        assertEquals(Trial.STEP1_SEMAGLUTIDE_2_4, Trials.referenceFor(Medication.WEGOVY, 0.25))
        assertEquals(Trial.STEP1_SEMAGLUTIDE_2_4, Trials.referenceFor(Medication.SEMAGLUTIDE_COMPOUNDED, 1.0))
        assertEquals(Trial.SUSTAIN7_SEMAGLUTIDE_0_5, Trials.referenceFor(Medication.OZEMPIC, 0.25))
        assertEquals(Trial.SUSTAIN7_SEMAGLUTIDE_1_0, Trials.referenceFor(Medication.OZEMPIC, 1.0))
        assertEquals(Trial.SUSTAIN7_SEMAGLUTIDE_1_0, Trials.referenceFor(Medication.OZEMPIC, 2.0))
        assertEquals(Trial.SURMOUNT1_TIRZEPATIDE_5, Trials.referenceFor(Medication.MOUNJARO, 2.5))
        assertEquals(Trial.SURMOUNT1_TIRZEPATIDE_10, Trials.referenceFor(Medication.ZEPBOUND, 7.5)) // tie goes up
        assertEquals(Trial.SURMOUNT1_TIRZEPATIDE_10, Trials.referenceFor(Medication.MOUNJARO, 10.0))
        assertEquals(Trial.SURMOUNT1_TIRZEPATIDE_15, Trials.referenceFor(Medication.TIRZEPATIDE_COMPOUNDED, 12.5))
        assertEquals(Trial.SCALE_LIRAGLUTIDE_3_0, Trials.referenceFor(Medication.SAXENDA, 1.8))
        assertEquals(Trial.AWARD11_DULAGLUTIDE_1_5, Trials.referenceFor(Medication.TRULICITY, 0.75))
        assertEquals(Trial.AWARD11_DULAGLUTIDE_4_5, Trials.referenceFor(Medication.TRULICITY, 4.5))
        assertNull(Trials.referenceFor(Medication.RYBELSUS, 14.0))
        assertNull(Trials.referenceFor(Medication.VICTOZA, 1.8))
        assertNull(Trials.referenceFor(Medication.OTHER, 1.0))
    }

    @Test
    fun userCurveIsPercentFromBaselineByWeek() {
        val weights = listOf(t0 - 2 * week to 101.0, t0 to 100.0, t0 + 4 * week to 97.0, t0 + 12 * week to 92.9)
        assertEquals(100.0, Trials.baselineKg(null, t0, weights)!!, 1e-9)
        // A weigh-in at the first dose beats a declared start weight (which may be months older).
        assertEquals(100.0, Trials.baselineKg(105.0, t0, weights)!!, 1e-9)
        // Same-day weigh-in a few hours after the injection still counts as week 0.
        assertEquals(99.5, Trials.baselineKg(105.0, t0, listOf(t0 + 3_600_000L to 99.5, t0 + 4 * week to 97.0))!!, 1e-9)
        // Nothing near the first dose: declared start, else last before, else first weigh-in.
        val later = listOf(t0 - 6 * week to 103.0, t0 + 4 * week to 97.0)
        assertEquals(105.0, Trials.baselineKg(105.0, t0, later)!!, 1e-9)
        assertEquals(103.0, Trials.baselineKg(0.0, t0, later)!!, 1e-9)
        assertNull(Trials.baselineKg(null, t0, listOf(t0 + 4 * week to 97.0))) // only later weigh-ins
        assertNull(Trials.baselineKg(null, t0, emptyList()))

        val curve = Trials.userCurve(t0, 100.0, weights)
        assertEquals(3, curve.size)
        assertEquals(0.0 to 0.0, curve[0])
        assertEquals(4.0, curve[1].first, 1e-9)
        assertEquals(-3.0, curve[1].second, 1e-9)
        assertEquals(12.0, curve[2].first, 1e-9)
        assertEquals(-7.1, curve[2].second, 1e-9)
    }

    @Test
    fun comparesWithTheStudy() {
        val t = Trial.STEP1_SEMAGLUTIDE_2_4
        val ahead = Trials.compare(t, 12.0, -7.1)
        assertEquals(Trials.Standing.AHEAD, ahead.standing)
        assertEquals(-6.0, ahead.trialPercent, 1e-9)
        assertEquals(1.1, ahead.pointsAhead, 1e-9)
        assertFalse(ahead.beyondTrial)
        assertEquals(Trials.Standing.ON_PAR, Trials.compare(t, 12.0, -5.5).standing)
        assertEquals(Trials.Standing.BEHIND, Trials.compare(t, 12.0, -3.0).standing)
        assertEquals(Trials.Standing.BEHIND, Trials.compare(t, 12.0, 1.0).standing) // gained weight
        assertTrue(Trials.compare(t, 80.0, -18.0).beyondTrial)
    }

    @Test
    fun reportHandlesMissingData() {
        val doses = listOf(t0 to Medication.WEGOVY, t0 + week to Medication.WEGOVY)
        assertEquals(Trials.Status.NO_TRIAL, Trials.report(Medication.RYBELSUS, 7.0, doses, emptyList(), 100.0).status)
        assertEquals(Trials.Status.NO_DOSES, Trials.report(Medication.WEGOVY, 0.25, emptyList(), emptyList(), 100.0).status)
        assertEquals(Trials.Status.NO_WEIGHTS, Trials.report(Medication.WEGOVY, 0.25, doses, emptyList(), 100.0).status)
        assertEquals(Trials.Status.NO_BASELINE, Trials.report(Medication.WEGOVY, 0.25, doses, emptyList(), null).status)
        assertEquals(Trials.Status.NO_BASELINE, Trials.report(Medication.WEGOVY, 0.25, doses, listOf(t0 + 5 * week to 95.0), 0.0).status)
        val early = Trials.report(Medication.WEGOVY, 0.25, doses, listOf(t0 + 6 * 86_400_000L to 99.0), 100.0)
        assertEquals(Trials.Status.TOO_EARLY, early.status)
        assertEquals(2, early.userCurve.size)
    }

    @Test
    fun reportReady() {
        val doses = (0 until 13).map { t0 + it * week to Medication.WEGOVY }.reversed()
        val weights = listOf(t0 to 100.0, t0 + 6 * week to 96.0, t0 + 12 * week to 92.9)
        val r = Trials.report(Medication.WEGOVY, 1.7, doses, weights, startWeightKg = null)
        assertEquals(Trials.Status.READY, r.status)
        assertEquals(Trial.STEP1_SEMAGLUTIDE_2_4, r.trial)
        assertEquals(t0, r.firstDoseMillis)
        assertEquals(100.0, r.baselineKg!!, 1e-9)
        val c = r.comparison!!
        assertEquals(12.0, c.week, 1e-9)
        assertEquals(-7.1, c.userPercent, 1e-9)
        assertEquals(Trials.Standing.AHEAD, c.standing)
    }

    @Test
    fun weekZeroIsTheFirstDoseOfTheCurrentMolecule() {
        // Six weeks of Ozempic, then a switch to Mounjaro: the comparison starts at the first Mounjaro dose,
        // with the weight at that time as baseline (the declared start weight belongs to the first drug).
        val doses = (0 until 6).map { t0 + it * week to Medication.OZEMPIC } +
            (6 until 12).map { t0 + it * week to Medication.MOUNJARO }
        val weights = listOf(t0 to 100.0, t0 + 6 * week to 96.0, t0 + 11 * week to 91.2)
        val r = Trials.report(Medication.MOUNJARO, 5.0, doses, weights, startWeightKg = 100.0)
        assertEquals(t0 + 6 * week, r.firstDoseMillis)
        assertEquals(96.0, r.baselineKg!!, 1e-9)
        assertEquals(5.0, r.comparison!!.week, 1e-9)
        assertEquals(-5.0, r.comparison!!.userPercent, 1e-9)
    }
}

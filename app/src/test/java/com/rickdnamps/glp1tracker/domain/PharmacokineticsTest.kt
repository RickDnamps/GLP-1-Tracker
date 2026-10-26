package com.rickdnamps.glp1tracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.ln

class PharmacokineticsTest {
    private val hour = 3_600_000L
    private val sema = Pharmacokinetics.Params(halfLifeHours = 168.0, tmaxHours = 56.0)

    @Test
    fun absorptionRateMatchesTmax() {
        val ke = ln(2.0) / 168.0
        val ka = Pharmacokinetics.absorptionRate(ke, 56.0)
        assertEquals(56.0, ln(ka / ke) / (ka - ke), 0.01)
    }

    @Test
    fun levelPeaksAroundTmaxThenHalvesAfterHalfLife() {
        val dose = Pharmacokinetics.Dose(0, 1.0, sema)
        val curve = (0..400).map { it to Pharmacokinetics.amountAt(dose, it * hour) }
        val peakHour = curve.maxBy { it.second }.first
        assertEquals(56.0, peakHour.toDouble(), 1.0)
        // Long after absorption, the amount halves every half-life.
        val a = Pharmacokinetics.amountAt(dose, 500 * hour)
        val b = Pharmacokinetics.amountAt(dose, 668 * hour)
        assertEquals(0.5, b / a, 0.01)
    }

    @Test
    fun noLevelBeforeDoseAndDosesAccumulate() {
        val d1 = Pharmacokinetics.Dose(100 * hour, 1.0, sema)
        val d2 = Pharmacokinetics.Dose(268 * hour, 1.0, sema)
        assertEquals(0.0, Pharmacokinetics.amountAt(d1, 0), 0.0)
        val single = Pharmacokinetics.levelAt(listOf(d1), 300 * hour)
        val both = Pharmacokinetics.levelAt(listOf(d1, d2), 300 * hour)
        assertTrue(both > single)
    }
}

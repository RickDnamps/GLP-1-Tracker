package com.rickdnamps.glp1tracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class InsightsTest {
    private val zone = ZoneId.systemDefault()
    private fun at(date: LocalDate, hour: Int = 9) = date.atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()
    private val day = 86_400_000L

    @Test
    fun nauseaPeaksTwoDaysAfterInjection() {
        val start = LocalDate.of(2026, 1, 5)
        val doses = (0 until 6).map { at(start.plusWeeks(it.toLong())) }
        val nausea = (0 until 6).flatMap { w ->
            listOf(at(start.plusWeeks(w.toLong()).plusDays(2), 14)) + if (w % 2 == 0) listOf(at(start.plusWeeks(w.toLong()).plusDays(1), 14)) else emptyList()
        }
        val p = Insights.cycleProfile(nausea, doses, 7)
        assertEquals(2, p.peakDay)
        assertEquals(9, p.total)
        assertEquals(6.0 / 9, p.peakShare, 1e-9)
        assertNull(Insights.cycleDay(at(start.minusDays(1)), doses, 7))
    }

    @Test
    fun appetiteReturnsLateInCycle() {
        val start = LocalDate.of(2026, 1, 5)
        val doses = (0 until 4).map { at(start.plusWeeks(it.toLong())) }
        val scores = (0 until 28).map { d -> start.plusDays(d.toLong()) to (if (d % 7 >= 5) 4 else 2) }
        val byDay = Insights.scoreByCycleDay(scores, doses, 7)
        assertEquals(5, Insights.firstDayAtLeast(byDay, 3.5))
    }

    @Test
    fun plateauDetectedOnFlatTrend() {
        val now = at(LocalDate.of(2026, 3, 1))
        val flat = (0 until 40).map { now - (40 - it) * day to 90.0 + if (it % 2 == 0) 0.4 else -0.4 }
        assertEquals(Insights.WeightPhase.PLATEAU, Insights.plateau(flat, now).phase)
        val losing = (0 until 60).map { now - (60 - it) * day to 100.0 - it * 0.15 }
        assertEquals(Insights.WeightPhase.LOSING, Insights.plateau(losing, now).phase)
    }

    @Test
    fun paceAndProtein() {
        val now = at(LocalDate.of(2026, 3, 1))
        // ~1 kg per week at 100 kg ≈ 1 %/week, smoothed trend lags so it reads a bit lower.
        val w = (0 until 120).map { now - (120 - it) * day to 110.0 - it / 7.0 }
        val (pace, pct) = Insights.pace(w, now)!!
        assertTrue(pace == Insights.Pace.HEALTHY || pace == Insights.Pace.FAST)
        assertTrue(pct in 0.6..1.3)
        assertEquals(96..128, Insights.proteinRange(100.0, 80.0))
    }

    @Test
    fun syringeMath() {
        val r = Syringe.compute(2.5, 0.5)!!
        assertEquals(0.2, r.ml, 1e-9)
        assertEquals(20.0, r.units, 1e-9)
        assertTrue(r.warnings.isEmpty())
        assertTrue(Syringe.Warning.TENFOLD_CHANGE in Syringe.compute(2.5, 5.0, usualDoseMg = 0.5)!!.warnings)
        assertTrue(Syringe.Warning.OVER_SYRINGE in Syringe.compute(1.0, 2.5)!!.warnings)
        assertNull(Syringe.compute(0.0, 1.0))
    }
}

package com.rickdnamps.glp1tracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class WeeklyRecapTest {
    private val zone = ZoneId.of("America/Toronto")
    private val end = LocalDate.of(2026, 10, 25) // a Sunday
    private fun at(date: LocalDate, hour: Int = 9) = date.atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()
    private fun daysBack(n: Long) = end.minusDays(n)

    private fun input(
        doses: List<LocalDate> = emptyList(),
        weights: List<Pair<LocalDate, Double>> = emptyList(),
        water: Map<LocalDate, Int> = emptyMap(),
        protein: Map<LocalDate, Int> = emptyMap(),
        effects: List<Pair<LocalDate, SideEffectType>> = emptyList(),
        journal: Set<LocalDate> = emptySet(),
        interval: Int = 7,
    ) = RecapInput(
        doseTimes = doses.map { at(it) },
        intervalDays = interval,
        weights = weights.map { at(it.first, 7) to it.second },
        waterByDay = water,
        proteinByDay = protein,
        waterGoalMl = 2000,
        proteinGoalG = 80,
        sideEffects = effects.map { at(it.first, 15) to it.second },
        journalDays = journal,
        end = end,
        zone = zone,
    )

    @Test
    fun emptyDataGivesAnEmptyRecap() {
        val r = WeeklyRecaps.compute(input())
        assertEquals(LocalDate.of(2026, 10, 19), r.start)
        assertEquals(0, r.dosesTaken)
        assertEquals(0, r.dosesExpected)
        assertNull(r.weightChangeKg)
        assertNull(r.waterAverageMl)
        assertNull(r.bestStreak)
        assertTrue(r.highlights.isEmpty())
    }

    @Test
    fun weeklyDoses() {
        // Weekly on Wednesdays: last week's dose anchors this week's due date.
        val onTime = WeeklyRecaps.compute(input(doses = listOf(daysBack(11), daysBack(4))))
        assertEquals(1, onTime.dosesTaken)
        assertEquals(1, onTime.dosesExpected)
        assertTrue(onTime.highlights.any { it.kind == HighlightKind.ALL_DOSES && it.count == 1 && it.total == 1 })
        assertTrue(onTime.highlights.none { it.kind == HighlightKind.MISSED_DOSE })

        val missed = WeeklyRecaps.compute(input(doses = listOf(daysBack(11))))
        assertEquals(0, missed.dosesTaken)
        assertEquals(1, missed.dosesExpected)
        assertTrue(missed.highlights.any { it.kind == HighlightKind.MISSED_DOSE && it.count == 0 && it.total == 1 })

        // Due today and not taken yet: not counted as missed.
        val dueToday = WeeklyRecaps.compute(input(doses = listOf(daysBack(7))))
        assertEquals(0, dueToday.dosesExpected)
        assertTrue(dueToday.highlights.none { it.kind == HighlightKind.MISSED_DOSE })

        // First dose ever this week: 1/1.
        val first = WeeklyRecaps.compute(input(doses = listOf(daysBack(2))))
        assertEquals(1, first.dosesExpected)

        // Paused for a month: no dose line at all.
        val paused = WeeklyRecaps.compute(input(doses = listOf(daysBack(40))))
        assertEquals(0, paused.dosesExpected)
    }

    @Test
    fun dailyDoses() {
        val all = (0L..13L).map { daysBack(it) }
        val r = WeeklyRecaps.compute(input(doses = all, interval = 1))
        assertEquals(7, r.dosesTaken)
        assertEquals(7, r.dosesExpected)
        val missingTwo = all.filterNot { it == daysBack(2) || it == daysBack(3) }
        val m = WeeklyRecaps.compute(input(doses = missingTwo, interval = 1))
        assertEquals(5, m.dosesTaken)
        assertEquals(7, m.dosesExpected)
    }

    @Test
    fun weightChangeUsesTheTrend() {
        // Daily weigh-ins losing 0.1 kg/day for 4 weeks.
        val weights = (0L..27L).map { daysBack(27 - it) to 100.0 - it * 0.1 }
        val r = WeeklyRecaps.compute(input(weights = weights))
        val change = r.weightChangeKg!!
        assertTrue("change $change", change < -0.4 && change > -0.8)
        assertEquals(HighlightKind.WEIGHT_LOST, r.highlights.first().kind)
        assertTrue(r.previousWeightChangeKg!! < 0)

        // One salty-dinner spike on the last day barely moves the trend.
        val spiky = weights.dropLast(1) + (end to 101.0)
        val s = WeeklyRecaps.compute(input(weights = spiky))
        assertTrue("spike ${s.weightChangeKg}", s.weightChangeKg!! < WeeklyRecaps.STABLE_KG)

        // No weigh-in this week: no weight line rather than a fake "stable".
        val old = WeeklyRecaps.compute(input(weights = weights.filter { it.first.isBefore(daysBack(8)) }))
        assertNull(old.weightChangeKg)
        assertTrue(old.highlights.none { it.kind.name.startsWith("WEIGHT") })
    }

    @Test
    fun weeklyWeighInsAreNotOverSmoothed() {
        // One weigh-in a week, −1 kg each week: alpha adapts to the gap.
        val weights = (0L..5L).map { daysBack(35 - it * 7) to 100.0 - it }
        assertEquals(1 - Math.pow(0.9, 7.0), WeeklyRecaps.trendAlpha(weights.map { at(it.first) to it.second }), 1e-6)
        val r = WeeklyRecaps.compute(input(weights = weights))
        assertTrue("change ${r.weightChangeKg}", r.weightChangeKg!! < -0.5)
    }

    @Test
    fun stableAndGain() {
        val flat = (0L..13L).map { daysBack(13 - it) to 90.0 + if (it % 2 == 0L) 0.1 else -0.1 }
        assertTrue(WeeklyRecaps.compute(input(weights = flat)).highlights.any { it.kind == HighlightKind.WEIGHT_STABLE })
        val gain = (0L..13L).map { daysBack(13 - it) to 90.0 + it * 0.15 }
        assertTrue(WeeklyRecaps.compute(input(weights = gain)).highlights.any { it.kind == HighlightKind.WEIGHT_GAINED })
    }

    @Test
    fun waterAndProteinAgainstGoals() {
        val water = (0L..6L).associate { daysBack(it) to if (it < 5) 2200 else 1000 } + (daysBack(10) to 1500)
        val protein = mapOf(daysBack(0) to 60, daysBack(1) to 70)
        val r = WeeklyRecaps.compute(input(water = water, protein = protein))
        assertEquals(5, r.waterDaysAtGoal)
        assertEquals(1857, r.waterAverageMl) // (5 × 2200 + 2 × 1000) / 7
        assertEquals(1500, r.previousWaterAverageMl)
        assertEquals(65, r.proteinAverageG)
        assertEquals(0, r.proteinDaysAtGoal)
        val w = r.highlights.first { it.kind == HighlightKind.WATER_GOAL }
        assertEquals(5, w.count)
        assertEquals(2000, w.total)
        assertTrue(r.highlights.any { it.kind == HighlightKind.PROTEIN_BELOW_GOAL && it.value == 65.0 })
    }

    @Test
    fun sideEffectsComparedWithLastWeek() {
        val effects = listOf(
            daysBack(1) to SideEffectType.NAUSEA,
            daysBack(2) to SideEffectType.NAUSEA,
            daysBack(3) to SideEffectType.FATIGUE,
            daysBack(8) to SideEffectType.NAUSEA,
            daysBack(9) to SideEffectType.NAUSEA,
            daysBack(10) to SideEffectType.NAUSEA,
            daysBack(11) to SideEffectType.HEADACHE,
        )
        val r = WeeklyRecaps.compute(input(effects = effects))
        assertEquals(3, r.sideEffectCount)
        assertEquals(4, r.previousSideEffectCount)
        assertEquals(SideEffectType.NAUSEA, r.topSideEffect)
        assertEquals(2, r.topSideEffectCount)
        val h = r.highlights.first { it.kind == HighlightKind.FEWER_SIDE_EFFECTS }
        assertEquals(3, h.count)
        assertEquals(4.0, h.previous!!, 1e-9)
        assertEquals(SideEffectType.NAUSEA, h.sideEffect)

        val none = WeeklyRecaps.compute(input(effects = effects.filter { it.first.isBefore(daysBack(6)) }))
        assertTrue(none.highlights.any { it.kind == HighlightKind.NO_SIDE_EFFECTS })
        val more = WeeklyRecaps.compute(input(effects = effects.filter { !it.first.isBefore(daysBack(6)) }))
        assertTrue(more.highlights.any { it.kind == HighlightKind.MORE_SIDE_EFFECTS })
    }

    @Test
    fun journalAndStreaks() {
        // Journal every day for 10 days; water goal the last 4 days.
        val journal = (0L..9L).map { daysBack(it) }.toSet()
        val water = (0L..3L).associate { daysBack(it) to 2500 }
        val r = WeeklyRecaps.compute(input(journal = journal, water = water))
        assertEquals(7, r.journalDays)
        assertEquals(3, r.previousJournalDays)
        assertEquals(Streak(StreakKind.JOURNAL, 10), r.bestStreak)
        assertTrue(r.highlights.any { it.kind == HighlightKind.STREAK && it.count == 10 && it.streak == StreakKind.JOURNAL })

        // A streak is still alive when today is not logged yet.
        val yesterday = WeeklyRecaps.compute(input(journal = (1L..4L).map { daysBack(it) }.toSet()))
        assertEquals(4, yesterday.bestStreak!!.days)
        // Too short to mention.
        assertNull(WeeklyRecaps.compute(input(journal = setOf(daysBack(0), daysBack(1)))).bestStreak)
    }

    @Test
    fun dayStripAndWhichWeek() {
        val r = WeeklyRecaps.compute(input(doses = listOf(daysBack(4)), journal = setOf(daysBack(0), daysBack(6)), water = mapOf(daysBack(1) to 2000)))
        assertEquals(7, r.days.size)
        assertEquals(r.start, r.days.first().date)
        assertEquals(end, r.days.last().date)
        assertEquals(listOf(true, false, false, false, false, false, true), r.days.map { it.journal })
        assertTrue(r.days[2].dose)
        assertTrue(r.days[5].waterGoal)
        val monday = LocalDate.of(2026, 10, 26)
        assertEquals(LocalDate.of(2026, 10, 25), WeeklyRecaps.defaultEnd(monday))
        assertEquals(LocalDate.of(2026, 10, 28), WeeklyRecaps.defaultEnd(LocalDate.of(2026, 10, 28)))
        assertTrue(WeeklyRecaps.isRecapDay(monday))
        assertTrue(WeeklyRecaps.isRecapDay(end))
        assertTrue(!WeeklyRecaps.isRecapDay(LocalDate.of(2026, 10, 28)))
    }

    @Test
    fun highlightsAreSortedByImportance() {
        val r = WeeklyRecaps.compute(
            input(
                doses = listOf(daysBack(11), daysBack(4)),
                weights = (0L..13L).map { daysBack(13 - it) to 100.0 - it * 0.15 },
                water = (0L..6L).associate { daysBack(it) to 2100 },
                journal = (0L..6L).map { daysBack(it) }.toSet(),
            ),
        )
        val kinds = r.highlights.map { it.kind }
        assertEquals(HighlightKind.WEIGHT_LOST, kinds.first())
        assertTrue(kinds.indexOf(HighlightKind.ALL_DOSES) < kinds.indexOf(HighlightKind.WATER_GOAL))
        assertEquals(HighlightKind.JOURNAL, kinds.last())
        assertEquals(r.highlights.sortedByDescending { it.importance }, r.highlights)
    }
}

package com.rickdnamps.glp1tracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class MilestonesTest {

    private val zone = ZoneOffset.UTC
    private val day0 = LocalDate.of(2026, 1, 1)
    private val t0 = day0.atStartOfDay(zone).toInstant().toEpochMilli()
    private val dayMs = 86_400_000L

    private fun at(day: Int, hour: Int = 9) = t0 + day * dayMs + hour * 3_600_000L

    private fun input(
        injections: List<MilestoneInput.Injection> = emptyList(),
        weights: List<Pair<Long, Double>> = emptyList(),
        journalDays: Set<LocalDate> = emptySet(),
        waterByDay: Map<LocalDate, Int> = emptyMap(),
        waterGoalMl: Int = 2000,
        startWeightKg: Double? = null,
        goalWeightKg: Double? = null,
        nowDay: Int = 0,
    ) = MilestoneInput(injections, weights, journalDays, waterByDay, waterGoalMl, startWeightKg, goalWeightKg, at(nowDay, 12), zone)

    private fun List<Progress>.of(badge: Badge) = first { it.badge == badge }

    private fun weekly(count: Int, site: (Int) -> InjectionSite = { InjectionSite.ABDOMEN_LEFT }) =
        (0 until count).map { MilestoneInput.Injection(at(it * 7), site(it)) }

    @Test
    fun emptyInputLocksEveryBadge() {
        val result = Milestones.evaluate(input())
        assertEquals(Badge.entries.size, result.size)
        assertEquals(Badge.entries.toList(), result.map { it.badge })
        result.forEach {
            assertFalse(it.badge.name, it.achieved)
            assertNull(it.achievedAt)
            assertEquals(it.badge.name, 0f, it.fraction, 0f)
        }
    }

    @Test
    fun doseCountsUseTheNthDoseDate() {
        // Given in reverse order on purpose: evaluate must sort.
        val doses = weekly(5).reversed()
        val r = Milestones.evaluate(input(injections = doses, nowDay = 30))
        assertTrue(r.of(Badge.FIRST_DOSE).achieved)
        assertEquals(at(0), r.of(Badge.FIRST_DOSE).achievedAt)
        assertTrue(r.of(Badge.DOSES_5).achieved)
        assertEquals(at(28), r.of(Badge.DOSES_5).achievedAt)
        assertEquals(1f, r.of(Badge.DOSES_5).fraction, 0f)
        assertFalse(r.of(Badge.DOSES_10).achieved)
        assertEquals(0.5f, r.of(Badge.DOSES_10).fraction, 1e-6f)
        assertEquals(0.2f, r.of(Badge.DOSES_25).fraction, 1e-6f)
    }

    @Test
    fun weeksOnTreatmentCountFromFirstDose() {
        val r = Milestones.evaluate(input(injections = weekly(5), nowDay = 30))
        val w4 = r.of(Badge.WEEKS_4)
        assertTrue(w4.achieved)
        assertEquals(at(28), w4.achievedAt)
        val w12 = r.of(Badge.WEEKS_12)
        assertFalse(w12.achieved)
        // Elapsed = from day 0 09:00 to now (day 30 12:00).
        assertEquals((30 * 24 + 3) / (84f * 24), w12.fraction, 1e-4f)
    }

    @Test
    fun treatmentTimeStopsAfterGraceWhenNoDosesAreLogged() {
        val r = Milestones.evaluate(input(injections = weekly(1), nowDay = 400))
        val w4 = r.of(Badge.WEEKS_4)
        assertFalse(w4.achieved)
        assertEquals(Milestones.TREATMENT_GRACE_DAYS / 28f, w4.fraction, 1e-6f)
        assertFalse(r.of(Badge.WEEKS_52).achieved)
    }

    @Test
    fun percentLostUsesFirstCrossingAndStartSetting() {
        val weights = listOf(at(1) to 99.0, at(10) to 97.9, at(20) to 95.0, at(25) to 96.0)
        val r = Milestones.evaluate(input(weights = weights, startWeightKg = 100.0, nowDay = 26))
        assertTrue(r.of(Badge.FIRST_WEIGH_IN).achieved)
        assertEquals(at(1), r.of(Badge.FIRST_WEIGH_IN).achievedAt)
        assertEquals(at(10), r.of(Badge.LOST_2).achievedAt)
        // Exactly 5 % counts, and the badge stays unlocked after regaining a little.
        assertTrue(r.of(Badge.LOST_5).achieved)
        assertEquals(at(20), r.of(Badge.LOST_5).achievedAt)
        // Locked progress uses the latest weight: 4 kg lost of the 10 kg needed.
        assertFalse(r.of(Badge.LOST_10).achieved)
        assertEquals(0.4f, r.of(Badge.LOST_10).fraction, 1e-6f)
        assertEquals(0.2f, r.of(Badge.LOST_20).fraction, 1e-6f)
    }

    @Test
    fun percentLostFallsBackToFirstWeight() {
        val weights = listOf(at(0) to 80.0, at(7) to 78.0)
        val r = Milestones.evaluate(input(weights = weights, startWeightKg = 0.0, nowDay = 8))
        assertTrue(r.of(Badge.LOST_2).achieved) // 2.5 %
        assertFalse(r.of(Badge.LOST_5).achieved)
        assertEquals(0.5f, r.of(Badge.LOST_5).fraction, 1e-6f)
    }

    @Test
    fun weightGainGivesZeroProgress() {
        val weights = listOf(at(0) to 80.0, at(7) to 82.0)
        val r = Milestones.evaluate(input(weights = weights, nowDay = 8))
        assertEquals(0f, r.of(Badge.LOST_2).fraction, 0f)
    }

    @Test
    fun goalReached() {
        val partial = Milestones.evaluate(
            input(weights = listOf(at(0) to 100.0, at(10) to 95.0), goalWeightKg = 90.0, nowDay = 11),
        ).of(Badge.GOAL_REACHED)
        assertFalse(partial.achieved)
        assertEquals(0.5f, partial.fraction, 1e-6f)

        val done = Milestones.evaluate(
            input(weights = listOf(at(0) to 100.0, at(10) to 89.9, at(12) to 90.4), goalWeightKg = 90.0, nowDay = 13),
        ).of(Badge.GOAL_REACHED)
        assertTrue(done.achieved)
        assertEquals(at(10), done.achievedAt)

        val noGoal = Milestones.evaluate(input(weights = listOf(at(0) to 100.0), nowDay = 1)).of(Badge.GOAL_REACHED)
        assertFalse(noGoal.achieved)
        assertEquals(0f, noGoal.fraction, 0f)
    }

    @Test
    fun journalStreakNeedsSevenConsecutiveDays() {
        val days = (0..2).map { day0.plusDays(it.toLong()) } + (4..10).map { day0.plusDays(it.toLong()) }
        val p = Milestones.evaluate(input(journalDays = days.toSet(), nowDay = 20)).of(Badge.JOURNAL_STREAK_7)
        assertTrue(p.achieved)
        assertEquals(day0.plusDays(10).atStartOfDay(zone).toInstant().toEpochMilli(), p.achievedAt)
    }

    @Test
    fun lockedStreakShowsCurrentRunIncludingYesterday() {
        val days = (6..9).map { day0.plusDays(it.toLong()) }.toSet()
        // Today (day 10) not logged yet: the streak ending yesterday is still alive.
        val alive = Milestones.evaluate(input(journalDays = days, nowDay = 10)).of(Badge.JOURNAL_STREAK_7)
        assertFalse(alive.achieved)
        assertEquals(4f / 7, alive.fraction, 1e-6f)
        // Two days later it is broken.
        val broken = Milestones.evaluate(input(journalDays = days, nowDay = 11)).of(Badge.JOURNAL_STREAK_7)
        assertEquals(0f, broken.fraction, 0f)
    }

    @Test
    fun waterStreakCountsOnlyDaysMeetingTheGoal() {
        val water = (0 until 30).associate { day0.plusDays(it.toLong()) to 2000 }
        val met = Milestones.evaluate(input(waterByDay = water, nowDay = 29)).of(Badge.WATER_STREAK_30)
        assertTrue(met.achieved)
        assertEquals(day0.plusDays(29).atStartOfDay(zone).toInstant().toEpochMilli(), met.achievedAt)

        val missed = water + (day0.plusDays(14) to 1500)
        val p = Milestones.evaluate(input(waterByDay = missed, nowDay = 29)).of(Badge.WATER_STREAK_30)
        assertFalse(p.achieved)
        assertEquals(15f / 30, p.fraction, 1e-6f)

        val noGoal = Milestones.evaluate(input(waterByDay = water, waterGoalMl = 0, nowDay = 29)).of(Badge.WATER_STREAK_30)
        assertFalse(noGoal.achieved)
    }

    @Test
    fun siteRotationNeedsSixDifferentSitesInARow() {
        val sites = InjectionSite.injectable
        val rotated = weekly(6) { sites[it] }
        val p = Milestones.evaluate(input(injections = rotated, nowDay = 40)).of(Badge.SITE_ROTATION)
        assertTrue(p.achieved)
        assertEquals(at(35), p.achievedAt)

        // A repeat in the window: latest run without repeat is the last 3 injections.
        val repeated = weekly(6) { listOf(0, 1, 2, 3, 2, 4).map(sites::get)[it] }
        val r = Milestones.evaluate(input(injections = repeated, nowDay = 40)).of(Badge.SITE_ROTATION)
        assertFalse(r.achieved)
        assertEquals(3f / 6, r.fraction, 1e-6f)

        // Tablets (no site) never unlock it.
        val oral = weekly(8) { InjectionSite.NONE }
        val o = Milestones.evaluate(input(injections = oral, nowDay = 60)).of(Badge.SITE_ROTATION)
        assertFalse(o.achieved)
        assertEquals(0f, o.fraction, 0f)
    }

    @Test
    fun newlyAchievedAndEncoding() {
        val before = Milestones.evaluate(input(injections = weekly(4), nowDay = 22))
        val after = Milestones.evaluate(input(injections = weekly(5), nowDay = 29))
        val beforeSet = Milestones.achievedSet(before)
        assertEquals(setOf(Badge.FIRST_DOSE), beforeSet)
        assertEquals(listOf(Badge.WEEKS_4, Badge.DOSES_5), Milestones.newlyAchieved(beforeSet, after))
        assertTrue(Milestones.newlyAchieved(Milestones.achievedSet(after), after).isEmpty())

        val set = setOf(Badge.DOSES_5, Badge.FIRST_DOSE, Badge.SITE_ROTATION)
        assertEquals("FIRST_DOSE,DOSES_5,SITE_ROTATION", Milestones.encode(set))
        assertEquals(set, Milestones.decode(Milestones.encode(set)))
        assertEquals(setOf(Badge.LOST_5), Milestones.decode(" LOST_5 ,UNKNOWN,"))
        assertTrue(Milestones.decode(null).isEmpty())
        assertTrue(Milestones.decode("").isEmpty())
    }

    // --- GoalForecast ---

    /** 100 kg on day 0, losing 0.1 kg/day, one weigh-in every 3 days up to [lastDay]. */
    private fun linear(lastDay: Int, startDay: Int = 0) =
        (startDay..lastDay step 3).map { at(it, 0) to 100.0 - 0.1 * it }

    @Test
    fun forecastFollowsTheTrendLine() {
        val weights = linear(21)
        val now = at(21, 0)
        // 90 kg is reached on day 100.
        val predicted = GoalForecast.predict(weights, 90.0, now)
        assertNotNull(predicted)
        assertEquals(at(100, 0).toDouble(), predicted!!.toDouble(), 60_000.0)
        assertEquals(-0.7, GoalForecast.weeklyRateKg(weights, now)!!, 1e-9)
    }

    @Test
    fun forecastOnlyUsesTheLastFourWeeks() {
        // Steep loss long ago, then a gentle loss of 0.05 kg/day over the last 4 weeks.
        val old = (0..30 step 3).map { at(it, 0) to 120.0 - 0.5 * it }
        val recent = (60..84 step 4).map { at(it, 0) to 100.0 - 0.05 * (it - 60) }
        val now = at(84, 0)
        val rate = GoalForecast.weeklyRateKg(old + recent, now)!!
        assertEquals(-0.35, rate, 1e-9)
        // 98.8 kg at day 84, 95 kg needs 76 more days.
        val predicted = GoalForecast.predict(recent + old, 95.0, now)!!
        assertEquals(at(160, 0).toDouble(), predicted.toDouble(), 60_000.0)
    }

    @Test
    fun forecastNeedsEnoughDataAndALoss() {
        val now = at(21, 0)
        assertNull(GoalForecast.predict(emptyList(), 90.0, now))
        // Only two points.
        assertNull(GoalForecast.predict(listOf(at(0, 0) to 100.0, at(21, 0) to 98.0), 90.0, now))
        assertNull(GoalForecast.weeklyRateKg(listOf(at(0, 0) to 100.0, at(21, 0) to 98.0), now))
        // Three points but only 6 days apart.
        val short = listOf(at(15, 0) to 100.0, at(18, 0) to 99.0, at(21, 0) to 98.0)
        assertNull(GoalForecast.predict(short, 90.0, now))
        assertNull(GoalForecast.weeklyRateKg(short, now))
        // Gaining weight: rate is known but there is no forecast.
        val gaining = (0..21 step 3).map { at(it, 0) to 90.0 + 0.1 * it }
        assertNull(GoalForecast.predict(gaining, 85.0, now))
        assertEquals(0.7, GoalForecast.weeklyRateKg(gaining, now)!!, 1e-9)
        // No goal, or goal already reached.
        assertNull(GoalForecast.predict(linear(21), 0.0, now))
        assertNull(GoalForecast.predict(linear(21), 98.0, now))
    }

    @Test
    fun forecastTooFarAwayIsDropped() {
        val slow = (0..21 step 3).map { at(it, 0) to 100.0 - 0.001 * it }
        assertNull(GoalForecast.predict(slow, 80.0, at(21, 0)))
    }

    @Test
    fun forecastIsNeverInThePast() {
        // Trend line already below the goal, but the latest weigh-in is just above it.
        val weights = listOf(at(0, 0) to 95.0, at(7, 0) to 89.0, at(14, 0) to 90.2)
        val now = at(14, 0)
        assertEquals(now, GoalForecast.predict(weights, 90.0, now))
    }
}

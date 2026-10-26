package com.rickdnamps.glp1tracker.domain

import androidx.annotation.StringRes
import com.rickdnamps.glp1tracker.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Motivation badges. Declaration order is the canonical display order. */
enum class Badge(@StringRes val title: Int, @StringRes val description: Int, val emoji: String) {
    FIRST_DOSE(R.string.ms_badge_first_dose, R.string.ms_badge_first_dose_desc, "💉"), // syringe
    WEEKS_4(R.string.ms_badge_weeks_4, R.string.ms_badge_weeks_4_desc, "🌱"), // seedling
    WEEKS_12(R.string.ms_badge_weeks_12, R.string.ms_badge_weeks_12_desc, "🌿"), // herb
    WEEKS_26(R.string.ms_badge_weeks_26, R.string.ms_badge_weeks_26_desc, "🌳"), // tree
    WEEKS_52(R.string.ms_badge_weeks_52, R.string.ms_badge_weeks_52_desc, "🎂"), // birthday cake
    DOSES_5(R.string.ms_badge_doses_5, R.string.ms_badge_doses_5_desc, "⭐"), // star
    DOSES_10(R.string.ms_badge_doses_10, R.string.ms_badge_doses_10_desc, "🌟"), // glowing star
    DOSES_25(R.string.ms_badge_doses_25, R.string.ms_badge_doses_25_desc, "🏅"), // medal
    FIRST_WEIGH_IN(R.string.ms_badge_first_weigh_in, R.string.ms_badge_first_weigh_in_desc, "⚖️"), // scales
    LOST_2(R.string.ms_badge_lost_2, R.string.ms_badge_lost_2_desc, "📉"), // chart decreasing
    LOST_5(R.string.ms_badge_lost_5, R.string.ms_badge_lost_5_desc, "🔥"), // fire
    LOST_10(R.string.ms_badge_lost_10, R.string.ms_badge_lost_10_desc, "💪"), // flexed biceps
    LOST_15(R.string.ms_badge_lost_15, R.string.ms_badge_lost_15_desc, "🚀"), // rocket
    LOST_20(R.string.ms_badge_lost_20, R.string.ms_badge_lost_20_desc, "🦋"), // butterfly
    GOAL_REACHED(R.string.ms_badge_goal, R.string.ms_badge_goal_desc, "🏆"), // trophy
    JOURNAL_STREAK_7(R.string.ms_badge_journal_7, R.string.ms_badge_journal_7_desc, "📔"), // notebook
    WATER_STREAK_30(R.string.ms_badge_water_30, R.string.ms_badge_water_30_desc, "💧"), // droplet
    SITE_ROTATION(R.string.ms_badge_rotation, R.string.ms_badge_rotation_desc, "🎯"), // direct hit
}

/** State of one badge: [fraction] is 0..1 (1 when achieved); [achievedAt] is epoch millis. */
data class Progress(val badge: Badge, val achieved: Boolean, val achievedAt: Long?, val fraction: Float)

/** Everything [Milestones.evaluate] needs, without Room entities. Weights are in kg. */
data class MilestoneInput(
    /** Logged injections, any order. */
    val injections: List<Injection> = emptyList(),
    /** (epoch millis, kg), any order. */
    val weights: List<Pair<Long, Double>> = emptyList(),
    /** Days with at least one journal entry (daily log, side effect…). */
    val journalDays: Set<LocalDate> = emptySet(),
    /** Water drunk per day, in ml. */
    val waterByDay: Map<LocalDate, Int> = emptyMap(),
    /** Daily water goal in ml; 0 disables the water badge. */
    val waterGoalMl: Int = 0,
    /** Starting weight; null or <= 0 means "use the first weight entry". */
    val startWeightKg: Double? = null,
    /** Goal weight; null or <= 0 means "no goal". */
    val goalWeightKg: Double? = null,
    val nowMillis: Long = System.currentTimeMillis(),
    val zone: ZoneId = ZoneId.systemDefault(),
) {
    data class Injection(val timestamp: Long, val site: InjectionSite = InjectionSite.NONE)
}

object Milestones {
    private const val DAY_MS = 86_400_000L
    private const val EPS = 1e-6

    /**
     * Treatment time only keeps counting while doses are being logged: it stops this long
     * after the last logged dose, so one old dose does not unlock "1 year".
     */
    const val TREATMENT_GRACE_DAYS = 21L
    const val JOURNAL_STREAK_DAYS = 7
    const val WATER_STREAK_DAYS = 30
    val ROTATION_SITES = InjectionSite.injectable.size

    fun evaluate(input: MilestoneInput): List<Progress> {
        val doses = input.injections.sortedBy { it.timestamp }
        val weights = input.weights.sortedBy { it.first }
        val start = input.startWeightKg?.takeIf { it > 0 } ?: weights.firstOrNull()?.second
        val current = weights.lastOrNull()?.second
        val goal = input.goalWeightKg?.takeIf { it > 0 }
        val today = Instant.ofEpochMilli(input.nowMillis).atZone(input.zone).toLocalDate()
        val waterDays = if (input.waterGoalMl > 0) {
            input.waterByDay.filterValues { it >= input.waterGoalMl }.keys
        } else emptySet()

        return Badge.entries.map { badge ->
            when (badge) {
                Badge.FIRST_DOSE -> doseCount(badge, doses, 1)
                Badge.DOSES_5 -> doseCount(badge, doses, 5)
                Badge.DOSES_10 -> doseCount(badge, doses, 10)
                Badge.DOSES_25 -> doseCount(badge, doses, 25)
                Badge.WEEKS_4 -> weeksOnTreatment(badge, doses, 4, input.nowMillis)
                Badge.WEEKS_12 -> weeksOnTreatment(badge, doses, 12, input.nowMillis)
                Badge.WEEKS_26 -> weeksOnTreatment(badge, doses, 26, input.nowMillis)
                Badge.WEEKS_52 -> weeksOnTreatment(badge, doses, 52, input.nowMillis)
                Badge.FIRST_WEIGH_IN -> weights.firstOrNull()
                    ?.let { Progress(badge, true, it.first, 1f) } ?: locked(badge, 0f)
                Badge.LOST_2 -> percentLost(badge, weights, start, current, 2.0)
                Badge.LOST_5 -> percentLost(badge, weights, start, current, 5.0)
                Badge.LOST_10 -> percentLost(badge, weights, start, current, 10.0)
                Badge.LOST_15 -> percentLost(badge, weights, start, current, 15.0)
                Badge.LOST_20 -> percentLost(badge, weights, start, current, 20.0)
                Badge.GOAL_REACHED -> goalReached(badge, weights, start, current, goal)
                Badge.JOURNAL_STREAK_7 -> streak(badge, input.journalDays, JOURNAL_STREAK_DAYS, today, input.zone)
                Badge.WATER_STREAK_30 -> streak(badge, waterDays, WATER_STREAK_DAYS, today, input.zone)
                Badge.SITE_ROTATION -> siteRotation(badge, doses)
            }
        }
    }

    /** Badges achieved in [after] that were not in [before], in display order. */
    fun newlyAchieved(before: Set<Badge>, after: List<Progress>): List<Badge> =
        after.filter { it.achieved && it.badge !in before }.map { it.badge }.sortedBy { it.ordinal }

    fun achievedSet(progress: List<Progress>): Set<Badge> =
        progress.filter { it.achieved }.mapTo(mutableSetOf()) { it.badge }

    /** Compact form for a DataStore string preference, e.g. "FIRST_DOSE,DOSES_5". */
    fun encode(badges: Set<Badge>): String = badges.sortedBy { it.ordinal }.joinToString(",") { it.name }

    fun decode(text: String?): Set<Badge> =
        text.orEmpty().split(',').mapNotNullTo(mutableSetOf()) { name -> Badge.entries.firstOrNull { it.name == name.trim() } }

    private fun locked(badge: Badge, fraction: Float) =
        Progress(badge, false, null, fraction.coerceIn(0f, 1f))

    private fun ratio(value: Double, target: Double): Float =
        if (target <= 0) 0f else (value / target).toFloat().coerceIn(0f, 1f)

    private fun doseCount(badge: Badge, doses: List<MilestoneInput.Injection>, n: Int): Progress =
        if (doses.size >= n) Progress(badge, true, doses[n - 1].timestamp, 1f)
        else locked(badge, ratio(doses.size.toDouble(), n.toDouble()))

    private fun weeksOnTreatment(badge: Badge, doses: List<MilestoneInput.Injection>, weeks: Int, now: Long): Progress {
        if (doses.isEmpty()) return locked(badge, 0f)
        val first = doses.first().timestamp
        val target = weeks * 7L * DAY_MS
        val end = minOf(now, doses.last().timestamp + TREATMENT_GRACE_DAYS * DAY_MS)
        val elapsed = (end - first).coerceAtLeast(0)
        return if (elapsed >= target) Progress(badge, true, first + target, 1f)
        else locked(badge, ratio(elapsed.toDouble(), target.toDouble()))
    }

    private fun percentLost(
        badge: Badge,
        weights: List<Pair<Long, Double>>,
        start: Double?,
        current: Double?,
        percent: Double,
    ): Progress {
        if (start == null || start <= 0) return locked(badge, 0f)
        val needed = start * percent / 100
        val hit = weights.firstOrNull { start - it.second >= needed - EPS }
        return if (hit != null) Progress(badge, true, hit.first, 1f)
        else locked(badge, current?.let { ratio(start - it, needed) } ?: 0f)
    }

    private fun goalReached(
        badge: Badge,
        weights: List<Pair<Long, Double>>,
        start: Double?,
        current: Double?,
        goal: Double?,
    ): Progress {
        if (goal == null || start == null) return locked(badge, 0f)
        val losing = goal < start
        val hit = weights.firstOrNull { if (losing) it.second <= goal + EPS else it.second >= goal - EPS }
        if (hit != null) return Progress(badge, true, hit.first, 1f)
        val fraction = when {
            current == null -> 0f
            losing -> ratio(start - current, start - goal)
            else -> ratio(current - start, goal - start)
        }
        return locked(badge, fraction)
    }

    /**
     * Unlocked by the first run of [target] consecutive days (achievedAt = start of its last day).
     * While locked, the fraction is the current streak, which stays alive until the end of today.
     */
    private fun streak(badge: Badge, days: Set<LocalDate>, target: Int, today: LocalDate, zone: ZoneId): Progress {
        var run = 0
        var previous: LocalDate? = null
        for (day in days.sorted()) {
            run = if (previous != null && day == previous.plusDays(1)) run + 1 else 1
            previous = day
            if (run >= target) {
                return Progress(badge, true, day.atStartOfDay(zone).toInstant().toEpochMilli(), 1f)
            }
        }
        var cursor = if (today in days) today else today.minusDays(1)
        var current = 0
        while (cursor in days) {
            current++
            cursor = cursor.minusDays(1)
        }
        return locked(badge, ratio(current.toDouble(), target.toDouble()))
    }

    /** Unlocked when [ROTATION_SITES] consecutive injections used as many different sites. */
    private fun siteRotation(badge: Badge, doses: List<MilestoneInput.Injection>): Progress {
        val n = ROTATION_SITES
        for (end in n - 1 until doses.size) {
            val window = doses.subList(end - n + 1, end + 1)
            if (window.none { it.site == InjectionSite.NONE } && window.mapTo(mutableSetOf()) { it.site }.size == n) {
                return Progress(badge, true, doses[end].timestamp, 1f)
            }
        }
        // Progress: distinct sites in the latest run without a repeat.
        val seen = mutableSetOf<InjectionSite>()
        for (i in doses.indices.reversed()) {
            val site = doses[i].site
            if (site == InjectionSite.NONE || !seen.add(site)) break
        }
        return locked(badge, ratio(seen.size.toDouble(), n.toDouble()))
    }
}

/** Goal-date forecast from the recent weight trend (least-squares line). Weights are (epoch millis, kg). */
object GoalForecast {
    private const val DAY_MS = 86_400_000.0
    const val WINDOW_DAYS = 28
    const val MIN_POINTS = 3
    const val MIN_SPAN_DAYS = 7.0
    /** Forecasts further away than this are not shown (a nearly flat trend gives absurd dates). */
    const val MAX_HORIZON_DAYS = 3 * 365

    private class Fit(val slopePerDay: Double, val interceptKg: Double, val originMillis: Long)

    private fun fit(weights: List<Pair<Long, Double>>, nowMillis: Long): Fit? {
        val from = nowMillis - WINDOW_DAYS * 86_400_000L
        val recent = weights.filter { it.first in from..nowMillis }
        if (recent.size < MIN_POINTS) return null
        val origin = recent.minOf { it.first }
        val span = (recent.maxOf { it.first } - origin) / DAY_MS
        if (span < MIN_SPAN_DAYS) return null
        val xs = recent.map { (it.first - origin) / DAY_MS }
        val ys = recent.map { it.second }
        val meanX = xs.average()
        val meanY = ys.average()
        var sxy = 0.0
        var sxx = 0.0
        for (i in xs.indices) {
            val dx = xs[i] - meanX
            sxy += dx * (ys[i] - meanY)
            sxx += dx * dx
        }
        if (sxx == 0.0) return null
        val slope = sxy / sxx
        return Fit(slope, meanY - slope * meanX, origin)
    }

    /**
     * Epoch millis when the last [WINDOW_DAYS]-day trend line reaches [goalKg], or null without a
     * clear downward trend (needs >= [MIN_POINTS] weights spanning >= [MIN_SPAN_DAYS] days), when the
     * latest weight is already at or below the goal, or when the date is beyond [MAX_HORIZON_DAYS].
     * Never earlier than [nowMillis].
     */
    fun predict(weights: List<Pair<Long, Double>>, goalKg: Double, nowMillis: Long): Long? {
        if (goalKg <= 0) return null
        val latest = weights.filter { it.first <= nowMillis }.maxByOrNull { it.first } ?: return null
        if (latest.second <= goalKg) return null
        val f = fit(weights, nowMillis) ?: return null
        if (f.slopePerDay >= 0) return null
        val days = (goalKg - f.interceptKg) / f.slopePerDay
        val millis = f.originMillis + (days * DAY_MS).toLong()
        if (millis - nowMillis > MAX_HORIZON_DAYS * 86_400_000L) return null
        return maxOf(millis, nowMillis)
    }

    /** Trend in kg per week over the last [WINDOW_DAYS] days (negative = losing), or null without enough data. */
    fun weeklyRateKg(weights: List<Pair<Long, Double>>, nowMillis: Long): Double? =
        fit(weights, nowMillis)?.slopePerDay?.times(7)
}

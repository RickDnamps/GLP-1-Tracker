package com.rickdnamps.glp1tracker.domain

import kotlin.math.abs

/** "Therapy dynamics" over a period: how many doses, at what dose, how regularly, how comfortable. */
object TherapyStats {

    data class Entry(val timeMillis: Long, val doseMg: Double, val comfort: Int = 0)

    data class Summary(
        val doses: Int,
        /** Doses in the same-length period just before, to show a trend. */
        val previousDoses: Int,
        val averageDoseMg: Double?,
        val averageIntervalDays: Double?,
        /** Share of intervals within the tolerance of the planned interval (0..1). */
        val onTimeShare: Double?,
        /** Average comfort (1..5) of the rated doses. */
        val averageComfort: Double?,
    )

    private const val DAY_MS = 86_400_000.0

    /** Intervals count as on time within ±15 % of the plan (±1 day weekly, ±6 h daily). */
    fun toleranceDays(plannedDays: Int): Double = maxOf(0.25, plannedDays * 0.15)

    fun summarize(entries: List<Entry>, fromMillis: Long, toMillis: Long, plannedIntervalDays: Int): Summary {
        val sorted = entries.sortedBy { it.timeMillis }
        val inPeriod = sorted.filter { it.timeMillis in fromMillis..toMillis }
        val length = toMillis - fromMillis
        val previous = sorted.count { it.timeMillis in (fromMillis - length) until fromMillis }
        // Intervals ending inside the period (the first one may start just before it).
        val intervals = sorted.zipWithNext()
            .filter { (_, b) -> b.timeMillis in fromMillis..toMillis }
            .map { (a, b) -> (b.timeMillis - a.timeMillis) / DAY_MS }
        val tolerance = toleranceDays(plannedIntervalDays)
        val rated = inPeriod.filter { it.comfort in 1..5 }
        return Summary(
            doses = inPeriod.size,
            previousDoses = previous,
            averageDoseMg = inPeriod.takeIf { it.isNotEmpty() }?.map { it.doseMg }?.average(),
            averageIntervalDays = intervals.takeIf { it.isNotEmpty() }?.average(),
            onTimeShare = intervals.takeIf { it.isNotEmpty() }?.let { list ->
                list.count { abs(it - plannedIntervalDays) <= tolerance }.toDouble() / list.size
            },
            averageComfort = rated.takeIf { it.isNotEmpty() }?.map { it.comfort.toDouble() }?.average(),
        )
    }

    /**
     * Time of the highest estimated level between [fromMillis] and [toMillis] (e.g. from the last
     * dose to the next one), sampled every 30 minutes. Null without doses.
     */
    fun peakTime(doses: List<Pharmacokinetics.Dose>, fromMillis: Long, toMillis: Long): Long? {
        if (doses.isEmpty() || toMillis <= fromMillis) return null
        val points = ((toMillis - fromMillis) / 1_800_000L).toInt().coerceIn(2, 2_000)
        return Pharmacokinetics.curve(doses, fromMillis, toMillis, points).maxByOrNull { it.second }?.first
    }

    /** Average comfort per site (only sites with at least one rated injection). */
    fun <S> comfortBySite(rated: List<Pair<S, Int>>): Map<S, Double> =
        rated.filter { it.second in 1..5 }.groupBy({ it.first }, { it.second.toDouble() }).mapValues { it.value.average() }
}

/** Phases of an injection cycle, used for the tip of the day (general information, not advice). */
enum class CyclePhase { INJECTION_DAY, EARLY, PEAK, LATE }

object CycleTips {
    /**
     * Phase for [daysSinceDose] in a cycle of [intervalDays] (weekly pens mostly): injection day,
     * the next 1–2 days (side effects most common), the middle (strongest effect), the end
     * (appetite often returns). Null for daily or unknown schedules.
     */
    fun phase(daysSinceDose: Int?, intervalDays: Int): CyclePhase? {
        if (daysSinceDose == null || daysSinceDose < 0 || intervalDays < 5) return null
        return when {
            daysSinceDose == 0 -> CyclePhase.INJECTION_DAY
            daysSinceDose <= 2 -> CyclePhase.EARLY
            daysSinceDose < intervalDays - 2 -> CyclePhase.PEAK
            else -> CyclePhase.LATE
        }
    }
}

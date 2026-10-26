package com.rickdnamps.glp1tracker.report

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Pure computations behind the doctor report. No Android imports so it stays unit-testable.
 * All times are epoch millis.
 */
object ReportMath {

    const val DAY_MS = 86_400_000L

    /**
     * A dose is not counted as missed until this long after its due time, so a report made on
     * the morning of the injection day does not show the dose of the day as missing.
     */
    const val GRACE_MS = DAY_MS

    data class Adherence(val taken: Int, val expected: Int) {
        /** Doses taken / expected, in percent, capped at 100. */
        val percent: Int get() = if (expected <= 0) 0 else minOf(100, (taken * 100.0 / expected).roundToInt())
    }

    /**
     * Number of doses expected in [fromMillis, toMillis] for one dose every [intervalDays].
     *
     * If the treatment started inside the period ([treatmentStartMillis] at or after [fromMillis]),
     * the count starts at that first dose, which is itself expected. Otherwise it is the number
     * of whole intervals in the period. Without any dose ever logged, nothing is expected.
     */
    fun expectedDoses(fromMillis: Long, toMillis: Long, intervalDays: Int, treatmentStartMillis: Long?): Int {
        if (treatmentStartMillis == null || treatmentStartMillis > toMillis) return 0
        val interval = intervalDays.coerceAtLeast(1) * DAY_MS
        val end = toMillis - GRACE_MS
        return if (treatmentStartMillis >= fromMillis) {
            ((end - treatmentStartMillis).coerceAtLeast(0) / interval + 1).toInt()
        } else {
            ((end - fromMillis).coerceAtLeast(0) / interval).toInt()
        }
    }

    /**
     * Adherence over the period, or null when no dose is expected yet (period shorter than one
     * interval, or no dose ever logged). [doseTimes] are all logged doses, any period.
     */
    fun adherence(doseTimes: List<Long>, fromMillis: Long, toMillis: Long, intervalDays: Int): Adherence? {
        val expected = expectedDoses(fromMillis, toMillis, intervalDays, doseTimes.minOrNull())
        if (expected <= 0) return null
        val taken = doseTimes.count { it in fromMillis..toMillis }
        return Adherence(taken, expected)
    }

    data class DoseRecord(val timeMillis: Long, val medication: String, val doseMg: Double)

    /** A run of consecutive doses with the same medication and dose. */
    data class DoseStep(val medication: String, val doseMg: Double, val startMillis: Long, val count: Int)

    /** Groups doses (any order) into chronological steps; a new step starts at each change. */
    fun doseSteps(doses: List<DoseRecord>): List<DoseStep> {
        val steps = mutableListOf<DoseStep>()
        for (d in doses.sortedBy { it.timeMillis }) {
            val last = steps.lastOrNull()
            if (last != null && last.medication == d.medication && sameDose(last.doseMg, d.doseMg)) {
                steps[steps.lastIndex] = last.copy(count = last.count + 1)
            } else {
                steps += DoseStep(d.medication, d.doseMg, d.timeMillis, 1)
            }
        }
        return steps
    }

    /** True for an obvious titration: one medication whose dose only goes up, step after step. */
    fun isTitration(steps: List<DoseStep>): Boolean =
        steps.size >= 2 && steps.zipWithNext().all { (a, b) -> a.medication == b.medication && b.doseMg > a.doseMg }

    private fun sameDose(a: Double, b: Double) = abs(a - b) < 1e-6

    data class SymptomStat(val type: String, val count: Int, val averageSeverity: Double, val maxSeverity: Int)

    /**
     * Frequency and severity (1 to 3) per symptom, most frequent first.
     * [entries] are (symptom type, severity) pairs.
     */
    fun symptomStats(entries: List<Pair<String, Int>>): List<SymptomStat> =
        entries.groupBy({ it.first }, { it.second.coerceIn(1, 3) })
            .map { (type, levels) -> SymptomStat(type, levels.size, levels.average(), levels.max()) }
            .sortedWith(compareByDescending<SymptomStat> { it.count }.thenByDescending { it.averageSeverity })

    /** Average of the values that were set (0 means "not entered"), or null if none was. */
    fun averageOfSet(values: List<Int>): Double? =
        values.filter { it > 0 }.takeIf { it.isNotEmpty() }?.average()

    /** Number of values that were set (greater than 0). */
    fun countSet(values: List<Int>): Int = values.count { it > 0 }

    /** Weight change in percent of [startKg] (negative when weight was lost). */
    fun changePercent(startKg: Double, currentKg: Double): Double? =
        if (startKg > 0) (currentKg - startKg) / startKg * 100 else null

    /**
     * "Nice" axis ticks (steps of 1, 2, 2.5 or 5 × 10^n) covering [min]..[max],
     * roughly [targetCount] of them. Always returns at least two ticks.
     */
    fun niceTicks(min: Double, max: Double, targetCount: Int = 5): List<Double> {
        var lo = minOf(min, max)
        var hi = maxOf(min, max)
        if (hi - lo < 1e-9) {
            lo -= 1.0
            hi += 1.0
        }
        val rawStep = (hi - lo) / (targetCount - 1).coerceAtLeast(1)
        val magnitude = 10.0.pow(floor(log10(rawStep)))
        val step = listOf(1.0, 2.0, 2.5, 5.0, 10.0).map { it * magnitude }.first { it >= rawStep * (1 - 1e-9) }
        val first = floor(lo / step) * step
        val last = ceil(hi / step) * step
        val count = ((last - first) / step).roundToInt().coerceAtLeast(1)
        return (0..count).map { first + it * step }
    }

    /** [count] evenly spaced instants from [fromMillis] to [toMillis], both included. */
    fun timeTicks(fromMillis: Long, toMillis: Long, count: Int): List<Long> {
        if (count < 2 || toMillis <= fromMillis) return listOf(fromMillis)
        val span = (toMillis - fromMillis).toDouble()
        return (0 until count).map { fromMillis + (span * it / (count - 1)).toLong() }
    }
}

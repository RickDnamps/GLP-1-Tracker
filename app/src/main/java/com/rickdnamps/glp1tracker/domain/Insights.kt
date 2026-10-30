package com.rickdnamps.glp1tracker.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs

/**
 * Personal insights computed on the device from the user's own logs. Plain statistics,
 * explained to the user; nothing here is medical advice.
 */
object Insights {

    private const val DAY_MS = 86_400_000L

    /**
     * Day of the injection cycle (0 = injection day) for [timestamp], based on the most recent
     * dose at or before it; null when no earlier dose exists or the gap exceeds [maxDays].
     */
    fun cycleDay(timestamp: Long, doseTimesAsc: List<Long>, maxDays: Int): Int? {
        val idx = doseTimesAsc.binarySearch(timestamp).let { if (it >= 0) it else -it - 2 }
        if (idx < 0) return null
        val zone = ZoneId.systemDefault()
        val doseDay = Instant.ofEpochMilli(doseTimesAsc[idx]).atZone(zone).toLocalDate()
        val day = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()
        val d = (day.toEpochDay() - doseDay.toEpochDay()).toInt()
        return d.takeIf { it in 0 until maxDays }
    }

    data class CycleProfile(
        /** Events per cycle day (index = day after injection). */
        val counts: IntArray,
        val total: Int,
        /** Day with the most events, null when there is not enough data. */
        val peakDay: Int?,
        /** Share of all events that happen on [peakDay] (0..1). */
        val peakShare: Double,
    )

    /** Distribution of events (e.g. nausea episodes) across the days after each injection. */
    fun cycleProfile(eventTimes: List<Long>, doseTimesAsc: List<Long>, intervalDays: Int, minEvents: Int = 3): CycleProfile {
        val days = intervalDays.coerceIn(1, 14)
        val counts = IntArray(days)
        eventTimes.forEach { t -> cycleDay(t, doseTimesAsc, days)?.let { counts[it]++ } }
        val total = counts.sum()
        val peak = counts.indices.maxByOrNull { counts[it] }
        return if (total >= minEvents && peak != null) {
            CycleProfile(counts, total, peak, counts[peak].toDouble() / total)
        } else {
            CycleProfile(counts, total, null, 0.0)
        }
    }

    /** Average of a 1..5 score per cycle day (0 scores are ignored); NaN where no data. */
    fun scoreByCycleDay(scores: List<Pair<LocalDate, Int>>, doseTimesAsc: List<Long>, intervalDays: Int): DoubleArray {
        val days = intervalDays.coerceIn(1, 14)
        val sum = DoubleArray(days)
        val n = IntArray(days)
        val zone = ZoneId.systemDefault()
        scores.filter { it.second > 0 }.forEach { (date, score) ->
            val t = date.atTime(20, 0).atZone(zone).toInstant().toEpochMilli()
            cycleDay(t, doseTimesAsc, days)?.let { sum[it] += score; n[it]++ }
        }
        return DoubleArray(days) { if (n[it] == 0) Double.NaN else sum[it] / n[it] }
    }

    /** First cycle day where the average score climbs to [threshold] or more after day 0. */
    fun firstDayAtLeast(byDay: DoubleArray, threshold: Double): Int? =
        (1 until byDay.size).firstOrNull { !byDay[it].isNaN() && byDay[it] >= threshold }

    /** Exponentially smoothed weight ("trend weight"), filtering water and scale noise. */
    fun trend(weights: List<Pair<Long, Double>>, alpha: Double = 0.1): List<Pair<Long, Double>> {
        if (weights.isEmpty()) return emptyList()
        val sorted = weights.sortedBy { it.first }
        var t = sorted.first().second
        return sorted.map { (time, w) ->
            t += alpha * (w - t)
            time to t
        }
    }

    /** Least-squares slope in kg per week over the points. */
    fun slopePerWeek(points: List<Pair<Long, Double>>): Double? {
        if (points.size < 2) return null
        val t0 = points.first().first
        val xs = points.map { (it.first - t0) / (7.0 * DAY_MS) }
        val ys = points.map { it.second }
        val mx = xs.average()
        val my = ys.average()
        val den = xs.sumOf { (it - mx) * (it - mx) }
        if (den == 0.0) return null
        return xs.indices.sumOf { (xs[it] - mx) * (ys[it] - my) } / den
    }

    enum class WeightPhase { LOSING, PLATEAU, GAINING, NOT_ENOUGH_DATA }

    data class PlateauResult(val phase: WeightPhase, val trendSlopeKgPerWeek: Double?, val days: Int)

    /**
     * A real plateau: over the last [windowDays] the *trend* weight moved less than 0.15 kg/week,
     * with at least 4 weigh-ins spanning 14+ days.
     */
    fun plateau(weights: List<Pair<Long, Double>>, nowMillis: Long, windowDays: Int = 21): PlateauResult {
        val trend = trend(weights)
        val recent = trend.filter { it.first >= nowMillis - windowDays * DAY_MS }
        val span = if (recent.size >= 2) ((recent.last().first - recent.first().first) / DAY_MS).toInt() else 0
        if (recent.size < 4 || span < 14) return PlateauResult(WeightPhase.NOT_ENOUGH_DATA, null, span)
        val slope = slopePerWeek(recent) ?: return PlateauResult(WeightPhase.NOT_ENOUGH_DATA, null, span)
        val phase = when {
            abs(slope) < 0.15 -> WeightPhase.PLATEAU
            slope < 0 -> WeightPhase.LOSING
            else -> WeightPhase.GAINING
        }
        return PlateauResult(phase, slope, span)
    }

    enum class Pace { SLOW, HEALTHY, FAST, VERY_FAST }

    /**
     * Weekly loss as % of body weight over the last 4 weeks of trend weight. Above ~1.5 %/week
     * the risk of muscle loss, hair shedding and gallstones increases.
     */
    fun pace(weights: List<Pair<Long, Double>>, nowMillis: Long): Pair<Pace, Double>? {
        val trend = trend(weights).filter { it.first >= nowMillis - 28 * DAY_MS }
        if (trend.size < 3 || trend.last().first - trend.first().first < 10 * DAY_MS) return null
        val slope = slopePerWeek(trend) ?: return null
        val percent = -slope / trend.last().second * 100
        val pace = when {
            percent < 0.25 -> Pace.SLOW
            percent <= 1.0 -> Pace.HEALTHY
            percent <= 1.5 -> Pace.FAST
            else -> Pace.VERY_FAST
        }
        return pace to percent
    }

    /**
     * Daily protein range (g) to preserve muscle while losing weight: 1.2–1.6 g per kg of a
     * reference weight (goal weight when known — closer to lean needs — else current weight).
     */
    fun proteinRange(currentKg: Double, goalKg: Double?): IntRange {
        val ref = goalKg?.takeIf { it > 0 && it < currentKg } ?: currentKg
        return (ref * 1.2).toInt()..(ref * 1.6).toInt()
    }

    /** A record low worth celebrating: below every earlier weigh-in (3+ of them) by at least 0.1 kg. */
    fun isNewLow(previousKg: List<Double>, kg: Double): Boolean =
        previousKg.size >= 3 && kg <= previousKg.min() - 0.1

    /** Average of the last 7 days minus the average of the 7 days before (kg); null without both. */
    fun weekOverWeek(weights: List<Pair<Long, Double>>, nowMillis: Long): Double? {
        val last = weights.filter { it.first in (nowMillis - 7 * DAY_MS)..nowMillis }.map { it.second }
        val before = weights.filter { it.first in (nowMillis - 14 * DAY_MS) until (nowMillis - 7 * DAY_MS) }.map { it.second }
        if (last.isEmpty() || before.isEmpty()) return null
        return last.average() - before.average()
    }
}

/** Compounded vials: dose (mg) ↔ volume (mL) ↔ units on a U-100 insulin syringe. */
object Syringe {
    data class Result(val ml: Double, val units: Double, val warnings: Set<Warning>)

    enum class Warning { TOO_SMALL_TO_MEASURE, OVER_SYRINGE, TENFOLD_CHANGE }

    /**
     * @param concentrationMgPerMl vial strength, e.g. 2.5 mg/mL
     * @param doseMg wanted dose
     * @param usualDoseMg last logged dose, used to catch decimal slips (×10 / ÷10)
     */
    fun compute(concentrationMgPerMl: Double, doseMg: Double, syringeUnits: Int = 100, usualDoseMg: Double? = null): Result? {
        if (concentrationMgPerMl <= 0 || doseMg <= 0) return null
        val ml = doseMg / concentrationMgPerMl
        val units = ml * 100
        val warnings = buildSet {
            if (units < 2) add(Warning.TOO_SMALL_TO_MEASURE)
            if (units > syringeUnits) add(Warning.OVER_SYRINGE)
            if (usualDoseMg != null && usualDoseMg > 0 && (doseMg / usualDoseMg >= 4 || usualDoseMg / doseMg >= 4)) {
                add(Warning.TENFOLD_CHANGE)
            }
        }
        return Result(ml, units, warnings)
    }
}

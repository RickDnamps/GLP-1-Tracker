package com.rickdnamps.glp1tracker.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/** Everything [WeeklyRecaps.compute] needs, without Room entities. Weights in kg. */
data class RecapInput(
    /** Logged doses (epoch millis), any order. */
    val doseTimes: List<Long> = emptyList(),
    /** Dosing interval in days (7 = weekly, 1 = daily). */
    val intervalDays: Int = 7,
    /** (epoch millis, kg), any order. */
    val weights: List<Pair<Long, Double>> = emptyList(),
    val waterByDay: Map<LocalDate, Int> = emptyMap(),
    /** Protein per day in g (journal + food entries). */
    val proteinByDay: Map<LocalDate, Int> = emptyMap(),
    /** 0 disables the water goal. */
    val waterGoalMl: Int = 0,
    /** 0 disables the protein goal. */
    val proteinGoalG: Int = 0,
    /** (epoch millis, type), any order. */
    val sideEffects: List<Pair<Long, SideEffectType>> = emptyList(),
    /** Days with at least one journal entry. */
    val journalDays: Set<LocalDate> = emptySet(),
    /** Last day of the recap, inclusive: the recap covers [end] − 6 … [end]. */
    val end: LocalDate,
    val zone: ZoneId = ZoneId.systemDefault(),
)

enum class StreakKind { WATER_GOAL, PROTEIN_GOAL, JOURNAL }

/** Habit streak still alive at the end of the recap (it may have started before the week). */
data class Streak(val kind: StreakKind, val days: Int)

enum class HighlightKind {
    WEIGHT_LOST, WEIGHT_STABLE, WEIGHT_GAINED,
    ALL_DOSES, MISSED_DOSE,
    NO_SIDE_EFFECTS, FEWER_SIDE_EFFECTS, MORE_SIDE_EFFECTS, SIDE_EFFECTS,
    WATER_GOAL, WATER_BELOW_GOAL,
    PROTEIN_GOAL, PROTEIN_BELOW_GOAL,
    STREAK, JOURNAL,
}

/**
 * One line of the recap. Meaning of the numbers per [kind]:
 * - WEIGHT_*: [value] = trend change in kg (negative = loss), [previous] = same for the week before;
 * - ALL_DOSES / MISSED_DOSE: [count] taken of [total] expected;
 * - *_SIDE_EFFECTS: [count] this week, [previous] last week, [sideEffect] the most frequent ([total] times);
 * - WATER_* / PROTEIN_*: [value] = average (ml / g) on logged days, [total] = goal, [count] = days at goal,
 *   [previous] = last week's average;
 * - STREAK: [count] days in a row of [streak];
 * - JOURNAL: [count] days logged this week, [previous] last week.
 */
data class Highlight(
    val kind: HighlightKind,
    /** Higher first. */
    val importance: Int,
    val value: Double = 0.0,
    val previous: Double? = null,
    val count: Int = 0,
    val total: Int = 0,
    val sideEffect: SideEffectType? = null,
    val streak: StreakKind? = null,
)

/** One day of the recap strip. */
data class RecapDay(val date: LocalDate, val journal: Boolean, val dose: Boolean, val waterGoal: Boolean)

data class WeeklyRecap(
    val start: LocalDate,
    val end: LocalDate,
    val dosesTaken: Int,
    val dosesExpected: Int,
    val weightChangeKg: Double?,
    val previousWeightChangeKg: Double?,
    val waterAverageMl: Int?,
    val previousWaterAverageMl: Int?,
    val waterDaysAtGoal: Int,
    val proteinAverageG: Int?,
    val previousProteinAverageG: Int?,
    val proteinDaysAtGoal: Int,
    val sideEffectCount: Int,
    val previousSideEffectCount: Int,
    val topSideEffect: SideEffectType?,
    val topSideEffectCount: Int,
    val journalDays: Int,
    val previousJournalDays: Int,
    val bestStreak: Streak?,
    /** Sorted by importance, most important first. */
    val highlights: List<Highlight>,
    /** The 7 days of the recap, oldest first. */
    val days: List<RecapDay>,
)

/**
 * "Your week": the last 7 days against the 7 before, as plain numbers plus a ranked list of
 * highlights. Never throws on missing data: every section is simply left out.
 */
object WeeklyRecaps {
    private const val DAY_MS = 86_400_000.0

    /** Trend changes smaller than this (kg) read as "stable". */
    const val STABLE_KG = 0.2

    /** Doses older than this before the week mean the treatment is paused: no dose line. */
    const val PAUSED_AFTER_DAYS = 21L

    /** Streaks shorter than this are not worth a line. */
    const val MIN_STREAK = 3

    /** Goal "met for the week" when reached on at least this many days (or on average). */
    const val GOAL_DAYS = 4

    /** Week shown on [today]: on Mondays the full week that just ended (Monday to Sunday), otherwise the last 7 days. */
    fun defaultEnd(today: LocalDate): LocalDate = if (today.dayOfWeek == DayOfWeek.MONDAY) today.minusDays(1) else today

    /** Sundays and Mondays, when the recap deserves the top of the Home screen. */
    fun isRecapDay(today: LocalDate): Boolean = today.dayOfWeek == DayOfWeek.SUNDAY || today.dayOfWeek == DayOfWeek.MONDAY

    fun compute(input: RecapInput): WeeklyRecap {
        val zone = input.zone
        val end = input.end
        val start = end.minusDays(6)
        val prevEnd = start.minusDays(1)
        val prevStart = end.minusDays(13)
        fun day(t: Long): LocalDate = Instant.ofEpochMilli(t).atZone(zone).toLocalDate()
        fun LocalDate.inWeek() = !isBefore(start) && !isAfter(end)
        fun LocalDate.inPrevious() = !isBefore(prevStart) && !isAfter(prevEnd)

        // Doses
        val doseDays = input.doseTimes.map(::day).sorted()
        val taken = doseDays.count { it.inWeek() }
        val expected = expectedDoses(doseDays, input.intervalDays, start, end)

        // Weight (trend, so one noisy weigh-in or a week without the scale does not mislead)
        val trend = Insights.trend(input.weights, trendAlpha(input.weights)).map { day(it.first) to it.second }
        val weightChange = trendChange(trend, start, end)
        val previousWeightChange = trendChange(trend, prevStart, prevEnd)

        // Water and protein
        val water = goalStats(input.waterByDay, input.waterGoalMl, start, end)
        val previousWater = goalStats(input.waterByDay, input.waterGoalMl, prevStart, prevEnd)
        val protein = goalStats(input.proteinByDay, input.proteinGoalG, start, end)
        val previousProtein = goalStats(input.proteinByDay, input.proteinGoalG, prevStart, prevEnd)

        // Side effects
        val effects = input.sideEffects.filter { day(it.first).inWeek() }.map { it.second }
        val previousEffects = input.sideEffects.count { day(it.first).inPrevious() }
        val top = effects.groupingBy { it }.eachCount().entries
            .sortedWith(compareByDescending<Map.Entry<SideEffectType, Int>> { it.value }.thenBy { it.key.ordinal })
            .firstOrNull()

        // Journal and streaks
        val journal = (0L..6L).count { start.plusDays(it) in input.journalDays }
        val previousJournal = (0L..6L).count { prevStart.plusDays(it) in input.journalDays }
        val streak = bestStreak(input, end)

        val highlights = buildList {
            weightChange?.let { d ->
                when {
                    d <= -STABLE_KG -> add(Highlight(HighlightKind.WEIGHT_LOST, 90 + min(9, (abs(d) * 5).toInt()), d, previousWeightChange))
                    d >= STABLE_KG -> add(Highlight(HighlightKind.WEIGHT_GAINED, 50, d, previousWeightChange))
                    else -> add(Highlight(HighlightKind.WEIGHT_STABLE, 55, d, previousWeightChange))
                }
            }
            if (expected > 0) {
                if (taken >= expected) add(Highlight(HighlightKind.ALL_DOSES, 85, count = taken, total = expected))
                else add(Highlight(HighlightKind.MISSED_DOSE, 88, count = taken, total = expected))
            }
            val count = effects.size
            val prev = previousEffects.toDouble()
            when {
                count == 0 && previousEffects > 0 -> add(Highlight(HighlightKind.NO_SIDE_EFFECTS, 80, previous = prev))
                count == 0 -> Unit // nothing logged either week: no news is not news
                count < previousEffects -> add(
                    Highlight(HighlightKind.FEWER_SIDE_EFFECTS, 75, previous = prev, count = count, total = top!!.value, sideEffect = top.key),
                )
                count > previousEffects -> add(
                    Highlight(HighlightKind.MORE_SIDE_EFFECTS, 58, previous = prev, count = count, total = top!!.value, sideEffect = top.key),
                )
                else -> add(Highlight(HighlightKind.SIDE_EFFECTS, 45, previous = prev, count = count, total = top!!.value, sideEffect = top.key))
            }
            goalHighlight(protein, previousProtein, input.proteinGoalG, HighlightKind.PROTEIN_GOAL, 70, HighlightKind.PROTEIN_BELOW_GOAL, 48)
                ?.let(::add)
            goalHighlight(water, previousWater, input.waterGoalMl, HighlightKind.WATER_GOAL, 68, HighlightKind.WATER_BELOW_GOAL, 46)
                ?.let(::add)
            streak?.let { add(Highlight(HighlightKind.STREAK, 60 + min(it.days, 20), count = it.days, streak = it.kind)) }
            if (journal > 0) add(Highlight(HighlightKind.JOURNAL, 35 + journal, count = journal, previous = previousJournal.toDouble()))
        }.sortedByDescending { it.importance }

        return WeeklyRecap(
            start = start,
            end = end,
            dosesTaken = taken,
            dosesExpected = expected,
            weightChangeKg = weightChange,
            previousWeightChangeKg = previousWeightChange,
            waterAverageMl = water?.average,
            previousWaterAverageMl = previousWater?.average,
            waterDaysAtGoal = water?.daysAtGoal ?: 0,
            proteinAverageG = protein?.average,
            previousProteinAverageG = previousProtein?.average,
            proteinDaysAtGoal = protein?.daysAtGoal ?: 0,
            sideEffectCount = effects.size,
            previousSideEffectCount = previousEffects,
            topSideEffect = top?.key,
            topSideEffectCount = top?.value ?: 0,
            journalDays = journal,
            previousJournalDays = previousJournal,
            bestStreak = streak,
            highlights = highlights,
            days = (0L..6L).map { i ->
                val d = start.plusDays(i)
                RecapDay(
                    date = d,
                    journal = d in input.journalDays,
                    dose = d in doseDays,
                    waterGoal = input.waterGoalMl > 0 && (input.waterByDay[d] ?: 0) >= input.waterGoalMl,
                )
            },
        )
    }

    /**
     * Doses due in the week: from the last dose before the week, every [intervalDays]. A dose due on
     * the last day only counts once it is taken (it may simply be later today). Never less than
     * [taken]; 0 when treatment had not started or seems paused (no dose for [PAUSED_AFTER_DAYS]+).
     */
    internal fun expectedDoses(doseDaysAsc: List<LocalDate>, intervalDays: Int, start: LocalDate, end: LocalDate): Int {
        val taken = doseDaysAsc.count { !it.isBefore(start) && !it.isAfter(end) }
        val interval = intervalDays.coerceAtLeast(1).toLong()
        val anchor = doseDaysAsc.lastOrNull { it.isBefore(start) } ?: return taken
        val gap = start.toEpochDay() - anchor.toEpochDay()
        if (gap > maxOf(PAUSED_AFTER_DAYS, 3 * interval)) return taken
        val takenOnLastDay = end in doseDaysAsc
        var due = anchor.plusDays(interval)
        var count = 0
        while (!due.isAfter(end)) {
            if (!due.isBefore(start) && (due != end || takenOnLastDay)) count++
            due = due.plusDays(interval)
        }
        return maxOf(count, taken)
    }

    /**
     * Smoothing factor for [Insights.trend], which smooths per weigh-in: 0.1 per *day* (as planned in
     * PARITY P11) converted to the user's usual gap, so weekly weigh-ins are not over-smoothed.
     */
    internal fun trendAlpha(weights: List<Pair<Long, Double>>): Double {
        if (weights.size < 2) return 0.1
        val times = weights.map { it.first }.sorted().takeLast(11)
        val gaps = times.zipWithNext { a, b -> (b - a) / DAY_MS }.sorted()
        val median = gaps[gaps.size / 2]
        return (1 - 0.9.pow(median)).coerceIn(0.1, 0.6)
    }

    /**
     * Trend change over [from]..[to]: last trend value of the period minus the last one before it
     * (or the first of the period). Null without a weigh-in in the period.
     */
    private fun trendChange(trend: List<Pair<LocalDate, Double>>, from: LocalDate, to: LocalDate): Double? {
        val inside = trend.filter { !it.first.isBefore(from) && !it.first.isAfter(to) }
        if (inside.isEmpty()) return null
        val base = trend.lastOrNull { it.first.isBefore(from) }?.second
            ?: if (inside.size >= 2) inside.first().second else return null
        return inside.last().second - base
    }

    private class GoalStats(val average: Int, val daysLogged: Int, val daysAtGoal: Int)

    /** Average over the days with a value (> 0), and days reaching [goal]; null when nothing was logged. */
    private fun goalStats(byDay: Map<LocalDate, Int>, goal: Int, from: LocalDate, to: LocalDate): GoalStats? {
        val values = (0L..(to.toEpochDay() - from.toEpochDay())).mapNotNull { byDay[from.plusDays(it)]?.takeIf { v -> v > 0 } }
        if (values.isEmpty()) return null
        val atGoal = if (goal > 0) values.count { it >= goal } else 0
        return GoalStats(values.average().roundToInt(), values.size, atGoal)
    }

    private fun goalHighlight(
        stats: GoalStats?,
        previous: GoalStats?,
        goal: Int,
        metKind: HighlightKind,
        metImportance: Int,
        belowKind: HighlightKind,
        belowImportance: Int,
    ): Highlight? {
        if (stats == null || goal <= 0) return null
        val met = stats.daysAtGoal >= GOAL_DAYS || stats.average >= goal
        return Highlight(
            kind = if (met) metKind else belowKind,
            importance = if (met) metImportance else belowImportance,
            value = stats.average.toDouble(),
            previous = previous?.average?.toDouble(),
            count = stats.daysAtGoal,
            total = goal,
        )
    }

    /** Longest habit streak still alive at [end] (counting [end] itself, or ending the day before). */
    internal fun bestStreak(input: RecapInput, end: LocalDate): Streak? {
        val waterDays = if (input.waterGoalMl > 0) input.waterByDay.filterValues { it >= input.waterGoalMl }.keys else emptySet()
        val proteinDays = if (input.proteinGoalG > 0) input.proteinByDay.filterValues { it >= input.proteinGoalG }.keys else emptySet()
        // Ties: goals before plain logging.
        return listOf(
            Streak(StreakKind.WATER_GOAL, run(waterDays, end)),
            Streak(StreakKind.PROTEIN_GOAL, run(proteinDays, end)),
            Streak(StreakKind.JOURNAL, run(input.journalDays, end)),
        ).maxByOrNull { it.days }?.takeIf { it.days >= MIN_STREAK }
    }

    private fun run(days: Set<LocalDate>, end: LocalDate): Int {
        var cursor = if (end in days) end else end.minusDays(1)
        var n = 0
        while (cursor in days && n < 3650) {
            n++
            cursor = cursor.minusDays(1)
        }
        return n
    }
}

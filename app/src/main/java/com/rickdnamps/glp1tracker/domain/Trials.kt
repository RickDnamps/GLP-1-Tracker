package com.rickdnamps.glp1tracker.domain

import kotlin.math.abs

/**
 * Published mean body-weight curves of the main GLP-1 trials, to show "where am I vs the studies?".
 *
 * Every curve is a list of (week since first dose, mean % change of body weight; negative = loss),
 * starting at (0, 0). **Endpoints** are the headline results of each publication (sources in the
 * comments). **Intermediate points** are approximate readings of the published time-course figures,
 * rounded to 0.5 %, and must be re-checked against the figures before each release (see docs/PARITY.md
 * P10). These are group averages under trial conditions (lifestyle programme, protocol doses), never
 * a target or a prediction for one person.
 */
enum class Trial(
    /** Proper name of the study, shown as is in every language. */
    val study: String,
    val molecule: Molecule,
    /** Maintenance dose of this arm, in mg. */
    val doseMg: Double,
    /** True for trials in people with type 2 diabetes (they usually lose less weight). */
    val diabetes: Boolean,
    /** (week, mean % change), ascending weeks, first point (0, 0.0). */
    val points: List<Pair<Int, Double>>,
    /** Mean % change with placebo (or comparator) at the end of the trial, when there was one. */
    val placeboPercent: Double?,
    /** Short citation, shown under the chart. */
    val citation: String,
) {
    // STEP 1: Wilding JPH et al., N Engl J Med 2021;384:989-1002. Adults without diabetes, BMI >= 30 (or >= 27 with a
    // comorbidity), semaglutide 2.4 mg weekly + lifestyle, 68 weeks, treatment-policy estimand.
    STEP1_SEMAGLUTIDE_2_4(
        "STEP 1", Molecule.SEMAGLUTIDE, 2.4, diabetes = false,
        points = listOf(
            0 to 0.0,
            4 to -2.5, // ≈ Fig. 1A (approximate reading)
            12 to -6.0, // ≈ Fig. 1A
            20 to -8.5, // ≈ Fig. 1A (approximate reading, to re-check)
            28 to -10.0, // ≈ Fig. 1A
            36 to -11.5, // ≈ Fig. 1A (approximate reading, to re-check)
            44 to -13.0, // ≈ Fig. 1A (approximate reading, to re-check)
            52 to -14.0, // ≈ Fig. 1A (approximate reading, to re-check)
            60 to -14.5, // ≈ Fig. 1A (approximate reading, to re-check)
            68 to -14.9, // primary endpoint, abstract: −14.9 %
        ),
        placeboPercent = -2.4, // abstract: placebo −2.4 % at week 68
        citation = "Wilding et al., N Engl J Med 2021",
    ),

    // SURMOUNT-1: Jastreboff AM et al., N Engl J Med 2022;387:205-216. Adults without diabetes, BMI >= 30 (or >= 27 with
    // a comorbidity), tirzepatide 5/10/15 mg weekly (2.5 mg start, +2.5 mg every 4 weeks), 72 weeks,
    // treatment-regimen estimand. Placebo −3.1 % at week 72 (abstract).
    SURMOUNT1_TIRZEPATIDE_5(
        "SURMOUNT-1", Molecule.TIRZEPATIDE, 5.0, diabetes = false,
        points = listOf(
            0 to 0.0,
            4 to -3.0, // ≈ Fig. 1 (approximate reading, to re-check)
            12 to -6.5, // ≈ Fig. 1 (approximate reading, to re-check)
            24 to -10.5, // ≈ Fig. 1 (approximate reading, to re-check)
            36 to -13.0, // ≈ Fig. 1 (approximate reading, to re-check)
            48 to -14.5, // ≈ Fig. 1 (approximate reading, to re-check)
            60 to -15.0, // ≈ Fig. 1 (approximate reading, to re-check)
            72 to -15.0, // primary endpoint, abstract: −15.0 % with 5 mg
        ),
        placeboPercent = -3.1,
        citation = "Jastreboff et al., N Engl J Med 2022",
    ),
    SURMOUNT1_TIRZEPATIDE_10(
        "SURMOUNT-1", Molecule.TIRZEPATIDE, 10.0, diabetes = false,
        points = listOf(
            0 to 0.0,
            4 to -3.0, // ≈ Fig. 1 (approximate reading, to re-check)
            12 to -7.0, // ≈ Fig. 1 (approximate reading, to re-check)
            24 to -12.0, // ≈ Fig. 1 (approximate reading, to re-check)
            36 to -15.5, // ≈ Fig. 1 (approximate reading, to re-check)
            48 to -18.0, // ≈ Fig. 1 (approximate reading, to re-check)
            60 to -19.0, // ≈ Fig. 1 (approximate reading, to re-check)
            72 to -19.5, // primary endpoint, abstract: −19.5 % with 10 mg
        ),
        placeboPercent = -3.1,
        citation = "Jastreboff et al., N Engl J Med 2022",
    ),
    SURMOUNT1_TIRZEPATIDE_15(
        "SURMOUNT-1", Molecule.TIRZEPATIDE, 15.0, diabetes = false,
        points = listOf(
            0 to 0.0,
            4 to -3.0, // ≈ Fig. 1 (approximate reading, to re-check)
            12 to -7.5, // ≈ Fig. 1 (approximate reading, to re-check)
            24 to -13.5, // ≈ Fig. 1 (approximate reading, to re-check)
            36 to -17.0, // ≈ Fig. 1 (approximate reading, to re-check)
            48 to -19.5, // ≈ Fig. 1 (approximate reading, to re-check)
            60 to -20.5, // ≈ Fig. 1 (approximate reading, to re-check)
            72 to -20.9, // primary endpoint, abstract: −20.9 % with 15 mg
        ),
        placeboPercent = -3.1,
        citation = "Jastreboff et al., N Engl J Med 2022",
    ),

    // SCALE Obesity and Prediabetes: Pi-Sunyer X et al., N Engl J Med 2015;373:11-22. Adults without diabetes, liraglutide
    // 3.0 mg daily + lifestyle, 56 weeks, full analysis set (LOCF). Placebo −2.6 % (abstract).
    SCALE_LIRAGLUTIDE_3_0(
        "SCALE", Molecule.LIRAGLUTIDE, 3.0, diabetes = false,
        points = listOf(
            0 to 0.0,
            4 to -2.5, // ≈ Fig. 1 (approximate reading, to re-check)
            12 to -5.5, // ≈ Fig. 1 (approximate reading, to re-check)
            28 to -7.5, // ≈ Fig. 1 (approximate reading, to re-check)
            40 to -8.0, // ≈ Fig. 1 (approximate reading, to re-check)
            56 to -8.0, // primary endpoint, abstract: −8.0 %
        ),
        placeboPercent = -2.6,
        citation = "Pi-Sunyer et al., N Engl J Med 2015",
    ),

    // SUSTAIN 7: Pratley RE et al., Lancet Diabetes Endocrinol 2018;6:275-286. Type 2 diabetes on metformin, semaglutide
    // 0.5 / 1.0 mg vs dulaglutide 0.75 / 1.5 mg, 40 weeks. Results are published in kg; % = kg change / arm baseline
    // (prescribing information table: sema 0.5 mg −4.6 kg from 96.4 kg, sema 1.0 mg −6.5 kg from 95.5 kg,
    // dulaglutide 1.5 mg −3.0 kg from 93.4 kg). Both arms: 0.25 mg for 4 weeks, then 0.5 mg; 1.0 mg from week 8.
    SUSTAIN7_SEMAGLUTIDE_0_5(
        "SUSTAIN 7", Molecule.SEMAGLUTIDE, 0.5, diabetes = true,
        points = listOf(
            0 to 0.0,
            8 to -2.5, // ≈ Fig. 2 (approximate reading, to re-check)
            16 to -3.5, // ≈ Fig. 2 (approximate reading, to re-check)
            24 to -4.0, // ≈ Fig. 2 (approximate reading, to re-check)
            40 to -4.8, // endpoint: −4.6 kg / 96.4 kg ≈ −4.8 %
        ),
        placeboPercent = null, // active comparator (dulaglutide), no placebo arm
        citation = "Pratley et al., Lancet Diabetes Endocrinol 2018",
    ),
    SUSTAIN7_SEMAGLUTIDE_1_0(
        "SUSTAIN 7", Molecule.SEMAGLUTIDE, 1.0, diabetes = true,
        points = listOf(
            0 to 0.0,
            8 to -2.5, // ≈ Fig. 2 (approximate reading, to re-check)
            16 to -4.5, // ≈ Fig. 2 (approximate reading, to re-check)
            24 to -5.5, // ≈ Fig. 2 (approximate reading, to re-check)
            40 to -6.8, // endpoint: −6.5 kg / 95.5 kg ≈ −6.8 %
        ),
        placeboPercent = null,
        citation = "Pratley et al., Lancet Diabetes Endocrinol 2018",
    ),

    // AWARD-11: Frias JP et al., Diabetes Care 2021;44:765-773. Type 2 diabetes on metformin, dulaglutide 1.5 / 3.0 /
    // 4.5 mg weekly (0.75 mg for 4 weeks, then 1.5 mg, then +1.5 mg every 4 weeks), 36 weeks, efficacy estimand:
    // −3.1 / −4.0 / −4.7 kg from a mean baseline of 95.7 kg (exploratory weight analysis, Diabetes Obes Metab 2021).
    AWARD11_DULAGLUTIDE_1_5(
        "AWARD-11", Molecule.DULAGLUTIDE, 1.5, diabetes = true,
        points = listOf(
            0 to 0.0,
            4 to -1.0, // ≈ Fig. 2 (approximate reading, to re-check)
            12 to -2.5, // ≈ Fig. 2 (approximate reading, to re-check)
            24 to -3.0, // ≈ Fig. 2 (approximate reading, to re-check)
            36 to -3.2, // endpoint: −3.1 kg / 95.7 kg ≈ −3.2 %
        ),
        placeboPercent = null, // no placebo arm (1.5 mg was the comparator)
        citation = "Frias et al., Diabetes Care 2021",
    ),
    AWARD11_DULAGLUTIDE_3_0(
        "AWARD-11", Molecule.DULAGLUTIDE, 3.0, diabetes = true,
        points = listOf(
            0 to 0.0,
            4 to -1.0, // ≈ Fig. 2 (approximate reading, to re-check)
            12 to -2.5, // ≈ Fig. 2 (approximate reading, to re-check)
            24 to -3.5, // ≈ Fig. 2 (approximate reading, to re-check)
            36 to -4.2, // endpoint: −4.0 kg / 95.7 kg ≈ −4.2 %
        ),
        placeboPercent = null,
        citation = "Frias et al., Diabetes Care 2021",
    ),
    AWARD11_DULAGLUTIDE_4_5(
        "AWARD-11", Molecule.DULAGLUTIDE, 4.5, diabetes = true,
        points = listOf(
            0 to 0.0,
            4 to -1.0, // ≈ Fig. 2 (approximate reading, to re-check)
            12 to -3.0, // ≈ Fig. 2 (approximate reading, to re-check)
            24 to -4.0, // ≈ Fig. 2 (approximate reading, to re-check)
            36 to -4.9, // endpoint: −4.7 kg / 95.7 kg ≈ −4.9 %
        ),
        placeboPercent = null,
        citation = "Frias et al., Diabetes Care 2021",
    );

    /** Last week of the published curve (end of the trial). */
    val endWeek: Int get() = points.last().first

    /** Mean % change at the end of the trial. */
    val endPercent: Double get() = points.last().second
}

object Trials {
    private const val WEEK_MS = 7 * 86_400_000.0

    /** Before this many weeks, weight noise is larger than the expected change: no verdict yet. */
    const val MIN_WEEKS = 2.0

    /** Within ± this many percentage points of the trial mean counts as "in line with the study". */
    const val ON_PAR_POINTS = 1.0

    /**
     * Reference trial for a medication and its current dose: the arm with the nearest maintenance
     * dose (ties go to the higher dose). Weight-loss brands use the obesity trials; diabetes brands
     * use the diabetes trials. Null when no curve is available (oral semaglutide, Victoza, other).
     */
    fun referenceFor(medication: Medication, doseMg: Double): Trial? {
        val arms = when (medication) {
            Medication.WEGOVY, Medication.SEMAGLUTIDE_COMPOUNDED -> listOf(Trial.STEP1_SEMAGLUTIDE_2_4)
            Medication.OZEMPIC -> listOf(Trial.SUSTAIN7_SEMAGLUTIDE_0_5, Trial.SUSTAIN7_SEMAGLUTIDE_1_0)
            Medication.MOUNJARO, Medication.ZEPBOUND, Medication.TIRZEPATIDE_COMPOUNDED -> listOf(
                Trial.SURMOUNT1_TIRZEPATIDE_5, Trial.SURMOUNT1_TIRZEPATIDE_10, Trial.SURMOUNT1_TIRZEPATIDE_15,
            )
            Medication.SAXENDA -> listOf(Trial.SCALE_LIRAGLUTIDE_3_0)
            Medication.TRULICITY -> listOf(
                Trial.AWARD11_DULAGLUTIDE_1_5, Trial.AWARD11_DULAGLUTIDE_3_0, Trial.AWARD11_DULAGLUTIDE_4_5,
            )
            // No published curve wired in yet (OASIS 4, PIONEER, LEAD…): better nothing than a wrong curve.
            Medication.WEGOVY_PILL, Medication.RYBELSUS, Medication.VICTOZA, Medication.OTHER -> emptyList()
        }
        return arms.sortedByDescending { it.doseMg }.minByOrNull { abs(it.doseMg - doseMg) }
    }

    /**
     * Mean % change expected at [week] in [trial], by linear interpolation between published points.
     * 0 before the first dose; the final value is held after the end of the trial.
     */
    fun expectedAt(trial: Trial, week: Double): Double {
        val p = trial.points
        if (week <= p.first().first) return p.first().second
        if (week >= p.last().first) return p.last().second
        val i = p.indexOfFirst { it.first >= week }
        val (w0, v0) = p[i - 1]
        val (w1, v1) = p[i]
        return v0 + (v1 - v0) * (week - w0) / (w1 - w0)
    }

    fun expectedAt(trial: Trial, week: Int): Double = expectedAt(trial, week.toDouble())

    /** A weigh-in this close to the first dose (before, or later the same day) is the week-0 weight. */
    private const val BASELINE_BEFORE_MS = 14 * 86_400_000L
    private const val BASELINE_AFTER_MS = 86_400_000L

    /**
     * Baseline weight for the comparison, in order: the weigh-in closest to the first dose (up to 14 days
     * before or 1 day after); the declared start weight; the last weigh-in before the first dose. A measured
     * week-0 weight wins over the declared one, which may predate the first logged dose by months when the
     * app was installed mid-treatment. Null when only later weigh-ins exist (week 0 would be a guess).
     */
    fun baselineKg(startWeightKg: Double?, firstDoseMillis: Long, weights: List<Pair<Long, Double>>): Double? {
        val sorted = weights.sortedBy { it.first }
        sorted.filter { it.first in (firstDoseMillis - BASELINE_BEFORE_MS)..(firstDoseMillis + BASELINE_AFTER_MS) }
            .minByOrNull { abs(it.first - firstDoseMillis) }
            ?.let { return it.second }
        startWeightKg?.takeIf { it > 0 }?.let { return it }
        return sorted.lastOrNull { it.first <= firstDoseMillis }?.second
    }

    /**
     * The user's own curve: (weeks since [firstDoseMillis], % change from [baselineKg]) for each
     * weigh-in from the first dose on, starting with (0, 0).
     */
    fun userCurve(firstDoseMillis: Long, baselineKg: Double, weights: List<Pair<Long, Double>>): List<Pair<Double, Double>> {
        if (baselineKg <= 0) return emptyList()
        val after = weights.filter { it.first > firstDoseMillis }.sortedBy { it.first }
        return listOf(0.0 to 0.0) + after.map { (t, kg) -> (t - firstDoseMillis) / WEEK_MS to (kg - baselineKg) / baselineKg * 100 }
    }

    enum class Standing { AHEAD, ON_PAR, BEHIND }

    /** The user's latest point against the trial mean at the same week. */
    data class Comparison(
        val week: Double,
        val userPercent: Double,
        val trialPercent: Double,
        val standing: Standing,
        /** True when the user is past the last week of the trial (compared with its final result). */
        val beyondTrial: Boolean,
    ) {
        /** Percentage points ahead of the trial mean (positive = more loss than the average). */
        val pointsAhead: Double get() = trialPercent - userPercent
    }

    fun compare(trial: Trial, week: Double, userPercent: Double): Comparison {
        val expected = expectedAt(trial, week)
        val ahead = expected - userPercent
        val standing = when {
            ahead >= ON_PAR_POINTS -> Standing.AHEAD
            ahead <= -ON_PAR_POINTS -> Standing.BEHIND
            else -> Standing.ON_PAR
        }
        return Comparison(week, userPercent, expected, standing, week > trial.endWeek)
    }

    enum class Status {
        /** No published curve for this medication. */
        NO_TRIAL,
        /** No dose logged yet: week 0 is unknown. */
        NO_DOSES,
        /** No start weight: no weigh-in around or before the first dose and none declared. */
        NO_BASELINE,
        /** No weigh-in since the first dose. */
        NO_WEIGHTS,
        /** Latest weigh-in is before [MIN_WEEKS]. */
        TOO_EARLY,
        READY,
    }

    data class Report(
        val status: Status,
        val trial: Trial?,
        val firstDoseMillis: Long?,
        val baselineKg: Double?,
        /** (week, % change), starts with (0, 0) when there is a baseline. */
        val userCurve: List<Pair<Double, Double>>,
        val comparison: Comparison?,
    )

    /**
     * Everything the trial card needs. [doses] are (epoch millis, medication) of every logged dose, any
     * order; week 0 is the first dose of the current medication's molecule. The declared start weight is
     * only used when that first dose is also the first dose overall (no earlier switch of molecule).
     */
    fun report(
        medication: Medication,
        doseMg: Double,
        doses: List<Pair<Long, Medication>>,
        weights: List<Pair<Long, Double>>,
        startWeightKg: Double?,
    ): Report {
        val trial = referenceFor(medication, doseMg)
            ?: return Report(Status.NO_TRIAL, null, null, null, emptyList(), null)
        val sameMolecule = doses.filter { it.second.molecule == medication.molecule }
        val first = (sameMolecule.minOfOrNull { it.first } ?: doses.minOfOrNull { it.first })
            ?: return Report(Status.NO_DOSES, trial, null, null, emptyList(), null)
        val switched = doses.any { it.first < first }
        val baseline = baselineKg(if (switched) null else startWeightKg, first, weights)
            ?: return Report(Status.NO_BASELINE, trial, first, null, emptyList(), null)
        val curve = userCurve(first, baseline, weights)
        if (curve.size < 2) return Report(Status.NO_WEIGHTS, trial, first, baseline, curve, null)
        val (week, percent) = curve.last()
        if (week < MIN_WEEKS) return Report(Status.TOO_EARLY, trial, first, baseline, curve, null)
        return Report(Status.READY, trial, first, baseline, curve, compare(trial, week, percent))
    }
}

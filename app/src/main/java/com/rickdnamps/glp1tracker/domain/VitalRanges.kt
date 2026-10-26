package com.rickdnamps.glp1tracker.domain

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToLong

/** Where a reading sits against general adult reference ranges; [severity] orders them (0 = normal). */
enum class VitalLevel(val severity: Int) {
    NORMAL(0),
    ELEVATED(1),
    LOW(1),
    HIGH(2),
}

/**
 * Blood-pressure categories, named after the AHA/ACC 2017 guideline. The [level] puts stage 1 with
 * "elevated" and only ≥ 140/90 in "high", the threshold Hypertension Canada uses for office readings.
 */
enum class BpStage(val level: VitalLevel) {
    LOW(VitalLevel.LOW),
    NORMAL(VitalLevel.NORMAL),
    ELEVATED(VitalLevel.ELEVATED),
    STAGE_1(VitalLevel.ELEVATED),
    STAGE_2(VitalLevel.HIGH),
    CRISIS(VitalLevel.HIGH),
}

/** Direction of a change against the reference ranges, not just the sign of the difference. */
enum class VitalTrend { BETTER, STABLE, WORSE }

/** A reading reduced to its numbers, so this file stays free of Room types. [value2] is the diastolic. */
data class VitalReading(val value: Double, val value2: Double? = null)

/** Change between two readings; [delta2] is only set for blood pressure. */
data class VitalChange(val delta: Double, val delta2: Double?, val percent: Double?, val trend: VitalTrend)

/** Why a value typed in the editor cannot be saved. */
enum class VitalInputError {
    MISSING_VALUE,
    VALUE_OUT_OF_RANGE,
    MISSING_SECOND,
    SECOND_OUT_OF_RANGE,
    SECOND_NOT_BELOW_FIRST,
}

/** A unit mix-up we can fix: the value in our unit and the unit it was probably typed in ("" = fraction). */
data class VitalConversion(val value: Double, val fromUnit: String)

/**
 * General adult reference ranges (estimates only, not personal targets):
 * - blood pressure: AHA/ACC 2017 stages, Hypertension Canada threshold 140/90; low below 90/60;
 * - resting heart rate: 50–100 bpm;
 * - fasting glucose (mmol/L): < 4.0 low, 4.0–6.0 normal, 6.1–6.9 elevated, ≥ 7.0 high (Diabetes Canada);
 * - HbA1c: < 5.7 % normal, 5.7–6.4 % prediabetes range, ≥ 6.5 % diabetes range;
 * - LDL cholesterol: < 3.5 mmol/L general target, ≥ 5.0 high;
 * - fasting triglycerides: < 1.7 mmol/L normal, 1.7–2.2 borderline, ≥ 2.3 high.
 * Values are rounded to their display precision first, so the category matches what the user sees.
 */
object VitalRanges {

    /** Decimals used to display (and classify) each type. */
    fun decimals(type: VitalType): Int = when (type) {
        VitalType.BLOOD_PRESSURE, VitalType.HEART_RATE -> 0
        VitalType.GLUCOSE, VitalType.HBA1C -> 1
        VitalType.LDL, VitalType.TRIGLYCERIDES -> 2
    }

    fun round(type: VitalType, value: Double): Double {
        val f = 10.0.pow(decimals(type))
        return (value * f).roundToLong() / f
    }

    fun classify(type: VitalType, value: Double, value2: Double? = null): VitalLevel {
        val v = round(type, value)
        return when (type) {
            VitalType.BLOOD_PRESSURE -> bpStage(value, value2).level
            VitalType.HEART_RATE -> when {
                v < 50 -> VitalLevel.LOW
                v > 100 -> VitalLevel.HIGH
                else -> VitalLevel.NORMAL
            }
            VitalType.GLUCOSE -> when {
                v < 4.0 -> VitalLevel.LOW
                v < 6.1 -> VitalLevel.NORMAL
                v < 7.0 -> VitalLevel.ELEVATED
                else -> VitalLevel.HIGH
            }
            VitalType.HBA1C -> when {
                v < 5.7 -> VitalLevel.NORMAL
                v < 6.5 -> VitalLevel.ELEVATED
                else -> VitalLevel.HIGH
            }
            VitalType.LDL -> when {
                v < 3.5 -> VitalLevel.NORMAL
                v < 5.0 -> VitalLevel.ELEVATED
                else -> VitalLevel.HIGH
            }
            VitalType.TRIGLYCERIDES -> when {
                v < 1.7 -> VitalLevel.NORMAL
                v < 2.3 -> VitalLevel.ELEVATED
                else -> VitalLevel.HIGH
            }
        }
    }

    fun classify(type: VitalType, reading: VitalReading): VitalLevel = classify(type, reading.value, reading.value2)

    /** The higher of the two categories wins (e.g. 125/85 is stage 1 because of the diastolic). */
    fun bpStage(systolic: Double, diastolic: Double?): BpStage {
        val s = Math.round(systolic).toDouble()
        val d = diastolic?.let { Math.round(it).toDouble() }
        return when {
            s > 180 || (d != null && d > 120) -> BpStage.CRISIS
            s >= 140 || (d != null && d >= 90) -> BpStage.STAGE_2
            s >= 130 || (d != null && d >= 80) -> BpStage.STAGE_1
            s >= 120 -> BpStage.ELEVATED
            s < 90 || (d != null && d < 60) -> BpStage.LOW
            else -> BpStage.NORMAL
        }
    }

    /** Upper limit of the normal range, drawn as the dashed line on the charts (systolic for BP). */
    fun upperNormal(type: VitalType): Double = when (type) {
        VitalType.BLOOD_PRESSURE -> 120.0
        VitalType.HEART_RATE -> 100.0
        VitalType.GLUCOSE -> 6.0
        VitalType.HBA1C -> 5.7
        VitalType.LDL -> 3.5
        VitalType.TRIGLYCERIDES -> 1.7
    }

    /** Values we accept in the editor (systolic for BP); anything outside is almost surely a typo. */
    fun plausible(type: VitalType): ClosedFloatingPointRange<Double> = when (type) {
        VitalType.BLOOD_PRESSURE -> 60.0..260.0
        VitalType.HEART_RATE -> 25.0..230.0
        VitalType.GLUCOSE -> 1.0..35.0
        VitalType.HBA1C -> 3.0..20.0
        VitalType.LDL -> 0.1..15.0
        VitalType.TRIGLYCERIDES -> 0.1..50.0
    }

    /** Accepted diastolic values. */
    val plausibleDiastolic: ClosedFloatingPointRange<Double> = 30.0..160.0

    /** Returns null when the reading can be saved. [value2] is only checked for types with a second value. */
    fun validate(type: VitalType, value: Double?, value2: Double?): VitalInputError? = when {
        value == null -> VitalInputError.MISSING_VALUE
        value !in plausible(type) -> VitalInputError.VALUE_OUT_OF_RANGE
        !type.hasSecondValue -> null
        value2 == null -> VitalInputError.MISSING_SECOND
        value2 !in plausibleDiastolic -> VitalInputError.SECOND_OUT_OF_RANGE
        value2 >= value -> VitalInputError.SECOND_NOT_BELOW_FIRST
        else -> null
    }

    /**
     * Suggests a conversion when a value is implausible in our unit but makes sense in a common other one:
     * mg/dL for glucose, LDL and triglycerides; mmol/mol (IFCC) or a fraction (0.065) for HbA1c.
     */
    fun suggestConversion(type: VitalType, value: Double): VitalConversion? {
        if (value in plausible(type) || value <= 0) return null
        val converted = when (type) {
            VitalType.GLUCOSE -> VitalConversion(value / 18.0, "mg/dL")
            VitalType.LDL -> VitalConversion(value / 38.67, "mg/dL")
            VitalType.TRIGLYCERIDES -> VitalConversion(value / 88.57, "mg/dL")
            VitalType.HBA1C ->
                if (value < 1) VitalConversion(value * 100, "")
                else VitalConversion(value * 0.09148 + 2.152, "mmol/mol")
            VitalType.BLOOD_PRESSURE, VitalType.HEART_RATE -> null
        } ?: return null
        val rounded = converted.copy(value = round(type, converted.value))
        return rounded.takeIf { it.value in plausible(type) }
    }

    /** Simple difference, latest minus first. */
    fun change(first: Double, latest: Double): Double = latest - first

    /** Relative change in percent, or null when [first] is zero. */
    fun percentChange(first: Double, latest: Double): Double? =
        if (first == 0.0) null else (latest - first) / abs(first) * 100.0

    /** Change from [first] to [latest], with deltas rounded to the display precision. */
    fun change(type: VitalType, first: VitalReading, latest: VitalReading): VitalChange {
        val delta = round(type, change(first.value, latest.value))
        val delta2 = if (type.hasSecondValue && first.value2 != null && latest.value2 != null) {
            round(type, change(first.value2, latest.value2))
        } else null
        return VitalChange(delta, delta2, percentChange(first.value, latest.value), trend(type, first, latest))
    }

    /** Smallest change worth colouring (smaller ones are day-to-day noise). */
    fun stableStep(type: VitalType): Double = when (type) {
        VitalType.BLOOD_PRESSURE, VitalType.HEART_RATE -> 3.0
        VitalType.GLUCOSE -> 0.3
        VitalType.HBA1C, VitalType.LDL, VitalType.TRIGLYCERIDES -> 0.1
    }

    /**
     * A move to a better category is BETTER, to a worse one WORSE. Inside the same category, going down
     * is better when above normal (and for LDL/triglycerides even when normal), going up is better when low.
     */
    fun trend(type: VitalType, first: VitalReading, latest: VitalReading): VitalTrend {
        val from = classify(type, first)
        val to = classify(type, latest)
        if (from.severity != to.severity) return if (to.severity < from.severity) VitalTrend.BETTER else VitalTrend.WORSE
        if (from != to) return VitalTrend.STABLE // e.g. from slightly high to slightly low
        val d = round(type, latest.value - first.value)
        val d2 = if (type.hasSecondValue && first.value2 != null && latest.value2 != null) {
            round(type, latest.value2 - first.value2)
        } else 0.0
        val step = stableStep(type) - 1e-9
        if (abs(d) < step && abs(d2) < step) return VitalTrend.STABLE
        val score = d + d2
        if (score == 0.0) return VitalTrend.STABLE
        val lowerIsBetter = when (to) {
            VitalLevel.LOW -> false
            VitalLevel.ELEVATED, VitalLevel.HIGH -> true
            VitalLevel.NORMAL -> if (type == VitalType.LDL || type == VitalType.TRIGLYCERIDES) true else return VitalTrend.STABLE
        }
        return if ((score < 0) == lowerIsBetter) VitalTrend.BETTER else VitalTrend.WORSE
    }

    /** Value at its display precision: "128", "6.0", "2.45" (locale decimal separator). */
    fun format(type: VitalType, value: Double, locale: Locale = Locale.getDefault()): String {
        val pattern = when (decimals(type)) {
            0 -> "0"
            1 -> "0.0"
            else -> "0.0#"
        }
        return DecimalFormat(pattern, DecimalFormatSymbols.getInstance(locale)).format(round(type, value))
    }

    /** Reading without unit: "128/82" for blood pressure. */
    fun formatReading(type: VitalType, value: Double, value2: Double?, locale: Locale = Locale.getDefault()): String =
        if (type.hasSecondValue && value2 != null) format(type, value, locale) + "/" + format(type, value2, locale)
        else format(type, value, locale)

    /** Signed difference with a true minus sign: "+3", "−0.4", "±0". */
    fun formatDelta(type: VitalType, delta: Double, locale: Locale = Locale.getDefault()): String {
        val r = round(type, delta)
        return when {
            r > 0 -> "+" + format(type, r, locale)
            r < 0 -> "−" + format(type, -r, locale)
            else -> "±" + format(type, 0.0, locale)
        }
    }

    /** Text for editing an existing value: no trailing zeros ("6", "2.45"). */
    fun inputText(type: VitalType, value: Double, locale: Locale = Locale.getDefault()): String {
        val d = decimals(type)
        val pattern = if (d == 0) "0" else "0." + "#".repeat(d)
        return DecimalFormat(pattern, DecimalFormatSymbols.getInstance(locale)).format(round(type, value))
    }
}

package com.rickdnamps.glp1tracker.domain

import androidx.annotation.StringRes
import com.rickdnamps.glp1tracker.R

/** Body composition reading attached to a weigh-in; every field is optional. */
data class BodyComp(
    val bodyFatPct: Double? = null,
    val muscleKg: Double? = null,
    val waterPct: Double? = null,
    val boneKg: Double? = null,
    val visceralFat: Double? = null,
    val bmrKcal: Double? = null,
    val metabolicAge: Int? = null,
    val proteinPct: Double? = null,
    val skeletalMusclePct: Double? = null,
    val subcutaneousFatPct: Double? = null,
    val impedance: Double? = null,
) {
    val isEmpty: Boolean
        get() = listOf(bodyFatPct, muscleKg, waterPct, boneKg, visceralFat, bmrKcal, proteinPct, skeletalMusclePct, subcutaneousFatPct)
            .all { it == null } && metabolicAge == null

    /** Fills missing fields from [other] (values already present win). */
    fun mergedWith(other: BodyComp): BodyComp = BodyComp(
        bodyFatPct ?: other.bodyFatPct, muscleKg ?: other.muscleKg, waterPct ?: other.waterPct,
        boneKg ?: other.boneKg, visceralFat ?: other.visceralFat, bmrKcal ?: other.bmrKcal,
        metabolicAge ?: other.metabolicAge, proteinPct ?: other.proteinPct,
        skeletalMusclePct ?: other.skeletalMusclePct, subcutaneousFatPct ?: other.subcutaneousFatPct,
        impedance ?: other.impedance,
    )
}

enum class Sex { MALE, FEMALE }

/** Metrics shown in the body-composition card, with how to read them from a [BodyComp]. */
enum class CompMetric(@StringRes val nameRes: Int, val unit: String, val decimals: Int, val read: (BodyComp, Double) -> Double?) {
    FAT(R.string.comp_fat, "%", 1, { c, _ -> c.bodyFatPct }),
    FAT_MASS(R.string.comp_fat_mass, "kg", 1, { c, w -> c.bodyFatPct?.let { w * it / 100 } }),
    MUSCLE(R.string.comp_muscle, "kg", 1, { c, _ -> c.muscleKg }),
    WATER(R.string.comp_water, "%", 1, { c, _ -> c.waterPct }),
    BONE(R.string.comp_bone, "kg", 1, { c, _ -> c.boneKg }),
    VISCERAL(R.string.comp_visceral, "", 0, { c, _ -> c.visceralFat }),
    BMR(R.string.comp_bmr, "kcal", 0, { c, _ -> c.bmrKcal }),
    METABOLIC_AGE(R.string.comp_metabolic_age, "", 0, { c, _ -> c.metabolicAge?.toDouble() }),
    PROTEIN(R.string.comp_protein, "%", 1, { c, _ -> c.proteinPct }),
    SKELETAL(R.string.comp_skeletal, "%", 1, { c, _ -> c.skeletalMusclePct }),
    SUBCUTANEOUS(R.string.comp_subcutaneous, "%", 1, { c, _ -> c.subcutaneousFatPct }),
}

enum class CompLevel { LOW, HEALTHY, HIGH, VERY_HIGH }

object BodyCompositionMath {

    /**
     * Estimates composition from foot-to-foot bioimpedance (ohms) with the Sun et al. (2003,
     * NHANES III) fat-free-mass equations. Scale apps use proprietary formulas, so values can
     * differ by a few points from the manufacturer's app; trends are what matter.
     */
    fun estimate(weightKg: Double, heightCm: Double, age: Int, sex: Sex, impedanceOhm: Double): BodyComp? {
        if (weightKg <= 0 || heightCm <= 0 || age !in 10..100 || impedanceOhm !in 150.0..1500.0) return null
        val h2r = heightCm * heightCm / impedanceOhm
        val ffm = when (sex) {
            Sex.MALE -> -10.68 + 0.65 * h2r + 0.26 * weightKg + 0.02 * impedanceOhm
            Sex.FEMALE -> -9.53 + 0.69 * h2r + 0.17 * weightKg + 0.02 * impedanceOhm
        }.coerceIn(weightKg * 0.35, weightKg * 0.95)
        val fatPct = (weightKg - ffm) / weightKg * 100
        val water = ffm * 0.73 // hydration of fat-free mass
        val bone = ffm * 0.042
        val bmr = 370 + 21.6 * ffm // Katch-McArdle
        return BodyComp(
            bodyFatPct = fatPct,
            muscleKg = ffm - bone,
            waterPct = water / weightKg * 100,
            boneKg = bone,
            bmrKcal = bmr,
            metabolicAge = metabolicAge(bmr, weightKg, heightCm, age, sex),
            proteinPct = (ffm - water - bone) / weightKg * 100,
            impedance = impedanceOhm,
        )
    }

    /**
     * Age at which a typical person of the same weight, height and sex (Mifflin-St Jeor) would
     * have this BMR: higher BMR (more muscle) → younger "body age". Clamped to ±15 years.
     */
    fun metabolicAge(bmrKcal: Double, weightKg: Double, heightCm: Double, age: Int, sex: Sex): Int {
        val s = if (sex == Sex.MALE) 5.0 else -161.0
        val a = (10 * weightKg + 6.25 * heightCm + s - bmrKcal) / 5
        return a.toInt().coerceIn(maxOf(12, age - 15), age + 15)
    }

    /** General adult body-fat categories (ACE), adjusted for sex. */
    fun fatLevel(fatPct: Double, sex: Sex?): CompLevel {
        val (low, high, veryHigh) = when (sex) {
            Sex.FEMALE -> Triple(21.0, 32.0, 39.0)
            Sex.MALE -> Triple(8.0, 25.0, 30.0)
            null -> Triple(14.0, 28.0, 35.0)
        }
        return when {
            fatPct < low -> CompLevel.LOW
            fatPct < high -> CompLevel.HEALTHY
            fatPct < veryHigh -> CompLevel.HIGH
            else -> CompLevel.VERY_HIGH
        }
    }

    /** Visceral fat rating as used by most consumer scales: 1–9 healthy, 10–14 high, 15+ very high. */
    fun visceralLevel(rating: Double): CompLevel = when {
        rating < 10 -> CompLevel.HEALTHY
        rating < 15 -> CompLevel.HIGH
        else -> CompLevel.VERY_HIGH
    }

    fun waterLevel(waterPct: Double, sex: Sex?): CompLevel {
        val low = if (sex == Sex.MALE) 50.0 else 45.0
        val high = if (sex == Sex.MALE) 65.0 else 60.0
        return when {
            waterPct < low -> CompLevel.LOW
            waterPct <= high -> CompLevel.HEALTHY
            else -> CompLevel.HIGH
        }
    }

    data class LossSplit(val totalKg: Double, val fatKg: Double, val leanKg: Double) {
        /** Share of the loss that was fat (0..1); ≥ 0.75 is considered good on GLP-1. */
        val fatShare: Double get() = if (totalKg > 0) (fatKg / totalKg).coerceIn(0.0, 1.0) else 0.0
    }

    /** How much of the weight lost between two readings was fat vs lean mass. */
    fun lossSplit(startKg: Double, startFatPct: Double, endKg: Double, endFatPct: Double): LossSplit? {
        val total = startKg - endKg
        if (total <= 0.2) return null
        val fat = startKg * startFatPct / 100 - endKg * endFatPct / 100
        return LossSplit(total, fat, total - fat)
    }

    /** Some QN firmwares report impedance in tenths of ohm; bring it into a plausible range. */
    fun normalizeImpedance(raw: Double): Double? = when {
        raw in 150.0..1500.0 -> raw
        raw / 10 in 150.0..1500.0 -> raw / 10
        else -> null
    }
}

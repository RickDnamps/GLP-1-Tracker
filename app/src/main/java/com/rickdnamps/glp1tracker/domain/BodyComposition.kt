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

/**
 * Metrics shown in the body-composition grid (same order as the Renpho app), with how to read
 * them from a [BodyComp]. Masses ("kg") are shown in the user's weight unit.
 */
enum class CompMetric(@StringRes val nameRes: Int, val unit: String, val decimals: Int, val read: (BodyComp, Double) -> Double?) {
    FAT(R.string.comp_fat, "%", 1, { c, _ -> c.bodyFatPct }),
    FAT_MASS(R.string.comp_fat_mass, "kg", 1, { c, w -> c.bodyFatPct?.let { w * it / 100 } }),
    LEAN(R.string.comp_lean, "kg", 1, { c, w -> c.bodyFatPct?.let { w * (1 - it / 100) } }),
    SUBCUTANEOUS(R.string.comp_subcutaneous, "%", 1, { c, _ -> c.subcutaneousFatPct }),
    VISCERAL(R.string.comp_visceral, "", 0, { c, _ -> c.visceralFat }),
    WATER(R.string.comp_water, "%", 1, { c, _ -> c.waterPct }),
    SKELETAL(R.string.comp_skeletal, "%", 1, { c, _ -> c.skeletalMusclePct }),
    MUSCLE(R.string.comp_muscle, "kg", 1, { c, _ -> c.muscleKg }),
    BONE(R.string.comp_bone, "kg", 1, { c, _ -> c.boneKg }),
    PROTEIN(R.string.comp_protein, "%", 1, { c, _ -> c.proteinPct }),
    BMR(R.string.comp_bmr, "kcal", 0, { c, _ -> c.bmrKcal }),
    METABOLIC_AGE(R.string.comp_metabolic_age, "", 0, { c, _ -> c.metabolicAge?.toDouble() });

    val isMass: Boolean get() = unit == "kg"
}

enum class CompLevel { LOW, HEALTHY, HIGH, VERY_HIGH, EXCELLENT }

object BodyCompositionMath {

    /**
     * Estimates composition from foot-to-foot bioimpedance (ohms) with the Sun et al. (2003,
     * NHANES III) fat-free-mass equations. Scale apps use proprietary formulas, so values can
     * differ by a few points from the manufacturer's app; trends are what matter.
     */
    fun estimate(weightKg: Double, heightCm: Double, age: Int, sex: Sex, impedanceOhm: Double): BodyComp? {
        if (weightKg <= 0 || heightCm <= 0 || age !in 10..100 || impedanceOhm !in 150.0..1500.0) return null
        val h2r = heightCm * heightCm / impedanceOhm
        val ffm = (
            when (sex) {
                Sex.MALE -> -10.68 + 0.65 * h2r + 0.26 * weightKg + 0.02 * impedanceOhm
                Sex.FEMALE -> -9.53 + 0.69 * h2r + 0.17 * weightKg + 0.02 * impedanceOhm
            } * FFM_CALIBRATION
            ).coerceIn(weightKg * 0.35, weightKg * 0.95)
        return fromFatFreeMass(weightKg, ffm, age, sex).copy(impedance = impedanceOhm)
    }

    /**
     * Fills what a reading lacks from its body fat %: Health Connect only carries weight, fat,
     * lean, bone, water and BMR, and Renpho shares just the fat there. Values already present win;
     * visceral fat cannot be derived. Metabolic age needs [age] and [sex].
     */
    fun complete(c: BodyComp, weightKg: Double, age: Int?, sex: Sex?): BodyComp {
        val fat = c.bodyFatPct?.takeIf { it in 2.0..75.0 } ?: return c
        if (weightKg <= 0) return c
        return c.mergedWith(fromFatFreeMass(weightKg, weightKg * (1 - fat / 100), age, sex).copy(bodyFatPct = fat))
    }

    /** Every value from the fat-free mass, with the shares the Renpho app reports for the same reading. */
    private fun fromFatFreeMass(weightKg: Double, ffm: Double, age: Int?, sex: Sex?): BodyComp {
        val fatPct = (weightKg - ffm) / weightKg * 100
        val water = ffm * 0.722
        val bone = ffm * 0.05
        val muscle = ffm - bone
        val bmr = 370 + 21.6 * ffm // Katch-McArdle (Renpho uses it too)
        return BodyComp(
            bodyFatPct = fatPct,
            muscleKg = muscle,
            waterPct = water / weightKg * 100,
            boneKg = bone,
            bmrKcal = bmr,
            metabolicAge = if (age != null && age in 10..100 && sex != null) metabolicAge(fatPct, age, sex) else null,
            proteinPct = (muscle - water) / weightKg * 100,
            skeletalMusclePct = muscle * 0.68 / weightKg * 100,
            subcutaneousFatPct = fatPct * 0.842,
        )
    }

    /**
     * Sun's equation gives ~5 % less fat-free mass than the Renpho app for the same Renpho
     * ES-CS20M reading (138 kg, 188 cm, 46 y, man: Renpho 38.6 % fat, 2201 kcal, 44.3 % water,
     * 9.4 lb bone, 14.0 % protein, 39.7 % skeletal muscle, metabolic age 53).
     */
    private const val FFM_CALIBRATION = 1.053

    /**
     * "Body age": real age moved by how far body fat is from the healthy mid-range for that age
     * and sex (Gallagher 2000: men ≈ 10 + 0.13 × age %, women ≈ 24 + 0.075 × age %), about one
     * year per 3 points of fat (matches the Renpho app on the reference reading above). Leaner than the norm → younger. Clamped to −12 / +15 years.
     * (Solving Mifflin-St Jeor for the age was far too sensitive: ±100 kcal = ±20 years.)
     */
    fun metabolicAge(fatPct: Double, age: Int, sex: Sex): Int {
        val reference = when (sex) {
            Sex.MALE -> 10.0 + 0.13 * age
            Sex.FEMALE -> 24.0 + 0.075 * age
        }
        val a = age + 0.31 * (fatPct - reference)
        return kotlin.math.round(a).toInt().coerceIn(maxOf(12, age - 12), age + 15)
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

    /** WHO adult BMI categories: < 18.5 low, 25–30 overweight, 30+ obesity. */
    fun bmiLevel(bmi: Double): CompLevel = when {
        bmi < 18.5 -> CompLevel.LOW
        bmi < 25 -> CompLevel.HEALTHY
        bmi < 30 -> CompLevel.HIGH
        else -> CompLevel.VERY_HIGH
    }

    // Reference ranges used by consumer scales (Renpho / Xiaomi style); indicative only.

    fun subcutaneousLevel(pct: Double, sex: Sex?): CompLevel {
        val (low, high) = if (sex == Sex.FEMALE) 18.5 to 26.7 else 8.6 to 16.7
        return when {
            pct < low -> CompLevel.LOW
            pct <= high -> CompLevel.HEALTHY
            else -> CompLevel.HIGH
        }
    }

    fun skeletalMuscleLevel(pct: Double, sex: Sex?): CompLevel? {
        val (low, high) = when (sex ?: return null) {
            Sex.FEMALE -> 40.0 to 50.0
            Sex.MALE -> 49.0 to 59.0
        }
        return when {
            pct < low -> CompLevel.LOW
            pct <= high -> CompLevel.HEALTHY
            else -> CompLevel.EXCELLENT
        }
    }

    fun muscleMassLevel(kg: Double, heightCm: Double, sex: Sex?): CompLevel? {
        if (heightCm <= 0) return null
        val (low, high) = when (sex ?: return null) {
            Sex.FEMALE -> when {
                heightCm < 150 -> 29.1 to 34.7
                heightCm < 160 -> 32.9 to 37.5
                else -> 36.5 to 42.5
            }
            Sex.MALE -> when {
                heightCm < 160 -> 38.5 to 46.5
                heightCm < 170 -> 44.0 to 52.4
                else -> 49.4 to 59.4
            }
        }
        return when {
            kg < low -> CompLevel.LOW
            kg <= high -> CompLevel.HEALTHY
            else -> CompLevel.EXCELLENT
        }
    }

    fun boneLevel(kg: Double, weightKg: Double, sex: Sex?): CompLevel {
        val standard = if (sex == Sex.FEMALE) {
            when { weightKg < 45 -> 1.8; weightKg < 60 -> 2.2; else -> 2.5 }
        } else {
            when { weightKg < 60 -> 2.5; weightKg < 75 -> 2.9; else -> 3.2 }
        }
        return if (kg < standard - 0.1) CompLevel.LOW else CompLevel.HEALTHY
    }

    fun proteinLevel(pct: Double): CompLevel = if (pct < 16) CompLevel.LOW else CompLevel.HEALTHY

    /** Body age above the real age is "high", below it "excellent". */
    fun metabolicAgeLevel(metabolicAge: Int, age: Int): CompLevel = when {
        metabolicAge > age -> CompLevel.HIGH
        metabolicAge < age -> CompLevel.EXCELLENT
        else -> CompLevel.HEALTHY
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

package com.rickdnamps.glp1tracker.domain

import androidx.annotation.StringRes
import com.rickdnamps.glp1tracker.R

enum class InjectionSite(@StringRes val nameRes: Int) {
    ABDOMEN_LEFT(R.string.site_abdomen_left),
    ABDOMEN_RIGHT(R.string.site_abdomen_right),
    THIGH_LEFT(R.string.site_thigh_left),
    THIGH_RIGHT(R.string.site_thigh_right),
    ARM_LEFT(R.string.site_arm_left),
    ARM_RIGHT(R.string.site_arm_right),
    NONE(R.string.site_none);

    companion object {
        val injectable = entries.filter { it != NONE }

        fun fromName(name: String?): InjectionSite =
            entries.firstOrNull { it.name == name } ?: NONE

        /**
         * Suggests the next site for rotation: a never-used site first, otherwise the
         * one whose last use is the oldest. [lastUse] maps sites to their last epoch millis.
         */
        fun suggest(lastUse: Map<InjectionSite, Long>): InjectionSite =
            injectable.minBy { lastUse[it] ?: Long.MIN_VALUE }
    }
}

enum class SideEffectType(@StringRes val nameRes: Int) {
    NAUSEA(R.string.se_nausea),
    VOMITING(R.string.se_vomiting),
    DIARRHEA(R.string.se_diarrhea),
    CONSTIPATION(R.string.se_constipation),
    HEARTBURN(R.string.se_heartburn),
    BURPING(R.string.se_burping),
    BLOATING(R.string.se_bloating),
    STOMACH_PAIN(R.string.se_stomach_pain),
    FATIGUE(R.string.se_fatigue),
    HEADACHE(R.string.se_headache),
    DIZZINESS(R.string.se_dizziness),
    INJECTION_SITE(R.string.se_injection_site),
    HAIR_LOSS(R.string.se_hair_loss),
    // Often reported on Reddit but missing from the labels (Sehgal et al., 2026).
    CHILLS(R.string.se_chills),
    HOT_FLASHES(R.string.se_hot_flashes),
    MENSTRUAL_CHANGES(R.string.se_menstrual),
    OTHER(R.string.se_other);

    companion object {
        fun fromName(name: String?): SideEffectType =
            entries.firstOrNull { it.name == name } ?: OTHER
    }
}

enum class Severity(val level: Int, @StringRes val nameRes: Int) {
    MILD(1, R.string.severity_mild),
    MODERATE(2, R.string.severity_moderate),
    SEVERE(3, R.string.severity_severe);

    companion object {
        fun fromLevel(level: Int): Severity = entries.firstOrNull { it.level == level } ?: MILD
    }
}

enum class WeightUnit(@StringRes val symbolRes: Int) {
    KG(R.string.unit_kg),
    LB(R.string.unit_lb);

    fun fromKg(kg: Double): Double = if (this == KG) kg else kg * LB_PER_KG
    fun toKg(value: Double): Double = if (this == KG) value else value / LB_PER_KG

    companion object {
        const val LB_PER_KG = 2.20462262
        fun fromName(name: String?): WeightUnit = entries.firstOrNull { it.name == name } ?: KG
    }
}

/** Unit for height and body measurements; values are always stored in cm. */
enum class LengthUnit(@StringRes val symbolRes: Int) {
    CM(R.string.unit_cm),
    IN(R.string.unit_in);

    fun fromCm(cm: Double): Double = if (this == CM) cm else cm / CM_PER_INCH
    fun toCm(value: Double): Double = if (this == CM) value else value * CM_PER_INCH

    companion object {
        const val CM_PER_INCH = 2.54

        fun fromValue(v: Int): LengthUnit = if (v == 1) IN else CM

        /** Splits a height in cm into whole feet and inches (rounded to the nearest inch). */
        fun feetInches(cm: Double): Pair<Int, Int> {
            val totalIn = kotlin.math.round(cm / CM_PER_INCH).toInt()
            return totalIn / 12 to totalIn % 12
        }

        fun fromFeetInches(feet: Int, inches: Double): Double = (feet * 12 + inches) * CM_PER_INCH
    }
}

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

package com.rickdnamps.glp1tracker.domain

import androidx.annotation.StringRes
import com.rickdnamps.glp1tracker.R

/** Vital signs and lab values the user can log; [unit] is what we store and display. */
enum class VitalType(@StringRes val nameRes: Int, val unit: String, val hasSecondValue: Boolean = false) {
    BLOOD_PRESSURE(R.string.vital_bp, "mmHg", hasSecondValue = true),
    HEART_RATE(R.string.vital_hr, "bpm"),
    GLUCOSE(R.string.vital_glucose, "mmol/L"),
    HBA1C(R.string.vital_hba1c, "%"),
    LDL(R.string.vital_ldl, "mmol/L"),
    TRIGLYCERIDES(R.string.vital_tg, "mmol/L");

    companion object {
        fun fromName(name: String?) = entries.firstOrNull { it.name == name } ?: HEART_RATE
    }
}

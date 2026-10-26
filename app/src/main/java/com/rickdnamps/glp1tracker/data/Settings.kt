package com.rickdnamps.glp1tracker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.rickdnamps.glp1tracker.domain.Medication
import com.rickdnamps.glp1tracker.domain.Pharmacokinetics
import com.rickdnamps.glp1tracker.domain.WeightUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalTime

data class UserSettings(
    val onboardingDone: Boolean = false,
    val name: String = "",
    val weightUnit: WeightUnit = WeightUnit.KG,
    val heightCm: Double = 0.0,
    /** 0 means "use the first weight entry". */
    val startWeightKg: Double = 0.0,
    val goalWeightKg: Double = 0.0,
    val medication: Medication = Medication.OZEMPIC,
    val doseMg: Double = 0.25,
    val intervalDays: Int = 7,
    val injectionDay: DayOfWeek = DayOfWeek.MONDAY,
    val reminderHour: Int = 9,
    val reminderMinute: Int = 0,
    val remindersEnabled: Boolean = true,
    val customMedName: String = "",
    val customHalfLifeHours: Double = 168.0,
    val waterGoalMl: Int = 2000,
    val proteinGoalG: Int = 80,
    val penTrackingEnabled: Boolean = false,
    val penDosesTotal: Int = 4,
    val penDosesRemaining: Int = 4,
    val scaleAddress: String = "",
    val scaleName: String = "",
    val healthConnectAutoImport: Boolean = false,
    val healthConnectLastImport: Long = 0,
) {
    val reminderTime: LocalTime get() = LocalTime.of(reminderHour, reminderMinute)

    fun pkParams(medication: Medication): Pharmacokinetics.Params =
        if (medication == Medication.OTHER) {
            Pharmacokinetics.Params(customHalfLifeHours, medication.molecule.tmaxHours)
        } else {
            Pharmacokinetics.Params(medication.molecule.halfLifeHours, medication.molecule.tmaxHours)
        }
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(context: Context) {
    private val store = context.applicationContext.dataStore

    private object Keys {
        val onboardingDone = booleanPreferencesKey("onboarding_done")
        val name = stringPreferencesKey("name")
        val weightUnit = stringPreferencesKey("weight_unit")
        val heightCm = doublePreferencesKey("height_cm")
        val startWeightKg = doublePreferencesKey("start_weight_kg")
        val goalWeightKg = doublePreferencesKey("goal_weight_kg")
        val medication = stringPreferencesKey("medication")
        val doseMg = doublePreferencesKey("dose_mg")
        val intervalDays = intPreferencesKey("interval_days")
        val injectionDay = intPreferencesKey("injection_day")
        val reminderHour = intPreferencesKey("reminder_hour")
        val reminderMinute = intPreferencesKey("reminder_minute")
        val remindersEnabled = booleanPreferencesKey("reminders_enabled")
        val customMedName = stringPreferencesKey("custom_med_name")
        val customHalfLifeHours = doublePreferencesKey("custom_half_life_hours")
        val waterGoalMl = intPreferencesKey("water_goal_ml")
        val proteinGoalG = intPreferencesKey("protein_goal_g")
        val penTrackingEnabled = booleanPreferencesKey("pen_tracking_enabled")
        val penDosesTotal = intPreferencesKey("pen_doses_total")
        val penDosesRemaining = intPreferencesKey("pen_doses_remaining")
        val scaleAddress = stringPreferencesKey("scale_address")
        val scaleName = stringPreferencesKey("scale_name")
        val healthConnectAutoImport = booleanPreferencesKey("hc_auto_import")
        val healthConnectLastImport = longPreferencesKey("hc_last_import")
    }

    val settings: Flow<UserSettings> = store.data.map(::read)

    private fun read(p: Preferences): UserSettings {
        val d = UserSettings()
        return UserSettings(
            onboardingDone = p[Keys.onboardingDone] ?: d.onboardingDone,
            name = p[Keys.name] ?: d.name,
            weightUnit = WeightUnit.fromName(p[Keys.weightUnit]),
            heightCm = p[Keys.heightCm] ?: d.heightCm,
            startWeightKg = p[Keys.startWeightKg] ?: d.startWeightKg,
            goalWeightKg = p[Keys.goalWeightKg] ?: d.goalWeightKg,
            medication = Medication.fromName(p[Keys.medication]),
            doseMg = p[Keys.doseMg] ?: d.doseMg,
            intervalDays = p[Keys.intervalDays] ?: d.intervalDays,
            injectionDay = DayOfWeek.of((p[Keys.injectionDay] ?: d.injectionDay.value).coerceIn(1, 7)),
            reminderHour = p[Keys.reminderHour] ?: d.reminderHour,
            reminderMinute = p[Keys.reminderMinute] ?: d.reminderMinute,
            remindersEnabled = p[Keys.remindersEnabled] ?: d.remindersEnabled,
            customMedName = p[Keys.customMedName] ?: d.customMedName,
            customHalfLifeHours = p[Keys.customHalfLifeHours] ?: d.customHalfLifeHours,
            waterGoalMl = p[Keys.waterGoalMl] ?: d.waterGoalMl,
            proteinGoalG = p[Keys.proteinGoalG] ?: d.proteinGoalG,
            penTrackingEnabled = p[Keys.penTrackingEnabled] ?: d.penTrackingEnabled,
            penDosesTotal = p[Keys.penDosesTotal] ?: d.penDosesTotal,
            penDosesRemaining = p[Keys.penDosesRemaining] ?: d.penDosesRemaining,
            scaleAddress = p[Keys.scaleAddress] ?: d.scaleAddress,
            scaleName = p[Keys.scaleName] ?: d.scaleName,
            healthConnectAutoImport = p[Keys.healthConnectAutoImport] ?: d.healthConnectAutoImport,
            healthConnectLastImport = p[Keys.healthConnectLastImport] ?: d.healthConnectLastImport,
        )
    }

    suspend fun current(): UserSettings = settings.first()

    suspend fun update(transform: (UserSettings) -> UserSettings) {
        store.edit { prefs -> write(prefs, transform(read(prefs))) }
    }

    private fun write(p: MutablePreferences, s: UserSettings) {
        p[Keys.onboardingDone] = s.onboardingDone
        p[Keys.name] = s.name
        p[Keys.weightUnit] = s.weightUnit.name
        p[Keys.heightCm] = s.heightCm
        p[Keys.startWeightKg] = s.startWeightKg
        p[Keys.goalWeightKg] = s.goalWeightKg
        p[Keys.medication] = s.medication.name
        p[Keys.doseMg] = s.doseMg
        p[Keys.intervalDays] = s.intervalDays
        p[Keys.injectionDay] = s.injectionDay.value
        p[Keys.reminderHour] = s.reminderHour
        p[Keys.reminderMinute] = s.reminderMinute
        p[Keys.remindersEnabled] = s.remindersEnabled
        p[Keys.customMedName] = s.customMedName
        p[Keys.customHalfLifeHours] = s.customHalfLifeHours
        p[Keys.waterGoalMl] = s.waterGoalMl
        p[Keys.proteinGoalG] = s.proteinGoalG
        p[Keys.penTrackingEnabled] = s.penTrackingEnabled
        p[Keys.penDosesTotal] = s.penDosesTotal
        p[Keys.penDosesRemaining] = s.penDosesRemaining
        p[Keys.scaleAddress] = s.scaleAddress
        p[Keys.scaleName] = s.scaleName
        p[Keys.healthConnectAutoImport] = s.healthConnectAutoImport
        p[Keys.healthConnectLastImport] = s.healthConnectLastImport
    }
}

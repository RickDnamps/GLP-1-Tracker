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
import com.rickdnamps.glp1tracker.domain.LengthUnit
import com.rickdnamps.glp1tracker.domain.Medication
import com.rickdnamps.glp1tracker.domain.Pharmacokinetics
import com.rickdnamps.glp1tracker.domain.Sex
import com.rickdnamps.glp1tracker.domain.WeightUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.Period

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
    val titrationEnabled: Boolean = false,
    val titrationPlan: String = "",
    val titrationStartDay: Long = 0,
    val penOpenedAt: Long = 0,
    val penDiscardDays: Int = 0,
    val stockPens: Int = 0,
    val doseEveReminder: Boolean = false,
    val weighInReminder: Boolean = false,
    val weighInDayValue: Int = 1,
    val weighInHour: Int = 7,
    val waterReminders: Boolean = false,
    val journalReminder: Boolean = false,
    val calorieGoal: Int = 1500,
    val fiberGoalG: Int = 25,
    val autoBackupUri: String = "",
    val lastAutoBackupAt: Long = 0,
    val appLock: Boolean = false,
    /** Epoch day of the last dose the user decided to skip (0 = none). */
    val skippedDueEpochDay: Long = 0,
    /** 0 = follow system, 1 = light, 2 = dark. */
    val themeMode: Int = 0,
    val accent: String = "TEAL",
    val dynamicColor: Boolean = false,
    /** Badges already celebrated with confetti (Milestones.encode). */
    val celebratedBadges: String = "",
    /** 0 = not set, 1 = male, 2 = female (used for lean-mass estimates). */
    val sex: Int = 0,
    val birthYear: Int = 0,
    /** Birth month (1-12) and day (1-31); 0 = only the year is known (older versions). */
    val birthMonth: Int = 0,
    val birthDay: Int = 0,
    /** 0 = cm, 1 = feet/inches (height) and inches (body measurements). */
    val lengthUnitValue: Int = 0,
) {
    val lengthUnit: LengthUnit get() = LengthUnit.fromValue(lengthUnitValue)

    val sexOrNull: Sex? get() = when (sex) { 1 -> Sex.MALE; 2 -> Sex.FEMALE; else -> null }
    /** Full date of birth when known (month and day set), else null. */
    val birthDateOrNull: LocalDate?
        get() = if (birthYear in 1900..2100 && birthMonth in 1..12 && birthDay in 1..31) {
            runCatching { LocalDate.of(birthYear, birthMonth, birthDay) }.getOrNull()
        } else null

    val ageOrNull: Int?
        get() = birthDateOrNull?.let { Period.between(it, LocalDate.now()).years }
            ?: birthYear.takeIf { it in 1900..2100 }?.let { LocalDate.now().year - it }

    val skippedThrough: LocalDate? get() = skippedDueEpochDay.takeIf { it > 0 }?.let(LocalDate::ofEpochDay)

    val weighInDay: DayOfWeek get() = DayOfWeek.of(weighInDayValue.coerceIn(1, 7))

    val reminderTime: LocalTime get() = LocalTime.of(reminderHour, reminderMinute)

    fun pkParams(medication: Medication): Pharmacokinetics.Params =
        if (medication == Medication.OTHER) {
            Pharmacokinetics.Params(customHalfLifeHours, medication.molecule.tmaxHours)
        } else if (medication.isOral) {
            // Oral semaglutide: absorbed within ~1 h but only ~1 % reaches the blood.
            Pharmacokinetics.Params(medication.molecule.halfLifeHours, 1.0, bioavailability = 0.01)
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
        val titrationEnabled = booleanPreferencesKey("titration_enabled")
        val titrationPlan = stringPreferencesKey("titration_plan")
        val titrationStartDay = longPreferencesKey("titration_start_day")
        val penOpenedAt = longPreferencesKey("pen_opened_at")
        val penDiscardDays = intPreferencesKey("pen_discard_days")
        val stockPens = intPreferencesKey("stock_pens")
        val doseEveReminder = booleanPreferencesKey("dose_eve_reminder")
        val weighInReminder = booleanPreferencesKey("weigh_in_reminder")
        val weighInDayValue = intPreferencesKey("weigh_in_day")
        val weighInHour = intPreferencesKey("weigh_in_hour")
        val waterReminders = booleanPreferencesKey("water_reminders")
        val journalReminder = booleanPreferencesKey("journal_reminder")
        val calorieGoal = intPreferencesKey("calorie_goal")
        val fiberGoalG = intPreferencesKey("fiber_goal_g")
        val autoBackupUri = stringPreferencesKey("auto_backup_uri")
        val lastAutoBackupAt = longPreferencesKey("last_auto_backup_at")
        val appLock = booleanPreferencesKey("app_lock")
        val skippedDueEpochDay = longPreferencesKey("skipped_due_epoch_day")
        val themeMode = intPreferencesKey("theme_mode")
        val accent = stringPreferencesKey("accent")
        val dynamicColor = booleanPreferencesKey("dynamic_color")
        val celebratedBadges = stringPreferencesKey("celebrated_badges")
        val sex = intPreferencesKey("sex")
        val birthYear = intPreferencesKey("birth_year")
        val birthMonth = intPreferencesKey("birth_month")
        val birthDay = intPreferencesKey("birth_day")
        val lengthUnitValue = intPreferencesKey("length_unit")
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
            titrationEnabled = p[Keys.titrationEnabled] ?: d.titrationEnabled,
            titrationPlan = p[Keys.titrationPlan] ?: d.titrationPlan,
            titrationStartDay = p[Keys.titrationStartDay] ?: d.titrationStartDay,
            penOpenedAt = p[Keys.penOpenedAt] ?: d.penOpenedAt,
            penDiscardDays = p[Keys.penDiscardDays] ?: d.penDiscardDays,
            stockPens = p[Keys.stockPens] ?: d.stockPens,
            doseEveReminder = p[Keys.doseEveReminder] ?: d.doseEveReminder,
            weighInReminder = p[Keys.weighInReminder] ?: d.weighInReminder,
            weighInDayValue = p[Keys.weighInDayValue] ?: d.weighInDayValue,
            weighInHour = p[Keys.weighInHour] ?: d.weighInHour,
            waterReminders = p[Keys.waterReminders] ?: d.waterReminders,
            journalReminder = p[Keys.journalReminder] ?: d.journalReminder,
            calorieGoal = p[Keys.calorieGoal] ?: d.calorieGoal,
            fiberGoalG = p[Keys.fiberGoalG] ?: d.fiberGoalG,
            autoBackupUri = p[Keys.autoBackupUri] ?: d.autoBackupUri,
            lastAutoBackupAt = p[Keys.lastAutoBackupAt] ?: d.lastAutoBackupAt,
            appLock = p[Keys.appLock] ?: d.appLock,
            skippedDueEpochDay = p[Keys.skippedDueEpochDay] ?: d.skippedDueEpochDay,
            themeMode = p[Keys.themeMode] ?: d.themeMode,
            accent = p[Keys.accent] ?: d.accent,
            dynamicColor = p[Keys.dynamicColor] ?: d.dynamicColor,
            celebratedBadges = p[Keys.celebratedBadges] ?: d.celebratedBadges,
            sex = p[Keys.sex] ?: d.sex,
            birthYear = p[Keys.birthYear] ?: d.birthYear,
            birthMonth = p[Keys.birthMonth] ?: d.birthMonth,
            birthDay = p[Keys.birthDay] ?: d.birthDay,
            lengthUnitValue = p[Keys.lengthUnitValue] ?: d.lengthUnitValue,
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
        p[Keys.titrationEnabled] = s.titrationEnabled
        p[Keys.titrationPlan] = s.titrationPlan
        p[Keys.titrationStartDay] = s.titrationStartDay
        p[Keys.penOpenedAt] = s.penOpenedAt
        p[Keys.penDiscardDays] = s.penDiscardDays
        p[Keys.stockPens] = s.stockPens
        p[Keys.doseEveReminder] = s.doseEveReminder
        p[Keys.weighInReminder] = s.weighInReminder
        p[Keys.weighInDayValue] = s.weighInDayValue
        p[Keys.weighInHour] = s.weighInHour
        p[Keys.waterReminders] = s.waterReminders
        p[Keys.journalReminder] = s.journalReminder
        p[Keys.calorieGoal] = s.calorieGoal
        p[Keys.fiberGoalG] = s.fiberGoalG
        p[Keys.autoBackupUri] = s.autoBackupUri
        p[Keys.lastAutoBackupAt] = s.lastAutoBackupAt
        p[Keys.appLock] = s.appLock
        p[Keys.skippedDueEpochDay] = s.skippedDueEpochDay
        p[Keys.themeMode] = s.themeMode
        p[Keys.accent] = s.accent
        p[Keys.dynamicColor] = s.dynamicColor
        p[Keys.celebratedBadges] = s.celebratedBadges
        p[Keys.sex] = s.sex
        p[Keys.birthYear] = s.birthYear
        p[Keys.birthMonth] = s.birthMonth
        p[Keys.birthDay] = s.birthDay
        p[Keys.lengthUnitValue] = s.lengthUnitValue
    }
}

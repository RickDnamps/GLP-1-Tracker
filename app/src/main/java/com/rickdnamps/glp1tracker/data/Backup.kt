package com.rickdnamps.glp1tracker.data

import com.rickdnamps.glp1tracker.domain.Medication
import com.rickdnamps.glp1tracker.domain.WeightUnit
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** JSON backup / restore and CSV export (for sharing with a health professional). */
object Backup {

    data class Snapshot(
        val settings: UserSettings,
        val injections: List<InjectionEntity>,
        val weights: List<WeightEntity>,
        val sideEffects: List<SideEffectEntity>,
        val dailyLogs: List<DailyLogEntity>,
        val measurements: List<MeasurementEntity> = emptyList(),
        val foods: List<FoodEntity> = emptyList(),
        val vitals: List<VitalEntity> = emptyList(),
        val supplements: List<SupplementEntity> = emptyList(),
        val supplementLogs: List<SupplementLogEntity> = emptyList(),
    )

    private const val FORMAT_VERSION = 3

    /** JSONObject.put throws on NaN/Infinity: write those as null instead. */
    private fun num(v: Double?): Any = if (v == null || !v.isFinite()) JSONObject.NULL else v

    fun toJson(s: Snapshot): String {
        val root = JSONObject()
        root.put("format", "glp1tracker")
        root.put("version", FORMAT_VERSION)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("settings", settingsToJson(s.settings))
        root.put("injections", JSONArray().apply {
            s.injections.forEach {
                put(JSONObject().put("id", it.id).put("timestamp", it.timestamp).put("medication", it.medication)
                    .put("doseMg", num(it.doseMg)).put("site", it.site).put("notes", it.notes).put("comfort", it.comfort))
            }
        })
        root.put("weights", JSONArray().apply {
            s.weights.forEach {
                put(JSONObject().put("id", it.id).put("timestamp", it.timestamp).put("weightKg", num(it.weightKg))
                    .put("waistCm", num(it.waistCm)).put("notes", it.notes)
                    .put("bodyFatPct", num(it.bodyFatPct))
                    .put("muscleKg", num(it.muscleKg))
                    .put("waterPct", num(it.waterPct))
                    .put("boneKg", num(it.boneKg))
                    .put("visceralFat", num(it.visceralFat))
                    .put("bmrKcal", num(it.bmrKcal))
                    .put("metabolicAge", it.metabolicAge ?: JSONObject.NULL)
                    .put("proteinPct", num(it.proteinPct))
                    .put("skeletalMusclePct", num(it.skeletalMusclePct))
                    .put("subcutaneousFatPct", num(it.subcutaneousFatPct))
                    .put("impedance", num(it.impedance)))
            }
        })
        root.put("sideEffects", JSONArray().apply {
            s.sideEffects.forEach {
                put(JSONObject().put("id", it.id).put("timestamp", it.timestamp).put("type", it.type)
                    .put("severity", it.severity).put("notes", it.notes))
            }
        })
        root.put("dailyLogs", JSONArray().apply {
            s.dailyLogs.forEach {
                put(JSONObject().put("date", it.date).put("waterMl", it.waterMl).put("proteinG", it.proteinG)
                    .put("appetite", it.appetite).put("mood", it.mood).put("notes", it.notes)
                    .put("energy", it.energy).put("sleep", it.sleep).put("foodNoise", it.foodNoise)
                    .put("bowel", it.bowel).put("exerciseMin", it.exerciseMin).put("strength", it.strength))
            }
        })
        root.put("measurements", JSONArray().apply {
            s.measurements.forEach {
                put(JSONObject().put("id", it.id).put("timestamp", it.timestamp)
                    .put("waistCm", num(it.waistCm)).put("hipsCm", num(it.hipsCm))
                    .put("chestCm", num(it.chestCm)).put("armCm", num(it.armCm))
                    .put("thighCm", num(it.thighCm)).put("neckCm", num(it.neckCm))
                    .put("notes", it.notes))
            }
        })
        root.put("foods", JSONArray().apply {
            s.foods.forEach {
                put(JSONObject().put("id", it.id).put("timestamp", it.timestamp).put("name", it.name)
                    .put("calories", it.calories).put("proteinG", it.proteinG).put("fiberG", it.fiberG)
                    .put("favorite", it.favorite))
            }
        })
        root.put("vitals", JSONArray().apply {
            s.vitals.forEach {
                put(JSONObject().put("id", it.id).put("timestamp", it.timestamp).put("type", it.type)
                    .put("value", num(it.value)).put("value2", num(it.value2)).put("notes", it.notes))
            }
        })
        root.put("supplements", JSONArray().apply {
            s.supplements.forEach {
                put(JSONObject().put("id", it.id).put("name", it.name).put("asNeeded", it.asNeeded).put("archived", it.archived))
            }
        })
        root.put("supplementLogs", JSONArray().apply {
            s.supplementLogs.forEach { put(JSONObject().put("id", it.id).put("supplementId", it.supplementId).put("timestamp", it.timestamp)) }
        })
        return root.toString(2)
    }

    /** @throws IllegalArgumentException if the file is not a backup of this app. */
    fun fromJson(text: String): Snapshot {
        val root = JSONObject(text)
        require(root.optString("format") == "glp1tracker") { "Not a GLP-1 Tracker backup" }
        return Snapshot(
            settings = settingsFromJson(root.optJSONObject("settings") ?: JSONObject()),
            injections = root.optJSONArray("injections").objects().map {
                InjectionEntity(
                    id = it.optLong("id"),
                    timestamp = it.getLong("timestamp"),
                    medication = it.optString("medication", Medication.OZEMPIC.name),
                    doseMg = it.optDouble("doseMg", 0.0),
                    site = it.optString("site"),
                    notes = it.optString("notes"),
                    comfort = it.optInt("comfort", 0).takeIf { c -> c in 0..5 } ?: 0,
                )
            },
            weights = root.optJSONArray("weights").objects().map {
                WeightEntity(
                    id = it.optLong("id"),
                    timestamp = it.getLong("timestamp"),
                    weightKg = it.getDouble("weightKg"),
                    waistCm = if (it.isNull("waistCm")) null else it.optDouble("waistCm"),
                    notes = it.optString("notes"),
                    bodyFatPct = it.optNullableDouble("bodyFatPct"),
                    muscleKg = it.optNullableDouble("muscleKg"),
                    waterPct = it.optNullableDouble("waterPct"),
                    boneKg = it.optNullableDouble("boneKg"),
                    visceralFat = it.optNullableDouble("visceralFat"),
                    bmrKcal = it.optNullableDouble("bmrKcal"),
                    metabolicAge = if (it.isNull("metabolicAge")) null else it.optInt("metabolicAge"),
                    proteinPct = it.optNullableDouble("proteinPct"),
                    skeletalMusclePct = it.optNullableDouble("skeletalMusclePct"),
                    subcutaneousFatPct = it.optNullableDouble("subcutaneousFatPct"),
                    impedance = it.optNullableDouble("impedance"),
                )
            },
            sideEffects = root.optJSONArray("sideEffects").objects().map {
                SideEffectEntity(
                    id = it.optLong("id"),
                    timestamp = it.getLong("timestamp"),
                    type = it.optString("type"),
                    severity = it.optInt("severity", 1),
                    notes = it.optString("notes"),
                )
            },
            dailyLogs = root.optJSONArray("dailyLogs").objects().map {
                DailyLogEntity(
                    date = it.getString("date"),
                    waterMl = it.optInt("waterMl"),
                    proteinG = it.optInt("proteinG"),
                    appetite = it.optInt("appetite"),
                    mood = it.optInt("mood"),
                    notes = it.optString("notes"),
                    energy = it.optInt("energy"),
                    sleep = it.optInt("sleep"),
                    foodNoise = it.optInt("foodNoise"),
                    bowel = it.optInt("bowel", -1),
                    exerciseMin = it.optInt("exerciseMin"),
                    strength = it.optBoolean("strength"),
                )
            },
            // v1 backups only had the waist on weigh-ins: rebuild measurements from it.
            measurements = if (!root.has("measurements")) legacyWaist(root) else root.optJSONArray("measurements").objects().map {
                MeasurementEntity(
                    id = it.optLong("id"),
                    timestamp = it.getLong("timestamp"),
                    waistCm = it.optNullableDouble("waistCm"),
                    hipsCm = it.optNullableDouble("hipsCm"),
                    chestCm = it.optNullableDouble("chestCm"),
                    armCm = it.optNullableDouble("armCm"),
                    thighCm = it.optNullableDouble("thighCm"),
                    neckCm = it.optNullableDouble("neckCm"),
                    notes = it.optString("notes"),
                )
            },
            supplements = root.optJSONArray("supplements").objects().map {
                SupplementEntity(
                    id = it.optLong("id"),
                    name = it.optString("name"),
                    asNeeded = it.optBoolean("asNeeded", false),
                    archived = it.optBoolean("archived", false),
                )
            }.filter { it.name.isNotBlank() },
            supplementLogs = root.optJSONArray("supplementLogs").objects().map {
                SupplementLogEntity(id = it.optLong("id"), supplementId = it.optLong("supplementId"), timestamp = it.getLong("timestamp"))
            },
            vitals = root.optJSONArray("vitals").objects().map {
                VitalEntity(
                    id = it.optLong("id"),
                    timestamp = it.getLong("timestamp"),
                    type = it.optString("type"),
                    value = it.optDouble("value", 0.0),
                    value2 = it.optNullableDouble("value2"),
                    notes = it.optString("notes"),
                )
            },
            foods = root.optJSONArray("foods").objects().map {
                FoodEntity(
                    id = it.optLong("id"),
                    timestamp = it.getLong("timestamp"),
                    name = it.optString("name"),
                    calories = it.optInt("calories"),
                    proteinG = it.optDouble("proteinG", 0.0),
                    fiberG = it.optDouble("fiberG", 0.0),
                    favorite = it.optBoolean("favorite"),
                )
            },
        )
    }

    private fun legacyWaist(root: JSONObject): List<MeasurementEntity> =
        root.optJSONArray("weights").objects()
            .filter { !it.isNull("waistCm") && it.has("timestamp") }
            .map { MeasurementEntity(timestamp = it.getLong("timestamp"), waistCm = it.optDouble("waistCm")) }

    private fun JSONObject.optNullableDouble(key: String): Double? = if (isNull(key)) null else optDouble(key)

    private fun JSONArray?.objects(): List<JSONObject> =
        if (this == null) emptyList() else (0 until length()).map { getJSONObject(it) }

    private fun settingsToJson(s: UserSettings) = JSONObject().apply {
        put("name", s.name)
        put("weightUnit", s.weightUnit.name)
        put("heightCm", s.heightCm)
        put("startWeightKg", s.startWeightKg)
        put("goalWeightKg", s.goalWeightKg)
        put("medication", s.medication.name)
        put("doseMg", s.doseMg)
        put("intervalDays", s.intervalDays)
        put("injectionDay", s.injectionDay.value)
        put("reminderHour", s.reminderHour)
        put("reminderMinute", s.reminderMinute)
        put("remindersEnabled", s.remindersEnabled)
        put("customMedName", s.customMedName)
        put("customHalfLifeHours", s.customHalfLifeHours)
        put("waterGoalMl", s.waterGoalMl)
        put("stepGoal", s.stepGoal)
        put("proteinGoalG", s.proteinGoalG)
        put("penTrackingEnabled", s.penTrackingEnabled)
        put("penDosesTotal", s.penDosesTotal)
        put("penDosesRemaining", s.penDosesRemaining)
        put("titrationEnabled", s.titrationEnabled)
        put("titrationPlan", s.titrationPlan)
        put("titrationStartDay", s.titrationStartDay)
        put("penOpenedAt", s.penOpenedAt)
        put("penDiscardDays", s.penDiscardDays)
        put("stockPens", s.stockPens)
        put("doseEveReminder", s.doseEveReminder)
        put("weighInReminder", s.weighInReminder)
        put("weighInDayValue", s.weighInDayValue)
        put("weighInHour", s.weighInHour)
        put("waterReminders", s.waterReminders)
        put("journalReminder", s.journalReminder)
        put("calorieGoal", s.calorieGoal)
        put("fiberGoalG", s.fiberGoalG)
        put("appLock", s.appLock)
        put("skippedDueEpochDay", s.skippedDueEpochDay)
        put("themeMode", s.themeMode)
        put("accent", s.accent)
        put("dynamicColor", s.dynamicColor)
        put("celebratedBadges", s.celebratedBadges)
        put("sex", s.sex)
        put("birthYear", s.birthYear)
        put("birthMonth", s.birthMonth)
        put("birthDay", s.birthDay)
        put("lengthUnitValue", s.lengthUnitValue)
    }

    private fun settingsFromJson(o: JSONObject): UserSettings {
        val d = UserSettings()
        return UserSettings(
            onboardingDone = true,
            name = o.optString("name", d.name),
            weightUnit = WeightUnit.fromName(o.optString("weightUnit")),
            heightCm = o.optDouble("heightCm", d.heightCm),
            startWeightKg = o.optDouble("startWeightKg", d.startWeightKg),
            goalWeightKg = o.optDouble("goalWeightKg", d.goalWeightKg),
            medication = Medication.fromName(o.optString("medication")),
            doseMg = o.optDouble("doseMg", d.doseMg),
            intervalDays = o.optInt("intervalDays", d.intervalDays).coerceIn(1, 60),
            injectionDay = DayOfWeek.of(o.optInt("injectionDay", d.injectionDay.value).coerceIn(1, 7)),
            reminderHour = o.optInt("reminderHour", d.reminderHour).coerceIn(0, 23),
            reminderMinute = o.optInt("reminderMinute", d.reminderMinute).coerceIn(0, 59),
            remindersEnabled = o.optBoolean("remindersEnabled", d.remindersEnabled),
            customMedName = o.optString("customMedName", d.customMedName),
            customHalfLifeHours = o.optDouble("customHalfLifeHours", d.customHalfLifeHours),
            waterGoalMl = o.optInt("waterGoalMl", d.waterGoalMl),
            stepGoal = o.optInt("stepGoal", d.stepGoal).takeIf { it in 500..100_000 } ?: d.stepGoal,
            proteinGoalG = o.optInt("proteinGoalG", d.proteinGoalG),
            penTrackingEnabled = o.optBoolean("penTrackingEnabled", d.penTrackingEnabled),
            penDosesTotal = o.optInt("penDosesTotal", d.penDosesTotal),
            penDosesRemaining = o.optInt("penDosesRemaining", d.penDosesRemaining),
            titrationEnabled = o.optBoolean("titrationEnabled", d.titrationEnabled),
            titrationPlan = o.optString("titrationPlan", d.titrationPlan),
            titrationStartDay = o.optLong("titrationStartDay", d.titrationStartDay),
            penOpenedAt = o.optLong("penOpenedAt", d.penOpenedAt),
            penDiscardDays = o.optInt("penDiscardDays", d.penDiscardDays),
            stockPens = o.optInt("stockPens", d.stockPens),
            doseEveReminder = o.optBoolean("doseEveReminder", d.doseEveReminder),
            weighInReminder = o.optBoolean("weighInReminder", d.weighInReminder),
            weighInDayValue = o.optInt("weighInDayValue", d.weighInDayValue),
            weighInHour = o.optInt("weighInHour", d.weighInHour).coerceIn(0, 23),
            waterReminders = o.optBoolean("waterReminders", d.waterReminders),
            journalReminder = o.optBoolean("journalReminder", d.journalReminder),
            calorieGoal = o.optInt("calorieGoal", d.calorieGoal),
            fiberGoalG = o.optInt("fiberGoalG", d.fiberGoalG),
            appLock = o.optBoolean("appLock", d.appLock),
            skippedDueEpochDay = o.optLong("skippedDueEpochDay", d.skippedDueEpochDay),
            themeMode = o.optInt("themeMode", d.themeMode),
            accent = o.optString("accent", d.accent),
            dynamicColor = o.optBoolean("dynamicColor", d.dynamicColor),
            celebratedBadges = o.optString("celebratedBadges", d.celebratedBadges),
            sex = o.optInt("sex", d.sex),
            birthYear = o.optInt("birthYear", d.birthYear),
            birthMonth = o.optInt("birthMonth", d.birthMonth).takeIf { it in 0..12 } ?: 0,
            birthDay = o.optInt("birthDay", d.birthDay).takeIf { it in 0..31 } ?: 0,
            lengthUnitValue = o.optInt("lengthUnitValue", d.lengthUnitValue),
        )
    }

    private val csvDate = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    private fun csvTime(millis: Long) =
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(csvDate)

    private fun csv(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' }) "\"" + value.replace("\"", "\"\"") + "\"" else value

    /** Human readable multi-section CSV; names are passed in already localised. */
    fun toCsv(
        s: Snapshot,
        medicationName: (String) -> String,
        siteName: (String) -> String,
        sideEffectName: (String) -> String,
    ): String = buildString {
        appendLine("# Injections")
        appendLine("date,medication,dose_mg,site,comfort_1_5,notes")
        s.injections.sortedBy { it.timestamp }.forEach {
            appendLine(listOf(csvTime(it.timestamp), csv(medicationName(it.medication)), it.doseMg.toString(),
                csv(siteName(it.site)), if (it.comfort in 1..5) it.comfort.toString() else "", csv(it.notes)).joinToString(","))
        }
        appendLine()
        appendLine("# Weight")
        appendLine("date,weight_kg,weight_lb,waist_cm,body_fat_pct,muscle_kg,water_pct,bone_kg,visceral_fat,bmr_kcal,metabolic_age,notes")
        fun d1(v: Double?) = v?.let { "%.1f".format(java.util.Locale.US, it) } ?: ""
        s.weights.sortedBy { it.timestamp }.forEach {
            appendLine(listOf(csvTime(it.timestamp), d1(it.weightKg), d1(it.weightKg * WeightUnit.LB_PER_KG), d1(it.waistCm),
                d1(it.bodyFatPct), d1(it.muscleKg), d1(it.waterPct), d1(it.boneKg), d1(it.visceralFat),
                it.bmrKcal?.toInt()?.toString() ?: "", it.metabolicAge?.toString() ?: "", csv(it.notes)).joinToString(","))
        }
        appendLine()
        appendLine("# Side effects")
        appendLine("date,symptom,severity_1_to_3,notes")
        s.sideEffects.sortedBy { it.timestamp }.forEach {
            appendLine(listOf(csvTime(it.timestamp), csv(sideEffectName(it.type)), it.severity.toString(),
                csv(it.notes)).joinToString(","))
        }
        appendLine()
        fun cm(v: Double?) = v?.let { "%.1f".format(java.util.Locale.US, it) } ?: ""
        appendLine("# Body measurements (cm)")
        appendLine("date,waist,hips,chest,arm,thigh,neck,notes")
        s.measurements.sortedBy { it.timestamp }.forEach {
            appendLine(listOf(csvTime(it.timestamp), cm(it.waistCm), cm(it.hipsCm), cm(it.chestCm), cm(it.armCm),
                cm(it.thighCm), cm(it.neckCm), csv(it.notes)).joinToString(","))
        }
        appendLine()
        appendLine("# Food")
        appendLine("date,name,kcal,protein_g,fiber_g")
        s.foods.sortedBy { it.timestamp }.forEach {
            appendLine(listOf(csvTime(it.timestamp), csv(it.name), it.calories.toString(), cm(it.proteinG), cm(it.fiberG)).joinToString(","))
        }
        appendLine()
        appendLine("# Supplements and remedies")
        appendLine("date,name,as_needed")
        val supplementNames = s.supplements.associateBy { it.id }
        s.supplementLogs.sortedBy { it.timestamp }.forEach { log ->
            val item = supplementNames[log.supplementId] ?: return@forEach
            appendLine(listOf(csvTime(log.timestamp), csv(item.name), item.asNeeded.toString()).joinToString(","))
        }
        appendLine()
        appendLine("# Vitals and labs")
        appendLine("date,type,value,value2,notes")
        s.vitals.sortedBy { it.timestamp }.forEach {
            appendLine(listOf(csvTime(it.timestamp), it.type, it.value.toString(), it.value2?.toString() ?: "", csv(it.notes)).joinToString(","))
        }
        appendLine()
        appendLine("# Daily log")
        appendLine("date,water_ml,protein_g,appetite_1_to_5,mood_1_to_5,energy_1_to_5,sleep_1_to_5,food_noise_1_to_5,bowel_movements,exercise_min,strength,notes")
        s.dailyLogs.sortedBy { it.date }.forEach {
            appendLine(listOf(it.date, it.waterMl.toString(), it.proteinG.toString(), it.appetite.toString(),
                it.mood.toString(), it.energy.toString(), it.sleep.toString(), it.foodNoise.toString(),
                if (it.bowel < 0) "" else it.bowel.toString(), it.exerciseMin.toString(), it.strength.toString(),
                csv(it.notes)).joinToString(","))
        }
    }
}

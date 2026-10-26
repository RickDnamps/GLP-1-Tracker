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
    )

    private const val FORMAT_VERSION = 1

    fun toJson(s: Snapshot): String {
        val root = JSONObject()
        root.put("format", "glp1tracker")
        root.put("version", FORMAT_VERSION)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("settings", settingsToJson(s.settings))
        root.put("injections", JSONArray().apply {
            s.injections.forEach {
                put(JSONObject().put("id", it.id).put("timestamp", it.timestamp).put("medication", it.medication)
                    .put("doseMg", it.doseMg).put("site", it.site).put("notes", it.notes))
            }
        })
        root.put("weights", JSONArray().apply {
            s.weights.forEach {
                put(JSONObject().put("id", it.id).put("timestamp", it.timestamp).put("weightKg", it.weightKg)
                    .put("waistCm", it.waistCm ?: JSONObject.NULL).put("notes", it.notes))
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
                    .put("appetite", it.appetite).put("mood", it.mood).put("notes", it.notes))
            }
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
                )
            },
            weights = root.optJSONArray("weights").objects().map {
                WeightEntity(
                    id = it.optLong("id"),
                    timestamp = it.getLong("timestamp"),
                    weightKg = it.getDouble("weightKg"),
                    waistCm = if (it.isNull("waistCm")) null else it.optDouble("waistCm"),
                    notes = it.optString("notes"),
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
                )
            },
        )
    }

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
        put("proteinGoalG", s.proteinGoalG)
        put("penTrackingEnabled", s.penTrackingEnabled)
        put("penDosesTotal", s.penDosesTotal)
        put("penDosesRemaining", s.penDosesRemaining)
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
            intervalDays = o.optInt("intervalDays", d.intervalDays),
            injectionDay = DayOfWeek.of(o.optInt("injectionDay", d.injectionDay.value).coerceIn(1, 7)),
            reminderHour = o.optInt("reminderHour", d.reminderHour),
            reminderMinute = o.optInt("reminderMinute", d.reminderMinute),
            remindersEnabled = o.optBoolean("remindersEnabled", d.remindersEnabled),
            customMedName = o.optString("customMedName", d.customMedName),
            customHalfLifeHours = o.optDouble("customHalfLifeHours", d.customHalfLifeHours),
            waterGoalMl = o.optInt("waterGoalMl", d.waterGoalMl),
            proteinGoalG = o.optInt("proteinGoalG", d.proteinGoalG),
            penTrackingEnabled = o.optBoolean("penTrackingEnabled", d.penTrackingEnabled),
            penDosesTotal = o.optInt("penDosesTotal", d.penDosesTotal),
            penDosesRemaining = o.optInt("penDosesRemaining", d.penDosesRemaining),
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
        appendLine("date,medication,dose_mg,site,notes")
        s.injections.sortedBy { it.timestamp }.forEach {
            appendLine(listOf(csvTime(it.timestamp), csv(medicationName(it.medication)), it.doseMg.toString(),
                csv(siteName(it.site)), csv(it.notes)).joinToString(","))
        }
        appendLine()
        appendLine("# Weight")
        appendLine("date,weight_kg,weight_lb,waist_cm,notes")
        s.weights.sortedBy { it.timestamp }.forEach {
            appendLine(listOf(csvTime(it.timestamp), "%.1f".format(java.util.Locale.US, it.weightKg),
                "%.1f".format(java.util.Locale.US, it.weightKg * WeightUnit.LB_PER_KG),
                it.waistCm?.let { w -> "%.1f".format(java.util.Locale.US, w) } ?: "", csv(it.notes)).joinToString(","))
        }
        appendLine()
        appendLine("# Side effects")
        appendLine("date,symptom,severity_1_to_3,notes")
        s.sideEffects.sortedBy { it.timestamp }.forEach {
            appendLine(listOf(csvTime(it.timestamp), csv(sideEffectName(it.type)), it.severity.toString(),
                csv(it.notes)).joinToString(","))
        }
        appendLine()
        appendLine("# Daily log")
        appendLine("date,water_ml,protein_g,appetite_1_to_5,mood_1_to_5,notes")
        s.dailyLogs.sortedBy { it.date }.forEach {
            appendLine(listOf(it.date, it.waterMl.toString(), it.proteinG.toString(), it.appetite.toString(),
                it.mood.toString(), csv(it.notes)).joinToString(","))
        }
    }
}

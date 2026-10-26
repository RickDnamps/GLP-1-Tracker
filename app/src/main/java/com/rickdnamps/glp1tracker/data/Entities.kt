package com.rickdnamps.glp1tracker.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "injections")
data class InjectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val medication: String,
    val doseMg: Double,
    val site: String,
    val notes: String = "",
)

@Entity(tableName = "weights")
data class WeightEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val weightKg: Double,
    val waistCm: Double? = null,
    val notes: String = "",
)

@Entity(tableName = "side_effects")
data class SideEffectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val type: String,
    val severity: Int,
    val notes: String = "",
)

/** One row per calendar day (ISO date, e.g. 2026-10-26). */
@Entity(tableName = "daily_logs")
data class DailyLogEntity(
    @PrimaryKey val date: String,
    val waterMl: Int = 0,
    val proteinG: Int = 0,
    val appetite: Int = 0,
    val mood: Int = 0,
    val notes: String = "",
    /** 1..5, 0 = not set (v3). */
    @ColumnInfo(defaultValue = "0") val energy: Int = 0,
    /** Sleep quality 1..5, 0 = not set. */
    @ColumnInfo(defaultValue = "0") val sleep: Int = 0,
    /** Food noise / cravings 1..5, 0 = not set. */
    @ColumnInfo(defaultValue = "0") val foodNoise: Int = 0,
    /** Number of bowel movements; -1 = not set. */
    @ColumnInfo(defaultValue = "-1") val bowel: Int = -1,
    @ColumnInfo(defaultValue = "0") val exerciseMin: Int = 0,
    @ColumnInfo(defaultValue = "0") val strength: Boolean = false,
)

/** Body measurements in cm; null = not measured that day. */
@Entity(tableName = "measurements")
data class MeasurementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val waistCm: Double? = null,
    val hipsCm: Double? = null,
    val chestCm: Double? = null,
    val armCm: Double? = null,
    val thighCm: Double? = null,
    val neckCm: Double? = null,
    val notes: String = "",
)

@Entity(tableName = "food_entries")
data class FoodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val name: String,
    val calories: Int,
    val proteinG: Double,
    val fiberG: Double,
    val favorite: Boolean = false,
)

/** Private progress photo stored in the app's internal files directory. */
@Entity(tableName = "photos")
data class PhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    /** File name inside filesDir/photos. */
    val fileName: String,
    val pose: String = "FRONT",
    val notes: String = "",
)

/** Vital signs and lab results. [value2] is used for diastolic blood pressure. */
@Entity(tableName = "vitals")
data class VitalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val type: String,
    val value: Double,
    val value2: Double? = null,
    val notes: String = "",
)

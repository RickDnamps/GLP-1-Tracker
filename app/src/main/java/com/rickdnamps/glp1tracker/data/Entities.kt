package com.rickdnamps.glp1tracker.data

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

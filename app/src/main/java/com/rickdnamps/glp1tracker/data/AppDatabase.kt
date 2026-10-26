package com.rickdnamps.glp1tracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        InjectionEntity::class, WeightEntity::class, SideEffectEntity::class, DailyLogEntity::class,
        MeasurementEntity::class, FoodEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun injectionDao(): InjectionDao
    abstract fun weightDao(): WeightDao
    abstract fun sideEffectDao(): SideEffectDao
    abstract fun dailyLogDao(): DailyLogDao
    abstract fun measurementDao(): MeasurementDao
    abstract fun foodDao(): FoodDao

    companion object {
        /** v2: body measurements and food log. Waist values move from weights to measurements. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `measurements` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`timestamp` INTEGER NOT NULL, `waistCm` REAL, `hipsCm` REAL, `chestCm` REAL, `armCm` REAL, " +
                        "`thighCm` REAL, `neckCm` REAL, `notes` TEXT NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `food_entries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`timestamp` INTEGER NOT NULL, `name` TEXT NOT NULL, `calories` INTEGER NOT NULL, " +
                        "`proteinG` REAL NOT NULL, `fiberG` REAL NOT NULL, `favorite` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "INSERT INTO `measurements` (`timestamp`, `waistCm`, `notes`) " +
                        "SELECT `timestamp`, `waistCm`, '' FROM `weights` WHERE `waistCm` IS NOT NULL"
                )
            }
        }

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "glp1tracker.db")
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}

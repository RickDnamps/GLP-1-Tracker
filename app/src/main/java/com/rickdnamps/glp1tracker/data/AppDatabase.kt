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
        MeasurementEntity::class, FoodEntity::class, PhotoEntity::class, VitalEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun injectionDao(): InjectionDao
    abstract fun weightDao(): WeightDao
    abstract fun sideEffectDao(): SideEffectDao
    abstract fun dailyLogDao(): DailyLogDao
    abstract fun measurementDao(): MeasurementDao
    abstract fun foodDao(): FoodDao
    abstract fun photoDao(): PhotoDao
    abstract fun vitalDao(): VitalDao

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

        /** v3: richer daily journal, progress photos, vitals & labs. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                listOf(
                    "`energy` INTEGER NOT NULL DEFAULT 0",
                    "`sleep` INTEGER NOT NULL DEFAULT 0",
                    "`foodNoise` INTEGER NOT NULL DEFAULT 0",
                    "`bowel` INTEGER NOT NULL DEFAULT -1",
                    "`exerciseMin` INTEGER NOT NULL DEFAULT 0",
                    "`strength` INTEGER NOT NULL DEFAULT 0",
                ).forEach { db.execSQL("ALTER TABLE `daily_logs` ADD COLUMN $it") }
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `photos` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`timestamp` INTEGER NOT NULL, `fileName` TEXT NOT NULL, `pose` TEXT NOT NULL, `notes` TEXT NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `vitals` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`timestamp` INTEGER NOT NULL, `type` TEXT NOT NULL, `value` REAL NOT NULL, `value2` REAL, `notes` TEXT NOT NULL)"
                )
            }
        }

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "glp1tracker.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
    }
}

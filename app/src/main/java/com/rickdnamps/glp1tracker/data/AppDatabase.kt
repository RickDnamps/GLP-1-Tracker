package com.rickdnamps.glp1tracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [InjectionEntity::class, WeightEntity::class, SideEffectEntity::class, DailyLogEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun injectionDao(): InjectionDao
    abstract fun weightDao(): WeightDao
    abstract fun sideEffectDao(): SideEffectDao
    abstract fun dailyLogDao(): DailyLogDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "glp1tracker.db").build()
    }
}

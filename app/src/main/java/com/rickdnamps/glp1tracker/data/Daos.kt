package com.rickdnamps.glp1tracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface InjectionDao {
    @Query("SELECT * FROM injections ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<InjectionEntity>>

    @Query("SELECT * FROM injections ORDER BY timestamp DESC")
    suspend fun getAll(): List<InjectionEntity>

    @Query("SELECT * FROM injections ORDER BY timestamp DESC LIMIT 1")
    suspend fun latest(): InjectionEntity?

    @Upsert
    suspend fun upsert(item: InjectionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<InjectionEntity>)

    @Delete
    suspend fun delete(item: InjectionEntity)

    @Query("DELETE FROM injections")
    suspend fun clear()
}

@Dao
interface WeightDao {
    @Query("SELECT * FROM weights ORDER BY timestamp ASC")
    fun observeAll(): Flow<List<WeightEntity>>

    @Query("SELECT * FROM weights ORDER BY timestamp ASC")
    suspend fun getAll(): List<WeightEntity>

    @Upsert
    suspend fun upsert(item: WeightEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<WeightEntity>)

    @Delete
    suspend fun delete(item: WeightEntity)

    @Query("DELETE FROM weights")
    suspend fun clear()
}

@Dao
interface SideEffectDao {
    @Query("SELECT * FROM side_effects ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<SideEffectEntity>>

    @Query("SELECT * FROM side_effects ORDER BY timestamp DESC")
    suspend fun getAll(): List<SideEffectEntity>

    @Upsert
    suspend fun upsert(item: SideEffectEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SideEffectEntity>)

    @Delete
    suspend fun delete(item: SideEffectEntity)

    @Query("DELETE FROM side_effects")
    suspend fun clear()
}

@Dao
interface DailyLogDao {
    @Query("SELECT * FROM daily_logs ORDER BY date DESC")
    fun observeAll(): Flow<List<DailyLogEntity>>

    @Query("SELECT * FROM daily_logs ORDER BY date DESC")
    suspend fun getAll(): List<DailyLogEntity>

    @Query("SELECT * FROM daily_logs WHERE date = :date")
    suspend fun get(date: String): DailyLogEntity?

    @Upsert
    suspend fun upsert(item: DailyLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<DailyLogEntity>)

    @Query("DELETE FROM daily_logs")
    suspend fun clear()
}

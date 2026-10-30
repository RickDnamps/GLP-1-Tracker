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

@Dao
interface MeasurementDao {
    @Query("SELECT * FROM measurements ORDER BY timestamp ASC")
    fun observeAll(): Flow<List<MeasurementEntity>>

    @Query("SELECT * FROM measurements ORDER BY timestamp ASC")
    suspend fun getAll(): List<MeasurementEntity>

    @Upsert
    suspend fun upsert(item: MeasurementEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MeasurementEntity>)

    @Delete
    suspend fun delete(item: MeasurementEntity)

    @Query("DELETE FROM measurements")
    suspend fun clear()
}

@Dao
interface FoodDao {
    @Query("SELECT * FROM food_entries ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<FoodEntity>>

    @Query("SELECT * FROM food_entries ORDER BY timestamp DESC")
    suspend fun getAll(): List<FoodEntity>

    @Upsert
    suspend fun upsert(item: FoodEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<FoodEntity>)

    @Delete
    suspend fun delete(item: FoodEntity)

    @Query("DELETE FROM food_entries")
    suspend fun clear()
}

@Dao
interface PhotoDao {
    @Query("SELECT * FROM photos ORDER BY timestamp ASC")
    fun observeAll(): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos ORDER BY timestamp ASC")
    suspend fun getAll(): List<PhotoEntity>

    @Upsert
    suspend fun upsert(item: PhotoEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<PhotoEntity>)

    @Delete
    suspend fun delete(item: PhotoEntity)

    @Query("DELETE FROM photos")
    suspend fun clear()
}

@Dao
interface VitalDao {
    @Query("SELECT * FROM vitals ORDER BY timestamp ASC")
    fun observeAll(): Flow<List<VitalEntity>>

    @Query("SELECT * FROM vitals ORDER BY timestamp ASC")
    suspend fun getAll(): List<VitalEntity>

    @Upsert
    suspend fun upsert(item: VitalEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<VitalEntity>)

    @Delete
    suspend fun delete(item: VitalEntity)

    @Query("DELETE FROM vitals")
    suspend fun clear()
}

@Dao
interface SupplementDao {
    @Query("SELECT * FROM supplements ORDER BY asNeeded ASC, name ASC")
    fun observeAll(): Flow<List<SupplementEntity>>

    @Query("SELECT * FROM supplements")
    suspend fun getAll(): List<SupplementEntity>

    @Upsert
    suspend fun upsert(item: SupplementEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SupplementEntity>)

    @Query("DELETE FROM supplements")
    suspend fun clear()
}

@Dao
interface SupplementLogDao {
    @Query("SELECT * FROM supplement_logs ORDER BY timestamp ASC")
    fun observeAll(): Flow<List<SupplementLogEntity>>

    @Query("SELECT * FROM supplement_logs ORDER BY timestamp ASC")
    suspend fun getAll(): List<SupplementLogEntity>

    @Query("SELECT * FROM supplement_logs WHERE supplementId = :id AND timestamp >= :from AND timestamp < :to")
    suspend fun between(id: Long, from: Long, to: Long): List<SupplementLogEntity>

    @Insert
    suspend fun insert(item: SupplementLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SupplementLogEntity>)

    @Delete
    suspend fun delete(item: SupplementLogEntity)

    @Query("DELETE FROM supplement_logs")
    suspend fun clear()
}

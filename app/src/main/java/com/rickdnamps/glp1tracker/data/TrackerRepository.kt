package com.rickdnamps.glp1tracker.data

import androidx.room.withTransaction
import com.rickdnamps.glp1tracker.domain.InjectionSite
import com.rickdnamps.glp1tracker.domain.Medication
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate

class TrackerRepository(
    private val db: AppDatabase,
    val settingsRepository: SettingsRepository,
) {
    val injections: Flow<List<InjectionEntity>> = db.injectionDao().observeAll()
    val weights: Flow<List<WeightEntity>> = db.weightDao().observeAll()
    val sideEffects: Flow<List<SideEffectEntity>> = db.sideEffectDao().observeAll()
    val dailyLogs: Flow<List<DailyLogEntity>> = db.dailyLogDao().observeAll()
    val settings: Flow<UserSettings> = settingsRepository.settings

    suspend fun latestInjection(): InjectionEntity? = db.injectionDao().latest()

    suspend fun saveInjection(item: InjectionEntity) {
        val isNew = item.id == 0L
        db.injectionDao().upsert(item)
        if (isNew) consumePenDose(Medication.fromName(item.medication))
    }

    /** Logs the planned dose right now (used by the notification "Done" action). */
    suspend fun logPlannedDose(now: Long) {
        val s = settingsRepository.current()
        val site = if (s.medication.isOral) InjectionSite.NONE else suggestSite()
        saveInjection(
            InjectionEntity(timestamp = now, medication = s.medication.name, doseMg = s.doseMg, site = site.name)
        )
    }

    suspend fun suggestSite(): InjectionSite {
        val lastUse = db.injectionDao().getAll()
            .groupBy { InjectionSite.fromName(it.site) }
            .mapValues { (_, list) -> list.maxOf { it.timestamp } }
        return InjectionSite.suggest(lastUse)
    }

    private suspend fun consumePenDose(medication: Medication) {
        if (medication.isOral) return
        settingsRepository.update {
            if (it.penTrackingEnabled) it.copy(penDosesRemaining = (it.penDosesRemaining - 1).coerceAtLeast(0)) else it
        }
    }

    suspend fun deleteInjection(item: InjectionEntity) = db.injectionDao().delete(item)

    suspend fun saveWeight(item: WeightEntity) {
        db.weightDao().upsert(item)
    }

    suspend fun weightsSnapshot(): List<WeightEntity> = db.weightDao().getAll()

    suspend fun addWeights(items: List<WeightEntity>) = db.weightDao().insertAll(items)

    suspend fun deleteWeight(item: WeightEntity) = db.weightDao().delete(item)

    suspend fun saveSideEffect(item: SideEffectEntity) {
        db.sideEffectDao().upsert(item)
    }

    suspend fun deleteSideEffect(item: SideEffectEntity) = db.sideEffectDao().delete(item)

    /** Serialises read-modify-write updates so rapid taps / keystrokes are applied in order. */
    private val dailyLogMutex = Mutex()

    suspend fun updateDailyLog(date: LocalDate, transform: (DailyLogEntity) -> DailyLogEntity) = dailyLogMutex.withLock {
        val key = date.toString()
        val current = db.dailyLogDao().get(key) ?: DailyLogEntity(date = key)
        db.dailyLogDao().upsert(transform(current).copy(date = key))
    }

    suspend fun snapshot(): Backup.Snapshot = Backup.Snapshot(
        settings = settingsRepository.current(),
        injections = db.injectionDao().getAll(),
        weights = db.weightDao().getAll(),
        sideEffects = db.sideEffectDao().getAll(),
        dailyLogs = db.dailyLogDao().getAll(),
    )

    suspend fun restore(snapshot: Backup.Snapshot) {
        db.withTransaction {
            db.injectionDao().clear()
            db.weightDao().clear()
            db.sideEffectDao().clear()
            db.dailyLogDao().clear()
            db.injectionDao().insertAll(snapshot.injections)
            db.weightDao().insertAll(snapshot.weights)
            db.sideEffectDao().insertAll(snapshot.sideEffects)
            db.dailyLogDao().insertAll(snapshot.dailyLogs)
        }
        settingsRepository.update { snapshot.settings.copy(onboardingDone = true) }
    }
}

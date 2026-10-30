package com.rickdnamps.glp1tracker.ui

import android.app.Application
import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rickdnamps.glp1tracker.Glp1App
import com.rickdnamps.glp1tracker.R
import com.rickdnamps.glp1tracker.data.AutoBackup
import com.rickdnamps.glp1tracker.data.Backup
import com.rickdnamps.glp1tracker.data.FoodEntity
import com.rickdnamps.glp1tracker.data.MeasurementEntity
import com.rickdnamps.glp1tracker.data.PhotoEntity
import com.rickdnamps.glp1tracker.data.PhotoStore
import com.rickdnamps.glp1tracker.data.VitalEntity
import com.rickdnamps.glp1tracker.data.SupplementLogEntity
import com.rickdnamps.glp1tracker.data.SupplementEntity
import com.rickdnamps.glp1tracker.data.DailyLogEntity
import com.rickdnamps.glp1tracker.data.InjectionEntity
import com.rickdnamps.glp1tracker.data.SideEffectEntity
import com.rickdnamps.glp1tracker.data.UserSettings
import com.rickdnamps.glp1tracker.data.WeightEntity
import com.rickdnamps.glp1tracker.data.composition
import com.rickdnamps.glp1tracker.data.withComposition
import com.rickdnamps.glp1tracker.domain.BodyComp
import com.rickdnamps.glp1tracker.domain.BodyCompositionMath
import com.rickdnamps.glp1tracker.domain.Insights
import com.rickdnamps.glp1tracker.domain.InjectionSite
import com.rickdnamps.glp1tracker.domain.Medication
import com.rickdnamps.glp1tracker.domain.SideEffectType
import com.rickdnamps.glp1tracker.notifications.Notifications
import com.rickdnamps.glp1tracker.report.DoctorReport
import com.rickdnamps.glp1tracker.notifications.ReminderScheduler
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

data class UiState(
    val loaded: Boolean = false,
    val settings: UserSettings = UserSettings(),
    val injections: List<InjectionEntity> = emptyList(),
    val weights: List<WeightEntity> = emptyList(),
    val sideEffects: List<SideEffectEntity> = emptyList(),
    val dailyLogs: List<DailyLogEntity> = emptyList(),
    val measurements: List<MeasurementEntity> = emptyList(),
    val foods: List<FoodEntity> = emptyList(),
    val photos: List<PhotoEntity> = emptyList(),
    val vitals: List<VitalEntity> = emptyList(),
    val supplements: List<SupplementEntity> = emptyList(),
    val supplementLogs: List<SupplementLogEntity> = emptyList(),
)

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as Glp1App
    private val repository = app.repository
    val scale = app.scaleManager
    val healthConnect = app.healthConnect

    val state: StateFlow<UiState> = combine(
        combine(
            combine(
                repository.settings,
                repository.injections,
                repository.weights,
                repository.sideEffects,
                repository.dailyLogs,
            ) { settings, injections, weights, sideEffects, dailyLogs ->
                UiState(true, settings, injections, weights, sideEffects, dailyLogs)
            },
            repository.measurements,
            repository.foods,
            repository.photos,
            repository.vitals,
        ) { core, measurements, foods, photos, vitals ->
            core.copy(measurements = measurements, foods = foods, photos = photos, vitals = vitals)
        },
        repository.supplements,
        repository.supplementLogs,
    ) { core, supplements, logs ->
        core.copy(supplements = supplements, supplementLogs = logs)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    init {
        viewModelScope.launch {
            runCatching { AutoBackup.runIfDue(getApplication(), repository) }
            ReminderScheduler.reschedule(getApplication(), repository)
        }
        viewModelScope.launch { runCatching { refreshMetabolicAges() } }
    }

    /**
     * When the composition formulas change (v1: body age, v2: calibrated on the Renpho app),
     * recompute past Bluetooth weigh-ins from their stored impedance. Fields the estimate does
     * not produce (e.g. visceral fat typed by hand) are kept.
     */
    private suspend fun refreshMetabolicAges() {
        val s = repository.settingsRepository.current()
        if (s.compFormulaVersion >= COMP_FORMULA_VERSION) return
        val sex = s.sexOrNull
        val born = s.birthDateOrNull ?: s.birthYear.takeIf { it in 1900..2100 }?.let { java.time.LocalDate.of(it, 7, 1) }
        if (sex != null && born != null && s.heightCm > 0) {
            val fixed = repository.weightsSnapshot().mapNotNull { w ->
                val c = w.composition
                val impedance = c.impedance ?: return@mapNotNull null
                val day = java.time.Instant.ofEpochMilli(w.timestamp).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                val age = java.time.Period.between(born, day).years
                val fresh = BodyCompositionMath.estimate(w.weightKg, s.heightCm, age, sex, impedance) ?: return@mapNotNull null
                w.withComposition(fresh.mergedWith(c))
            }
            if (fixed.isNotEmpty()) repository.addWeights(fixed)
        }
        repository.settingsRepository.update { it.copy(compFormulaVersion = COMP_FORMULA_VERSION) }
    }

    /** App lock state: kept here (not in saved state) so a killed process always starts locked. */
    var unlocked by androidx.compose.runtime.mutableStateOf(false)
    var stoppedAt = 0L

    fun disableLockAndUnlock() {
        unlocked = true
        updateSettings { it.copy(appLock = false) }
    }

    private val _messages = MutableSharedFlow<Int>(extraBufferCapacity = 4)
    /** One-off snackbar messages (string resource ids). */
    val messages: SharedFlow<Int> = _messages

    private fun message(@StringRes res: Int) {
        _messages.tryEmit(res)
    }

    private fun launchAndReschedule(block: suspend () -> Unit) = viewModelScope.launch {
        block()
        ReminderScheduler.reschedule(getApplication(), repository)
        app.onDataChanged()
    }

    // --- Injections ---
    fun saveInjection(item: InjectionEntity) = launchAndReschedule {
        repository.saveInjection(item)
        if (item.id == 0L) Notifications.cancelDoseReminder(getApplication())
        message(R.string.msg_dose_saved)
        runCatching { AutoBackup.runIfDue(getApplication(), repository) }
    }

    fun deleteInjection(item: InjectionEntity) = launchAndReschedule {
        repository.deleteInjection(item)
        message(R.string.msg_deleted)
    }

    // --- Weight ---
    fun saveWeight(item: WeightEntity) = viewModelScope.launch {
        val previous = repository.weightsSnapshot().filter { it.id != item.id }
        repository.saveWeight(item)
        message(R.string.msg_weight_saved)
        if (item.id == 0L) celebrateIfNewLow(previous, item)
    }

    private val _newLows = MutableSharedFlow<Double>(extraBufferCapacity = 1)
    /** Fresh weigh-ins that are a record low (kg), for a small celebration. */
    val newLows: SharedFlow<Double> = _newLows

    /**
     * Celebrates a record low on a weigh-in from the last day, except when the loss is very fast
     * or the goal is already passed (no cheering for losing too much).
     */
    private suspend fun celebrateIfNewLow(previous: List<WeightEntity>, item: WeightEntity) {
        val now = System.currentTimeMillis()
        if (now - item.timestamp > 86_400_000L) return
        if (!Insights.isNewLow(previous.map { it.weightKg }, item.weightKg)) return
        val s = repository.settingsRepository.current()
        if (s.goalWeightKg > 0 && item.weightKg < s.goalWeightKg) return
        val points = (previous + item).map { it.timestamp to it.weightKg }.sortedBy { it.first }
        if (Insights.pace(points, now)?.first == Insights.Pace.VERY_FAST) return
        _newLows.tryEmit(item.weightKg)
    }

    fun deleteWeight(item: WeightEntity) = viewModelScope.launch {
        repository.deleteWeight(item)
        message(R.string.msg_deleted)
    }

    // --- Side effects ---
    fun saveSideEffect(item: SideEffectEntity) = viewModelScope.launch {
        repository.saveSideEffect(item)
        message(R.string.msg_side_effect_saved)
    }

    fun deleteSideEffect(item: SideEffectEntity) = viewModelScope.launch {
        repository.deleteSideEffect(item)
        message(R.string.msg_deleted)
    }

    // --- Daily log ---
    // --- Supplements & remedies ---
    fun saveSupplement(item: SupplementEntity) = viewModelScope.launch { repository.saveSupplement(item) }

    fun toggleSupplement(item: SupplementEntity, day: LocalDate) = viewModelScope.launch {
        val now = java.time.LocalDateTime.now()
        val at = if (day == now.toLocalDate()) now else day.atTime(12, 0)
        repository.toggleSupplement(item, day, at.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli())
    }

    fun removeLastIntake(item: SupplementEntity, day: LocalDate) = viewModelScope.launch { repository.removeLastIntake(item, day) }

    fun updateDailyLog(date: LocalDate, transform: (DailyLogEntity) -> DailyLogEntity) = viewModelScope.launch {
        repository.updateDailyLog(date, transform)
    }

    // --- Settings ---
    fun updateSettings(transform: (UserSettings) -> UserSettings) = launchAndReschedule {
        repository.settingsRepository.update(transform)
    }

    fun completeOnboarding(settings: UserSettings, currentWeightKg: Double?) = launchAndReschedule {
        if (currentWeightKg != null && currentWeightKg > 0) {
            repository.saveWeight(WeightEntity(timestamp = System.currentTimeMillis(), weightKg = currentWeightKg))
        }
        repository.settingsRepository.update {
            settings.copy(
                onboardingDone = true,
                startWeightKg = if (settings.startWeightKg > 0) settings.startWeightKg else currentWeightKg ?: 0.0,
            )
        }
    }

    fun reschedule() = launchAndReschedule {}

    /**
     * Records [achieved] badges and returns those never celebrated before. The very first call
     * only remembers existing badges (no confetti for badges earned before this feature).
     */
    suspend fun claimNewBadges(achieved: Set<com.rickdnamps.glp1tracker.domain.Badge>): List<com.rickdnamps.glp1tracker.domain.Badge> {
        var fresh: List<com.rickdnamps.glp1tracker.domain.Badge> = emptyList()
        repository.settingsRepository.update { s ->
            val initialized = s.celebratedBadges.isNotEmpty()
            val known = com.rickdnamps.glp1tracker.domain.Milestones.decode(s.celebratedBadges)
            fresh = if (initialized) achieved.filter { it !in known }.sortedBy { it.ordinal } else emptyList()
            // "-" marks an initialised but empty set.
            val all = known + achieved
            s.copy(celebratedBadges = com.rickdnamps.glp1tracker.domain.Milestones.encode(all).ifEmpty { "-" })
        }
        return fresh
    }

    /** Skips the overdue dose: the schedule moves on to the next slot. */
    fun skipDose(due: LocalDate) = updateSettings { it.copy(skippedDueEpochDay = due.toEpochDay()) }

    fun applyPlannedDose(doseMg: Double) = updateSettings { it.copy(doseMg = doseMg) }

    // --- Bluetooth scale ---
    fun saveScaleWeight(
        kg: Double,
        address: String,
        name: String,
        composition: BodyComp? = null,
        impedance: Double? = null,
    ) = viewModelScope.launch {
        val s = repository.settingsRepository.current()
        // Scales that only send impedance: estimate the composition from the profile.
        val estimated = impedance?.let { imp ->
            val sex = s.sexOrNull
            val age = s.ageOrNull
            if (sex != null && age != null && s.heightCm > 0) BodyCompositionMath.estimate(kg, s.heightCm, age, sex, imp) else null
        }
        val comp = when {
            composition != null && estimated != null -> composition.mergedWith(estimated)
            else -> composition ?: estimated ?: impedance?.let { BodyComp(impedance = it) }
        }
        val entity = WeightEntity(timestamp = System.currentTimeMillis(), weightKg = kg)
        val previous = repository.weightsSnapshot()
        repository.saveWeight(if (comp != null) entity.withComposition(comp) else entity)
        celebrateIfNewLow(previous, entity)
        repository.settingsRepository.update { it.copy(scaleAddress = address, scaleName = name) }
        message(R.string.msg_weight_saved)
    }

    fun forgetScale() = updateSettings { it.copy(scaleAddress = "", scaleName = "") }

    // --- Health Connect ---
    /** Imports new weights; [manual] shows a message even when nothing was found. */
    private val importMutex = kotlinx.coroutines.sync.Mutex()

    fun importFromHealthConnect(manual: Boolean) = viewModelScope.launch {
        // One import at a time: two concurrent runs would both insert the same new weigh-ins.
        if (!importMutex.tryLock()) return@launch
        try {
            importLocked(manual)
        } finally {
            importMutex.unlock()
        }
    }

    private suspend fun importLocked(manual: Boolean) {
        runCatching {
            if (!healthConnect.hasPermissions()) return
            val settings = repository.settingsRepository.current()
            // First import looks back one year, then re-reads 30 days (scale apps often sync late); duplicates are skipped.
            val since = if (settings.healthConnectLastImport > 0) settings.healthConnectLastImport - 30L * 86_400_000L
            else System.currentTimeMillis() - 365L * 86_400_000L
            val result = healthConnect.import(java.time.Instant.ofEpochMilli(since), repository.weightsSnapshot())
            val news = result.newWeights
            repository.addWeights(news + result.updated)
            repository.settingsRepository.update { it.copy(healthConnectLastImport = System.currentTimeMillis()) }
            news.size
        }.onSuccess { count ->
            if (count > 0 || manual) _countMessages.tryEmit(count)
        }.onFailure { if (manual) message(R.string.msg_hc_failed) }
    }

    private val _activity = kotlinx.coroutines.flow.MutableStateFlow<com.rickdnamps.glp1tracker.health.HealthConnect.Activity?>(null)
    /** Steps and sleep from Health Connect for the home screen (null: not allowed / unavailable). */
    val activity: StateFlow<com.rickdnamps.glp1tracker.health.HealthConnect.Activity?> = _activity

    fun refreshActivity() = viewModelScope.launch {
        _activity.value = runCatching {
            if (healthConnect.hasActivityPermission()) healthConnect.activity() else null
        }.getOrNull()
    }

    private val _countMessages = MutableSharedFlow<Int>(extraBufferCapacity = 2)
    /** Number of weights imported from Health Connect. */
    val importedCounts: SharedFlow<Int> = _countMessages

    /** Starts a new pen: full doses, opened today, one less in stock. */
    fun refillPen() = updateSettings {
        it.copy(
            penDosesRemaining = it.penDosesTotal,
            penOpenedAt = System.currentTimeMillis(),
            stockPens = (it.stockPens - 1).coerceAtLeast(0),
        )
    }

    // --- Measurements & food ---
    fun saveMeasurement(item: MeasurementEntity) = viewModelScope.launch {
        repository.saveMeasurement(item)
        message(R.string.msg_measurement_saved)
    }

    fun deleteMeasurement(item: MeasurementEntity) = viewModelScope.launch {
        repository.deleteMeasurement(item)
        message(R.string.msg_deleted)
    }

    fun saveFood(item: FoodEntity) = viewModelScope.launch {
        repository.saveFood(item)
        message(R.string.msg_food_saved)
    }

    fun deleteFood(item: FoodEntity) = viewModelScope.launch {
        repository.deleteFood(item)
        message(R.string.msg_deleted)
    }

    // --- Photos & vitals ---
    fun savePhoto(item: PhotoEntity) = viewModelScope.launch {
        repository.savePhoto(item)
        message(R.string.msg_photo_saved)
    }

    /** Deletes the row and its image file. */
    fun deletePhoto(item: PhotoEntity) = viewModelScope.launch {
        repository.deletePhoto(item)
        withContext(Dispatchers.IO) { PhotoStore.file(getApplication(), item.fileName).delete() }
        message(R.string.msg_deleted)
    }

    fun saveVital(item: VitalEntity) = viewModelScope.launch {
        repository.saveVital(item)
        message(R.string.msg_vital_saved)
    }

    fun deleteVital(item: VitalEntity) = viewModelScope.launch {
        repository.deleteVital(item)
        message(R.string.msg_deleted)
    }

    // --- Auto backup ---
    fun setAutoBackupFolder(uri: Uri) = viewModelScope.launch {
        val app = getApplication<Application>()
        runCatching {
            app.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
            repository.settingsRepository.update { it.copy(autoBackupUri = uri.toString()) }
            AutoBackup.runIfDue(app, repository, force = true)
        }.onSuccess { message(R.string.msg_backup_done) }.onFailure { message(R.string.msg_export_failed) }
    }

    fun disableAutoBackup() = updateSettings { it.copy(autoBackupUri = "") }

    // --- Import / export ---
    fun exportCsv(uri: Uri) = viewModelScope.launch {
        val app = getApplication<Application>()
        runCatching {
            val snapshot = repository.snapshot()
            val csv = Backup.toCsv(
                snapshot,
                medicationName = { app.getString(Medication.fromName(it).nameRes) },
                siteName = { app.getString(InjectionSite.fromName(it).nameRes) },
                sideEffectName = { app.getString(SideEffectType.fromName(it).nameRes) },
            )
            withContext(Dispatchers.IO) {
                app.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(("\uFEFF" + csv).toByteArray()) }
            }
        }.onSuccess { message(R.string.msg_export_done) }.onFailure { message(R.string.msg_export_failed) }
    }

    fun exportDoctorReport(uri: Uri, fromMillis: Long, toMillis: Long = System.currentTimeMillis()) = viewModelScope.launch {
        val app = getApplication<Application>()
        runCatching {
            val snapshot = repository.snapshot()
            withContext(Dispatchers.IO) {
                app.contentResolver.openOutputStream(uri, "wt")!!.use {
                    DoctorReport.write(app, it, snapshot, fromMillis, toMillis)
                }
            }
        }.onSuccess { message(R.string.report_msg_done) }.onFailure { message(R.string.msg_export_failed) }
    }

    fun exportBackup(uri: Uri) = viewModelScope.launch {
        val app = getApplication<Application>()
        runCatching {
            val json = Backup.toJson(repository.snapshot())
            withContext(Dispatchers.IO) {
                app.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(json.toByteArray()) }
            }
        }.onSuccess { message(R.string.msg_backup_done) }.onFailure { message(R.string.msg_export_failed) }
    }

    fun importBackup(uri: Uri) = launchAndReschedule {
        val app = getApplication<Application>()
        runCatching {
            val text = withContext(Dispatchers.IO) {
                app.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() }
            }
            repository.restore(Backup.fromJson(text))
        }.onSuccess { message(R.string.msg_restore_done) }.onFailure { message(R.string.msg_restore_failed) }
    }

    private companion object {
        /** Bump when [BodyCompositionMath.estimate] changes so stored weigh-ins are recomputed. */
        const val COMP_FORMULA_VERSION = 2
    }
}

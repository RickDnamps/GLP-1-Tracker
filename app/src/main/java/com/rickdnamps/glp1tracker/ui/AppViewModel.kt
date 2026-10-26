package com.rickdnamps.glp1tracker.ui

import android.app.Application
import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rickdnamps.glp1tracker.Glp1App
import com.rickdnamps.glp1tracker.R
import com.rickdnamps.glp1tracker.data.Backup
import com.rickdnamps.glp1tracker.data.DailyLogEntity
import com.rickdnamps.glp1tracker.data.InjectionEntity
import com.rickdnamps.glp1tracker.data.SideEffectEntity
import com.rickdnamps.glp1tracker.data.UserSettings
import com.rickdnamps.glp1tracker.data.WeightEntity
import com.rickdnamps.glp1tracker.domain.InjectionSite
import com.rickdnamps.glp1tracker.domain.Medication
import com.rickdnamps.glp1tracker.domain.SideEffectType
import com.rickdnamps.glp1tracker.notifications.ReminderScheduler
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
)

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as Glp1App).repository

    val state: StateFlow<UiState> = combine(
        repository.settings,
        repository.injections,
        repository.weights,
        repository.sideEffects,
        repository.dailyLogs,
    ) { settings, injections, weights, sideEffects, dailyLogs ->
        UiState(true, settings, injections, weights, sideEffects, dailyLogs)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    private val _messages = MutableSharedFlow<Int>(extraBufferCapacity = 4)
    /** One-off snackbar messages (string resource ids). */
    val messages: SharedFlow<Int> = _messages

    private fun message(@StringRes res: Int) {
        _messages.tryEmit(res)
    }

    private fun launchAndReschedule(block: suspend () -> Unit) = viewModelScope.launch {
        block()
        ReminderScheduler.reschedule(getApplication(), repository)
    }

    // --- Injections ---
    fun saveInjection(item: InjectionEntity) = launchAndReschedule {
        repository.saveInjection(item)
        message(R.string.msg_dose_saved)
    }

    fun deleteInjection(item: InjectionEntity) = launchAndReschedule {
        repository.deleteInjection(item)
        message(R.string.msg_deleted)
    }

    // --- Weight ---
    fun saveWeight(item: WeightEntity) = viewModelScope.launch {
        repository.saveWeight(item)
        message(R.string.msg_weight_saved)
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

    fun refillPen() = updateSettings { it.copy(penDosesRemaining = it.penDosesTotal) }

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
                app.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(csv.toByteArray()) }
            }
        }.onSuccess { message(R.string.msg_export_done) }.onFailure { message(R.string.msg_export_failed) }
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
}

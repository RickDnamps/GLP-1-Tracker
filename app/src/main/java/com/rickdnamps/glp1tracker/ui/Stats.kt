package com.rickdnamps.glp1tracker.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rickdnamps.glp1tracker.data.InjectionEntity
import com.rickdnamps.glp1tracker.data.UserSettings
import com.rickdnamps.glp1tracker.data.WeightEntity
import com.rickdnamps.glp1tracker.domain.Format
import com.rickdnamps.glp1tracker.domain.InjectionSite
import com.rickdnamps.glp1tracker.domain.LengthUnit
import com.rickdnamps.glp1tracker.R
import com.rickdnamps.glp1tracker.domain.Medication
import com.rickdnamps.glp1tracker.domain.MilestoneInput
import com.rickdnamps.glp1tracker.domain.Milestones
import com.rickdnamps.glp1tracker.domain.Progress
import com.rickdnamps.glp1tracker.domain.Pharmacokinetics
import com.rickdnamps.glp1tracker.domain.Schedule
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/** Derived numbers shown across screens. All weights in kg. */
data class WeightStats(
    val startKg: Double?,
    val currentKg: Double?,
    val goalKg: Double?,
    val lostKg: Double?,
    val percentLost: Double?,
    val bmi: Double?,
    val progressToGoal: Float?,
    val remainingKg: Double?,
    val weeklyAverageKg: Double?,
)

fun weightStats(settings: UserSettings, weights: List<WeightEntity>): WeightStats {
    val start = settings.startWeightKg.takeIf { it > 0 } ?: weights.firstOrNull()?.weightKg
    val current = weights.lastOrNull()?.weightKg
    val goal = settings.goalWeightKg.takeIf { it > 0 }
    val lost = if (start != null && current != null) start - current else null
    val percent = if (lost != null && start != null && start > 0) lost / start * 100 else null
    val bmi = if (current != null && settings.heightCm > 0) current / ((settings.heightCm / 100) * (settings.heightCm / 100)) else null
    val progress = if (start != null && current != null && goal != null && start > goal) {
        ((start - current) / (start - goal)).toFloat().coerceIn(0f, 1f)
    } else null
    val remaining = if (current != null && goal != null) (current - goal).coerceAtLeast(0.0) else null
    val weekly = if (weights.size >= 2) {
        val days = (weights.last().timestamp - weights.first().timestamp) / 86_400_000.0
        if (days >= 6) (weights.first().weightKg - weights.last().weightKg) / (days / 7) else null
    } else null
    return WeightStats(start, current, goal, lost, percent, bmi, progress, remaining, weekly)
}

fun nextDue(settings: UserSettings, injections: List<InjectionEntity>, today: LocalDate = LocalDate.now()): LocalDateTime =
    Schedule.nextDue(
        injections.firstOrNull()?.let { Format.toLocalDateTime(it.timestamp) },
        settings.intervalDays,
        settings.injectionDay,
        settings.reminderTime,
        today,
        settings.skippedThrough,
    )

/** Days until the due date (negative when late). */
fun daysUntil(due: LocalDateTime, today: LocalDate = LocalDate.now()): Long =
    ChronoUnit.DAYS.between(today, due.toLocalDate())

fun siteLastUse(injections: List<InjectionEntity>): Map<InjectionSite, Long> =
    injections.groupBy { InjectionSite.fromName(it.site) }.mapValues { (_, l) -> l.maxOf { it.timestamp } }

fun doses(settings: UserSettings, injections: List<InjectionEntity>): List<Pharmacokinetics.Dose> =
    injections.map {
        Pharmacokinetics.Dose(it.timestamp, it.doseMg, settings.pkParams(Medication.fromName(it.medication)))
    }

@Composable
fun medicationName(medication: Medication, settings: UserSettings): String =
    if (medication == Medication.OTHER && settings.customMedName.isNotBlank()) settings.customMedName
    else stringResource(medication.nameRes)

@Composable
fun medicationName(name: String, settings: UserSettings): String = medicationName(Medication.fromName(name), settings)

/** Formats a kg value in the user's unit, e.g. "82,4 kg". */
@Composable
fun weightText(kg: Double, settings: UserSettings, signed: Boolean = false): String {
    val v = settings.weightUnit.fromKg(kg)
    val prefix = if (signed && v > 0) "+" else ""
    return prefix + Format.oneDecimal(v) + " " + stringResource(settings.weightUnit.symbolRes)
}

fun milestoneInput(state: UiState, nowMillis: Long = System.currentTimeMillis()): MilestoneInput {
    fun day(millis: Long) = Format.toLocalDateTime(millis).toLocalDate()
    val logs = state.dailyLogs.mapNotNull { log -> runCatching { LocalDate.parse(log.date) }.getOrNull()?.let { it to log } }
    return MilestoneInput(
        injections = state.injections.map { MilestoneInput.Injection(it.timestamp, InjectionSite.fromName(it.site)) },
        weights = state.weights.map { it.timestamp to it.weightKg },
        journalDays = buildSet {
            logs.forEach { (date, l) ->
                if (l.waterMl > 0 || l.proteinG > 0 || l.appetite > 0 || l.mood > 0 || l.notes.isNotBlank()) add(date)
            }
            state.sideEffects.forEach { add(day(it.timestamp)) }
            state.foods.forEach { add(day(it.timestamp)) }
        },
        waterByDay = logs.associate { (date, l) -> date to l.waterMl },
        waterGoalMl = state.settings.waterGoalMl,
        startWeightKg = state.settings.startWeightKg,
        goalWeightKg = state.settings.goalWeightKg,
        nowMillis = nowMillis,
    )
}

fun milestones(state: UiState): List<Progress> = Milestones.evaluate(milestoneInput(state))

/** Height in the user's unit: "178 cm" or "5 ft 10 in" / "5 pi 10 po". */
@Composable
fun heightText(cm: Double, settings: UserSettings): String =
    if (settings.lengthUnit == LengthUnit.IN) {
        val (ft, inch) = LengthUnit.feetInches(cm)
        stringResource(R.string.height_ft_in, ft, inch)
    } else {
        Format.number(cm, 0) + " " + stringResource(R.string.unit_cm)
    }

/** A body measurement (stored in cm) in the user's unit, e.g. "92,5 cm" or "36,4 po". */
@Composable
fun lengthText(cm: Double, settings: UserSettings, signed: Boolean = false): String {
    val v = settings.lengthUnit.fromCm(cm)
    val prefix = if (signed && v > 0) "+" else ""
    return prefix + Format.oneDecimal(v) + " " + stringResource(settings.lengthUnit.symbolRes)
}

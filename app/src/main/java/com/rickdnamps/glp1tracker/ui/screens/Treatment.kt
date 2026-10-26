package com.rickdnamps.glp1tracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rickdnamps.glp1tracker.R
import com.rickdnamps.glp1tracker.data.UserSettings
import com.rickdnamps.glp1tracker.domain.Format
import com.rickdnamps.glp1tracker.domain.MissedDose
import com.rickdnamps.glp1tracker.domain.MissedDoseAdvice
import com.rickdnamps.glp1tracker.domain.Supply
import com.rickdnamps.glp1tracker.domain.Titration
import com.rickdnamps.glp1tracker.domain.TitrationStep
import com.rickdnamps.glp1tracker.ui.components.DatePickerDialogFor
import com.rickdnamps.glp1tracker.ui.components.SectionCard
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/** Missed-dose guidance shown inside the next-dose card when the dose is late. */
@Composable
fun MissedDoseBlock(settings: UserSettings, due: LocalDateTime, contentColor: Color, onSkip: () -> Unit) {
    val now = LocalDateTime.now()
    val advice = MissedDose.advise(settings.medication, due, settings.intervalDays, now)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            when (advice.action) {
                MissedDoseAdvice.Action.TAKE_NOW -> stringResource(
                    R.string.missed_take_now,
                    MissedDose.hoursLeft(advice.deadline!!, now).toInt(),
                    Format.weekdayDate(advice.deadline) + " " + Format.time(advice.deadline),
                )
                MissedDoseAdvice.Action.SKIP -> stringResource(
                    R.string.missed_skip,
                    Format.weekdayDate(due.plusDays(settings.intervalDays.toLong())),
                )
                MissedDoseAdvice.Action.ASK -> stringResource(R.string.missed_ask)
            },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = contentColor,
        )
        advice.minGapHours?.let {
            Text(stringResource(R.string.missed_gap, it), style = MaterialTheme.typography.bodySmall, color = contentColor)
        }
        Text(stringResource(R.string.missed_disclaimer), style = MaterialTheme.typography.bodySmall, color = contentColor)
        if (advice.action != MissedDoseAdvice.Action.TAKE_NOW) {
            OutlinedButton(
                onClick = onSkip,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = contentColor),
                border = androidx.compose.foundation.BorderStroke(1.dp, contentColor.copy(alpha = 0.6f)),
            ) { Text(stringResource(R.string.skip_this_dose)) }
        }
    }
}

@Composable
fun TitrationCard(settings: UserSettings, onApplyDose: (Double) -> Unit, onEdit: () -> Unit) {
    val steps = Titration.parse(settings.titrationPlan)
    val today = LocalDate.now()
    val status = Titration.status(steps, LocalDate.ofEpochDay(settings.titrationStartDay), today) ?: return
    SectionCard(
        title = stringResource(R.string.titration_title),
        icon = Icons.AutoMirrored.Outlined.TrendingUp,
        action = { TextButton(onClick = onEdit) { Text(stringResource(R.string.edit)) } },
    ) {
        Text(
            stringResource(R.string.titration_step, status.index + 1, status.total, Format.dose(status.current.doseMg)),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        LinearProgressIndicator(
            progress = { (status.index + 1f) / status.total },
            modifier = Modifier.fillMaxWidth(),
        )
        val next = status.next
        val nextDate = status.nextStepDate
        if (next != null && nextDate != null) {
            val days = ChronoUnit.DAYS.between(today, nextDate).toInt()
            Text(
                stringResource(R.string.titration_next, Format.dose(next.doseMg), Format.date(nextDate)) + " · " +
                    pluralStringResource(R.plurals.in_days, days, days),
                style = MaterialTheme.typography.bodyMedium,
                color = if (days <= 7) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(stringResource(R.string.titration_maintenance), style = MaterialTheme.typography.bodyMedium)
        }
        if (settings.doseMg != status.current.doseMg) {
            FilledTonalButton(onClick = { onApplyDose(status.current.doseMg) }) {
                Text(stringResource(R.string.titration_apply, Format.dose(status.current.doseMg)))
            }
        }
    }
}

@Composable
fun SupplyCard(settings: UserSettings, nextDue: LocalDate, onNewPen: () -> Unit) {
    val s = settings
    val dosesLeft = Supply.dosesLeft(s.penDosesRemaining, s.stockPens, s.penDosesTotal)
    val runsOut = Supply.runsOutOn(nextDue, dosesLeft, s.intervalDays)
    val today = LocalDate.now()
    val opened = s.penOpenedAt.takeIf { it > 0 }?.let { Format.toLocalDateTime(it).toLocalDate() }
    val discard = opened?.let { Supply.discardDate(it, s.penDiscardDays) }
    SectionCard(
        title = stringResource(R.string.supply_title),
        icon = Icons.Outlined.Inventory2,
        action = { TextButton(onClick = onNewPen) { Text(stringResource(R.string.new_pen)) } },
    ) {
        Text(
            pluralStringResource(R.plurals.pen_doses_left, s.penDosesRemaining, s.penDosesRemaining, s.penDosesTotal),
            style = MaterialTheme.typography.bodyLarge,
        )
        LinearProgressIndicator(
            progress = { if (s.penDosesTotal > 0) s.penDosesRemaining.toFloat() / s.penDosesTotal else 0f },
            modifier = Modifier.fillMaxWidth(),
            color = if (s.penDosesRemaining <= 1) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        )
        Text(
            pluralStringResource(R.plurals.stock_pens, s.stockPens, s.stockPens),
            style = MaterialTheme.typography.bodyMedium,
        )
        if (runsOut != null) {
            val days = ChronoUnit.DAYS.between(today, runsOut)
            Text(
                stringResource(R.string.supply_until, Format.date(runsOut)),
                style = MaterialTheme.typography.bodyMedium,
                color = if (days <= 14) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (days <= 14) FontWeight.SemiBold else null,
            )
            if (days <= 14) {
                Text(stringResource(R.string.supply_refill), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        } else {
            Text(stringResource(R.string.supply_empty), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }
        if (discard != null) {
            val left = ChronoUnit.DAYS.between(today, discard)
            Text(
                stringResource(R.string.pen_discard_on, Format.date(discard)),
                style = MaterialTheme.typography.bodySmall,
                color = if (left <= 3) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Editable dose-escalation plan. */
@Composable
fun TitrationSheet(settings: UserSettings, onDismiss: () -> Unit, onSave: (Boolean, List<TitrationStep>, LocalDate) -> Unit) {
    val weekly = settings.intervalDays == 7
    val unitDays = if (weekly) 7 else 1
    var enabled by remember { mutableStateOf(settings.titrationEnabled || settings.titrationPlan.isBlank()) }
    val steps = remember {
        mutableStateListOf<Pair<String, String>>().apply {
            val existing = Titration.parse(settings.titrationPlan).ifEmpty { Titration.preset(settings.medication) }
            existing.forEach { add(Format.dose(it.doseMg) to if (it.days == 0) "" else (it.days / unitDays).toString()) }
        }
    }
    var start by remember {
        mutableStateOf(if (settings.titrationStartDay > 0) LocalDate.ofEpochDay(settings.titrationStartDay) else LocalDate.now())
    }
    var pickStart by remember { mutableStateOf(false) }

    fun parsed(): List<TitrationStep> = steps.mapIndexedNotNull { i, (dose, dur) ->
        val d = Format.parse(dose) ?: return@mapIndexedNotNull null
        val n = if (i == steps.lastIndex) 0 else (dur.trim().toIntOrNull() ?: 0) * unitDays
        TitrationStep(d, n)
    }

    EditorSheet(
        title = stringResource(R.string.titration_title),
        canSave = !enabled || parsed().isNotEmpty(),
        onDismiss = onDismiss,
        onSave = { onSave(enabled, parsed(), start) },
        onDelete = null,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.titration_enable), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Switch(checked = enabled, onCheckedChange = { enabled = it })
        }
        if (enabled) {
            Text(stringResource(R.string.titration_help), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = { pickStart = true }) {
                Text(stringResource(R.string.titration_start, Format.date(start)))
            }
            steps.forEachIndexed { i, (dose, dur) ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${i + 1}.", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = dose,
                        onValueChange = { steps[i] = it to dur },
                        suffix = { Text("mg") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                    if (i < steps.lastIndex) {
                        OutlinedTextField(
                            value = dur,
                            onValueChange = { steps[i] = dose to it },
                            suffix = { Text(stringResource(if (weekly) R.string.weeks_short else R.string.days_short_unit)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Text(
                            stringResource(R.string.titration_maintenance_short),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f).padding(start = 8.dp),
                        )
                    }
                    IconButton(onClick = { steps.removeAt(i) }, enabled = steps.size > 1) {
                        Icon(Icons.Outlined.Close, stringResource(R.string.delete))
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { steps.add((steps.lastOrNull()?.first ?: "") to (if (weekly) "4" else "7")) }) {
                    Text(stringResource(R.string.titration_add_step))
                }
                Button(onClick = {
                    steps.clear()
                    Titration.preset(settings.medication).forEach {
                        steps.add(Format.dose(it.doseMg) to if (it.days == 0) "" else (it.days / unitDays).toString())
                    }
                }) { Text(stringResource(R.string.titration_preset)) }
            }
        }
    }
    if (pickStart) {
        DatePickerDialogFor(start, { pickStart = false }) { start = it; pickStart = false }
    }
}

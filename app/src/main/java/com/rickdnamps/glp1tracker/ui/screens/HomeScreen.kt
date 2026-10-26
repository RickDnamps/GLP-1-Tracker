package com.rickdnamps.glp1tracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Healing
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.Vaccines
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Scale
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rickdnamps.glp1tracker.R
import com.rickdnamps.glp1tracker.data.DailyLogEntity
import com.rickdnamps.glp1tracker.domain.Format
import com.rickdnamps.glp1tracker.domain.InjectionSite
import com.rickdnamps.glp1tracker.domain.Pharmacokinetics
import com.rickdnamps.glp1tracker.ui.AppViewModel
import com.rickdnamps.glp1tracker.ui.UiState
import com.rickdnamps.glp1tracker.ui.components.LineChart
import com.rickdnamps.glp1tracker.ui.components.SectionCard
import com.rickdnamps.glp1tracker.ui.components.StatTile
import com.rickdnamps.glp1tracker.ui.daysUntil
import com.rickdnamps.glp1tracker.ui.doses
import com.rickdnamps.glp1tracker.ui.medicationName
import com.rickdnamps.glp1tracker.ui.nextDue
import com.rickdnamps.glp1tracker.ui.siteLastUse
import com.rickdnamps.glp1tracker.ui.weightStats
import com.rickdnamps.glp1tracker.ui.weightText
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun HomeScreen(
    state: UiState,
    vm: AppViewModel,
    contentPadding: PaddingValues,
    openEditor: (Editor) -> Unit,
) {
    val settings = state.settings
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Greeting(settings.name) }
        item { NextDoseCard(state, onLog = { openEditor(Editor.Injection()) }) }
        if (settings.penTrackingEnabled && !settings.medication.isOral) {
            item { PenCard(state, onRefill = vm::refillPen) }
        }
        item { QuickActions(openEditor) }
        item { LevelCard(state) }
        item { WeightCard(state, onAdd = { openEditor(Editor.Weight()) }) }
        item { TodayCard(state, vm) }
        item {
            Text(
                stringResource(R.string.disclaimer_short),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

@Composable
private fun Greeting(name: String) {
    val hour = LocalTime.now().hour
    val greeting = stringResource(
        when (hour) {
            in 5..11 -> R.string.greeting_morning
            in 12..17 -> R.string.greeting_afternoon
            else -> R.string.greeting_evening
        }
    )
    Column(Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
        Text(
            if (name.isBlank()) greeting else "$greeting, $name",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            Format.date(LocalDate.now()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NextDoseCard(state: UiState, onLog: () -> Unit) {
    val settings = state.settings
    val due = nextDue(settings, state.injections)
    val days = daysUntil(due)
    val late = days < 0
    val container = when {
        late -> MaterialTheme.colorScheme.errorContainer
        days == 0L -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val onContainer = when {
        late -> MaterialTheme.colorScheme.onErrorContainer
        days == 0L -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }
    val suggested = remember(state.injections) { InjectionSite.suggest(siteLastUse(state.injections)) }

    Card(
        colors = CardDefaults.cardColors(containerColor = container, contentColor = onContainer),
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (settings.medication.isOral) Icons.Outlined.Medication else Icons.Outlined.Vaccines, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.next_dose), style = MaterialTheme.typography.titleMedium)
            }
            Text(
                when {
                    late -> pluralStringResource(R.plurals.late_by_days, (-days).toInt(), (-days).toInt())
                    days == 0L -> stringResource(R.string.due_today)
                    days == 1L -> stringResource(R.string.due_tomorrow)
                    else -> pluralStringResource(R.plurals.in_days, days.toInt(), days.toInt())
                },
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(R.string.due_on, Format.weekdayDate(due), Format.time(due)),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                "${medicationName(settings.medication, settings)} · ${Format.dose(settings.doseMg)} mg",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (!settings.medication.isOral) {
                Text(
                    stringResource(R.string.suggested_site, stringResource(suggested.nameRes)),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            state.injections.firstOrNull()?.let {
                Text(
                    stringResource(R.string.last_dose_on, Format.dateTime(it.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(Modifier.height(4.dp))
            Button(onClick = onLog, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Add, null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.log_dose))
            }
        }
    }
}

@Composable
private fun PenCard(state: UiState, onRefill: () -> Unit) {
    val s = state.settings
    SectionCard(
        title = stringResource(R.string.pen_title),
        icon = Icons.Outlined.Healing,
        action = { TextButton(onClick = onRefill) { Text(stringResource(R.string.new_pen)) } },
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
        if (s.penDosesRemaining <= 1) {
            Text(
                stringResource(R.string.pen_low_warning),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun QuickActions(openEditor: (Editor) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        QuickAction(Icons.Outlined.MonitorWeight, stringResource(R.string.weight), Modifier.weight(1f)) {
            openEditor(Editor.Weight())
        }
        QuickAction(Icons.Outlined.Scale, stringResource(R.string.scale_short), Modifier.weight(1f)) {
            openEditor(Editor.Scale)
        }
        QuickAction(Icons.Outlined.Healing, stringResource(R.string.side_effect), Modifier.weight(1f)) {
            openEditor(Editor.SideEffect())
        }
    }
}

@Composable
private fun QuickAction(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick, modifier = modifier.height(52.dp)) {
        Icon(icon, null, Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, maxLines = 1)
    }
}

@Composable
private fun LevelCard(state: UiState) {
    val settings = state.settings
    val now = System.currentTimeMillis()
    val dayMs = 86_400_000L
    val doseList = remember(state.injections, settings) { doses(settings, state.injections) }
    val curve = remember(doseList, now / 3_600_000) {
        Pharmacokinetics.curve(doseList, now - 21 * dayMs, now + 7 * dayMs, 160)
    }
    val current = Pharmacokinetics.levelAt(doseList, now)
    SectionCard(title = stringResource(R.string.estimated_level), icon = Icons.AutoMirrored.Outlined.ShowChart) {
        if (state.injections.isEmpty()) {
            Text(stringResource(R.string.no_dose_yet), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text(
                stringResource(R.string.current_level, Format.number(current, 2)),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            LineChart(
                points = curve,
                height = 150.dp,
                marker = now,
                dashedAfter = now,
                yMinZero = true,
                yLabel = { Format.number(it, 1) },
                xLabel = { Format.shortDate(it) },
            )
            Text(
                stringResource(R.string.level_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WeightCard(state: UiState, onAdd: () -> Unit) {
    val settings = state.settings
    val stats = weightStats(settings, state.weights)
    SectionCard(
        title = stringResource(R.string.weight),
        icon = Icons.AutoMirrored.Outlined.TrendingDown,
        action = { TextButton(onClick = onAdd) { Text(stringResource(R.string.add)) } },
    ) {
        if (stats.currentKg == null) {
            Text(stringResource(R.string.no_weight_yet), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Row(Modifier.fillMaxWidth()) {
                StatTile(stringResource(R.string.current), weightText(stats.currentKg, settings), Modifier.weight(1f), emphasize = true)
                stats.lostKg?.let {
                    StatTile(
                        stringResource(if (it >= 0) R.string.lost else R.string.gained),
                        weightText(kotlin.math.abs(it), settings),
                        Modifier.weight(1f),
                    )
                }
                stats.percentLost?.let {
                    StatTile(stringResource(R.string.percent_lost), Format.oneDecimal(it) + " %", Modifier.weight(1f))
                }
            }
            if (stats.progressToGoal != null && stats.goalKg != null) {
                LinearProgressIndicator(progress = { stats.progressToGoal }, modifier = Modifier.fillMaxWidth())
                Text(
                    stringResource(
                        R.string.goal_progress,
                        (stats.progressToGoal * 100).toInt(),
                        weightText(stats.goalKg, settings),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TodayCard(state: UiState, vm: AppViewModel) {
    val today = LocalDate.now()
    val log = state.dailyLogs.firstOrNull { it.date == today.toString() } ?: DailyLogEntity(today.toString())
    val s = state.settings
    SectionCard(title = stringResource(R.string.today)) {
        GoalRow(
            icon = Icons.Outlined.WaterDrop,
            label = stringResource(R.string.water),
            value = log.waterMl,
            goal = s.waterGoalMl,
            unit = "ml",
            stepLabel = "+250",
            onAdd = { vm.updateDailyLog(today) { it.copy(waterMl = it.waterMl + 250) } },
        )
        GoalRow(
            icon = Icons.Outlined.Restaurant,
            label = stringResource(R.string.protein),
            value = log.proteinG,
            goal = s.proteinGoalG,
            unit = "g",
            stepLabel = "+10",
            onAdd = { vm.updateDailyLog(today) { it.copy(proteinG = it.proteinG + 10) } },
        )
    }
}

@Composable
fun GoalRow(
    icon: ImageVector,
    label: String,
    value: Int,
    goal: Int,
    unit: String,
    stepLabel: String,
    onAdd: () -> Unit,
    onRemove: (() -> Unit)? = null,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("$label · $value / $goal $unit", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { if (goal > 0) (value.toFloat() / goal).coerceIn(0f, 1f) else 0f },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.width(8.dp))
        if (onRemove != null) {
            OutlinedButton(onClick = onRemove, contentPadding = PaddingValues(horizontal = 10.dp)) { Text("−") }
            Spacer(Modifier.width(4.dp))
        }
        OutlinedButton(onClick = onAdd, contentPadding = PaddingValues(horizontal = 10.dp)) {
            Text(stepLabel)
        }
    }
}


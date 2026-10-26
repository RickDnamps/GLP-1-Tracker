package com.rickdnamps.glp1tracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.rickdnamps.glp1tracker.ui.components.AnimatedValue
import com.rickdnamps.glp1tracker.ui.components.GlassTile
import com.rickdnamps.glp1tracker.ui.components.HeroCard
import com.rickdnamps.glp1tracker.ui.components.ProgressRing
import com.rickdnamps.glp1tracker.ui.components.pulseAlpha
import com.rickdnamps.glp1tracker.ui.theme.LocalBrand
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
import androidx.compose.material.icons.outlined.Lightbulb
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rickdnamps.glp1tracker.R
import com.rickdnamps.glp1tracker.data.DailyLogEntity
import com.rickdnamps.glp1tracker.domain.Format
import com.rickdnamps.glp1tracker.domain.InjectionSite
import com.rickdnamps.glp1tracker.domain.Pharmacokinetics
import com.rickdnamps.glp1tracker.domain.WeeklyRecaps
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
    openRoute: (String) -> Unit = {},
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
        item {
            NextDoseCard(
                state,
                onLog = { openEditor(Editor.Injection()) },
                onSkip = { vm.skipDose(it) },
            )
        }
        if (WeeklyRecaps.isRecapDay(LocalDate.now())) {
            item { WeeklyRecapCard(state) }
        }
        if (settings.titrationEnabled) {
            item { TitrationCard(settings, onApplyDose = { vm.applyPlannedDose(it) }, onEdit = { openEditor(Editor.Titration) }) }
        }
        if (settings.penTrackingEnabled && !settings.medication.isOral) {
            item { SupplyCard(settings, nextDue(settings, state.injections).toLocalDate(), onNewPen = vm::refillPen) }
        }
        item { OverviewStrip(state) }
        item { QuickActions(openEditor) }
        item { InsightsTeaser(state, openRoute) }
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
    Row(Modifier.padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                Format.date(LocalDate.now()),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                if (name.isBlank()) greeting else "$greeting, $name 👋",
                style = MaterialTheme.typography.headlineMedium,
            )
        }
        if (name.isNotBlank()) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(LocalBrand.current.hero),
                contentAlignment = Alignment.Center,
            ) {
                Text(name.take(1).uppercase(), style = MaterialTheme.typography.titleLarge, color = Color.White)
            }
        }
    }
}

@Composable
private fun NextDoseCard(state: UiState, onLog: () -> Unit, onSkip: (LocalDate) -> Unit) {
    val settings = state.settings
    val due = nextDue(settings, state.injections)
    val days = daysUntil(due)
    val late = days < 0
    val scheme = MaterialTheme.colorScheme
    val brush = if (late) Brush.linearGradient(listOf(scheme.error, Color(0xFFE8710A))) else LocalBrand.current.hero
    val interval = settings.intervalDays.coerceAtLeast(1)
    val progress = if (late) 1f else (1f - days.toFloat() / interval).coerceIn(0.04f, 1f)
    val suggested = remember(state.injections) { InjectionSite.suggest(siteLastUse(state.injections)) }
    val urgent = days <= 0
    val pulse = if (urgent) pulseAlpha() else null

    HeroCard(brush = brush) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProgressRing(
                progress = progress,
                size = 116.dp,
                stroke = 11.dp,
                trackColor = Color.White.copy(alpha = 0.22f),
                colors = listOf(Color.White, Color.White.copy(alpha = 0.75f)),
                modifier = Modifier.graphicsLayer { alpha = pulse?.value ?: 1f },
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AnimatedValue(
                        text = if (days == 0L) "💉" else kotlin.math.abs(days).toString(),
                        style = MaterialTheme.typography.displaySmall,
                        color = Color.White,
                    )
                    Text(
                        if (days == 0L) stringResource(R.string.ring_today)
                        else pluralStringResource(R.plurals.ring_days, kotlin.math.abs(days).toInt()),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                }
            }
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(R.string.next_dose).uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.8f),
                )
                Text(
                    when {
                        late -> pluralStringResource(R.plurals.late_by_days, (-days).toInt(), (-days).toInt())
                        days == 0L -> stringResource(R.string.due_today)
                        days == 1L -> stringResource(R.string.due_tomorrow)
                        else -> pluralStringResource(R.plurals.in_days, days.toInt(), days.toInt())
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                )
                Text(
                    stringResource(R.string.due_on, Format.weekdayDate(due), Format.time(due)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            GlassTile(Modifier.weight(1f)) {
                Column {
                    Text(stringResource(R.string.medication), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                    Text(
                        "${medicationName(settings.medication, settings).substringBefore(" (")} · ${Format.dose(settings.doseMg)} mg",
                        style = MaterialTheme.typography.titleSmall, color = Color.White, maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (!settings.medication.isOral) {
                GlassTile(Modifier.weight(1f)) {
                    Column {
                        Text(stringResource(R.string.next_site), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                        Text(
                            stringResource(suggested.nameRes),
                            style = MaterialTheme.typography.titleSmall, color = Color.White, maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
        if (late) {
            Spacer(Modifier.height(12.dp))
            MissedDoseBlock(settings, due, Color.White, onSkip = { onSkip(due.toLocalDate()) })
        }
        state.injections.firstOrNull()?.let {
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.last_dose_on, Format.dateTime(it.timestamp)),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f),
            )
        }
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = onLog,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = if (late) scheme.error else scheme.primary,
            ),
        ) {
            Icon(Icons.Outlined.Add, null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.log_dose), style = MaterialTheme.typography.titleMedium)
        }
    }
}

/** Three key numbers at a glance under the hero. */
@Composable
private fun OverviewStrip(state: UiState) {
    val settings = state.settings
    val stats = weightStats(settings, state.weights)
    val level = Pharmacokinetics.levelAt(doses(settings, state.injections), System.currentTimeMillis())
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        OverviewTile(
            stringResource(R.string.current),
            stats.currentKg?.let { weightText(it, settings) } ?: "—",
            Modifier.weight(1f),
        )
        OverviewTile(
            stringResource(R.string.percent_lost),
            stats.percentLost?.let { Format.oneDecimal(it) + " %" } ?: "—",
            Modifier.weight(1f),
            highlight = (stats.percentLost ?: 0.0) > 0,
        )
        OverviewTile(
            stringResource(R.string.level_short),
            if (state.injections.isEmpty()) "—" else Format.number(level, if (level < 1) 2 else 1) + " mg",
            Modifier.weight(1f),
        )
    }
}

@Composable
private fun OverviewTile(label: String, value: String, modifier: Modifier, highlight: Boolean = false) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (highlight) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            AnimatedValue(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = if (highlight) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
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
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(68.dp),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, Modifier.size(22.dp))
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
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
        val food = foodTotals(state.foods, today)
        GoalRow(
            icon = Icons.Outlined.Restaurant,
            label = stringResource(R.string.protein),
            value = log.proteinG + food.proteinG.toInt(),
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

/** Entry to the insights and tools screens, with one live headline from the user's data. */
@Composable
private fun InsightsTeaser(state: UiState, openRoute: (String) -> Unit) {
    val doseTimes = remember(state.injections) { state.injections.map { it.timestamp }.sorted() }
    val interval = state.settings.intervalDays.coerceIn(1, 14)
    val headline = remember(state.sideEffects, doseTimes, interval) {
        if (interval < 2) null
        else state.sideEffects.groupBy { com.rickdnamps.glp1tracker.domain.SideEffectType.fromName(it.type) }
            .mapValues { (_, l) -> com.rickdnamps.glp1tracker.domain.Insights.cycleProfile(l.map { it.timestamp }, doseTimes, interval) }
            .filter { it.value.peakDay != null }
            .maxByOrNull { it.value.total }
    }
    SectionCard(title = stringResource(R.string.insights_title), icon = Icons.Outlined.Lightbulb) {
        Text(
            if (headline != null) stringResource(
                R.string.insights_peak,
                stringResource(headline.key.nameRes),
                headline.value.peakDay ?: 0,
                (headline.value.peakShare * 100).toInt(),
            ) else stringResource(R.string.insights_teaser),
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = { openRoute("insights") }) { Text(stringResource(R.string.insights_open)) }
            OutlinedButton(onClick = { openRoute("tools") }) { Text(stringResource(R.string.tools_title)) }
        }
    }
}

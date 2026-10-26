package com.rickdnamps.glp1tracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.graphics.Color
import com.rickdnamps.glp1tracker.ui.components.AnimatedValue
import com.rickdnamps.glp1tracker.ui.components.HeroCard
import com.rickdnamps.glp1tracker.ui.components.Pill
import com.rickdnamps.glp1tracker.ui.components.ProgressRing
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Scale
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rickdnamps.glp1tracker.R
import com.rickdnamps.glp1tracker.data.UserSettings
import com.rickdnamps.glp1tracker.data.WeightEntity
import com.rickdnamps.glp1tracker.domain.Format
import com.rickdnamps.glp1tracker.ui.UiState
import com.rickdnamps.glp1tracker.ui.components.LineChart
import com.rickdnamps.glp1tracker.ui.components.SectionCard
import com.rickdnamps.glp1tracker.ui.components.StatTile
import com.rickdnamps.glp1tracker.ui.weightStats
import com.rickdnamps.glp1tracker.ui.milestones
import com.rickdnamps.glp1tracker.domain.GoalForecast
import androidx.compose.runtime.remember
import com.rickdnamps.glp1tracker.ui.weightText
import kotlin.math.abs

@Composable
fun ProgressScreen(
    state: UiState,
    contentPadding: PaddingValues,
    openEditor: (Editor) -> Unit,
    onOpenPhotos: () -> Unit,
    onSyncHealthConnect: () -> Unit,
) {
    val settings = state.settings
    val stats = weightStats(settings, state.weights)
    // Newest first for the list, each with the change since the previous entry.
    val history = state.weights.mapIndexed { i, w -> w to state.weights.getOrNull(i - 1)?.let { w.weightKg - it.weightKg } }.reversed()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 88.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FilledTonalButton(onClick = { openEditor(Editor.Scale) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.Scale, null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.scale_weigh_me), maxLines = 1)
                }
                if (settings.healthConnectAutoImport) {
                    OutlinedButton(onClick = onSyncHealthConnect) {
                        Icon(Icons.Outlined.Sync, null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.hc_short), maxLines = 1)
                    }
                }
            }
        }
        item { ProgressHero(stats, settings) }
        item { PhotosEntryCard(state.photos, onClick = onOpenPhotos) }
        item {
            SectionCard(title = stringResource(R.string.summary), icon = Icons.Outlined.Insights) {
                Row(Modifier.fillMaxWidth()) {
                    StatTile(stringResource(R.string.start), stats.startKg?.let { weightText(it, settings) } ?: "—", Modifier.weight(1f))
                    StatTile(stringResource(R.string.goal), stats.goalKg?.let { weightText(it, settings) } ?: "—", Modifier.weight(1f))
                    StatTile(stringResource(R.string.bmi), stats.bmi?.let { Format.oneDecimal(it) } ?: "—", Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth()) {
                    StatTile(
                        stringResource(R.string.weekly_average),
                        stats.weeklyAverageKg?.let { weightText(-it, settings, signed = true) } ?: "—",
                        Modifier.weight(1f),
                    )
                    StatTile(stringResource(R.string.to_goal), stats.remainingKg?.let { weightText(it, settings) } ?: "—", Modifier.weight(1f))
                    StatTile(stringResource(R.string.entries), state.weights.size.toString(), Modifier.weight(1f))
                }
            }
        }
        item {
            val progress = remember(state) { milestones(state) }
            val points = remember(state.weights) { state.weights.map { it.timestamp to it.weightKg } }
            val goal = settings.goalWeightKg
            val now = System.currentTimeMillis()
            val forecast = remember(points, goal) { if (goal > 0) GoalForecast.predict(points, goal, now) else null }
            val rate = remember(points) { GoalForecast.weeklyRateKg(points, now) }
            MilestonesCard(progress, forecast, rate, settings)
        }
        item { WeightChartCard(state.weights, settings, stats.goalKg, doseChanges(state)) }
        item { BodyCompositionCard(state.weights, settings) }
        item { MeasurementsCard(state.measurements, settings, openEditor) }
        item {
            Text(
                stringResource(R.string.history),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp),
            )
        }
        if (history.isEmpty()) {
            item { Text(stringResource(R.string.no_weight_yet), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(history, key = { it.first.id }) { (entry, delta) ->
            WeightRow(entry, delta, settings) { openEditor(Editor.Weight(entry)) }
        }
    }
}

@Composable
private fun WeightChartCard(weights: List<WeightEntity>, settings: UserSettings, goalKg: Double?, events: List<Pair<Long, String>>) {
    var rangeDays by rememberSaveable { mutableIntStateOf(0) }
    val now = System.currentTimeMillis()
    val filtered = if (rangeDays == 0) weights else weights.filter { it.timestamp >= now - rangeDays * 86_400_000L }
    val unit = settings.weightUnit
    SectionCard(title = stringResource(R.string.weight_trend), icon = Icons.AutoMirrored.Outlined.TrendingDown) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(30 to R.string.range_1m, 90 to R.string.range_3m, 180 to R.string.range_6m, 0 to R.string.range_all).forEach { (d, label) ->
                FilterChip(selected = rangeDays == d, onClick = { rangeDays = d }, label = { Text(stringResource(label)) })
            }
        }
        LineChart(
            points = filtered.map { it.timestamp to unit.fromKg(it.weightKg) },
            goal = goalKg?.let { unit.fromKg(it) },
            showDots = filtered.size <= 40,
            events = events,
            yLabel = { Format.number(it, 1) },
            xLabel = { Format.shortDate(it) },
            height = 200.dp,
            emptyText = stringResource(R.string.chart_need_two),
        )
    }
}

@Composable
private fun WeightRow(entry: WeightEntity, delta: Double?, settings: UserSettings, onClick: () -> Unit) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 12.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(weightText(entry.weightKg, settings), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(
                    Format.dateTime(entry.timestamp) + (entry.waistCm?.let { " · " + Format.oneDecimal(it) + " cm" } ?: "") +
                        (entry.bodyFatPct?.let { " · " + Format.oneDecimal(it) + " % " + stringResource(R.string.comp_fat).lowercase() } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (entry.notes.isNotBlank()) Text(entry.notes, style = MaterialTheme.typography.bodySmall, maxLines = 2)
            }
            if (delta != null && abs(delta) > 0.0001) {
                Text(
                    weightText(delta, settings, signed = true),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (delta < 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}

@Composable
private fun ProgressHero(stats: com.rickdnamps.glp1tracker.ui.WeightStats, settings: UserSettings) {
    HeroCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProgressRing(
                progress = stats.progressToGoal ?: 0f,
                size = 128.dp,
                stroke = 12.dp,
                trackColor = Color.White.copy(alpha = 0.22f),
                colors = listOf(Color.White, Color.White.copy(alpha = 0.7f)),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AnimatedValue(
                        text = stats.progressToGoal?.let { "${(it * 100).toInt()} %" } ?: "—",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                    )
                    Text(stringResource(R.string.of_goal), style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.85f))
                }
            }
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.current).uppercase(), style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.8f))
                AnimatedValue(
                    text = stats.currentKg?.let { weightText(it, settings) } ?: "—",
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White,
                )
                val lost = stats.lostKg
                if (lost != null) {
                    Pill(
                        (if (lost >= 0) "−" else "+") + weightText(abs(lost), settings) +
                            (stats.percentLost?.let { " · " + Format.oneDecimal(abs(it)) + " %" } ?: ""),
                    )
                }
            }
        }
    }
}

/** Points where the logged dose changed, labelled "0,5 mg", for chart markers. */
private fun doseChanges(state: UiState): List<Pair<Long, String>> {
    val asc = state.injections.sortedBy { it.timestamp }
    return asc.filterIndexed { i, inj -> i == 0 || inj.doseMg != asc[i - 1].doseMg }
        .map { it.timestamp to Format.dose(it.doseMg) + " mg" }
}

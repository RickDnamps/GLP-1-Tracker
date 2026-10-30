package com.rickdnamps.glp1tracker.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Healing
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rickdnamps.glp1tracker.R
import com.rickdnamps.glp1tracker.domain.Format
import com.rickdnamps.glp1tracker.domain.Insights
import com.rickdnamps.glp1tracker.domain.SideEffectType
import com.rickdnamps.glp1tracker.ui.AppViewModel
import com.rickdnamps.glp1tracker.ui.UiState
import com.rickdnamps.glp1tracker.ui.components.LineChart
import com.rickdnamps.glp1tracker.ui.components.SectionCard
import com.rickdnamps.glp1tracker.ui.weightText
import java.time.LocalDate
import kotlin.math.roundToInt

@Composable
fun InsightsScreen(state: UiState, vm: AppViewModel, contentPadding: PaddingValues) {
    val s = state.settings
    val doseTimes = remember(state.injections) { state.injections.map { it.timestamp }.sorted() }
    val interval = s.intervalDays.coerceIn(1, 14)
    val now = System.currentTimeMillis()
    val weights = remember(state.weights) { state.weights.map { it.timestamp to it.weightKg } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                stringResource(R.string.insights_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // 1. Side effects by day of the injection cycle
        item {
            SectionCard(title = stringResource(R.string.insights_side_effects_title), icon = Icons.Outlined.Healing) {
                val profiles = remember(state.sideEffects, doseTimes, interval) {
                    state.sideEffects.groupBy { SideEffectType.fromName(it.type) }
                        .mapValues { (_, list) -> Insights.cycleProfile(list.map { it.timestamp }, doseTimes, interval) }
                        .filter { it.value.peakDay != null }
                        .entries.sortedByDescending { it.value.total }
                        .take(3)
                }
                if (interval < 2 || profiles.isEmpty()) {
                    Text(stringResource(R.string.insights_need_more_side_effects), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (interval >= 2) profiles.forEach { (type, p) ->
                    Text(
                        stringResource(
                            R.string.insights_peak,
                            stringResource(type.nameRes),
                            p.peakDay ?: 0,
                            (p.peakShare * 100).roundToInt(),
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                    CycleBars(p.counts.map { it.toFloat() }, p.peakDay, MaterialTheme.colorScheme.tertiary)
                }
            }
        }

        // 2. Appetite through the cycle
        item {
            SectionCard(title = stringResource(R.string.insights_appetite_title), icon = Icons.Outlined.Restaurant) {
                val byDay = remember(state.dailyLogs, doseTimes, interval) {
                    val scores = state.dailyLogs.mapNotNull { l ->
                        runCatching { LocalDate.parse(l.date) }.getOrNull()?.let { it to l.appetite }
                    }
                    Insights.scoreByCycleDay(scores, doseTimes, interval)
                }
                val known = byDay.count { !it.isNaN() }
                if (interval < 2 || known < 3) {
                    Text(stringResource(R.string.insights_need_more_appetite), color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    val back = Insights.firstDayAtLeast(byDay, 3.5)
                    Text(
                        if (back != null) stringResource(R.string.insights_appetite_back, back)
                        else stringResource(R.string.insights_appetite_steady),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                    CycleBars(byDay.map { if (it.isNaN()) 0f else it.toFloat() }, back, MaterialTheme.colorScheme.primary, maxValue = 5f)
                }
            }
        }

        // 3. Trend weight and plateau
        item {
            SectionCard(title = stringResource(R.string.insights_trend_title), icon = Icons.AutoMirrored.Outlined.TrendingDown) {
                val result = remember(weights) { Insights.plateau(weights, now) }
                val trend = remember(weights) { Insights.trend(weights) }
                Text(
                    when (result.phase) {
                        Insights.WeightPhase.PLATEAU -> stringResource(R.string.insights_plateau, result.days)
                        Insights.WeightPhase.LOSING -> stringResource(
                            R.string.insights_losing,
                            weightText(-(result.trendSlopeKgPerWeek ?: 0.0), s),
                        )
                        Insights.WeightPhase.GAINING -> stringResource(R.string.insights_gaining)
                        Insights.WeightPhase.NOT_ENOUGH_DATA -> stringResource(R.string.insights_need_more_weights)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                if (result.phase == Insights.WeightPhase.PLATEAU) {
                    Text(stringResource(R.string.insights_plateau_tips), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                LineChart(
                    points = trend.map { it.first to s.weightUnit.fromKg(it.second) },
                    height = 150.dp,
                    yLabel = { Format.number(it, 1) },
                    xLabel = { Format.shortDate(it) },
                    emptyText = stringResource(R.string.chart_need_two),
                )
                Insights.weekOverWeek(weights, now)?.let { diff ->
                    Text(
                        stringResource(R.string.week_compare, weightText(diff, s, signed = true)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (diff <= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(stringResource(R.string.insights_trend_explain), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // 4. Pace of loss
        item {
            SectionCard(title = stringResource(R.string.insights_pace_title), icon = Icons.Outlined.Speed) {
                val pace = remember(weights) { Insights.pace(weights, now) }
                if (pace == null) {
                    Text(stringResource(R.string.insights_need_more_weights), color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    val (p, pct) = pace
                    PaceGauge(pct)
                    Text(
                        stringResource(
                            when (p) {
                                Insights.Pace.SLOW -> R.string.insights_pace_slow
                                Insights.Pace.HEALTHY -> R.string.insights_pace_healthy
                                Insights.Pace.FAST -> R.string.insights_pace_fast
                                Insights.Pace.VERY_FAST -> R.string.insights_pace_very_fast
                            },
                            Format.number(pct, 1),
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = when (p) {
                            Insights.Pace.VERY_FAST -> MaterialTheme.colorScheme.error
                            Insights.Pace.FAST -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                    )
                }
            }
        }

        // 5. Protein coach
        item {
            SectionCard(title = stringResource(R.string.insights_protein_title), icon = Icons.Outlined.FitnessCenter) {
                val current = state.weights.lastOrNull()?.weightKg
                if (current == null) {
                    Text(stringResource(R.string.no_weight_yet), color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    val range = Insights.proteinRange(current, s.goalWeightKg.takeIf { it > 0 })
                    val week = (0 until 7).map { LocalDate.now().minusDays(it.toLong()) }
                    val avg = week.map { d ->
                        val log = state.dailyLogs.firstOrNull { it.date == d.toString() }?.proteinG ?: 0
                        log + foodTotals(state.foods, d).proteinG.toInt()
                    }.average()
                    Text(
                        stringResource(R.string.insights_protein_range, range.first, range.last),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        stringResource(R.string.insights_protein_avg, avg.roundToInt()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (avg >= range.first) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                    )
                    Text(stringResource(R.string.insights_protein_tip), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (s.proteinGoalG < range.first) {
                        FilledTonalButton(onClick = { vm.updateSettings { it.copy(proteinGoalG = range.first) } }) {
                            Text(stringResource(R.string.insights_protein_set, range.first))
                        }
                    }
                }
            }
        }
        item {
            Text(
                stringResource(R.string.insights_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** One bar per day of the injection cycle (D0 = injection day), [highlight] drawn in [color]. */
@Composable
private fun CycleBars(values: List<Float>, highlight: Int?, color: Color, maxValue: Float? = null) {
    val measurer = rememberTextMeasurer()
    val muted = MaterialTheme.colorScheme.surfaceVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val prefix = stringResource(R.string.insights_day_prefix)
    Canvas(Modifier.fillMaxWidth().height(110.dp)) {
        val n = values.size.coerceAtLeast(1)
        val max = (maxValue ?: values.maxOrNull() ?: 1f).coerceAtLeast(1f)
        val labelH = 16.dp.toPx()
        val slot = size.width / n
        val barW = slot * 0.62f
        val chartH = size.height - labelH
        values.forEachIndexed { i, v ->
            val h = (v / max) * (chartH - 4.dp.toPx())
            val x = i * slot + (slot - barW) / 2
            drawRoundRect(
                color = if (i == highlight) color else muted,
                topLeft = Offset(x, chartH - h.coerceAtLeast(3f)),
                size = Size(barW, h.coerceAtLeast(3f)),
                cornerRadius = CornerRadius(6.dp.toPx()),
            )
            val layout = measurer.measure("$prefix$i", TextStyle(fontSize = 10.sp, color = labelColor))
            drawText(layout, topLeft = Offset(i * slot + (slot - layout.size.width) / 2, size.height - layout.size.height))
        }
    }
}

/** Horizontal gauge 0–2 %/week with the healthy zone (0.5–1 %) highlighted. */
@Composable
private fun PaceGauge(percentPerWeek: Double) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    val healthy = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    val fast = MaterialTheme.colorScheme.error.copy(alpha = 0.25f)
    val needle = MaterialTheme.colorScheme.onSurface
    Canvas(Modifier.fillMaxWidth().height(28.dp)) {
        val maxPct = 2f
        fun x(p: Float) = (p / maxPct).coerceIn(0f, 1f) * size.width
        val h = 12.dp.toPx()
        val top = (size.height - h) / 2
        drawRoundRect(track, Offset(0f, top), Size(size.width, h), CornerRadius(h / 2))
        drawRect(healthy, Offset(x(0.5f), top), Size(x(1f) - x(0.5f), h))
        drawRect(fast, Offset(x(1.5f), top), Size(size.width - x(1.5f), h))
        val nx = x(percentPerWeek.toFloat())
        drawRoundRect(needle, Offset(nx - 2.dp.toPx(), 0f), Size(4.dp.toPx(), size.height), CornerRadius(2.dp.toPx()))
    }
}

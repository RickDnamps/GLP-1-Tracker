package com.rickdnamps.glp1tracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.rickdnamps.glp1tracker.domain.TherapyStats
import com.rickdnamps.glp1tracker.ui.agoText
import com.rickdnamps.glp1tracker.ui.comfortEmoji
import com.rickdnamps.glp1tracker.ui.dueText
import com.rickdnamps.glp1tracker.ui.nextDue
import com.rickdnamps.glp1tracker.ui.peakText
import kotlin.math.roundToInt
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.PersonPinCircle
import androidx.compose.material.icons.outlined.Vaccines
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rickdnamps.glp1tracker.R
import com.rickdnamps.glp1tracker.data.InjectionEntity
import com.rickdnamps.glp1tracker.data.UserSettings
import com.rickdnamps.glp1tracker.domain.Format
import com.rickdnamps.glp1tracker.domain.InjectionSite
import com.rickdnamps.glp1tracker.domain.Medication
import com.rickdnamps.glp1tracker.domain.Pharmacokinetics
import com.rickdnamps.glp1tracker.ui.UiState
import com.rickdnamps.glp1tracker.ui.components.BodyMap
import com.rickdnamps.glp1tracker.ui.components.LineChart
import com.rickdnamps.glp1tracker.ui.components.SectionCard
import com.rickdnamps.glp1tracker.ui.doses
import com.rickdnamps.glp1tracker.ui.medicationName
import com.rickdnamps.glp1tracker.ui.siteLastUse

@Composable
fun InjectionsScreen(state: UiState, contentPadding: PaddingValues, openEditor: (Editor) -> Unit) {
    val settings = state.settings
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 88.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { TherapyCard(state) }
        if (!settings.medication.isOral) item { SiteRotationCard(state.injections) }
        item {
            Text(
                stringResource(R.string.history),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp),
            )
        }
        if (state.injections.isEmpty()) {
            item { Text(stringResource(R.string.no_dose_yet), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(state.injections, key = { it.id }) { injection ->
            InjectionRow(injection, settings) { openEditor(Editor.Injection(injection)) }
        }
    }
}

/**
 * Treatment over a period (30 d, 90 d, all): doses, average dose and interval, regularity,
 * comfort, and the estimated level with the weight drawn over it.
 */
@Composable
private fun TherapyCard(state: UiState) {
    val settings = state.settings
    var period by rememberSaveable { mutableIntStateOf(30) } // days, 0 = since the first dose
    val now = System.currentTimeMillis()
    val dayMs = 86_400_000L
    val first = state.injections.minOfOrNull { it.timestamp }
    val from = if (period == 0) minOf(first ?: now, now - 7 * dayMs) else now - period * dayMs
    val entries = remember(state.injections) { state.injections.map { TherapyStats.Entry(it.timestamp, it.doseMg, it.comfort) } }
    val summary = remember(entries, from / 3_600_000, settings.intervalDays) {
        TherapyStats.summarize(entries, from, now, settings.intervalDays)
    }
    val totalMg = state.injections.filter { it.timestamp in from..now }.sumOf { it.doseMg }
    val doseList = remember(state.injections, settings) { doses(settings, state.injections) }
    val curve = remember(doseList, from / 3_600_000, now / 3_600_000) {
        Pharmacokinetics.curve(doseList, from, now + 7 * dayMs, 200)
    }
    val weights = state.weights.filter { it.timestamp >= from }.map { it.timestamp to settings.weightUnit.fromKg(it.weightKg) }
    val weightColor = MaterialTheme.colorScheme.tertiary

    SectionCard(title = stringResource(R.string.therapy_title), icon = Icons.AutoMirrored.Outlined.ShowChart) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(30, 90, 0).forEach { d ->
                FilterChip(
                    selected = period == d,
                    onClick = { period = d },
                    label = { Text(if (d == 0) stringResource(R.string.period_all) else stringResource(R.string.days_short, d)) },
                )
            }
        }
        Row(Modifier.fillMaxWidth()) {
            SmallStat(stringResource(R.string.therapy_doses), summary.doses.toString(), Modifier.weight(1f), emphasize = true)
            SmallStat(stringResource(R.string.therapy_avg_dose), summary.averageDoseMg?.let { Format.dose(it) + " mg" } ?: "—", Modifier.weight(1f))
            SmallStat(
                stringResource(R.string.therapy_interval),
                summary.averageIntervalDays?.let { Format.oneDecimal(it) + " " + stringResource(R.string.days_short_unit) } ?: "—",
                Modifier.weight(1f),
            )
        }
        Row(Modifier.fillMaxWidth()) {
            SmallStat(stringResource(R.string.therapy_on_time), summary.onTimeShare?.let { "${(it * 100).roundToInt()} %" } ?: "—", Modifier.weight(1f))
            SmallStat(
                stringResource(R.string.therapy_comfort),
                summary.averageComfort?.let { (comfortEmoji(it.roundToInt())?.plus(" ") ?: "") + Format.oneDecimal(it) } ?: "—",
                Modifier.weight(1f),
            )
            SmallStat(stringResource(R.string.therapy_total), Format.number(totalMg, 2) + " mg", Modifier.weight(1f))
        }
        if (period != 0 && (summary.doses > 0 || summary.previousDoses > 0)) {
            val diff = summary.doses - summary.previousDoses
            Text(
                stringResource(R.string.therapy_vs_previous, (if (diff > 0) "+" else "") + diff),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LineChart(
            points = if (state.injections.isEmpty()) emptyList() else curve,
            marker = now,
            dashedAfter = now,
            yMinZero = true,
            overlay = weights,
            overlayColor = weightColor,
            overlayLabel = { Format.number(it, 0) },
            yLabel = { Format.number(it, 1) },
            xLabel = { Format.shortDate(it) },
            emptyText = stringResource(R.string.no_dose_yet),
        )
        if (state.injections.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                LegendDot(MaterialTheme.colorScheme.primary, stringResource(R.string.therapy_legend_level))
                if (weights.isNotEmpty()) LegendDot(weightColor, stringResource(R.string.therapy_legend_weight, stringResource(settings.weightUnit.symbolRes)))
            }
            DoseTiming(state)
        }
        Text(
            stringResource(
                R.string.half_life_info,
                stringResource(settings.medication.molecule.nameRes),
                Format.number(settings.pkParams(settings.medication).halfLifeHours / 24.0, 1),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Last dose, next dose and the estimated peak of the current cycle. */
@Composable
internal fun DoseTiming(state: UiState) {
    val settings = state.settings
    val last = state.injections.firstOrNull() ?: return
    val now = System.currentTimeMillis()
    val due = nextDue(settings, state.injections)
    val dueMillis = Format.toMillis(due)
    val doseList = remember(state.injections, settings) { doses(settings, state.injections) }
    val peak = remember(doseList, dueMillis) {
        TherapyStats.peakTime(doseList, last.timestamp, maxOf(dueMillis, last.timestamp + 86_400_000L))
    }
    Row(Modifier.fillMaxWidth()) {
        SmallStat(stringResource(R.string.last_dose_label), agoText(last.timestamp), Modifier.weight(1f))
        SmallStat(stringResource(R.string.next_dose_label), dueText(due), Modifier.weight(1f))
        SmallStat(stringResource(R.string.peak_label), peak?.let { peakText(it, now) } ?: "—", Modifier.weight(1f))
    }
}

@Composable
internal fun SmallStat(label: String, value: String, modifier: Modifier = Modifier, emphasize: Boolean = false) {
    Column(modifier.padding(vertical = 2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (emphasize) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SiteRotationCard(injections: List<InjectionEntity>) {
    val lastUse = remember(injections) { siteLastUse(injections) }
    val suggested = InjectionSite.suggest(lastUse)
    val comfort = remember(injections) {
        TherapyStats.comfortBySite(injections.map { InjectionSite.fromName(it.site) to it.comfort })
            .filterKeys { it != InjectionSite.NONE }
    }
    SectionCard(title = stringResource(R.string.site_rotation), icon = Icons.Outlined.PersonPinCircle) {
        BodyMap(selected = null, suggested = suggested, lastUse = lastUse, onSelect = null)
        Text(
            stringResource(R.string.suggested_site, stringResource(suggested.nameRes)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.tertiary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        // Which sites hurt least, from the comfort rated after each injection.
        if (comfort.size >= 2) {
            val best = comfort.maxBy { it.value }
            val worst = comfort.minBy { it.value }
            if (best.value > worst.value) {
                Text(
                    stringResource(R.string.comfort_best_site, stringResource(best.key.nameRes), comfortEmoji(best.value.roundToInt()) ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    stringResource(R.string.comfort_worst_site, stringResource(worst.key.nameRes), comfortEmoji(worst.value.roundToInt()) ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else if (comfort.isEmpty() && injections.isNotEmpty()) {
            Text(stringResource(R.string.comfort_rate_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun InjectionRow(injection: InjectionEntity, settings: UserSettings, onClick: () -> Unit) {
    val med = Medication.fromName(injection.medication)
    val site = InjectionSite.fromName(injection.site)
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 10.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(40.dp)) {
                Icon(
                    if (med.isOral) Icons.Outlined.Medication else Icons.Outlined.Vaccines,
                    null,
                    modifier = Modifier.padding(8.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "${medicationName(med, settings)} · ${Format.dose(injection.doseMg)} mg",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    Format.dateTime(injection.timestamp) +
                        if (site != InjectionSite.NONE) " · " + stringResource(site.nameRes) else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (injection.notes.isNotBlank()) {
                    Text(injection.notes, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                }
            }
            comfortEmoji(injection.comfort)?.let { Text(it, style = MaterialTheme.typography.titleLarge) }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}

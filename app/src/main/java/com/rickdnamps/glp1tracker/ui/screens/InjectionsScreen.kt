package com.rickdnamps.glp1tracker.ui.screens

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
import com.rickdnamps.glp1tracker.ui.components.StatTile
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
        item { SummaryCard(state) }
        item { LevelHistoryCard(state) }
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

@Composable
private fun SummaryCard(state: UiState) {
    val total = state.injections.size
    val totalMg = state.injections.sumOf { it.doseMg }
    val first = state.injections.lastOrNull()
    SectionCard {
        Row(Modifier.fillMaxWidth()) {
            StatTile(stringResource(R.string.doses_logged), total.toString(), Modifier.weight(1f), emphasize = true)
            StatTile(stringResource(R.string.total_mg), Format.number(totalMg, 2) + " mg", Modifier.weight(1f))
            StatTile(
                stringResource(R.string.since),
                first?.let { Format.shortDate(it.timestamp) } ?: "—",
                Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun LevelHistoryCard(state: UiState) {
    val settings = state.settings
    var rangeDays by rememberSaveable { mutableIntStateOf(30) }
    val now = System.currentTimeMillis()
    val dayMs = 86_400_000L
    val doseList = remember(state.injections, settings) { doses(settings, state.injections) }
    val curve = remember(doseList, rangeDays, now / 3_600_000) {
        Pharmacokinetics.curve(doseList, now - rangeDays * dayMs, now + 7 * dayMs, 200)
    }
    SectionCard(title = stringResource(R.string.estimated_level), icon = Icons.AutoMirrored.Outlined.ShowChart) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(14, 30, 90, 180).forEach { d ->
                FilterChip(selected = rangeDays == d, onClick = { rangeDays = d }, label = { Text(stringResource(R.string.days_short, d)) })
            }
        }
        LineChart(
            points = if (state.injections.isEmpty()) emptyList() else curve,
            marker = now,
            dashedAfter = now,
            yMinZero = true,
            yLabel = { Format.number(it, 1) },
            xLabel = { Format.shortDate(it) },
            emptyText = stringResource(R.string.no_dose_yet),
        )
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

@Composable
private fun SiteRotationCard(injections: List<InjectionEntity>) {
    val lastUse = remember(injections) { siteLastUse(injections) }
    val suggested = InjectionSite.suggest(lastUse)
    SectionCard(title = stringResource(R.string.site_rotation), icon = Icons.Outlined.PersonPinCircle) {
        BodyMap(selected = null, suggested = suggested, lastUse = lastUse, onSelect = null)
        Text(
            stringResource(R.string.suggested_site, stringResource(suggested.nameRes)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.tertiary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
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
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}

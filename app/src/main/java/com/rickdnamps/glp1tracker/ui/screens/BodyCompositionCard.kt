package com.rickdnamps.glp1tracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Accessibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rickdnamps.glp1tracker.R
import com.rickdnamps.glp1tracker.data.UserSettings
import com.rickdnamps.glp1tracker.data.WeightEntity
import com.rickdnamps.glp1tracker.data.composition
import com.rickdnamps.glp1tracker.domain.BodyComp
import com.rickdnamps.glp1tracker.domain.BodyCompositionMath
import com.rickdnamps.glp1tracker.domain.CompLevel
import com.rickdnamps.glp1tracker.domain.CompMetric
import com.rickdnamps.glp1tracker.domain.Format
import com.rickdnamps.glp1tracker.ui.components.LineChart
import com.rickdnamps.glp1tracker.ui.components.SectionCard
import com.rickdnamps.glp1tracker.ui.weightText
import kotlin.math.abs
import kotlin.math.roundToInt

private fun CompMetric.format(v: Double): String = when (this) {
    CompMetric.METABOLIC_AGE -> v.roundToInt().toString()
    else -> Format.number(v, decimals) + if (unit.isEmpty()) "" else " $unit"
}

@Composable
private fun levelColor(level: CompLevel?): Color = when (level) {
    CompLevel.HEALTHY -> MaterialTheme.colorScheme.primary
    CompLevel.HIGH, CompLevel.LOW -> MaterialTheme.colorScheme.tertiary
    CompLevel.VERY_HIGH -> MaterialTheme.colorScheme.error
    null -> MaterialTheme.colorScheme.onSurface
}

@Composable
fun BodyCompositionCard(weights: List<WeightEntity>, settings: UserSettings) {
    val withComp = remember(weights) { weights.filter { !it.composition.isEmpty } }
    SectionCard(title = stringResource(R.string.comp_title), icon = Icons.Outlined.Accessibility) {
        if (withComp.isEmpty()) {
            Text(stringResource(R.string.comp_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (settings.sexOrNull == null || settings.ageOrNull == null) {
                Text(stringResource(R.string.comp_profile_needed), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@SectionCard
        }
        val metrics = CompMetric.entries.filter { m -> withComp.any { m.read(it.composition, it.weightKg) != null } }
        var selected by rememberSaveable { mutableStateOf(metrics.first().name) }
        val metric = metrics.firstOrNull { it.name == selected } ?: metrics.first()
        val latest = withComp.last()
        val sex = settings.sexOrNull

        // Tiles with the latest value of every available metric (tap = chart).
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            metrics.forEach { m ->
                val v = m.read(latest.composition, latest.weightKg) ?: return@forEach
                val first = withComp.firstNotNullOfOrNull { m.read(it.composition, it.weightKg) }
                val level = when (m) {
                    CompMetric.FAT -> BodyCompositionMath.fatLevel(v, sex)
                    CompMetric.VISCERAL -> BodyCompositionMath.visceralLevel(v)
                    CompMetric.WATER -> BodyCompositionMath.waterLevel(v, sex)
                    else -> null
                }
                Card(
                    modifier = Modifier.clickable { selected = m.name },
                    colors = CardDefaults.cardColors(
                        containerColor = if (m == metric) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                        Text(stringResource(m.nameRes), style = MaterialTheme.typography.labelMedium, maxLines = 1)
                        Text(m.format(v), style = MaterialTheme.typography.titleLarge, color = levelColor(level))
                        if (level != null) {
                            Text(
                                stringResource(
                                    when (level) {
                                        CompLevel.LOW -> R.string.comp_level_low
                                        CompLevel.HEALTHY -> R.string.comp_level_healthy
                                        CompLevel.HIGH -> R.string.comp_level_high
                                        CompLevel.VERY_HIGH -> R.string.comp_level_very_high
                                    }
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = levelColor(level),
                            )
                        }
                        if (first != null && withComp.size > 1 && abs(v - first) >= 0.05) {
                            Text(
                                (if (v > first) "+" else "−") + m.format(abs(v - first)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (m == CompMetric.METABOLIC_AGE) {
                            settings.ageOrNull?.let {
                                Text(stringResource(R.string.comp_real_age, it), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }

        LineChart(
            points = withComp.mapNotNull { w -> metric.read(w.composition, w.weightKg)?.let { w.timestamp to it } },
            height = 160.dp,
            showDots = true,
            yLabel = { Format.number(it, if (metric.decimals == 0) 0 else 1) },
            xLabel = { Format.shortDate(it) },
            emptyText = stringResource(R.string.chart_need_two),
        )

        // Fat vs lean loss: the number GLP-1 users care about.
        val firstFat = withComp.firstOrNull { it.bodyFatPct != null }
        val lastFat = withComp.lastOrNull { it.bodyFatPct != null }
        if (firstFat != null && lastFat != null && firstFat !== lastFat) {
            BodyCompositionMath.lossSplit(firstFat.weightKg, firstFat.bodyFatPct!!, lastFat.weightKg, lastFat.bodyFatPct!!)?.let { split ->
                Text(
                    stringResource(R.string.comp_split, weightText(split.totalKg, settings), (split.fatShare * 100).roundToInt()) +
                        " " + stringResource(R.string.comp_since, Format.date(firstFat.timestamp)),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    stringResource(if (split.fatShare >= 0.75) R.string.comp_split_good else R.string.comp_split_warn),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (split.fatShare >= 0.75) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                )
            }
        }
        Text(stringResource(R.string.comp_estimated), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Editable composition fields used by the weight editor (strings, parsed on save). */
class CompositionFields(initial: BodyComp) {
    val fat = mutableStateOf(initial.bodyFatPct?.let { Format.number(it, 1) } ?: "")
    val muscle = mutableStateOf(initial.muscleKg?.let { Format.number(it, 1) } ?: "")
    val water = mutableStateOf(initial.waterPct?.let { Format.number(it, 1) } ?: "")
    val bone = mutableStateOf(initial.boneKg?.let { Format.number(it, 1) } ?: "")
    val visceral = mutableStateOf(initial.visceralFat?.let { Format.number(it, 1) } ?: "")
    val bmr = mutableStateOf(initial.bmrKcal?.let { it.roundToInt().toString() } ?: "")
    val metabolicAge = mutableStateOf(initial.metabolicAge?.toString() ?: "")
    private val keep = initial

    fun toComposition(): BodyComp = keep.copy(
        bodyFatPct = Format.parse(fat.value)?.takeIf { it in 2.0..75.0 },
        muscleKg = Format.parse(muscle.value)?.takeIf { it > 0 },
        waterPct = Format.parse(water.value)?.takeIf { it in 20.0..80.0 },
        boneKg = Format.parse(bone.value)?.takeIf { it > 0 },
        visceralFat = Format.parse(visceral.value)?.takeIf { it in 1.0..60.0 },
        bmrKcal = Format.parse(bmr.value)?.takeIf { it in 500.0..5000.0 },
        metabolicAge = Format.parse(metabolicAge.value)?.toInt()?.takeIf { it in 10..110 },
    )
}

@Composable
private fun box(state: MutableState<String>, label: Int, suffix: String, modifier: Modifier) {
    var v by state
    OutlinedTextField(
        value = v,
        onValueChange = { v = it },
        label = { Text(stringResource(label), maxLines = 1) },
        suffix = { if (suffix.isNotEmpty()) Text(suffix) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}

@Composable
fun CompositionInputs(fields: CompositionFields) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            box(fields.fat, R.string.comp_fat, "%", Modifier.weight(1f))
            box(fields.muscle, R.string.comp_muscle, "kg", Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            box(fields.water, R.string.comp_water, "%", Modifier.weight(1f))
            box(fields.bone, R.string.comp_bone, "kg", Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            box(fields.visceral, R.string.comp_visceral, "", Modifier.weight(1f))
            box(fields.bmr, R.string.comp_bmr, "kcal", Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            box(fields.metabolicAge, R.string.comp_metabolic_age, "", Modifier.weight(1f))
            androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
        }
    }
}


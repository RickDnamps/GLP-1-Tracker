package com.rickdnamps.glp1tracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import com.rickdnamps.glp1tracker.ui.theme.LocalBrand
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import com.rickdnamps.glp1tracker.domain.WeightUnit
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Accessibility
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
import com.rickdnamps.glp1tracker.data.fullComposition
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

/** Value in the unit shown to the user (for charts and differences). */
private fun CompMetric.shown(v: Double, settings: UserSettings): Double = if (isMass) settings.weightUnit.fromKg(v) else v

/** Every value of the latest reading at a glance (3 per row, like the Renpho app); tap one for its trend. */
@Composable
fun BodyCompositionCard(weights: List<WeightEntity>, settings: UserSettings) {
    // Each weigh-in with its composition, completed from the body fat % when the source shared only that.
    val withComp = remember(weights, settings) {
        weights.mapNotNull { w -> w.fullComposition(settings).takeIf { !it.isEmpty }?.let { w to it } }
    }
    SectionCard(title = stringResource(R.string.comp_title), icon = Icons.Outlined.Accessibility) {
        if (withComp.isEmpty()) {
            Text(stringResource(R.string.comp_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (settings.sexOrNull == null || settings.ageOrNull == null) {
                Text(stringResource(R.string.comp_profile_needed), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@SectionCard
        }
        val (latest, latestComp) = withComp.last()
        var selected by rememberSaveable { mutableStateOf<String?>(null) }
        val massUnit = stringResource(settings.weightUnit.symbolRes)
        val bmi = settings.heightCm.takeIf { it > 0 }?.let { h -> latest.weightKg / ((h / 100) * (h / 100)) }
        val tiles = buildList<Tile> {
            // Weight and BMI first, then every composition value of the latest reading (Renpho order).
            add(
                Tile(
                    stringResource(R.string.weight), Format.oneDecimal(settings.weightUnit.fromKg(latest.weightKg)), massUnit,
                    bmi?.let { BodyCompositionMath.bmiLevel(it) }, null, null,
                )
            )
            bmi?.let { add(Tile(stringResource(R.string.bmi), Format.oneDecimal(it), "", BodyCompositionMath.bmiLevel(it), null, null)) }
            CompMetric.entries.forEach { m ->
                val v = m.read(latestComp, latest.weightKg) ?: return@forEach
                val first = withComp.firstNotNullOfOrNull { (w, c) -> m.read(c, w.weightKg) }
                val caption = when {
                    m == CompMetric.METABOLIC_AGE -> settings.ageOrNull?.let { stringResource(R.string.comp_real_age, it) }
                    first != null && withComp.size > 1 && abs(m.shown(v, settings) - m.shown(first, settings)) >= 0.05 -> {
                        val d = m.shown(v, settings) - m.shown(first, settings)
                        (if (d > 0) "+" else "−") + Format.oneDecimal(abs(d))
                    }
                    else -> null
                }
                add(Tile(stringResource(m.nameRes), m.number(v, settings), if (m.isMass) massUnit else m.unit, levelOf(m, v, latest.weightKg, settings), m, caption))
            }
        }
        val metric = tiles.firstOrNull { it.metric?.name == selected }?.metric

        Text(
            stringResource(R.string.comp_measured_on, Format.date(latest.timestamp) + " · " + Format.time(Format.toLocalDateTime(latest.timestamp))),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        tiles.chunked(3).forEach { row ->
            Row(
                Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { tile ->
                    val m = tile.metric
                    CompTile(
                        tile = tile,
                        selected = m != null && m == metric,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        onClick = m?.let { { selected = if (it == metric) null else it.name } },
                    )
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }

        if (metric != null) {
            val points = withComp.mapNotNull { (w, c) -> metric.read(c, w.weightKg)?.let { w.timestamp to metric.shown(it, settings) } }
            Text(stringResource(metric.nameRes), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            if (points.size >= 2) {
                LineChart(
                    points = points,
                    height = 160.dp,
                    showDots = true,
                    yLabel = { Format.number(it, if (metric.decimals == 0) 0 else 1) },
                    xLabel = { Format.shortDate(it) },
                    emptyText = stringResource(R.string.chart_need_two),
                )
            } else {
                Text(stringResource(R.string.comp_need_two), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Text(stringResource(R.string.comp_tap_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Fat vs lean loss: the number GLP-1 users care about.
        val firstFat = withComp.firstOrNull { it.first.bodyFatPct != null }?.first
        val lastFat = withComp.lastOrNull { it.first.bodyFatPct != null }?.first
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
        // Imported readings (Health Connect, manual entry) often bring only the fat %: say where the rest comes from.
        if (latest.composition.waterPct == null && latestComp.waterPct != null) {
            Text(stringResource(R.string.comp_derived_from_fat), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(stringResource(R.string.comp_estimated), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** One value of the grid: label, number with a small unit, and a coloured level pill. */
private data class Tile(
    val label: String,
    val number: String,
    val unit: String,
    val level: CompLevel?,
    /** Tapping shows this metric's trend; null for weight and BMI. */
    val metric: CompMetric?,
    val caption: String?,
)

/** Number part of a metric (masses in the user's unit, kcal and ages without decimals). */
private fun CompMetric.number(v: Double, settings: UserSettings): String = when {
    isMass -> Format.oneDecimal(settings.weightUnit.fromKg(v))
    decimals == 0 -> v.roundToInt().toString()
    else -> Format.oneDecimal(v)
}

/** Level shown on a metric's pill, from general or consumer-scale reference ranges. */
private fun levelOf(m: CompMetric, v: Double, weightKg: Double, settings: UserSettings): CompLevel? {
    val sex = settings.sexOrNull
    return when (m) {
        CompMetric.FAT -> BodyCompositionMath.fatLevel(v, sex)
        CompMetric.SUBCUTANEOUS -> BodyCompositionMath.subcutaneousLevel(v, sex)
        CompMetric.VISCERAL -> BodyCompositionMath.visceralLevel(v)
        CompMetric.WATER -> BodyCompositionMath.waterLevel(v, sex)
        CompMetric.SKELETAL -> BodyCompositionMath.skeletalMuscleLevel(v, sex)
        CompMetric.MUSCLE -> BodyCompositionMath.muscleMassLevel(v, settings.heightCm, sex)
        CompMetric.BONE -> BodyCompositionMath.boneLevel(v, weightKg, sex)
        CompMetric.PROTEIN -> BodyCompositionMath.proteinLevel(v)
        CompMetric.METABOLIC_AGE -> settings.ageOrNull?.let { BodyCompositionMath.metabolicAgeLevel(v.roundToInt(), it) }
        CompMetric.FAT_MASS, CompMetric.LEAN, CompMetric.BMR -> null
    }
}

/** Blue below, green normal, teal excellent, orange high, red very high (lighter on dark themes). */
@Composable
private fun pillColor(level: CompLevel): Color {
    val base = when (level) {
        CompLevel.LOW -> Color(0xFF1E88E5)
        CompLevel.HEALTHY -> Color(0xFF2E7D32)
        CompLevel.EXCELLENT -> Color(0xFF00897B)
        CompLevel.HIGH -> Color(0xFFEF6C00)
        CompLevel.VERY_HIGH -> Color(0xFFD32F2F)
    }
    return if (LocalBrand.current.isDark) lerp(base, Color.White, 0.35f) else base
}

@Composable
private fun CompTile(tile: Tile, selected: Boolean, modifier: Modifier, onClick: (() -> Unit)?) {
    val cs = MaterialTheme.colorScheme
    val content: @Composable () -> Unit = {
        Column(
            Modifier.padding(horizontal = 6.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                tile.label,
                style = MaterialTheme.typography.labelMedium,
                color = cs.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(tile.number) }
                    if (tile.unit.isNotEmpty()) {
                        withStyle(SpanStyle(fontSize = MaterialTheme.typography.labelMedium.fontSize, fontWeight = FontWeight.Medium)) {
                            append(" " + tile.unit)
                        }
                    }
                },
                style = MaterialTheme.typography.titleLarge,
                color = cs.onSurface,
                maxLines = 1,
                textAlign = TextAlign.Center,
            )
            tile.level?.let { level ->
                val color = pillColor(level)
                Text(
                    stringResource(
                        when (level) {
                            CompLevel.LOW -> R.string.comp_level_low
                            CompLevel.HEALTHY -> R.string.comp_level_healthy
                            CompLevel.HIGH -> R.string.comp_level_high
                            CompLevel.VERY_HIGH -> R.string.comp_level_very_high
                            CompLevel.EXCELLENT -> R.string.comp_level_excellent
                        }
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = color,
                    maxLines = 1,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(color.copy(alpha = 0.13f))
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                )
            }
            tile.caption?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant, maxLines = 1, textAlign = TextAlign.Center)
            }
        }
    }
    val shape = RoundedCornerShape(20.dp)
    val color = if (selected) cs.primaryContainer else cs.surfaceContainer
    if (onClick != null) {
        Surface(onClick = onClick, shape = shape, color = color, modifier = modifier) { content() }
    } else {
        Surface(shape = shape, color = color, modifier = modifier) { content() }
    }
}

/** Editable composition fields used by the weight editor (strings, parsed on save). */
class CompositionFields(initial: BodyComp, private val unit: WeightUnit = WeightUnit.KG) {
    val fat = mutableStateOf(initial.bodyFatPct?.let { Format.number(it, 1) } ?: "")
    val muscle = mutableStateOf(initial.muscleKg?.let { Format.number(unit.fromKg(it), 1) } ?: "")
    val water = mutableStateOf(initial.waterPct?.let { Format.number(it, 1) } ?: "")
    val bone = mutableStateOf(initial.boneKg?.let { Format.number(unit.fromKg(it), 1) } ?: "")
    val visceral = mutableStateOf(initial.visceralFat?.let { Format.number(it, 1) } ?: "")
    val bmr = mutableStateOf(initial.bmrKcal?.let { it.roundToInt().toString() } ?: "")
    val metabolicAge = mutableStateOf(initial.metabolicAge?.toString() ?: "")
    private val keep = initial
    private val original = listOf(fat, muscle, water, bone, visceral, bmr, metabolicAge).map { it.value }

    /** Untouched fields keep their exact stored value (no rounding, no range filtering). */
    private fun <T> field(index: Int, state: MutableState<String>, old: T?, parse: (String) -> T?): T? =
        if (state.value == original[index]) old else parse(state.value)

    fun toComposition(): BodyComp = keep.copy(
        bodyFatPct = field(0, fat, keep.bodyFatPct) { t -> Format.parse(t)?.takeIf { it in 2.0..75.0 } },
        muscleKg = field(1, muscle, keep.muscleKg) { t -> Format.parse(t)?.takeIf { it > 0 }?.let(unit::toKg) },
        waterPct = field(2, water, keep.waterPct) { t -> Format.parse(t)?.takeIf { it in 20.0..80.0 } },
        boneKg = field(3, bone, keep.boneKg) { t -> Format.parse(t)?.takeIf { it > 0 }?.let(unit::toKg) },
        visceralFat = field(4, visceral, keep.visceralFat) { t -> Format.parse(t)?.takeIf { it in 1.0..60.0 } },
        bmrKcal = field(5, bmr, keep.bmrKcal) { t -> Format.parse(t)?.takeIf { it in 500.0..5000.0 } },
        metabolicAge = field(6, metabolicAge, keep.metabolicAge) { t -> Format.parse(t)?.toInt()?.takeIf { it in 10..110 } },
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
fun CompositionInputs(fields: CompositionFields, massUnit: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            box(fields.fat, R.string.comp_fat, "%", Modifier.weight(1f))
            box(fields.muscle, R.string.comp_muscle, massUnit, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            box(fields.water, R.string.comp_water, "%", Modifier.weight(1f))
            box(fields.bone, R.string.comp_bone, massUnit, Modifier.weight(1f))
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


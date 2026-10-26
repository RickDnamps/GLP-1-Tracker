package com.rickdnamps.glp1tracker.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingFlat
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bloodtype
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Opacity
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rickdnamps.glp1tracker.R
import com.rickdnamps.glp1tracker.data.VitalEntity
import com.rickdnamps.glp1tracker.domain.BpStage
import com.rickdnamps.glp1tracker.domain.Format
import com.rickdnamps.glp1tracker.domain.VitalInputError
import com.rickdnamps.glp1tracker.domain.VitalLevel
import com.rickdnamps.glp1tracker.domain.VitalRanges
import com.rickdnamps.glp1tracker.domain.VitalReading
import com.rickdnamps.glp1tracker.domain.VitalTrend
import com.rickdnamps.glp1tracker.domain.VitalType
import com.rickdnamps.glp1tracker.ui.AppViewModel
import com.rickdnamps.glp1tracker.ui.UiState
import com.rickdnamps.glp1tracker.ui.components.AnimatedValue
import com.rickdnamps.glp1tracker.ui.components.DateTimeRow
import com.rickdnamps.glp1tracker.ui.components.HeroCard
import com.rickdnamps.glp1tracker.ui.components.LineChart
import com.rickdnamps.glp1tracker.ui.components.Pill
import com.rickdnamps.glp1tracker.ui.components.ProgressRing
import com.rickdnamps.glp1tracker.ui.components.SectionCard
import com.rickdnamps.glp1tracker.ui.theme.LocalBrand
import java.time.LocalDateTime
import kotlin.math.abs
import kotlin.math.roundToInt

/** Which reading the editor sheet shows; a null [item] means "add a [type] reading". */
private data class VitalEditRequest(val item: VitalEntity?, val type: VitalType)

/** Number of readings listed under each card before "Show all". */
private const val COLLAPSED_ROWS = 3

/** Blood pressure, heart rate and lab results: one card per type with data, plus the editor sheet. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VitalsScreen(state: UiState, vm: AppViewModel, contentPadding: PaddingValues) {
    val byType = remember(state.vitals) {
        state.vitals.sortedBy { it.timestamp }.groupBy { VitalType.fromName(it.type) }
    }
    val types = VitalType.entries.filter { !byType[it].isNullOrEmpty() }
    val defaultType = state.vitals.maxByOrNull { it.timestamp }?.let { VitalType.fromName(it.type) } ?: VitalType.BLOOD_PRESSURE
    var editing by remember { mutableStateOf<VitalEditRequest?>(null) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = contentPadding.calculateTopPadding() + 8.dp,
                bottom = contentPadding.calculateBottomPadding() + 88.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (types.isNotEmpty()) item { VitalsHero(byType) }
            item {
                WhyCard(
                    empty = types.isEmpty(),
                    onAdd = { editing = VitalEditRequest(null, it) },
                )
            }
            items(types, key = { it.name }) { type ->
                VitalCard(
                    type = type,
                    readings = byType.getValue(type),
                    onAdd = { editing = VitalEditRequest(null, type) },
                    onEdit = { editing = VitalEditRequest(it, type) },
                )
            }
            item {
                Text(
                    stringResource(R.string.vitals_disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
        ExtendedFloatingActionButton(
            onClick = { editing = VitalEditRequest(null, defaultType) },
            icon = { Icon(Icons.Outlined.Add, null) },
            text = { Text(stringResource(R.string.vitals_add)) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = contentPadding.calculateBottomPadding() + 16.dp),
        )
    }

    editing?.let { request ->
        VitalEditor(
            item = request.item,
            initialType = request.type,
            lastOf = { byType[it]?.lastOrNull() },
            onDismiss = { editing = null },
            onSave = { vm.saveVital(it); editing = null },
            onDelete = { vm.deleteVital(it); editing = null },
        )
    }
}

/** Entry card for the home or progress screen: latest value of each type, and a button to open the screen. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VitalsTeaserCard(state: UiState, onOpen: () -> Unit) {
    val latest = remember(state.vitals) {
        state.vitals.groupBy { VitalType.fromName(it.type) }.mapValues { (_, list) -> list.maxBy { it.timestamp } }
    }
    SectionCard(
        title = stringResource(R.string.vitals_title),
        icon = Icons.Outlined.MonitorHeart,
        action = { TextButton(onClick = onOpen) { Text(stringResource(R.string.vitals_open)) } },
    ) {
        if (latest.isEmpty()) {
            Text(stringResource(R.string.vitals_teaser), style = MaterialTheme.typography.bodyMedium)
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                VitalType.entries.forEach { type ->
                    val e = latest[type] ?: return@forEach
                    val (container, content) = levelColors(VitalRanges.classify(type, e.value, e.value2))
                    Pill(
                        stringResource(type.nameRes) + " · " + VitalRanges.formatReading(type, e.value, e.value2) + " " + type.unit,
                        container = container,
                        content = content,
                    )
                }
            }
        }
    }
}

@Composable
private fun VitalsHero(byType: Map<VitalType, List<VitalEntity>>) {
    val latest = byType.filterValues { it.isNotEmpty() }.mapValues { it.value.last() }
    if (latest.isEmpty()) return
    val inRange = latest.count { (type, e) -> VitalRanges.classify(type, e.value, e.value2) == VitalLevel.NORMAL }
    val lastTime = latest.values.maxOf { it.timestamp }
    HeroCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProgressRing(
                progress = inRange.toFloat() / latest.size,
                size = 104.dp,
                stroke = 10.dp,
                trackColor = Color.White.copy(alpha = 0.22f),
                colors = listOf(Color.White, Color.White.copy(alpha = 0.7f)),
            ) {
                AnimatedValue("$inRange/${latest.size}", MaterialTheme.typography.headlineSmall, Color.White)
            }
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    stringResource(R.string.vitals_in_range).uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.85f),
                )
                Text(stringResource(R.string.vitals_in_range_hint), style = MaterialTheme.typography.bodyMedium, color = Color.White)
                Pill(stringResource(R.string.vitals_latest, Format.date(lastTime)))
            }
        }
    }
}

/** Why these numbers matter on a GLP-1; expanded until the first reading, then collapsible. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WhyCard(empty: Boolean, onAdd: (VitalType) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val open = empty || expanded
    SectionCard(
        title = stringResource(R.string.vitals_why_title),
        icon = Icons.Outlined.HealthAndSafety,
        action = {
            if (!empty) {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        stringResource(if (expanded) R.string.vitals_show_less else R.string.vitals_more),
                    )
                }
            }
        },
    ) {
        if (open) {
            Text(stringResource(R.string.vitals_why), style = MaterialTheme.typography.bodyMedium)
            Text(
                stringResource(R.string.vitals_chart_legend),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                stringResource(R.string.vitals_why_short),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable { expanded = true },
            )
        }
        if (empty) {
            Text(
                stringResource(R.string.vitals_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VitalType.entries.forEach { type ->
                    AssistChip(
                        onClick = { onAdd(type) },
                        label = { Text(stringResource(type.nameRes)) },
                        leadingIcon = { Icon(vitalIcon(type), null, Modifier.size(18.dp)) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VitalCard(type: VitalType, readings: List<VitalEntity>, onAdd: () -> Unit, onEdit: (VitalEntity) -> Unit) {
    val first = readings.first()
    val latest = readings.last()
    var showAll by rememberSaveable(type) { mutableStateOf(false) }
    SectionCard(
        title = stringResource(type.nameRes),
        icon = vitalIcon(type),
        action = { TextButton(onClick = onAdd) { Text(stringResource(R.string.add)) } },
    ) {
        Row {
            Text(
                VitalRanges.formatReading(type, latest.value, latest.value2),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.alignByBaseline(),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                type.unit,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.alignByBaseline(),
            )
        }
        // Long chip texts ("Stage 2 hypertension") push the date to the next line on narrow phones.
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            LevelPill(type, latest.value, latest.value2)
            Text(
                stringResource(R.string.vitals_latest, Format.dateTime(latest.timestamp)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
        }
        if (readings.size >= 2) {
            ChangeLine(type, first, latest)
        } else {
            Text(
                stringResource(R.string.vitals_first_reading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ReadingWarning(type, latest)
        if (readings.size >= 2) {
            LineChart(
                points = readings.map { it.timestamp to it.value },
                height = 130.dp,
                goal = VitalRanges.upperNormal(type),
                showDots = readings.size <= 30,
                yLabel = { VitalRanges.format(type, it) },
                xLabel = { Format.shortDate(it) },
            )
            val d1 = first.value2
            val d2 = latest.value2
            if (type.hasSecondValue && d1 != null && d2 != null) {
                Text(
                    stringResource(R.string.vitals_chart_systolic, VitalRanges.format(type, d1), VitalRanges.format(type, d2)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            stringResource(referenceText(type)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        val newestFirst = readings.asReversed()
        (if (showAll) newestFirst else newestFirst.take(COLLAPSED_ROWS)).forEach { entry ->
            ReadingRow(type, entry) { onEdit(entry) }
        }
        if (readings.size > COLLAPSED_ROWS) {
            TextButton(onClick = { showAll = !showAll }) {
                Text(
                    if (showAll) stringResource(R.string.vitals_show_less)
                    else stringResource(R.string.vitals_show_all, readings.size),
                )
            }
        }
    }
}

/** "−12/−6 mmHg since 3 Mar 2026", coloured by whether the change is good news. */
@Composable
private fun ChangeLine(type: VitalType, first: VitalEntity, latest: VitalEntity) {
    val change = VitalRanges.change(type, VitalReading(first.value, first.value2), VitalReading(latest.value, latest.value2))
    val delta2 = change.delta2
    val unchanged = change.delta == 0.0 && (delta2 == null || delta2 == 0.0)
    val color = when (change.trend) {
        VitalTrend.BETTER -> levelColors(VitalLevel.NORMAL).second
        VitalTrend.WORSE -> MaterialTheme.colorScheme.error
        VitalTrend.STABLE -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val direction = if (change.delta != 0.0) change.delta else delta2 ?: 0.0
    val icon = when {
        direction < 0 -> Icons.AutoMirrored.Outlined.TrendingDown
        direction > 0 -> Icons.AutoMirrored.Outlined.TrendingUp
        else -> Icons.AutoMirrored.Outlined.TrendingFlat
    }
    val since = Format.date(first.timestamp)
    val text = if (unchanged) {
        stringResource(R.string.vitals_no_change, since)
    } else {
        var amount = VitalRanges.formatDelta(type, change.delta)
        if (delta2 != null) amount += "/" + VitalRanges.formatDelta(type, delta2)
        amount += " " + type.unit
        val percent = change.percent
        if ((type == VitalType.LDL || type == VitalType.TRIGLYCERIDES) && percent != null && abs(percent) >= 1) {
            val p = percent.roundToInt()
            amount += " (" + (if (p > 0) "+" else "−") + abs(p) + " %)"
        }
        stringResource(R.string.vitals_since_first, amount, since)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = color)
    }
}

/** Safety notes for a very high blood pressure or a low glucose. */
@Composable
private fun ReadingWarning(type: VitalType, latest: VitalEntity) {
    val res = when {
        type == VitalType.BLOOD_PRESSURE && VitalRanges.bpStage(latest.value, latest.value2) == BpStage.CRISIS ->
            R.string.vitals_warn_bp_crisis
        type == VitalType.GLUCOSE && VitalRanges.classify(type, latest.value) == VitalLevel.LOW ->
            R.string.vitals_warn_glucose_low
        else -> return
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(Icons.Outlined.WarningAmber, null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(stringResource(res), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
    }
}

@Composable
private fun ReadingRow(type: VitalType, entry: VitalEntity, onClick: () -> Unit) {
    val level = VitalRanges.classify(type, entry.value, entry.value2)
    val label = levelLabel(type, entry.value, entry.value2)
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(levelColors(level).second)
                .semantics { contentDescription = label },
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(Format.dateTime(entry.timestamp), style = MaterialTheme.typography.bodyMedium)
            if (entry.notes.isNotBlank()) {
                Text(
                    entry.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text(
            VitalRanges.formatReading(type, entry.value, entry.value2) + " " + type.unit,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun LevelPill(type: VitalType, value: Double, value2: Double?) {
    val (container, content) = levelColors(VitalRanges.classify(type, value, value2))
    Pill(levelLabel(type, value, value2), container = container, content = content)
}

/** Chip text: BP stage names, HbA1c ranges, otherwise the generic category. */
@Composable
private fun levelLabel(type: VitalType, value: Double, value2: Double?): String {
    val level = VitalRanges.classify(type, value, value2)
    val generic = when (level) {
        VitalLevel.NORMAL -> R.string.vitals_level_normal
        VitalLevel.ELEVATED -> R.string.vitals_level_elevated
        VitalLevel.HIGH -> R.string.vitals_level_high
        VitalLevel.LOW -> R.string.vitals_level_low
    }
    val res = when (type) {
        VitalType.BLOOD_PRESSURE -> when (VitalRanges.bpStage(value, value2)) {
            BpStage.LOW -> R.string.vitals_level_low
            BpStage.NORMAL -> R.string.vitals_level_normal
            BpStage.ELEVATED -> R.string.vitals_bp_elevated
            BpStage.STAGE_1 -> R.string.vitals_bp_stage1
            BpStage.STAGE_2 -> R.string.vitals_bp_stage2
            BpStage.CRISIS -> R.string.vitals_bp_crisis
        }
        VitalType.HBA1C -> when (level) {
            VitalLevel.ELEVATED -> R.string.vitals_a1c_prediabetes
            VitalLevel.HIGH -> R.string.vitals_a1c_diabetes
            else -> generic
        }
        VitalType.LDL -> if (level == VitalLevel.ELEVATED) R.string.vitals_ldl_above_target else generic
        VitalType.TRIGLYCERIDES -> if (level == VitalLevel.ELEVATED) R.string.vitals_level_borderline else generic
        VitalType.HEART_RATE, VitalType.GLUCOSE -> generic
    }
    return stringResource(res)
}

/** (container, content) colours for a category; independent of the accent so red always means "high". */
@Composable
private fun levelColors(level: VitalLevel): Pair<Color, Color> {
    val dark = LocalBrand.current.isDark
    return when (level) {
        VitalLevel.NORMAL -> if (dark) Color(0xFF1E4D2B) to Color(0xFFB7E4C0) else Color(0xFFD4F2DC) to Color(0xFF1B5E20)
        VitalLevel.ELEVATED -> if (dark) Color(0xFF5C4300) to Color(0xFFFFDF99) else Color(0xFFFFEBC2) to Color(0xFF6B4500)
        VitalLevel.HIGH -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        VitalLevel.LOW -> if (dark) Color(0xFF1D3A66) to Color(0xFFC2D7FF) else Color(0xFFDCE6FF) to Color(0xFF123A80)
    }
}

private fun vitalIcon(type: VitalType): ImageVector = when (type) {
    VitalType.BLOOD_PRESSURE -> Icons.Outlined.MonitorHeart
    VitalType.HEART_RATE -> Icons.Outlined.FavoriteBorder
    VitalType.GLUCOSE -> Icons.Outlined.WaterDrop
    VitalType.HBA1C -> Icons.Outlined.Bloodtype
    VitalType.LDL -> Icons.Outlined.Science
    VitalType.TRIGLYCERIDES -> Icons.Outlined.Opacity
}

@StringRes
private fun referenceText(type: VitalType): Int = when (type) {
    VitalType.BLOOD_PRESSURE -> R.string.vitals_ref_bp
    VitalType.HEART_RATE -> R.string.vitals_ref_hr
    VitalType.GLUCOSE -> R.string.vitals_ref_glucose
    VitalType.HBA1C -> R.string.vitals_ref_hba1c
    VitalType.LDL -> R.string.vitals_ref_ldl
    VitalType.TRIGLYCERIDES -> R.string.vitals_ref_tg
}

@StringRes
private fun inputHint(type: VitalType): Int = when (type) {
    VitalType.BLOOD_PRESSURE -> R.string.vitals_hint_bp
    VitalType.HEART_RATE -> R.string.vitals_hint_hr
    VitalType.GLUCOSE -> R.string.vitals_hint_glucose
    VitalType.HBA1C, VitalType.LDL, VitalType.TRIGLYCERIDES -> R.string.vitals_hint_lab
}

/** Bottom sheet to add or edit a reading; [lastOf] gives the latest reading of a type for placeholders. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VitalEditor(
    item: VitalEntity?,
    initialType: VitalType,
    lastOf: (VitalType) -> VitalEntity?,
    onDismiss: () -> Unit,
    onSave: (VitalEntity) -> Unit,
    onDelete: (VitalEntity) -> Unit,
) {
    var dateTime by remember {
        mutableStateOf(item?.let { Format.toLocalDateTime(it.timestamp) } ?: LocalDateTime.now().withSecond(0).withNano(0))
    }
    var type by remember { mutableStateOf(item?.let { VitalType.fromName(it.type) } ?: initialType) }
    var valueText by remember { mutableStateOf(item?.let { VitalRanges.inputText(type, it.value) } ?: "") }
    var value2Text by remember { mutableStateOf(item?.value2?.let { VitalRanges.inputText(VitalType.BLOOD_PRESSURE, it) } ?: "") }
    var notes by remember { mutableStateOf(item?.notes ?: "") }

    val value = Format.parse(valueText)
    val value2 = if (type.hasSecondValue) Format.parse(value2Text) else null
    val error = VitalRanges.validate(type, value, value2)
    val last = lastOf(type)?.takeIf { it.id != item?.id }
    val integerInput = type == VitalType.BLOOD_PRESSURE || type == VitalType.HEART_RATE

    EditorSheet(
        title = stringResource(if (item == null) R.string.vitals_add else R.string.vitals_edit),
        canSave = error == null,
        onDismiss = onDismiss,
        onSave = {
            if (value != null) {
                onSave(
                    VitalEntity(
                        id = item?.id ?: 0,
                        timestamp = Format.toMillis(dateTime),
                        type = type.name,
                        value = value,
                        value2 = if (type.hasSecondValue) value2 else null,
                        notes = notes.trim(),
                    )
                )
            }
        },
        onDelete = item?.let { { onDelete(it) } },
    ) {
        FieldLabel(stringResource(R.string.vitals_what))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VitalType.entries.forEach { t ->
                FilterChip(
                    selected = type == t,
                    onClick = {
                        if (t != type) {
                            type = t
                            valueText = ""
                            value2Text = ""
                        }
                    },
                    label = { Text(stringResource(t.nameRes)) },
                )
            }
        }
        DateTimeRow(dateTime, { dateTime = it })

        val firstError = valueText.isNotBlank() && error == VitalInputError.VALUE_OUT_OF_RANGE
        val secondError = value2Text.isNotBlank() &&
            (error == VitalInputError.SECOND_OUT_OF_RANGE || error == VitalInputError.SECOND_NOT_BELOW_FIRST)
        if (type.hasSecondValue) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ValueField(
                    text = valueText,
                    onChange = { valueText = it },
                    label = stringResource(R.string.vitals_systolic),
                    unit = type.unit,
                    placeholder = last?.let { VitalRanges.inputText(type, it.value) },
                    decimal = false,
                    isError = firstError,
                    modifier = Modifier.weight(1f),
                )
                ValueField(
                    text = value2Text,
                    onChange = { value2Text = it },
                    label = stringResource(R.string.vitals_diastolic),
                    unit = type.unit,
                    placeholder = last?.value2?.let { VitalRanges.inputText(type, it) },
                    decimal = false,
                    isError = secondError,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            ValueField(
                text = valueText,
                onChange = { valueText = it },
                label = stringResource(type.nameRes),
                unit = type.unit,
                placeholder = last?.let { VitalRanges.inputText(type, it.value) },
                decimal = !integerInput,
                isError = firstError,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        val errorText = when {
            firstError -> {
                val range = VitalRanges.plausible(type)
                stringResource(
                    R.string.vitals_err_range,
                    VitalRanges.inputText(type, range.start),
                    VitalRanges.inputText(type, range.endInclusive),
                    type.unit,
                )
            }
            secondError && error == VitalInputError.SECOND_NOT_BELOW_FIRST -> stringResource(R.string.vitals_err_diastolic)
            secondError -> stringResource(
                R.string.vitals_err_range,
                VitalRanges.inputText(type, VitalRanges.plausibleDiastolic.start),
                VitalRanges.inputText(type, VitalRanges.plausibleDiastolic.endInclusive),
                type.unit,
            )
            else -> null
        }
        if (errorText != null) {
            Text(errorText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        val conversion = value?.let { VitalRanges.suggestConversion(type, it) }
        if (value != null && conversion != null) {
            // Typed in mg/dL, mmol/mol or as a fraction: offer the value in our unit.
            val from = VitalRanges.inputText(type, value).let { if (conversion.fromUnit.isEmpty()) it else "$it ${conversion.fromUnit}" }
            AssistChip(
                onClick = { valueText = VitalRanges.inputText(type, conversion.value) },
                label = { Text(stringResource(R.string.vitals_convert, from, VitalRanges.format(type, conversion.value) + " " + type.unit)) },
                leadingIcon = { Icon(Icons.Outlined.SwapHoriz, null, Modifier.size(18.dp)) },
            )
        }
        if (error == null && value != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FieldLabel(stringResource(R.string.vitals_category))
                Spacer(Modifier.width(8.dp))
                LevelPill(type, value, value2)
            }
        }
        Text(
            stringResource(inputHint(type)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text(stringResource(R.string.notes)) },
            placeholder = { Text(stringResource(R.string.vitals_notes_hint)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
        )
    }
}

@Composable
private fun ValueField(
    text: String,
    onChange: (String) -> Unit,
    label: String,
    unit: String,
    placeholder: String?,
    decimal: Boolean,
    isError: Boolean,
    modifier: Modifier,
) {
    OutlinedTextField(
        value = text,
        onValueChange = onChange,
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        placeholder = if (placeholder != null) { { Text(placeholder) } } else null,
        suffix = { Text(unit) },
        singleLine = true,
        isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
        modifier = modifier,
    )
}

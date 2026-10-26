package com.rickdnamps.glp1tracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Healing
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rickdnamps.glp1tracker.R
import com.rickdnamps.glp1tracker.data.DailyLogEntity
import com.rickdnamps.glp1tracker.data.SideEffectEntity
import com.rickdnamps.glp1tracker.domain.Format
import com.rickdnamps.glp1tracker.domain.Severity
import com.rickdnamps.glp1tracker.domain.SideEffectType
import com.rickdnamps.glp1tracker.ui.AppViewModel
import com.rickdnamps.glp1tracker.ui.UiState
import com.rickdnamps.glp1tracker.ui.components.DatePickerDialogFor
import com.rickdnamps.glp1tracker.ui.components.SectionCard
import java.time.LocalDate

private val appetiteLabels = listOf(R.string.appetite_1, R.string.appetite_2, R.string.appetite_3, R.string.appetite_4, R.string.appetite_5)
private val moodEmojis = listOf("😣", "🙁", "😐", "🙂", "😄")

@Composable
fun JournalScreen(state: UiState, vm: AppViewModel, contentPadding: PaddingValues, openEditor: (Editor) -> Unit) {
    var dateText by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    val date = LocalDate.parse(dateText)
    val log = state.dailyLogs.firstOrNull { it.date == dateText } ?: DailyLogEntity(dateText)
    val daySideEffects = state.sideEffects.filter { Format.toLocalDateTime(it.timestamp).toLocalDate() == date }
    var pickDate by remember { mutableStateOf(false) }

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
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = { dateText = date.minusDays(1).toString() }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.previous_day))
                }
                Text(
                    if (date == LocalDate.now()) stringResource(R.string.today) else Format.date(date),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f).clickable { pickDate = true },
                )
                IconButton(onClick = { dateText = date.plusDays(1).toString() }, enabled = date.isBefore(LocalDate.now())) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.next_day))
                }
            }
        }
        item { HabitsCard(state, log, date, vm) }
        item { FeelingCard(log, date, vm) }
        item {
            SectionCard(
                title = stringResource(R.string.side_effects),
                icon = Icons.Outlined.Healing,
                action = { TextButton(onClick = { openEditor(Editor.SideEffect()) }) { Text(stringResource(R.string.add)) } },
            ) {
                if (daySideEffects.isEmpty()) {
                    Text(stringResource(R.string.no_side_effect_day), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                daySideEffects.forEach { se -> SideEffectRow(se) { openEditor(Editor.SideEffect(se)) } }
            }
        }
        item { SideEffectSummaryCard(state.sideEffects) }
        item {
            Text(
                stringResource(R.string.recent_side_effects),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp),
            )
        }
        if (state.sideEffects.isEmpty()) {
            item { Text(stringResource(R.string.no_side_effect), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(state.sideEffects.take(50), key = { it.id }) { se ->
            Column {
                SideEffectRow(se, showDate = true) { openEditor(Editor.SideEffect(se)) }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        }
    }

    if (pickDate) {
        DatePickerDialogFor(
            initial = date,
            onDismiss = { pickDate = false },
            onPick = {
                dateText = (if (it.isAfter(LocalDate.now())) LocalDate.now() else it).toString()
                pickDate = false
            },
        )
    }
}

@Composable
private fun HabitsCard(state: UiState, log: DailyLogEntity, date: LocalDate, vm: AppViewModel) {
    val s = state.settings
    SectionCard(title = stringResource(R.string.hydration_nutrition)) {
        GoalRow(
            icon = Icons.Outlined.WaterDrop,
            label = stringResource(R.string.water),
            value = log.waterMl,
            goal = s.waterGoalMl,
            unit = "ml",
            stepLabel = "+250",
            onAdd = { vm.updateDailyLog(date) { it.copy(waterMl = it.waterMl + 250) } },
            onRemove = { vm.updateDailyLog(date) { it.copy(waterMl = (it.waterMl - 250).coerceAtLeast(0)) } },
        )
        GoalRow(
            icon = Icons.Outlined.Restaurant,
            label = stringResource(R.string.protein),
            value = log.proteinG,
            goal = s.proteinGoalG,
            unit = "g",
            stepLabel = "+10",
            onAdd = { vm.updateDailyLog(date) { it.copy(proteinG = it.proteinG + 10) } },
            onRemove = { vm.updateDailyLog(date) { it.copy(proteinG = (it.proteinG - 10).coerceAtLeast(0)) } },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FeelingCard(log: DailyLogEntity, date: LocalDate, vm: AppViewModel) {
    // Local copy so typing is not disturbed by database round-trips; reset when the day changes.
    var notes by remember(date) { mutableStateOf(log.notes) }
    SectionCard(title = stringResource(R.string.how_do_you_feel), icon = Icons.Outlined.Mood) {
        Text(stringResource(R.string.appetite), style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            appetiteLabels.forEachIndexed { i, res ->
                val level = i + 1
                FilterChip(
                    selected = log.appetite == level,
                    onClick = { vm.updateDailyLog(date) { it.copy(appetite = if (it.appetite == level) 0 else level) } },
                    label = { Text(stringResource(res)) },
                )
            }
        }
        Text(stringResource(R.string.mood), style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            moodEmojis.forEachIndexed { i, emoji ->
                val level = i + 1
                FilterChip(
                    selected = log.mood == level,
                    onClick = { vm.updateDailyLog(date) { it.copy(mood = if (it.mood == level) 0 else level) } },
                    label = { Text(emoji, style = MaterialTheme.typography.titleLarge) },
                )
            }
        }
        OutlinedTextField(
            value = notes,
            onValueChange = { text ->
                notes = text
                vm.updateDailyLog(date) { it.copy(notes = text) }
            },
            label = { Text(stringResource(R.string.notes_of_the_day)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
        )
    }
}

@Composable
private fun SideEffectRow(se: SideEffectEntity, showDate: Boolean = false, onClick: () -> Unit) {
    val severity = Severity.fromLevel(se.severity)
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(SideEffectType.fromName(se.type).nameRes), style = MaterialTheme.typography.bodyLarge)
            Text(
                (if (showDate) Format.dateTime(se.timestamp) else Format.time(Format.toLocalDateTime(se.timestamp))) +
                    if (se.notes.isNotBlank()) " · ${se.notes}" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
        }
        Text(
            stringResource(severity.nameRes),
            style = MaterialTheme.typography.labelLarge,
            color = when (severity) {
                Severity.MILD -> MaterialTheme.colorScheme.primary
                Severity.MODERATE -> MaterialTheme.colorScheme.tertiary
                Severity.SEVERE -> MaterialTheme.colorScheme.error
            },
        )
    }
}

@Composable
private fun SideEffectSummaryCard(sideEffects: List<SideEffectEntity>) {
    val since = System.currentTimeMillis() - 30 * 86_400_000L
    val counts = sideEffects.filter { it.timestamp >= since }
        .groupingBy { SideEffectType.fromName(it.type) }.eachCount()
        .entries.sortedByDescending { it.value }
    if (counts.isEmpty()) return
    val max = counts.first().value
    SectionCard(title = stringResource(R.string.last_30_days), icon = Icons.Outlined.BarChart) {
        counts.forEach { (type, count) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(type.nameRes), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.45f))
                LinearProgressIndicator(
                    progress = { count.toFloat() / max },
                    modifier = Modifier.weight(0.45f),
                    color = MaterialTheme.colorScheme.tertiary,
                )
                Text(count.toString(), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.End, modifier = Modifier.weight(0.1f))
            }
        }
    }
}

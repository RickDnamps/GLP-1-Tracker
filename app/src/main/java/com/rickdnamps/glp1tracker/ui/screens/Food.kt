package com.rickdnamps.glp1tracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.LunchDining
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rickdnamps.glp1tracker.R
import com.rickdnamps.glp1tracker.data.FoodEntity
import com.rickdnamps.glp1tracker.data.UserSettings
import com.rickdnamps.glp1tracker.domain.Format
import com.rickdnamps.glp1tracker.ui.components.DateTimeRow
import com.rickdnamps.glp1tracker.ui.components.SectionCard
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class FoodTotals(val calories: Int, val proteinG: Double, val fiberG: Double)

fun foodTotals(foods: List<FoodEntity>, date: LocalDate): FoodTotals {
    val day = foods.filter { Format.toLocalDateTime(it.timestamp).toLocalDate() == date }
    return FoodTotals(day.sumOf { it.calories }, day.sumOf { it.proteinG }, day.sumOf { it.fiberG })
}

/** Distinct favourite / frequent meals to re-log in one tap (favourites first). */
fun quickFoods(foods: List<FoodEntity>): List<FoodEntity> =
    foods.groupBy { it.name.trim().lowercase() }
        .values
        .map { group -> group.first() to group.size }
        .sortedWith(compareByDescending<Pair<FoodEntity, Int>> { (f, _) -> f.favorite }.thenByDescending { it.second })
        .map { it.first }
        .filter { it.favorite || foods.count { f -> f.name.equals(it.name, ignoreCase = true) } > 1 }
        .take(8)

@Composable
fun FoodCard(
    foods: List<FoodEntity>,
    date: LocalDate,
    settings: UserSettings,
    openEditor: (Editor) -> Unit,
    onQuickAdd: (FoodEntity) -> Unit,
) {
    val day = foods.filter { Format.toLocalDateTime(it.timestamp).toLocalDate() == date }
    val totals = foodTotals(foods, date)
    val quick = remember(foods) { quickFoods(foods) }
    SectionCard(
        title = stringResource(R.string.meals),
        icon = Icons.Outlined.LunchDining,
        action = { TextButton(onClick = { openEditor(Editor.Food()) }) { Text(stringResource(R.string.add)) } },
    ) {
        TotalRow(stringResource(R.string.calories), totals.calories.toDouble(), settings.calorieGoal.toDouble(), "kcal")
        TotalRow(stringResource(R.string.fiber), totals.fiberG, settings.fiberGoalG.toDouble(), "g")
        if (quick.isNotEmpty()) {
            Text(stringResource(R.string.quick_add), style = MaterialTheme.typography.labelLarge)
            QuickFoods(quick, onQuickAdd)
        }
        day.forEach { f ->
            Row(
                Modifier.fillMaxWidth().clickable { openEditor(Editor.Food(f)) }.padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(f.name, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        Format.time(Format.toLocalDateTime(f.timestamp)) + " · " +
                            stringResource(R.string.food_macros, Format.number(f.proteinG, 1), Format.number(f.fiberG, 1)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text("${f.calories} kcal", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickFoods(quick: List<FoodEntity>, onQuickAdd: (FoodEntity) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        quick.forEach { f ->
            AssistChip(
                onClick = { onQuickAdd(f) },
                label = { Text("${f.name} · ${f.calories}") },
                leadingIcon = if (f.favorite) { { Icon(Icons.Filled.Star, null) } } else null,
            )
        }
    }
}

@Composable
private fun TotalRow(label: String, value: Double, goal: Double, unit: String) {
    Column {
        Text("$label · ${Format.number(value, 0)} / ${Format.number(goal, 0)} $unit", style = MaterialTheme.typography.bodyMedium)
        LinearProgressIndicator(
            progress = { if (goal > 0) (value / goal).toFloat().coerceIn(0f, 1f) else 0f },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        )
    }
}

@Composable
fun FoodEditor(
    item: FoodEntity?,
    date: LocalDate,
    onDismiss: () -> Unit,
    onSave: (FoodEntity) -> Unit,
    onDelete: (FoodEntity) -> Unit,
) {
    val initialTime = if (date == LocalDate.now()) LocalDateTime.now() else date.atTime(LocalTime.NOON)
    var dateTime by remember { mutableStateOf(item?.let { Format.toLocalDateTime(it.timestamp) } ?: initialTime.withSecond(0).withNano(0)) }
    var name by remember { mutableStateOf(item?.name ?: "") }
    var kcal by remember { mutableStateOf(item?.calories?.toString() ?: "") }
    var protein by remember { mutableStateOf(item?.proteinG?.let { Format.number(it, 1) } ?: "") }
    var fiber by remember { mutableStateOf(item?.fiberG?.let { Format.number(it, 1) } ?: "") }
    var favorite by remember { mutableStateOf(item?.favorite ?: false) }

    EditorSheet(
        title = stringResource(if (item == null) R.string.add_food else R.string.edit_food),
        canSave = name.isNotBlank(),
        onDismiss = onDismiss,
        onSave = {
            onSave(
                FoodEntity(
                    id = item?.id ?: 0,
                    timestamp = Format.toMillis(dateTime),
                    name = name.trim(),
                    calories = Format.parse(kcal)?.toInt() ?: 0,
                    proteinG = Format.parse(protein) ?: 0.0,
                    fiberG = Format.parse(fiber) ?: 0.0,
                    favorite = favorite,
                )
            )
        },
        onDelete = item?.let { { onDelete(it) } },
    ) {
        DateTimeRow(dateTime, { dateTime = it })
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.food_name)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            IconToggleButton(checked = favorite, onCheckedChange = { favorite = it }) {
                Icon(
                    if (favorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                    contentDescription = stringResource(R.string.favorite),
                    tint = MaterialTheme.colorScheme.tertiary,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberBox(kcal, { kcal = it }, stringResource(R.string.calories), "kcal", Modifier.weight(1f))
            NumberBox(protein, { protein = it }, stringResource(R.string.protein), "g", Modifier.weight(1f))
            NumberBox(fiber, { fiber = it }, stringResource(R.string.fiber), "g", Modifier.weight(1f))
        }
        Text(
            stringResource(R.string.food_tip),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NumberBox(value: String, onChange: (String) -> Unit, label: String, suffix: String, modifier: Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, maxLines = 1) },
        suffix = { Text(suffix) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}

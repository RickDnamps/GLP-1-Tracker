package com.rickdnamps.glp1tracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.dp
import com.rickdnamps.glp1tracker.R
import com.rickdnamps.glp1tracker.data.SupplementEntity
import com.rickdnamps.glp1tracker.ui.AppViewModel
import com.rickdnamps.glp1tracker.ui.UiState
import com.rickdnamps.glp1tracker.ui.components.SectionCard
import java.time.LocalDate
import java.time.ZoneId

private val dailyPresets = listOf(
    R.string.sup_multivitamin, R.string.sup_b12, R.string.sup_vitamin_d, R.string.sup_iron, R.string.sup_fiber,
    R.string.sup_magnesium, R.string.sup_electrolytes, R.string.sup_creatine, R.string.sup_protein_shake, R.string.sup_probiotic,
)
private val remedyPresets = listOf(
    R.string.rem_antinausea, R.string.rem_laxative, R.string.rem_antacid, R.string.rem_simethicone,
    R.string.rem_ginger, R.string.rem_antidiarrheal,
)

/**
 * Supplements ticked once a day and as-needed remedies counted per intake, for [date].
 * [compact] (home screen) shows only the daily ones.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SupplementsCard(state: UiState, vm: AppViewModel, date: LocalDate, compact: Boolean = false) {
    val items = state.supplements.filter { !it.archived }
    var manage by rememberSaveable { mutableStateOf(false) }
    val zone = ZoneId.systemDefault()
    val from = date.atStartOfDay(zone).toInstant().toEpochMilli()
    val to = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    val counts = remember(state.supplementLogs, from) {
        state.supplementLogs.filter { it.timestamp in from until to }.groupingBy { it.supplementId }.eachCount()
    }
    val daily = items.filter { !it.asNeeded }
    val remedies = items.filter { it.asNeeded }

    SectionCard(
        title = stringResource(R.string.supplements_title),
        icon = Icons.Outlined.Medication,
        action = { TextButton(onClick = { manage = true }) { Text(stringResource(R.string.supplements_manage)) } },
    ) {
        if (items.isEmpty()) {
            Text(stringResource(R.string.supplements_empty), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (daily.isNotEmpty()) {
            val taken = daily.count { (counts[it.id] ?: 0) > 0 }
            Text(stringResource(R.string.supplements_taken, taken, daily.size), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                daily.forEach { item ->
                    val on = (counts[item.id] ?: 0) > 0
                    FilterChip(
                        selected = on,
                        onClick = { vm.toggleSupplement(item, date) },
                        label = { Text(item.name) },
                        leadingIcon = if (on) {
                            { Icon(Icons.Outlined.Check, null, Modifier.size(18.dp)) }
                        } else null,
                    )
                }
            }
        }
        if (!compact && remedies.isNotEmpty()) {
            Text(stringResource(R.string.supplements_remedies), style = MaterialTheme.typography.labelLarge)
            remedies.forEach { item ->
                val n = counts[item.id] ?: 0
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(item.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    if (n > 0) {
                        OutlinedButton(onClick = { vm.removeLastIntake(item, date) }, contentPadding = PaddingValues(horizontal = 10.dp)) { Text("−") }
                        Spacer(Modifier.width(6.dp))
                    }
                    Text("× $n", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.width(8.dp))
                    FilledTonalButton(onClick = { vm.toggleSupplement(item, date) }, contentPadding = PaddingValues(horizontal = 12.dp)) { Text("+1") }
                }
            }
        }
    }

    if (manage) ManageSupplementsDialog(state.supplements, onDismiss = { manage = false }, onSave = { vm.saveSupplement(it) })
}

/** Add presets or custom items, hide the ones no longer taken (their history is kept). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ManageSupplementsDialog(all: List<SupplementEntity>, onDismiss: () -> Unit, onSave: (SupplementEntity) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var asNeeded by rememberSaveable { mutableStateOf(false) }
    val active = all.filter { !it.archived }
    fun add(n: String, remedy: Boolean) {
        val clean = n.trim()
        if (clean.isEmpty()) return
        val existing = all.firstOrNull { it.name.equals(clean, ignoreCase = true) }
        onSave(existing?.copy(archived = false, asNeeded = remedy) ?: SupplementEntity(name = clean, asNeeded = remedy))
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.supplements_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                active.forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            item.name + if (item.asNeeded) " · " + stringResource(R.string.supplements_as_needed) else "",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { onSave(item.copy(archived = true)) }) {
                            Icon(Icons.Outlined.Close, stringResource(R.string.delete))
                        }
                    }
                }
                Text(stringResource(R.string.supplements_daily_presets), style = MaterialTheme.typography.labelLarge)
                PresetChips(dailyPresets, active) { add(it, false) }
                Text(stringResource(R.string.supplements_remedy_presets), style = MaterialTheme.typography.labelLarge)
                PresetChips(remedyPresets, active) { add(it, true) }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.supplements_custom)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.supplements_as_needed), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Switch(checked = asNeeded, onCheckedChange = { asNeeded = it })
                }
                FilledTonalButton(onClick = { add(name, asNeeded); name = "" }, enabled = name.isNotBlank()) {
                    Text(stringResource(R.string.add))
                }
                Text(stringResource(R.string.supplements_disclaimer), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ok)) } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PresetChips(presets: List<Int>, active: List<SupplementEntity>, onAdd: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        presets.forEach { res ->
            val label = stringResource(res)
            if (active.none { it.name.equals(label, ignoreCase = true) }) {
                AssistChip(onClick = { onAdd(label) }, label = { Text("+ $label") })
            }
        }
    }
}

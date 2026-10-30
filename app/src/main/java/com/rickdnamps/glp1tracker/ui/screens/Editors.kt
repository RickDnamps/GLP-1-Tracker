package com.rickdnamps.glp1tracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import com.rickdnamps.glp1tracker.ui.COMFORT_EMOJIS
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rickdnamps.glp1tracker.R
import com.rickdnamps.glp1tracker.data.FoodEntity
import com.rickdnamps.glp1tracker.data.InjectionEntity
import com.rickdnamps.glp1tracker.data.MeasurementEntity
import com.rickdnamps.glp1tracker.data.SideEffectEntity
import com.rickdnamps.glp1tracker.data.UserSettings
import com.rickdnamps.glp1tracker.data.WeightEntity
import com.rickdnamps.glp1tracker.data.composition
import com.rickdnamps.glp1tracker.data.withComposition
import com.rickdnamps.glp1tracker.domain.BodyComp
import com.rickdnamps.glp1tracker.domain.Format
import com.rickdnamps.glp1tracker.domain.InjectionSite
import com.rickdnamps.glp1tracker.domain.Medication
import com.rickdnamps.glp1tracker.domain.MissedDose
import com.rickdnamps.glp1tracker.domain.Severity
import com.rickdnamps.glp1tracker.domain.SideEffectType
import com.rickdnamps.glp1tracker.ui.components.BodyMap
import com.rickdnamps.glp1tracker.ui.components.ChoiceDialog
import com.rickdnamps.glp1tracker.ui.components.ConfirmDialog
import com.rickdnamps.glp1tracker.ui.components.DateTimeRow
import com.rickdnamps.glp1tracker.ui.medicationName
import java.time.LocalDate
import java.time.LocalDateTime

/** Which editor sheet is open; a null item means "create". */
sealed interface Editor {
    data class Injection(val item: InjectionEntity? = null) : Editor
    data class Weight(val item: WeightEntity? = null) : Editor
    data class SideEffect(val item: SideEffectEntity? = null, val date: LocalDate? = null) : Editor
    data object Scale : Editor
    data class Measurement(val item: MeasurementEntity? = null) : Editor
    data class Food(val item: FoodEntity? = null, val date: LocalDate = LocalDate.now()) : Editor
    data object Titration : Editor
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditorSheet(
    title: String,
    canSave: Boolean,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onDelete: (() -> Unit)?,
    content: @Composable () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var confirmDelete by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            content()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (onDelete != null) {
                    TextButton(onClick = { confirmDelete = true }) {
                        Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                    }
                }
                Spacer(Modifier.weight(1f))
                OutlinedButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                Button(onClick = onSave, enabled = canSave) { Text(stringResource(R.string.save)) }
            }
        }
    }
    if (confirmDelete && onDelete != null) {
        ConfirmDialog(
            title = stringResource(R.string.delete_confirm_title),
            text = stringResource(R.string.delete_confirm_text),
            confirmLabel = stringResource(R.string.delete),
            onDismiss = { confirmDelete = false },
            onConfirm = { confirmDelete = false; onDelete() },
        )
    }
}

@Composable
internal fun FieldLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

private val comfortLabels = listOf(R.string.comfort_1, R.string.comfort_2, R.string.comfort_3, R.string.comfort_4, R.string.comfort_5)

/** How the injection felt: five faces, tap again to clear. */
@Composable
private fun ComfortPicker(value: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        COMFORT_EMOJIS.forEachIndexed { i, emoji ->
            FilterChip(
                selected = value == i + 1,
                onClick = { onSelect(i + 1) },
                label = { Text(emoji, style = MaterialTheme.typography.titleMedium) },
            )
        }
    }
    Text(
        stringResource(if (value in 1..5) comfortLabels[value - 1] else R.string.comfort_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InjectionEditor(
    item: InjectionEntity?,
    settings: UserSettings,
    injections: List<InjectionEntity>,
    onDismiss: () -> Unit,
    onSave: (InjectionEntity) -> Unit,
    onDelete: (InjectionEntity) -> Unit,
) {
    val lastUse = remember(injections, item) {
        injections.filter { it.id != item?.id }
            .groupBy { InjectionSite.fromName(it.site) }
            .mapValues { (_, l) -> l.maxOf { it.timestamp } }
    }
    val suggested = remember(lastUse) { InjectionSite.suggest(lastUse) }

    var dateTime by remember { mutableStateOf(item?.let { Format.toLocalDateTime(it.timestamp) } ?: LocalDateTime.now().withSecond(0).withNano(0)) }
    var medication by remember { mutableStateOf(item?.let { Medication.fromName(it.medication) } ?: settings.medication) }
    var doseText by remember { mutableStateOf(Format.dose(item?.doseMg ?: settings.doseMg)) }
    var site by remember {
        mutableStateOf(item?.let { InjectionSite.fromName(it.site) } ?: if (settings.medication.isOral) InjectionSite.NONE else suggested)
    }
    var notes by remember { mutableStateOf(item?.notes ?: "") }
    var comfort by remember { mutableIntStateOf(item?.comfort ?: 0) }
    var chooseMed by remember { mutableStateOf(false) }
    var confirmEarly by remember { mutableStateOf<InjectionEntity?>(null) }
    val dose = Format.parse(doseText)
    // Previous dose before the chosen time, to catch accidental double doses.
    val previous = injections.filter { it.id != item?.id && it.timestamp < Format.toMillis(dateTime) }.maxByOrNull { it.timestamp }
    val guardHours = MissedDose.guardHours(medication, settings.intervalDays)

    EditorSheet(
        title = stringResource(if (item == null) R.string.add_dose else R.string.edit_dose),
        canSave = dose != null && dose > 0,
        onDismiss = onDismiss,
        onSave = {
            val entity = InjectionEntity(
                id = item?.id ?: 0,
                timestamp = Format.toMillis(dateTime),
                medication = medication.name,
                doseMg = dose ?: 0.0,
                site = if (medication.isOral) InjectionSite.NONE.name else site.name,
                notes = notes.trim(),
                comfort = if (medication.isOral) 0 else comfort,
            )
            val tooSoon = item == null && previous != null && entity.timestamp - previous.timestamp < guardHours * 3_600_000L
            if (tooSoon) confirmEarly = entity else onSave(entity)
        },
        onDelete = item?.let { { onDelete(it) } },
    ) {
        DateTimeRow(dateTime, { dateTime = it })

        FieldLabel(stringResource(R.string.medication))
        OutlinedButton(onClick = { chooseMed = true }, modifier = Modifier.fillMaxWidth()) {
            Text(medicationName(medication, settings))
        }

        FieldLabel(stringResource(R.string.dose))
        if (medication.commonDosesMg.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                medication.commonDosesMg.forEach { d ->
                    FilterChip(
                        selected = dose == d,
                        onClick = { doseText = Format.dose(d) },
                        label = { Text("${Format.dose(d)} mg") },
                    )
                }
            }
        }
        OutlinedTextField(
            value = doseText,
            onValueChange = { doseText = it },
            label = { Text(stringResource(R.string.dose)) },
            suffix = { Text("mg") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )

        if (!medication.isOral) {
            FieldLabel(stringResource(R.string.injection_site))
            InjectionSitePicker(selected = site, suggested = suggested, lastUse = lastUse, onSelect = { site = it }, female = settings.sex == 2)
            FieldLabel(stringResource(R.string.comfort_label))
            ComfortPicker(comfort) { comfort = if (comfort == it) 0 else it }
        }

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text(stringResource(R.string.notes)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
        )
    }

    confirmEarly?.let { entity ->
        val prev = previous
        ConfirmDialog(
            title = stringResource(R.string.double_dose_title),
            text = stringResource(
                R.string.double_dose_text,
                prev?.let { Format.dateTime(it.timestamp) } ?: "—",
                prev?.let { stringResource(InjectionSite.fromName(it.site).nameRes) } ?: "",
                guardHours,
            ),
            confirmLabel = stringResource(R.string.double_dose_confirm),
            onDismiss = { confirmEarly = null },
            onConfirm = { confirmEarly = null; onSave(entity) },
        )
    }

    if (chooseMed) {
        ChoiceDialog(
            title = stringResource(R.string.medication),
            options = Medication.entries,
            selected = medication,
            label = { medicationName(it, settings) },
            onDismiss = { chooseMed = false },
            onSelect = {
                medication = it
                val current = Format.parse(doseText)
                if (it.commonDosesMg.isNotEmpty() && (current == null || current !in it.commonDosesMg)) {
                    doseText = Format.dose(it.commonDosesMg.first())
                }
                if (!it.isOral && site == InjectionSite.NONE) site = suggested
                chooseMed = false
            },
        )
    }
}

@Composable
fun InjectionSitePicker(
    selected: InjectionSite,
    suggested: InjectionSite,
    lastUse: Map<InjectionSite, Long>,
    onSelect: (InjectionSite) -> Unit,
    female: Boolean = false,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BodyMap(selected = selected, suggested = suggested, lastUse = lastUse, onSelect = onSelect, female = female)
        if (selected != InjectionSite.NONE) {
            Text(
                stringResource(R.string.bodymap_selected, stringResource(selected.nameRes)),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Star, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.padding(end = 4.dp))
            Text(
                stringResource(R.string.suggested_site, stringResource(suggested.nameRes)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun WeightEditor(
    item: WeightEntity?,
    settings: UserSettings,
    lastWeightKg: Double?,
    onDismiss: () -> Unit,
    onSave: (WeightEntity) -> Unit,
    onDelete: (WeightEntity) -> Unit,
) {
    val unit = settings.weightUnit
    var dateTime by remember { mutableStateOf(item?.let { Format.toLocalDateTime(it.timestamp) } ?: LocalDateTime.now().withSecond(0).withNano(0)) }
    var weightText by remember {
        mutableStateOf((item?.weightKg ?: lastWeightKg)?.let { Format.oneDecimal(unit.fromKg(it)) } ?: "")
    }
    var notes by remember { mutableStateOf(item?.notes ?: "") }
    val compFields = remember { CompositionFields(item?.composition ?: BodyComp(), unit) }
    var showComp by remember { mutableStateOf(item?.composition?.isEmpty == false) }
    val weight = Format.parse(weightText)

    EditorSheet(
        title = stringResource(if (item == null) R.string.add_weight else R.string.edit_weight),
        canSave = weight != null && weight > 0,
        onDismiss = onDismiss,
        onSave = {
            onSave(
                WeightEntity(
                    id = item?.id ?: 0,
                    timestamp = Format.toMillis(dateTime),
                    weightKg = unit.toKg(weight ?: 0.0),
                    waistCm = item?.waistCm,
                    notes = notes.trim(),
                ).withComposition(compFields.toComposition())
            )
        },
        onDelete = item?.let { { onDelete(it) } },
    ) {
        DateTimeRow(dateTime, { dateTime = it })
        OutlinedTextField(
            value = weightText,
            onValueChange = { weightText = it },
            label = { Text(stringResource(R.string.weight)) },
            suffix = { Text(stringResource(unit.symbolRes)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        TextButton(onClick = { showComp = !showComp }) {
            Text((if (showComp) "▾ " else "▸ ") + stringResource(R.string.comp_section))
        }
        if (showComp) CompositionInputs(compFields, stringResource(unit.symbolRes))
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text(stringResource(R.string.notes)) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SideEffectEditor(
    item: SideEffectEntity?,
    date: LocalDate? = null,
    onDismiss: () -> Unit,
    onSave: (SideEffectEntity) -> Unit,
    onDelete: (SideEffectEntity) -> Unit,
) {
    var dateTime by remember {
        mutableStateOf(
            item?.let { Format.toLocalDateTime(it.timestamp) }
                ?: date?.takeIf { it != LocalDate.now() }?.atTime(12, 0)
                ?: LocalDateTime.now().withSecond(0).withNano(0)
        )
    }
    var type by remember { mutableStateOf(item?.let { SideEffectType.fromName(it.type) }) }
    var severity by remember { mutableStateOf(Severity.fromLevel(item?.severity ?: 1)) }
    var notes by remember { mutableStateOf(item?.notes ?: "") }

    EditorSheet(
        title = stringResource(if (item == null) R.string.add_side_effect else R.string.edit_side_effect),
        canSave = type != null,
        onDismiss = onDismiss,
        onSave = {
            onSave(
                SideEffectEntity(
                    id = item?.id ?: 0,
                    timestamp = Format.toMillis(dateTime),
                    type = (type ?: SideEffectType.OTHER).name,
                    severity = severity.level,
                    notes = notes.trim(),
                )
            )
        },
        onDelete = item?.let { { onDelete(it) } },
    ) {
        DateTimeRow(dateTime, { dateTime = it })
        FieldLabel(stringResource(R.string.symptom))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SideEffectType.entries.forEach { t ->
                FilterChip(selected = type == t, onClick = { type = t }, label = { Text(stringResource(t.nameRes)) })
            }
        }
        FieldLabel(stringResource(R.string.severity))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            Severity.entries.forEachIndexed { i, s ->
                SegmentedButton(
                    selected = severity == s,
                    onClick = { severity = s },
                    shape = SegmentedButtonDefaults.itemShape(i, Severity.entries.size),
                ) { Text(stringResource(s.nameRes)) }
            }
        }
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text(stringResource(R.string.notes)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
        )
    }
}

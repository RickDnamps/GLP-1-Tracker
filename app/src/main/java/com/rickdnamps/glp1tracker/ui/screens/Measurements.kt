package com.rickdnamps.glp1tracker.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Straighten
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rickdnamps.glp1tracker.R
import com.rickdnamps.glp1tracker.data.MeasurementEntity
import com.rickdnamps.glp1tracker.data.UserSettings
import com.rickdnamps.glp1tracker.domain.Format
import com.rickdnamps.glp1tracker.ui.components.DateTimeRow
import com.rickdnamps.glp1tracker.ui.components.SectionCard
import java.time.LocalDateTime

/** One body measurement field, with accessor and copy function for the entity. */
private enum class BodyPart(@StringRes val label: Int, val get: (MeasurementEntity) -> Double?) {
    WAIST(R.string.m_waist, { it.waistCm }),
    HIPS(R.string.m_hips, { it.hipsCm }),
    CHEST(R.string.m_chest, { it.chestCm }),
    ARM(R.string.m_arm, { it.armCm }),
    THIGH(R.string.m_thigh, { it.thighCm }),
    NECK(R.string.m_neck, { it.neckCm }),
}

@Composable
fun MeasurementsCard(measurements: List<MeasurementEntity>, settings: UserSettings, openEditor: (Editor) -> Unit) {
    SectionCard(
        title = stringResource(R.string.measurements),
        icon = Icons.Outlined.Straighten,
        action = { TextButton(onClick = { openEditor(Editor.Measurement()) }) { Text(stringResource(R.string.add)) } },
    ) {
        if (measurements.isEmpty()) {
            Text(stringResource(R.string.no_measurement_yet), color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@SectionCard
        }
        BodyPart.entries.forEach { part ->
            val values = measurements.mapNotNull { m -> part.get(m)?.let { m.timestamp to it } }
            if (values.isEmpty()) return@forEach
            val latest = values.last().second
            val delta = latest - values.first().second
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(part.label), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Text(Format.oneDecimal(latest) + " cm", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                if (values.size > 1 && kotlin.math.abs(delta) >= 0.05) {
                    Text(
                        (if (delta > 0) "  +" else "  ") + Format.oneDecimal(delta),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (delta < 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
        // Waist-to-height ratio: < 0.5 is the usual healthy threshold.
        val waist = measurements.lastOrNull { it.waistCm != null }?.waistCm
        if (waist != null && settings.heightCm > 0) {
            val ratio = waist / settings.heightCm
            Text(
                stringResource(R.string.waist_height_ratio, Format.number(ratio, 2)),
                style = MaterialTheme.typography.bodyMedium,
                color = if (ratio < 0.5) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
            )
        }
        Text(
            stringResource(R.string.last_measured, Format.date(measurements.last().timestamp)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.clickable { openEditor(Editor.Measurement(measurements.last())) }.padding(vertical = 4.dp),
        )
    }
}

@Composable
fun MeasurementEditor(
    item: MeasurementEntity?,
    last: MeasurementEntity?,
    onDismiss: () -> Unit,
    onSave: (MeasurementEntity) -> Unit,
    onDelete: (MeasurementEntity) -> Unit,
) {
    var dateTime by remember { mutableStateOf(item?.let { Format.toLocalDateTime(it.timestamp) } ?: LocalDateTime.now().withSecond(0).withNano(0)) }
    val fields = remember {
        BodyPart.entries.associateWith { part -> mutableStateOf(item?.let(part.get)?.let(Format::oneDecimal) ?: "") }
    }
    var notes by remember { mutableStateOf(item?.notes ?: "") }
    fun value(part: BodyPart) = Format.parse(fields.getValue(part).value)?.takeIf { it > 0 }
    val any = BodyPart.entries.any { value(it) != null }

    EditorSheet(
        title = stringResource(if (item == null) R.string.add_measurement else R.string.edit_measurement),
        canSave = any,
        onDismiss = onDismiss,
        onSave = {
            onSave(
                MeasurementEntity(
                    id = item?.id ?: 0,
                    timestamp = Format.toMillis(dateTime),
                    waistCm = value(BodyPart.WAIST),
                    hipsCm = value(BodyPart.HIPS),
                    chestCm = value(BodyPart.CHEST),
                    armCm = value(BodyPart.ARM),
                    thighCm = value(BodyPart.THIGH),
                    neckCm = value(BodyPart.NECK),
                    notes = notes.trim(),
                )
            )
        },
        onDelete = item?.let { { onDelete(it) } },
    ) {
        DateTimeRow(dateTime, { dateTime = it })
        BodyPart.entries.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { part ->
                    var text by fields.getValue(part)
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        label = { Text(stringResource(part.label)) },
                        placeholder = { last?.let(part.get)?.let { Text(Format.oneDecimal(it)) } },
                        suffix = { Text("cm") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text(stringResource(R.string.notes)) },
            modifier = Modifier.fillMaxWidth(),
        )
        Column {
            Text(
                stringResource(R.string.measure_tip),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

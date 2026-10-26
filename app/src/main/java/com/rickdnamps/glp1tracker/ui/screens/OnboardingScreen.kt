package com.rickdnamps.glp1tracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.graphics.Color
import com.rickdnamps.glp1tracker.ui.components.HeroCard
import com.rickdnamps.glp1tracker.ui.components.Pill
import com.rickdnamps.glp1tracker.ui.components.topGlow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Vaccines
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rickdnamps.glp1tracker.R
import com.rickdnamps.glp1tracker.data.UserSettings
import com.rickdnamps.glp1tracker.domain.Format
import com.rickdnamps.glp1tracker.domain.Medication
import com.rickdnamps.glp1tracker.domain.WeightUnit
import com.rickdnamps.glp1tracker.ui.components.ChoiceDialog
import com.rickdnamps.glp1tracker.ui.components.TimePickerDialogFor
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(onFinish: (UserSettings, Double?) -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf("") }
    var medName by rememberSaveable { mutableStateOf(Medication.OZEMPIC.name) }
    val medication = Medication.fromName(medName)
    var doseText by rememberSaveable { mutableStateOf(Format.dose(medication.commonDosesMg.first())) }
    var interval by rememberSaveable { mutableIntStateOf(7) }
    var day by rememberSaveable { mutableIntStateOf(LocalDate.now().dayOfWeek.value) }
    var hour by rememberSaveable { mutableIntStateOf(9) }
    var minute by rememberSaveable { mutableIntStateOf(0) }
    // Lb by default for US locale, kg elsewhere.
    var unitName by rememberSaveable {
        mutableStateOf(if (Locale.getDefault().country == "US") WeightUnit.LB.name else WeightUnit.KG.name)
    }
    val unit = WeightUnit.fromName(unitName)
    var weightText by rememberSaveable { mutableStateOf("") }
    var goalText by rememberSaveable { mutableStateOf("") }
    var heightText by rememberSaveable { mutableStateOf("") }
    var chooseMed by rememberSaveable { mutableStateOf(false) }
    var chooseTime by rememberSaveable { mutableStateOf(false) }
    var finishing by rememberSaveable { mutableStateOf(false) }

    val steps = 3
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
      Box(Modifier.fillMaxSize().topGlow(MaterialTheme.colorScheme.primary)) {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .imePadding()
                .padding(20.dp),
        ) {
            LinearProgressIndicator(progress = { (step + 1f) / steps }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(20.dp))
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                when (step) {
                    0 -> {
                        HeroCard {
                            Icon(Icons.Outlined.Vaccines, null, modifier = Modifier.size(56.dp), tint = Color.White)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                stringResource(R.string.welcome_title),
                                style = MaterialTheme.typography.displaySmall,
                                color = Color.White,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(stringResource(R.string.welcome_text), style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.9f))
                            Spacer(Modifier.height(10.dp))
                            Pill(stringResource(R.string.free_forever))
                        }
                        listOf(
                            R.string.feature_doses, R.string.feature_level, R.string.feature_sites, R.string.feature_weight,
                            R.string.feature_side_effects, R.string.feature_private,
                        ).forEach { Text("•  " + stringResource(it), style = MaterialTheme.typography.bodyMedium) }
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text(stringResource(R.string.first_name_optional)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            stringResource(R.string.disclaimer_long),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    1 -> {
                        Text(stringResource(R.string.your_treatment), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.medication), style = MaterialTheme.typography.labelLarge)
                        OutlinedButton(onClick = { chooseMed = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(medication.nameRes))
                        }
                        Text(stringResource(R.string.current_dose), style = MaterialTheme.typography.labelLarge)
                        if (medication.commonDosesMg.isNotEmpty()) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                medication.commonDosesMg.forEach { d ->
                                    FilterChip(
                                        selected = Format.parse(doseText) == d,
                                        onClick = { doseText = Format.dose(d) },
                                        label = { Text("${Format.dose(d)} mg") },
                                    )
                                }
                            }
                        }
                        OutlinedTextField(
                            value = doseText,
                            onValueChange = { doseText = it },
                            suffix = { Text("mg") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(stringResource(R.string.frequency), style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(1, 7, 14).forEach { d ->
                                FilterChip(selected = interval == d, onClick = { interval = d }, label = { Text(intervalLabel(d)) })
                            }
                        }
                        if (interval == 7) {
                            Text(stringResource(R.string.dose_day), style = MaterialTheme.typography.labelLarge)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                DayOfWeek.entries.forEach { d ->
                                    FilterChip(
                                        selected = day == d.value,
                                        onClick = { day = d.value },
                                        label = { Text(dayName(d).take(3)) },
                                    )
                                }
                            }
                        }
                        Text(stringResource(R.string.reminder_time), style = MaterialTheme.typography.labelLarge)
                        OutlinedButton(onClick = { chooseTime = true }) {
                            Text(Format.time(LocalDate.now().atTime(hour, minute)))
                        }
                    }
                    else -> {
                        Text(stringResource(R.string.your_goals), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            WeightUnit.entries.forEachIndexed { i, u ->
                                SegmentedButton(
                                    selected = unit == u,
                                    onClick = { unitName = u.name },
                                    shape = SegmentedButtonDefaults.itemShape(i, WeightUnit.entries.size),
                                ) { Text(stringResource(u.symbolRes)) }
                            }
                        }
                        NumberField(weightText, { weightText = it }, stringResource(R.string.current_weight), stringResource(unit.symbolRes))
                        NumberField(goalText, { goalText = it }, stringResource(R.string.goal_weight), stringResource(unit.symbolRes))
                        NumberField(heightText, { heightText = it }, stringResource(R.string.height_optional), "cm")
                        Text(
                            stringResource(R.string.goals_help),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                if (step > 0) TextButton(onClick = { step-- }) { Text(stringResource(R.string.back)) }
                Spacer(Modifier.weight(1f))
                val dose = Format.parse(doseText)
                Button(
                    enabled = !finishing && (step != 1 || (dose != null && dose > 0)),
                    onClick = {
                        if (step < steps - 1) {
                            step++
                        } else {
                            finishing = true
                            val settings = UserSettings(
                                name = name.trim(),
                                weightUnit = unit,
                                heightCm = Format.parse(heightText) ?: 0.0,
                                goalWeightKg = Format.parse(goalText)?.let(unit::toKg) ?: 0.0,
                                medication = medication,
                                doseMg = dose ?: medication.commonDosesMg.firstOrNull() ?: 0.0,
                                intervalDays = interval,
                                injectionDay = DayOfWeek.of(day),
                                reminderHour = hour,
                                reminderMinute = minute,
                            )
                            onFinish(settings, Format.parse(weightText)?.let(unit::toKg))
                        }
                    },
                ) { Text(stringResource(if (step < steps - 1) R.string.next else R.string.start_tracking)) }
            }
        }
      }
    }

    if (chooseMed) {
        ChoiceDialog(
            title = stringResource(R.string.medication),
            options = Medication.entries,
            selected = medication,
            label = { stringResource(it.nameRes) },
            onDismiss = { chooseMed = false },
            onSelect = {
                medName = it.name
                interval = it.defaultIntervalDays
                doseText = it.commonDosesMg.firstOrNull()?.let(Format::dose) ?: ""
                chooseMed = false
            },
        )
    }
    if (chooseTime) {
        TimePickerDialogFor(LocalTime.of(hour, minute), { chooseTime = false }) {
            hour = it.hour
            minute = it.minute
            chooseTime = false
        }
    }
}

@Composable
private fun NumberField(value: String, onChange: (String) -> Unit, label: String, suffix: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        suffix = { Text(suffix) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
    )
}

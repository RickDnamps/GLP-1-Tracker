package com.rickdnamps.glp1tracker.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rickdnamps.glp1tracker.R
import com.rickdnamps.glp1tracker.domain.Format
import com.rickdnamps.glp1tracker.domain.Syringe
import com.rickdnamps.glp1tracker.ui.UiState
import com.rickdnamps.glp1tracker.ui.components.SectionCard

@Composable
fun ToolsScreen(state: UiState, contentPadding: PaddingValues) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { SyringeCalculator(state) }
        item { RedFlags() }
    }
}

@Composable
private fun SyringeCalculator(state: UiState) {
    var concentration by rememberSaveable { mutableStateOf("") }
    var dose by rememberSaveable { mutableStateOf(Format.dose(state.settings.doseMg)) }
    val usual = state.injections.firstOrNull()?.doseMg
    val result = Syringe.compute(Format.parse(concentration) ?: 0.0, Format.parse(dose) ?: 0.0, usualDoseMg = usual)

    SectionCard(title = stringResource(R.string.syringe_title), icon = Icons.Outlined.Calculate) {
        Text(stringResource(R.string.syringe_help), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = concentration,
                onValueChange = { concentration = it },
                label = { Text(stringResource(R.string.syringe_concentration)) },
                suffix = { Text("mg/mL") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = dose,
                onValueChange = { dose = it },
                label = { Text(stringResource(R.string.dose)) },
                suffix = { Text("mg") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
        }
        if (result != null) {
            Text(
                stringResource(R.string.syringe_result, Format.number(result.ml, 3), Format.number(result.units, 1)),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            SyringeDrawing(result.units.toFloat())
            result.warnings.forEach { w ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.WarningAmber, null, tint = MaterialTheme.colorScheme.error)
                    Text(
                        stringResource(
                            when (w) {
                                Syringe.Warning.TOO_SMALL_TO_MEASURE -> R.string.syringe_warn_small
                                Syringe.Warning.OVER_SYRINGE -> R.string.syringe_warn_over
                                Syringe.Warning.TENFOLD_CHANGE -> R.string.syringe_warn_tenfold
                            }
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
        }
        Text(stringResource(R.string.syringe_disclaimer), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** U-100 syringe with 100 graduations; the barrel fills up to [units]. */
@Composable
private fun SyringeDrawing(units: Float) {
    val fill by animateFloatAsState((units / 100f).coerceIn(0f, 1f), tween(700), label = "fill")
    val outline = MaterialTheme.colorScheme.onSurfaceVariant
    val liquid = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
    val plunger = MaterialTheme.colorScheme.outline
    Canvas(Modifier.fillMaxWidth().height(70.dp)) {
        val needle = size.width * 0.12f
        val barrelLeft = needle
        val barrelRight = size.width * 0.92f
        val top = size.height * 0.25f
        val bottom = size.height * 0.75f
        val w = barrelRight - barrelLeft
        // needle
        drawLine(outline, Offset(0f, size.height / 2), Offset(barrelLeft, size.height / 2), strokeWidth = 2.dp.toPx())
        // liquid
        drawRect(liquid, Offset(barrelLeft, top), Size(w * fill, bottom - top))
        // plunger at the fill level
        drawRect(plunger, Offset(barrelLeft + w * fill, top), Size(4.dp.toPx(), bottom - top))
        drawLine(plunger, Offset(barrelLeft + w * fill, size.height / 2), Offset(size.width, size.height / 2), strokeWidth = 3.dp.toPx())
        // barrel and graduations (every 2 units, long every 10)
        drawRoundRect(outline, Offset(barrelLeft, top), Size(w, bottom - top), CornerRadius(4.dp.toPx()), style = Stroke(2.dp.toPx()))
        for (u in 0..100 step 2) {
            val x = barrelLeft + w * u / 100f
            val len = if (u % 10 == 0) (bottom - top) * 0.45f else (bottom - top) * 0.2f
            drawLine(outline, Offset(x, top), Offset(x, top + len), strokeWidth = 1.dp.toPx())
        }
    }
}

private data class RedFlag(@StringRes val title: Int, @StringRes val detail: Int, val emergency: Boolean)

private val redFlags = listOf(
    RedFlag(R.string.rf_pancreatitis, R.string.rf_pancreatitis_detail, true),
    RedFlag(R.string.rf_gallbladder, R.string.rf_gallbladder_detail, true),
    RedFlag(R.string.rf_dehydration, R.string.rf_dehydration_detail, false),
    RedFlag(R.string.rf_allergy, R.string.rf_allergy_detail, true),
    RedFlag(R.string.rf_hypoglycemia, R.string.rf_hypoglycemia_detail, false),
    RedFlag(R.string.rf_vision, R.string.rf_vision_detail, false),
    RedFlag(R.string.rf_thyroid, R.string.rf_thyroid_detail, false),
    RedFlag(R.string.rf_mood, R.string.rf_mood_detail, true),
)

@Composable
private fun RedFlags() {
    val context = LocalContext.current
    fun dial(number: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))) }
    }
    SectionCard(title = stringResource(R.string.rf_title), icon = Icons.Outlined.ReportProblem) {
        Text(stringResource(R.string.rf_intro), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        redFlags.forEach { flag ->
            var open by remember { mutableStateOf(false) }
            Column(
                Modifier
                    .fillMaxWidth()
                    .clickable { open = !open }
                    .animateContentSize()
                    .padding(vertical = 8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(flag.title),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (flag.emergency) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(if (open) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null)
                }
                if (open) {
                    Text(stringResource(flag.detail), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { dial("911") },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Outlined.Call, null)
                Text(" 911")
            }
            OutlinedButton(onClick = { dial("811") }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.rf_811)) }
            OutlinedButton(onClick = { dial("988") }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.rf_988)) }
        }
        Text(stringResource(R.string.rf_disclaimer), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

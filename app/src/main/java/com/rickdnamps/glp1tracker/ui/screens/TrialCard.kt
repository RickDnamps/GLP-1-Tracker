package com.rickdnamps.glp1tracker.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rickdnamps.glp1tracker.R
import com.rickdnamps.glp1tracker.domain.Format
import com.rickdnamps.glp1tracker.domain.Medication
import com.rickdnamps.glp1tracker.domain.Trial
import com.rickdnamps.glp1tracker.domain.Trials
import com.rickdnamps.glp1tracker.ui.UiState
import com.rickdnamps.glp1tracker.ui.components.Pill
import com.rickdnamps.glp1tracker.ui.components.SectionCard
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * "Where am I vs the studies?": the user's % weight change since the first dose against the mean
 * curve of the reference trial for their medication and dose (P10). Group averages only, with the
 * source and a permanent disclaimer.
 */
@Composable
fun TrialComparisonCard(state: UiState) {
    val settings = state.settings
    val report = remember(settings.medication, settings.doseMg, settings.startWeightKg, state.injections, state.weights) {
        Trials.report(
            medication = settings.medication,
            doseMg = settings.doseMg,
            doses = state.injections.map { it.timestamp to Medication.fromName(it.medication) },
            weights = state.weights.map { it.timestamp to it.weightKg },
            startWeightKg = settings.startWeightKg,
        )
    }
    val trial = report.trial
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    SectionCard(title = stringResource(R.string.trial_title), icon = Icons.Outlined.Science) {
        if (trial == null) {
            Text(stringResource(R.string.trial_no_trial), style = MaterialTheme.typography.bodyMedium, color = muted)
        } else {
            val armLabel = stringResource(R.string.trial_arm, trial.study, stringResource(trial.molecule.nameRes), Format.dose(trial.doseMg))
            Pill(
                armLabel,
                container = MaterialTheme.colorScheme.secondaryContainer,
                content = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            TrialChart(
                user = report.userCurve,
                trial = trial,
                description = stringResource(R.string.trial_chart_cd, trial.study),
            )
            ChartLegend(
                you = stringResource(R.string.trial_legend_you),
                study = stringResource(R.string.trial_legend_trial, trial.study),
            )
            when (report.status) {
                Trials.Status.NO_DOSES -> Hint(stringResource(R.string.trial_no_doses))
                Trials.Status.NO_BASELINE -> Hint(stringResource(R.string.trial_no_baseline))
                Trials.Status.NO_WEIGHTS -> Hint(stringResource(R.string.trial_no_weights))
                Trials.Status.TOO_EARLY -> Hint(stringResource(R.string.trial_too_early))
                Trials.Status.READY -> report.comparison?.let { Verdict(it, trial) }
                Trials.Status.NO_TRIAL -> Unit
            }
            val placebo = trial.placeboPercent?.let { " " + stringResource(R.string.trial_placebo, signedPercent(it), trial.endWeek) }.orEmpty()
            Text(
                stringResource(R.string.trial_source, armLabel, trial.citation) + placebo,
                style = MaterialTheme.typography.labelSmall,
                color = muted,
            )
            if (trial.diabetes) {
                Text(stringResource(R.string.trial_diabetes_note), style = MaterialTheme.typography.labelSmall, color = muted)
            }
            Text(stringResource(R.string.trial_disclaimer), style = MaterialTheme.typography.labelSmall, color = muted)
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** Highlighted sentence: "Week 12: you're at −7.1 % vs ≈ −6.0 % on average in STEP 1 — ahead of the study average 🎉". */
@Composable
private fun Verdict(c: Trials.Comparison, trial: Trial) {
    val you = signedPercent(c.userPercent)
    val week = floor(c.week).toInt()
    val fact = if (c.beyondTrial) {
        stringResource(R.string.trial_compare_end, week, you, signedPercent(trial.endPercent), trial.study, trial.endWeek)
    } else {
        stringResource(R.string.trial_compare, week, you, signedPercent(c.trialPercent), trial.study)
    }
    val verdict = stringResource(
        when (c.standing) {
            Trials.Standing.AHEAD -> R.string.trial_verdict_ahead
            Trials.Standing.ON_PAR -> R.string.trial_verdict_on_par
            Trials.Standing.BEHIND -> R.string.trial_verdict_behind
        }
    )
    val scheme = MaterialTheme.colorScheme
    val brush = remember(scheme.primaryContainer, scheme.tertiaryContainer) {
        Brush.linearGradient(listOf(scheme.primaryContainer, scheme.tertiaryContainer))
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(brush)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            stringResource(R.string.trial_sentence, fact, verdict),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = scheme.onPrimaryContainer,
        )
        if (c.standing == Trials.Standing.BEHIND) {
            Text(
                stringResource(R.string.trial_behind_note),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onPrimaryContainer.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
private fun ChartLegend(you: String, study: String) {
    val userColor = MaterialTheme.colorScheme.primary
    val trialColor = MaterialTheme.colorScheme.tertiary
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        LegendItem(you, userColor, dashed = false)
        LegendItem(study, trialColor, dashed = true)
    }
}

@Composable
private fun LegendItem(label: String, color: Color, dashed: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(width = 22.dp, height = 10.dp)) {
            val y = size.height / 2
            drawLine(
                color, Offset(0f, y), Offset(size.width, y),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())) else null,
            )
        }
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * Two hand-drawn lines on a weeks × % grid: the user's curve (solid, filled down from 0 %) and the trial
 * mean (dashed). Zooms on the first weeks early on and shows the whole trial once the user is a third in.
 */
@Composable
private fun TrialChart(user: List<Pair<Double, Double>>, trial: Trial, description: String) {
    val lastWeek = user.lastOrNull()?.first ?: 0.0
    val xMax = remember(lastWeek, trial) { chartWeeks(lastWeek, trial.endWeek) }
    // The trial line stops at the last published week (never extended past the end of the study).
    val trialLine = remember(trial, xMax) {
        val shown = trial.points.filter { it.first <= xMax }.map { it.first.toDouble() to it.second }
        if (xMax < trial.endWeek) shown + (xMax to Trials.expectedAt(trial, xMax)) else shown
    }
    val xStep = weekStep(xMax)
    val xTicks = (0..(xMax / xStep).toInt()).map { it * xStep }
    val xLabels = xTicks.map { stringResource(R.string.trial_week_axis, it) }
    val values = user.map { it.second } + trialLine.map { it.second } + 0.0
    val yStep = percentStep(values.max() - values.min())
    val yMin = min(-yStep, floor(values.min() / yStep) * yStep)
    val yMax = max(0.0, ceil(values.max() / yStep) * yStep)
    val yTicks = (0..((yMax - yMin) / yStep).roundToInt()).map { yMin + it * yStep }

    val measurer = rememberTextMeasurer()
    val scheme = MaterialTheme.colorScheme
    val labelStyle = TextStyle(fontSize = 10.sp, color = scheme.onSurfaceVariant)
    val gridColor = scheme.outline.copy(alpha = 0.22f)
    val zeroColor = scheme.outline.copy(alpha = 0.55f)
    val userColor = scheme.primary
    val trialColor = scheme.tertiary
    val dotRing = scheme.surfaceContainerLow
    val reveal = remember(user.size, trial) { Animatable(0f) }
    LaunchedEffect(reveal) { reveal.animateTo(1f, tween(1_000, easing = FastOutSlowInEasing)) }

    Canvas(
        Modifier
            .fillMaxWidth()
            .height(190.dp)
            .semantics { contentDescription = description },
    ) {
        val leftPad = 40.dp.toPx()
        val bottomPad = 18.dp.toPx()
        val topPad = 8.dp.toPx()
        val rightPad = 6.dp.toPx()
        val w = size.width - leftPad - rightPad
        val h = size.height - bottomPad - topPad
        fun x(week: Double) = leftPad + (week / xMax).toFloat() * w
        fun y(p: Double) = topPad + ((yMax - p) / (yMax - yMin)).toFloat() * h

        yTicks.forEach { v ->
            val yy = y(v)
            val zero = abs0(v)
            drawLine(if (zero) zeroColor else gridColor, Offset(leftPad, yy), Offset(leftPad + w, yy), strokeWidth = if (zero) 1.5.dp.toPx() else 1f)
            val layout = measurer.measure(signedPercent(v, decimals = false), labelStyle)
            drawText(layout, topLeft = Offset(leftPad - layout.size.width - 6.dp.toPx(), yy - layout.size.height / 2))
        }
        xTicks.forEachIndexed { i, week ->
            val layout = measurer.measure(xLabels[i], labelStyle)
            val xx = (x(week.toDouble()) - layout.size.width / 2).coerceIn(leftPad, size.width - layout.size.width)
            drawText(layout, topLeft = Offset(xx, size.height - layout.size.height))
        }

        fun pathOf(points: List<Pair<Double, Double>>) = Path().apply {
            points.forEachIndexed { i, (wk, p) -> if (i == 0) moveTo(x(wk), y(p)) else lineTo(x(wk), y(p)) }
        }

        clipRect(right = leftPad + w * reveal.value + 8.dp.toPx()) {
            // Trial mean: dashed.
            drawPath(
                pathOf(trialLine), trialColor,
                style = Stroke(
                    width = 2.5.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(9.dp.toPx(), 6.dp.toPx())),
                ),
            )
            if (trial.endWeek <= xMax) {
                drawCircle(trialColor, radius = 3.5.dp.toPx(), center = Offset(x(trial.endWeek.toDouble()), y(trial.endPercent)))
            }
            // User: solid, with a soft fill between 0 % and the curve.
            if (user.size >= 2) {
                val fill = pathOf(user).apply {
                    lineTo(x(user.last().first), y(0.0))
                    lineTo(x(user.first().first), y(0.0))
                    close()
                }
                drawPath(fill, Brush.verticalGradient(listOf(userColor.copy(alpha = 0.04f), userColor.copy(alpha = 0.26f)), startY = y(0.0), endY = topPad + h))
                drawPath(pathOf(user), userColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                if (user.size <= 30) {
                    user.drop(1).dropLast(1).forEach { (wk, p) -> drawCircle(userColor, radius = 2.5.dp.toPx(), center = Offset(x(wk), y(p))) }
                }
                val (lw, lp) = user.last()
                drawCircle(dotRing, radius = 6.5.dp.toPx(), center = Offset(x(lw), y(lp)))
                drawCircle(userColor, radius = 4.5.dp.toPx(), center = Offset(x(lw), y(lp)))
            }
        }
    }
}

private fun abs0(v: Double) = kotlin.math.abs(v) < 1e-9

/** Visible weeks: zoomed on the start early on, the whole trial later, and past its end if the user is. */
internal fun chartWeeks(lastUserWeek: Double, trialEndWeek: Int): Double {
    val wanted = ceil((lastUserWeek * 1.5 + 4) / 4) * 4
    return max(ceil(lastUserWeek), min(trialEndWeek.toDouble(), max(12.0, wanted)))
}

private fun weekStep(xMax: Double): Int = listOf(2, 4, 8, 12, 16, 24, 52).first { xMax / it <= 5 || it == 52 }

private fun percentStep(range: Double): Double = when {
    range <= 6 -> 2.0
    range <= 16 -> 5.0
    else -> 10.0
}

/** "−7.1 %" / "+0.8 %" / "0.0 %" with a true minus sign; [decimals] = false gives "−10 %". */
internal fun signedPercent(value: Double, decimals: Boolean = true): String {
    val rounded = if (decimals) (value * 10).roundToInt() / 10.0 else value.roundToInt().toDouble()
    val number = if (decimals) Format.oneDecimal(kotlin.math.abs(rounded)) else kotlin.math.abs(rounded).roundToInt().toString()
    val sign = when {
        rounded < 0 -> "−"
        rounded > 0 -> "+"
        else -> ""
    }
    return "$sign$number %"
}

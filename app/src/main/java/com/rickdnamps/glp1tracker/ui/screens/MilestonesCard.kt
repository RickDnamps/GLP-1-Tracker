package com.rickdnamps.glp1tracker.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rickdnamps.glp1tracker.R
import com.rickdnamps.glp1tracker.data.UserSettings
import com.rickdnamps.glp1tracker.domain.Badge
import com.rickdnamps.glp1tracker.domain.Format
import com.rickdnamps.glp1tracker.domain.Progress
import com.rickdnamps.glp1tracker.ui.weightText
import kotlin.math.roundToInt

/**
 * Motivation card: goal-date forecast plus a scrolling row of badges. Achieved badges come first
 * (most recent first), then locked ones, closest to completion first.
 */
@Composable
fun MilestonesCard(
    progress: List<Progress>,
    forecastMillis: Long?,
    weeklyRateKg: Double?,
    settings: UserSettings,
    modifier: Modifier = Modifier,
) {
    var selectedName by rememberSaveable { mutableStateOf<String?>(null) }
    val ordered = remember(progress) {
        progress.sortedWith(
            compareByDescending<Progress> { it.achieved }
                .thenByDescending { if (it.achieved) it.achievedAt ?: 0L else 0L }
                .thenByDescending { if (it.achieved) 0f else it.fraction }
                .thenBy { it.badge.ordinal },
        )
    }
    val unlocked = progress.count { it.achieved }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.EmojiEvents,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.ms_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                if (progress.isNotEmpty()) {
                    Text(
                        pluralStringResource(R.plurals.ms_unlocked_count, unlocked, unlocked, progress.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            ForecastPanel(progress, forecastMillis, weeklyRateKg, settings, Modifier.padding(horizontal = 16.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                items(ordered, key = { it.badge.name }) { p ->
                    BadgeTile(p) { selectedName = p.badge.name }
                }
            }
        }
    }

    val selected = selectedName?.let { name -> progress.firstOrNull { it.badge.name == name } }
    if (selected != null) {
        BadgeDialog(selected) { selectedName = null }
    }
}

@Composable
private fun ForecastPanel(
    progress: List<Progress>,
    forecastMillis: Long?,
    weeklyRateKg: Double?,
    settings: UserSettings,
    modifier: Modifier = Modifier,
) {
    val goalKg = settings.goalWeightKg.takeIf { it > 0 }
    val goalReached = progress.any { it.badge == Badge.GOAL_REACHED && it.achieved }
    val rate: Double? = weeklyRateKg
    val showNote: Boolean
    val emoji: String
    val message: String
    when {
        goalKg == null -> {
            emoji = "🎯" // direct hit
            message = stringResource(R.string.ms_forecast_no_goal)
            showNote = false
        }
        forecastMillis != null && rate != null -> {
            emoji = "📅" // calendar
            message = stringResource(
                R.string.ms_forecast,
                weightText(rate, settings, signed = true),
                weightText(goalKg, settings),
                Format.date(forecastMillis),
            )
            showNote = true
        }
        goalReached -> {
            emoji = "🏆" // trophy
            message = stringResource(R.string.ms_forecast_goal_reached)
            showNote = false
        }
        rate == null -> {
            emoji = "⚖️" // scales
            message = stringResource(R.string.ms_forecast_need_data)
            showNote = false
        }
        rate > -0.05 -> {
            emoji = "🌄" // sunrise over mountains
            message = stringResource(R.string.ms_forecast_stable, weightText(rate, settings, signed = true))
            showNote = false
        }
        else -> {
            emoji = "📉" // chart decreasing
            message = stringResource(R.string.ms_forecast_trending, weightText(rate, settings, signed = true))
            showNote = true
        }
    }

    val scheme = MaterialTheme.colorScheme
    val brush = remember(scheme.primaryContainer, scheme.tertiaryContainer) {
        Brush.linearGradient(listOf(scheme.primaryContainer, scheme.tertiaryContainer))
    }
    Row(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(brush)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(emoji, fontSize = 28.sp, modifier = Modifier.clearAndSetSemantics { })
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = scheme.onPrimaryContainer,
            )
            if (showNote) {
                Text(
                    stringResource(R.string.ms_forecast_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onPrimaryContainer.copy(alpha = 0.75f),
                )
            }
        }
    }
}

@Composable
private fun BadgeTile(p: Progress, onClick: () -> Unit) {
    val title = stringResource(p.badge.title)
    val percent = "${(p.fraction * 100).roundToInt()} %"
    val description = if (p.achieved) stringResource(R.string.ms_badge_achieved_cd, title)
    else stringResource(R.string.ms_badge_locked_cd, title, percent)
    Column(
        Modifier
            .width(84.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(role = Role.Button, onClick = onClick)
            .clearAndSetSemantics { contentDescription = description }
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        BadgeMedallion(p, 64.dp)
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (p.achieved) FontWeight.SemiBold else FontWeight.Normal,
            color = if (p.achieved) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        val achievedAt = p.achievedAt
        Text(
            if (p.achieved && achievedAt != null) Format.shortDate(achievedAt) else percent,
            style = MaterialTheme.typography.labelSmall,
            color = if (p.achieved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

/** Round badge: vivid gradient when achieved, greyed emoji inside a progress ring when locked. */
@Composable
private fun BadgeMedallion(p: Progress, diameter: Dp) {
    val colors = gradientFor(p.badge)
    val emojiSize = (diameter.value * 0.44f).sp
    Box(Modifier.size(diameter), contentAlignment = Alignment.Center) {
        if (p.achieved) {
            Box(
                Modifier
                    .fillMaxSize()
                    .shadow(6.dp, CircleShape)
                    .clip(CircleShape)
                    .drawWithCache {
                        val fill = Brush.linearGradient(colors, start = Offset.Zero, end = Offset(size.width, size.height))
                        val shine = Brush.radialGradient(
                            listOf(Color.White.copy(alpha = 0.55f), Color.Transparent),
                            center = Offset(size.width * 0.3f, size.height * 0.22f),
                            radius = size.minDimension * 0.6f,
                        )
                        onDrawBehind {
                            drawRect(fill)
                            drawRect(shine)
                        }
                    }
                    .border(2.dp, Color.White.copy(alpha = 0.6f), CircleShape),
            )
            Text(p.badge.emoji, fontSize = emojiSize)
        } else {
            val track = MaterialTheme.colorScheme.surfaceVariant
            val inner = MaterialTheme.colorScheme.surfaceContainerHighest
            val arc = colors.last()
            val fraction = p.fraction
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 4.dp.toPx()
                val radius = this.size.minDimension / 2f
                drawCircle(inner, radius = radius - stroke)
                drawCircle(track, radius = radius - stroke / 2f, style = Stroke(stroke))
                if (fraction > 0f) {
                    drawArc(
                        color = arc,
                        startAngle = -90f,
                        sweepAngle = 360f * fraction,
                        useCenter = false,
                        topLeft = Offset(stroke / 2f, stroke / 2f),
                        size = Size(this.size.width - stroke, this.size.height - stroke),
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                }
            }
            Text(p.badge.emoji, fontSize = emojiSize * 0.85f, modifier = Modifier.grayscale())
        }
    }
}

@Composable
private fun BadgeDialog(p: Progress, onDismiss: () -> Unit) {
    val percent = "${(p.fraction * 100).roundToInt()} %"
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ok)) } },
        icon = { BadgeMedallion(p, 80.dp) },
        title = { Text(stringResource(p.badge.title), textAlign = TextAlign.Center) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(p.badge.description), style = MaterialTheme.typography.bodyMedium)
                val achievedAt = p.achievedAt
                if (p.achieved && achievedAt != null) {
                    Text(
                        stringResource(R.string.ms_achieved_on, Format.date(achievedAt)),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    Text(
                        stringResource(R.string.ms_locked),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    LinearProgressIndicator(progress = { p.fraction }, modifier = Modifier.fillMaxWidth())
                    Text(
                        stringResource(R.string.ms_progress, percent),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
    )
}

// --- Styling helpers ---

private val TreatmentColors = listOf(Color(0xFF4DD0E1), Color(0xFF00897B))
private val TimeColors = listOf(Color(0xFFAED581), Color(0xFF2E7D32))
private val WeightColors = listOf(Color(0xFFFFB74D), Color(0xFFF4511E))
private val GoalColors = listOf(Color(0xFFFFE082), Color(0xFFFFB300), Color(0xFFFF6F00))
private val JournalColors = listOf(Color(0xFFCE93D8), Color(0xFF7B1FA2))
private val WaterColors = listOf(Color(0xFF81D4FA), Color(0xFF1976D2))
private val RotationColors = listOf(Color(0xFFF48FB1), Color(0xFFC2185B))

private fun gradientFor(badge: Badge): List<Color> = when (badge) {
    Badge.FIRST_DOSE, Badge.DOSES_5, Badge.DOSES_10, Badge.DOSES_25 -> TreatmentColors
    Badge.WEEKS_4, Badge.WEEKS_12, Badge.WEEKS_26, Badge.WEEKS_52 -> TimeColors
    Badge.FIRST_WEIGH_IN, Badge.LOST_2, Badge.LOST_5, Badge.LOST_10, Badge.LOST_15, Badge.LOST_20 -> WeightColors
    Badge.GOAL_REACHED -> GoalColors
    Badge.JOURNAL_STREAK_7 -> JournalColors
    Badge.WATER_STREAK_30 -> WaterColors
    Badge.SITE_ROTATION -> RotationColors
}

/** Desaturated, half-transparent layer: works for colour emoji on every API level (no RenderEffect). */
private val GrayscalePaint by lazy {
    Paint().apply {
        colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
        alpha = 0.5f
    }
}

private fun Modifier.grayscale(): Modifier = drawWithContent {
    drawIntoCanvas { canvas ->
        canvas.saveLayer(Rect(Offset.Zero, size), GrayscalePaint)
        drawContent()
        canvas.restore()
    }
}

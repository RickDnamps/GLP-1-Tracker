package com.rickdnamps.glp1tracker.ui.screens

import android.content.Intent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rickdnamps.glp1tracker.R
import com.rickdnamps.glp1tracker.domain.Format
import com.rickdnamps.glp1tracker.domain.Highlight
import com.rickdnamps.glp1tracker.domain.HighlightKind
import com.rickdnamps.glp1tracker.domain.RecapDay
import com.rickdnamps.glp1tracker.domain.RecapInput
import com.rickdnamps.glp1tracker.domain.SideEffectType
import com.rickdnamps.glp1tracker.domain.StreakKind
import com.rickdnamps.glp1tracker.domain.WeeklyRecap
import com.rickdnamps.glp1tracker.domain.WeeklyRecaps
import com.rickdnamps.glp1tracker.data.UserSettings
import com.rickdnamps.glp1tracker.ui.UiState
import com.rickdnamps.glp1tracker.ui.components.GlassTile
import com.rickdnamps.glp1tracker.ui.components.HeroCard
import com.rickdnamps.glp1tracker.ui.components.Pill
import com.rickdnamps.glp1tracker.ui.milestoneInput
import com.rickdnamps.glp1tracker.ui.weightText
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.util.Locale

/** At most this many highlight rows on the card (the share text uses the same rows). */
private const val MAX_ROWS = 5

/** Fixed vivid gradient (deep purple → magenta) so the recap stands out from the dose hero; white text stays readable. */
private val RecapBrush = Brush.linearGradient(listOf(Color(0xFF4527A0), Color(0xFF7B1FA2), Color(0xFFC2185B)))
private val RecapInk = Color(0xFF5E1F9A)

/**
 * "Your week" (P08): the last 7 days against the 7 before, as 3 to 5 ranked highlight rows, a day
 * strip and a Share button (plain text via the system share sheet). On Mondays it shows the week that
 * just ended; see [WeeklyRecaps.defaultEnd].
 */
@Composable
fun WeeklyRecapCard(state: UiState, modifier: Modifier = Modifier, today: LocalDate = LocalDate.now()) {
    val settings = state.settings
    val end = WeeklyRecaps.defaultEnd(today)
    val recap = remember(state, end) { WeeklyRecaps.compute(recapInput(state, end)) }
    val lines = recap.highlights.take(MAX_ROWS).map { recapLine(it, settings) }
    val range = stringResource(R.string.recap_range, Format.shortDate(millisOf(recap.start)), Format.shortDate(millisOf(recap.end)))

    val context = LocalContext.current
    val chooserTitle = stringResource(R.string.recap_share_chooser)
    val shareText = buildString {
        append(stringResource(R.string.recap_share_header, range))
        lines.forEach { append('\n').append(it.emoji).append(' ').append(it.text) }
        append("\n\n").append(stringResource(R.string.recap_share_footer, stringResource(R.string.app_name)))
    }

    HeroCard(modifier = modifier, brush = RecapBrush) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.recap_title), style = MaterialTheme.typography.headlineSmall, color = Color.White)
                Spacer(Modifier.height(4.dp))
                Pill(range)
            }
            Text("✨", fontSize = 34.sp, modifier = Modifier.clearAndSetSemantics { }) // sparkles
        }
        Spacer(Modifier.height(14.dp))
        DayStrip(recap.days, today, settings.medication.isOral)
        Spacer(Modifier.height(14.dp))
        if (lines.isEmpty()) {
            Text(stringResource(R.string.recap_empty), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f))
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                lines.forEachIndexed { i, line -> HighlightRow(line, i) }
            }
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    runCatching { context.startActivity(Intent.createChooser(send, chooserTitle)) }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = RecapInk),
            ) {
                Icon(Icons.Outlined.Share, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.recap_share), style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

/** One highlight as displayed (and shared): decorative emoji, main text, optional detail line. */
private data class RecapLine(val emoji: String, val text: String, val detail: String?)

@Composable
private fun HighlightRow(line: RecapLine, index: Int) {
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(90L * index)
        appear.animateTo(1f, tween(450, easing = FastOutSlowInEasing))
    }
    GlassTile(
        Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = appear.value
                translationY = (1f - appear.value) * 16.dp.toPx()
            },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(if (index == 0) 44.dp else 36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(line.emoji, fontSize = if (index == 0) 24.sp else 19.sp, modifier = Modifier.clearAndSetSemantics { })
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    line.text,
                    style = if (index == 0) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
                line.detail?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.82f))
                }
            }
        }
    }
}

/** Seven bubbles, oldest first: filled when the journal was used, a syringe (or pill) on dose days. */
@Composable
private fun DayStrip(days: List<RecapDay>, today: LocalDate, oral: Boolean) {
    val logged = days.count { it.journal || it.dose }
    val label = stringResource(R.string.recap_days_cd, logged)
    Row(
        Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = label },
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        days.forEach { d ->
            val filled = d.journal || d.dose
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    d.date.dayOfWeek.getDisplayName(java.time.format.TextStyle.NARROW, Locale.getDefault()).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = if (d.date == today) 1f else 0.75f),
                    fontWeight = if (d.date == today) FontWeight.Bold else FontWeight.Normal,
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = if (filled) 0.92f else 0.14f))
                        .border(
                            width = if (d.date == today) 2.dp else 1.dp,
                            color = Color.White.copy(alpha = if (d.date == today) 0.95f else 0.3f),
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        d.dose -> Text(if (oral) "💊" else "💉", fontSize = 15.sp)
                        d.waterGoal -> Text("💧", fontSize = 14.sp)
                        filled -> Text("✓", fontSize = 15.sp, color = RecapInk, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun recapLine(h: Highlight, settings: UserSettings): RecapLine {
    val previousWeight = h.previous?.let { stringResource(R.string.recap_last_week, weightText(it, settings, signed = true)) }
    return when (h.kind) {
        HighlightKind.WEIGHT_LOST -> RecapLine(
            "📉", // chart decreasing
            stringResource(R.string.recap_weight_lost, weightText(h.value, settings, signed = true)),
            previousWeight,
        )
        HighlightKind.WEIGHT_STABLE -> RecapLine(
            "⚖️", // balance scale
            stringResource(R.string.recap_weight_stable, weightText(h.value, settings, signed = true)),
            previousWeight,
        )
        HighlightKind.WEIGHT_GAINED -> RecapLine(
            "🌊", // water wave: fluctuations
            stringResource(R.string.recap_weight_gained, weightText(h.value, settings, signed = true)),
            previousWeight,
        )
        HighlightKind.ALL_DOSES -> RecapLine(
            "✅", // check mark
            pluralStringResource(R.plurals.recap_doses_all, h.count, h.count),
            null,
        )
        HighlightKind.MISSED_DOSE -> RecapLine(
            "⏰", // alarm clock
            stringResource(R.string.recap_doses_missed, h.count, h.total),
            stringResource(R.string.recap_doses_missed_note),
        )
        HighlightKind.NO_SIDE_EFFECTS -> RecapLine(
            "🙌", // raised hands
            stringResource(R.string.recap_no_side_effects, (h.previous ?: 0.0).toInt()),
            null,
        )
        HighlightKind.FEWER_SIDE_EFFECTS -> RecapLine(
            "😌", // relieved face
            pluralStringResource(R.plurals.recap_fewer_side_effects, h.count, h.count, (h.previous ?: 0.0).toInt()),
            topSideEffect(h.sideEffect, h.total),
        )
        HighlightKind.MORE_SIDE_EFFECTS -> RecapLine(
            "🌤️", // sun behind small cloud
            pluralStringResource(R.plurals.recap_more_side_effects, h.count, h.count, (h.previous ?: 0.0).toInt()),
            listOfNotNull(topSideEffect(h.sideEffect, h.total), stringResource(R.string.recap_side_effects_care)).joinToString("\n"),
        )
        HighlightKind.SIDE_EFFECTS -> RecapLine(
            "📝", // memo
            pluralStringResource(R.plurals.recap_same_side_effects, h.count, h.count),
            topSideEffect(h.sideEffect, h.total),
        )
        HighlightKind.WATER_GOAL -> RecapLine(
            "💧", // droplet
            pluralStringResource(R.plurals.recap_water_goal, h.count, h.count, ml(h.value)),
            h.previous?.let { stringResource(R.string.recap_last_week, ml(it)) },
        )
        HighlightKind.WATER_BELOW_GOAL -> RecapLine(
            "🚰", // potable water
            stringResource(R.string.recap_water_below, ml(h.value), ml(h.total.toDouble())),
            h.previous?.let { stringResource(R.string.recap_last_week, ml(it)) },
        )
        HighlightKind.PROTEIN_GOAL -> RecapLine(
            "💪", // flexed biceps
            pluralStringResource(R.plurals.recap_protein_goal, h.count, h.count, grams(h.value)),
            h.previous?.let { stringResource(R.string.recap_last_week, grams(it)) },
        )
        HighlightKind.PROTEIN_BELOW_GOAL -> RecapLine(
            "🍳", // cooking
            stringResource(R.string.recap_protein_below, grams(h.value), grams(h.total.toDouble())),
            h.previous?.let { stringResource(R.string.recap_last_week, grams(it)) },
        )
        HighlightKind.STREAK -> RecapLine(
            "🔥", // fire
            pluralStringResource(
                when (h.streak) {
                    StreakKind.WATER_GOAL -> R.plurals.recap_streak_water
                    StreakKind.PROTEIN_GOAL -> R.plurals.recap_streak_protein
                    StreakKind.JOURNAL, null -> R.plurals.recap_streak_journal
                },
                h.count,
                h.count,
            ),
            null,
        )
        HighlightKind.JOURNAL -> RecapLine(
            "📔", // notebook
            pluralStringResource(R.plurals.recap_journal, h.count, h.count),
            h.previous?.toInt()?.let { pluralStringResource(R.plurals.recap_last_week_days, it, it) },
        )
    }
}

@Composable
private fun topSideEffect(type: SideEffectType?, count: Int): String? =
    type?.let { pluralStringResource(R.plurals.recap_top_side_effect, count, stringResource(it.nameRes), count) }

private fun ml(value: Double) = "${value.toInt()} ml"

private fun grams(value: Double) = "${value.toInt()} g"

private fun millisOf(date: LocalDate): Long = Format.toMillis(date.atTime(12, 0))

/** Builds the pure recap input from the Room-backed UI state (protein = journal + food entries). */
internal fun recapInput(state: UiState, end: LocalDate): RecapInput {
    val s = state.settings
    fun day(millis: Long) = Format.toLocalDateTime(millis).toLocalDate()
    val logs = state.dailyLogs.mapNotNull { log -> runCatching { LocalDate.parse(log.date) }.getOrNull()?.let { it to log } }
    val foodProtein = state.foods.groupBy { day(it.timestamp) }.mapValues { (_, l) -> l.sumOf { it.proteinG } }
    val protein = buildMap<LocalDate, Int> {
        logs.forEach { (date, l) -> if (l.proteinG > 0) put(date, l.proteinG) }
        foodProtein.forEach { (date, g) -> put(date, (get(date) ?: 0) + g.toInt()) }
    }
    return RecapInput(
        doseTimes = state.injections.map { it.timestamp },
        intervalDays = s.intervalDays,
        weights = state.weights.map { it.timestamp to it.weightKg },
        waterByDay = logs.associate { (date, l) -> date to l.waterMl },
        proteinByDay = protein,
        waterGoalMl = s.waterGoalMl,
        proteinGoalG = s.proteinGoalG,
        sideEffects = state.sideEffects.map { it.timestamp to SideEffectType.fromName(it.type) },
        journalDays = milestoneInput(state).journalDays,
        end = end,
    )
}

/** The recap for [today] (same week as the card), e.g. for a notification or a share shortcut. */
fun weeklyRecap(state: UiState, today: LocalDate = LocalDate.now()): WeeklyRecap =
    WeeklyRecaps.compute(recapInput(state, WeeklyRecaps.defaultEnd(today)))

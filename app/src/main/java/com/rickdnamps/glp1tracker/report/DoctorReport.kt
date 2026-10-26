package com.rickdnamps.glp1tracker.report

import com.rickdnamps.glp1tracker.data.composition
import com.rickdnamps.glp1tracker.domain.BodyCompositionMath
import com.rickdnamps.glp1tracker.domain.CompMetric
import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.text.Layout
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import com.rickdnamps.glp1tracker.R
import com.rickdnamps.glp1tracker.data.Backup
import com.rickdnamps.glp1tracker.data.DailyLogEntity
import com.rickdnamps.glp1tracker.domain.InjectionSite
import com.rickdnamps.glp1tracker.domain.Medication
import com.rickdnamps.glp1tracker.domain.Severity
import com.rickdnamps.glp1tracker.domain.SideEffectType
import com.rickdnamps.glp1tracker.report.ReportLayout.Column
import com.rickdnamps.glp1tracker.report.ReportLayout.Stat
import com.rickdnamps.glp1tracker.ui.weightStats
import java.io.OutputStream
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * "Report for my doctor": a multi-page A4 PDF summarising the treatment over a period
 * (treatment, weight, doses, side effects, daily habits, questions, disclaimer).
 * Android framework only (PdfDocument + Canvas + StaticLayout). Numbers are estimates, never advice.
 */
object DoctorReport {

    /**
     * Writes the report covering [fromMillis]..[toMillis] (epoch millis, both included) to [out],
     * which is flushed by the PDF writer but left open. Blocking: call it on Dispatchers.IO.
     *
     * Pass `fromMillis = 0` for "since the beginning": the period then starts at the first record.
     * [locale] forces the report language (e.g. a French report from an English phone); null keeps
     * the app's current language. [questions] are printed above the blank lines of the
     * "Questions for my doctor" section.
     */
    fun write(
        context: Context,
        out: OutputStream,
        snapshot: Backup.Snapshot,
        fromMillis: Long,
        toMillis: Long,
        locale: Locale? = null,
        questions: List<String> = emptyList(),
    ) {
        val reportContext = if (locale != null) localizedContext(context, locale) else context
        val reportLocale = locale ?: context.resources.configuration.locales[0] ?: Locale.getDefault()
        ReportBuilder(
            context = reportContext,
            format = ReportFormatter(reportLocale),
            snapshot = snapshot,
            fromMillis = minOf(fromMillis, toMillis),
            toMillis = maxOf(fromMillis, toMillis),
            questions = questions,
        ).build().writeTo(out)
    }

    private fun localizedContext(context: Context, locale: Locale): Context {
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}

private const val CHART_HEIGHT = 190f
private const val QUESTION_LINE_SPACING = 24f
private const val MIN_QUESTION_LINES = 6
private const val MAX_QUESTION_LINES = 40
private const val DASH = "—"

private val APPETITE_LABELS = listOf(
    R.string.appetite_1, R.string.appetite_2, R.string.appetite_3, R.string.appetite_4, R.string.appetite_5,
)

/** Lays out the sections of one report, top to bottom. */
private class ReportBuilder(
    private val context: Context,
    private val format: ReportFormatter,
    snapshot: Backup.Snapshot,
    private val fromMillis: Long,
    private val toMillis: Long,
    private val questions: List<String>,
) {
    private val settings = snapshot.settings
    private val unit = settings.weightUnit
    private val unitSymbol = string(unit.symbolRes)
    private val appName = string(R.string.app_name)
    private val generatedOn = format.date(System.currentTimeMillis())

    private val allDoseTimes = snapshot.injections.map { it.timestamp }
    private val injections = snapshot.injections.filter { it.timestamp in fromMillis..toMillis }.sortedBy { it.timestamp }
    private val weights = snapshot.weights.filter { it.timestamp in fromMillis..toMillis }.sortedBy { it.timestamp }
    private val sideEffects = snapshot.sideEffects.filter { it.timestamp in fromMillis..toMillis }.sortedBy { it.timestamp }
    private val dailyLogs: List<DailyLogEntity> = run {
        val first = format.localDate(fromMillis)
        val last = format.localDate(toMillis)
        snapshot.dailyLogs.filter { log ->
            val date = runCatching { LocalDate.parse(log.date) }.getOrNull()
            date != null && !date.isBefore(first) && !date.isAfter(last)
        }
    }

    /** Start shown in the header: the first record when the period starts before it ("since the beginning"). */
    private val periodStart: Long = run {
        val firstRecord = listOfNotNull(
            snapshot.injections.minOfOrNull { it.timestamp },
            snapshot.weights.minOfOrNull { it.timestamp },
            snapshot.sideEffects.minOfOrNull { it.timestamp },
        ).minOrNull()
        when {
            firstRecord != null && firstRecord > fromMillis -> minOf(firstRecord, toMillis)
            firstRecord == null && fromMillis <= 0 -> toMillis
            else -> fromMillis
        }
    }

    private val layout = ReportLayout(::decoratePage)

    fun build(): ReportLayout {
        headerSection()
        treatmentSection()
        weightSection()
        dosesSection()
        sideEffectsSection()
        habitsSection()
        questionsSection()
        return layout
    }

    // --- Sections ---

    private fun headerSection() {
        layout.paragraph(appName.uppercase(), ReportStyle.brand, after = 3f)
        layout.paragraph(string(R.string.report_title), ReportStyle.title, after = 1f)
        layout.paragraph(string(R.string.report_subtitle), ReportStyle.subtitle, after = 12f)
        val days = ChronoUnit.DAYS.between(format.localDate(periodStart), format.localDate(toMillis)).toInt() + 1
        val items = buildList {
            if (settings.name.isNotBlank()) {
                val height = settings.heightCm.takeIf { it > 0 }?.let { string(R.string.report_height_cm, format.integer(it)) }
                add(Stat(string(R.string.report_patient), settings.name.trim(), height))
            }
            add(
                Stat(
                    string(R.string.report_period),
                    string(R.string.report_period_range, format.date(periodStart), format.date(toMillis)),
                    quantity(R.plurals.report_days, days),
                )
            )
            add(Stat(string(R.string.report_generated_on), generatedOn))
        }
        layout.infoBand(items)
    }

    private fun treatmentSection() {
        layout.sectionTitle(string(R.string.report_section_treatment), keepWithNext = 120f)
        layout.keyValues(
            listOf(
                string(R.string.report_current_medication) to medicationName(settings.medication.name),
                string(R.string.current_dose) to mg(settings.doseMg),
                string(R.string.frequency) to intervalLabel(settings.intervalDays),
            )
        )
        layout.gap(6f)

        val adherence = ReportMath.adherence(allDoseTimes, fromMillis, toMillis, settings.intervalDays)
        val first = injections.firstOrNull()
        val last = injections.lastOrNull()
        layout.statCards(
            listOf(
                Stat(string(R.string.report_doses_in_period), injections.size.toString()),
                Stat(
                    string(R.string.report_adherence),
                    adherence?.let { percent(it.percent.toString()) } ?: DASH,
                    when {
                        adherence != null -> string(R.string.report_adherence_detail, adherence.taken, adherence.expected)
                        allDoseTimes.isNotEmpty() -> string(R.string.report_adherence_na)
                        else -> null
                    },
                ),
                Stat(string(R.string.report_first_dose), first?.let { format.date(it.timestamp) } ?: DASH, first?.let { mg(it.doseMg) }),
                Stat(string(R.string.report_last_dose), last?.let { format.date(it.timestamp) } ?: DASH, last?.let { mg(it.doseMg) }),
            )
        )
        if (injections.isEmpty()) {
            layout.paragraph(string(R.string.report_no_dose), ReportStyle.muted)
            return
        }

        val steps = ReportMath.doseSteps(injections.map { ReportMath.DoseRecord(it.timestamp, it.medication, it.doseMg) })
        if (steps.size == 1) {
            layout.paragraph(string(R.string.report_same_dose, mg(steps[0].doseMg)), ReportStyle.body)
            return
        }
        layout.subTitle(string(R.string.report_dose_changes))
        if (ReportMath.isTitration(steps)) {
            layout.paragraph(string(R.string.report_titration, steps.joinToString(" → ") { mg(it.doseMg) }), ReportStyle.bodyBold)
        }
        layout.table(
            listOf(
                Column(string(R.string.report_since), 1.3f),
                Column(string(R.string.medication), 2.6f),
                Column(string(R.string.report_col_dose), 1f, Layout.Alignment.ALIGN_OPPOSITE),
                Column(string(R.string.report_col_doses), 0.8f, Layout.Alignment.ALIGN_OPPOSITE),
            ),
            steps.map { listOf(format.date(it.startMillis), medicationName(it.medication), format.dose(it.doseMg), it.count.toString()) },
        )
    }

    private fun weightSection() {
        layout.sectionTitle(string(R.string.weight), keepWithNext = 90f)
        if (weights.isEmpty()) {
            layout.paragraph(string(R.string.report_no_weight), ReportStyle.muted)
            return
        }
        // Start of the period = first entry of the period, not the treatment's starting weight.
        val stats = weightStats(settings.copy(startWeightKg = 0.0), weights)
        val first = weights.first()
        val last = weights.last()
        val goalKg = stats.goalKg

        val cards = mutableListOf(
            Stat(string(R.string.report_start), weightText(first.weightKg), format.date(first.timestamp)),
            Stat(string(R.string.current), weightText(last.weightKg), format.date(last.timestamp)),
            Stat(
                string(R.string.report_change),
                signedWeight(last.weightKg - first.weightKg),
                ReportMath.changePercent(first.weightKg, last.weightKg)?.let { percent(signed(it, format::oneDecimal)) },
            ),
        )
        stats.weeklyAverageKg?.let { lossPerWeek -> cards += Stat(string(R.string.report_per_week), signedWeight(-lossPerWeek)) }
        stats.bmi?.let { bmi ->
            cards += Stat(string(R.string.bmi), format.oneDecimal(bmi), string(R.string.report_height_cm, format.integer(settings.heightCm)))
        }
        if (goalKg != null) {
            val remaining = stats.remainingKg ?: 0.0
            val caption = if (remaining >= 0.05) string(R.string.report_to_goal, weightText(remaining)) else string(R.string.report_goal_reached)
            cards += Stat(string(R.string.goal), weightText(goalKg), caption)
        }
        layout.statCards(cards, perRow = if (cards.size > 4) 3 else 4)

        layout.subTitle(string(R.string.report_weight_chart), keepWithNext = CHART_HEIGHT)
        if (weights.size < 2) {
            layout.paragraph(string(R.string.report_chart_need_two), ReportStyle.muted)
        } else {
            val longSpan = last.timestamp - first.timestamp > 300 * ReportMath.DAY_MS
            val data = WeightChart.Data(
                points = weights.map { it.timestamp to unit.fromKg(it.weightKg) },
                goal = goalKg?.let { unit.fromKg(it) },
                goalLabel = goalKg?.let { string(R.string.report_goal_line, weightText(it)) },
                yLabel = { format.compact(it) },
                xLabel = { format.axisDate(it, longSpan) },
            )
            val chartLeft = layout.left
            val chartWidth = layout.width
            layout.block(CHART_HEIGHT) { canvas, top -> WeightChart.draw(canvas, chartLeft, top, chartWidth, CHART_HEIGHT, data) }
        }

        compositionBlock()

        layout.subTitle(string(R.string.report_weight_entries))
        layout.compactGrid(
            string(R.string.report_col_date) to string(R.string.report_weight_col, unitSymbol),
            weights.map { format.date(it.timestamp) to format.oneDecimal(unit.fromKg(it.weightKg)) },
        )
    }

    /** Body composition (fat, muscle, water, visceral fat…) when the scale or Health Connect provided it. */
    private fun compositionBlock() {
        val withComp = weights.filter { !it.composition.isEmpty }
        if (withComp.isEmpty()) return
        layout.subTitle(string(R.string.comp_title), keepWithNext = 70f)
        val first = withComp.first()
        val last = withComp.last()
        val preferred = listOf(
            CompMetric.FAT, CompMetric.FAT_MASS, CompMetric.MUSCLE, CompMetric.WATER,
            CompMetric.VISCERAL, CompMetric.BMR, CompMetric.LEAN, CompMetric.METABOLIC_AGE,
        )
        val cards = preferred.mapNotNull { m ->
            val v = m.read(last.composition, last.weightKg)?.let { if (m.isMass) unit.fromKg(it) else it } ?: return@mapNotNull null
            val start = m.read(first.composition, first.weightKg)?.let { if (m.isMass) unit.fromKg(it) else it }
            val unitText = if (m.isMass) " $unitSymbol" else if (m.unit.isEmpty()) "" else " " + m.unit
            val value = format.compact(v) + unitText
            val caption = if (start != null && withComp.size > 1) signed(v - start, format::compact) + unitText else null
            Stat(string(m.nameRes), value, caption)
        }.take(6)
        layout.statCards(cards, perRow = 3)
        val f0 = withComp.firstOrNull { it.bodyFatPct != null }
        val f1 = withComp.lastOrNull { it.bodyFatPct != null }
        if (f0 != null && f1 != null && f0 !== f1) {
            BodyCompositionMath.lossSplit(f0.weightKg, f0.bodyFatPct!!, f1.weightKg, f1.bodyFatPct!!)?.let { split ->
                layout.paragraph(string(R.string.comp_split, weightText(split.totalKg), (split.fatShare * 100).toInt()), ReportStyle.body)
            }
        }
    }

    private fun dosesSection() {
        layout.sectionTitle(string(R.string.report_section_doses), keepWithNext = 50f)
        if (injections.isEmpty()) {
            layout.paragraph(string(R.string.report_no_dose), ReportStyle.muted)
            return
        }
        layout.table(
            listOf(
                Column(string(R.string.report_col_date), 1.25f),
                Column(string(R.string.medication), 2.1f),
                Column(string(R.string.report_col_dose), 0.85f, Layout.Alignment.ALIGN_OPPOSITE),
                Column(string(R.string.report_col_site), 1.3f),
                Column(string(R.string.notes), 2.6f),
            ),
            injections.map {
                listOf(
                    format.date(it.timestamp),
                    medicationName(it.medication),
                    format.dose(it.doseMg),
                    string(InjectionSite.fromName(it.site).nameRes),
                    it.notes.trim(),
                )
            },
        )
    }

    private fun sideEffectsSection() {
        layout.sectionTitle(string(R.string.report_section_side_effects), keepWithNext = 60f)
        if (sideEffects.isEmpty()) {
            layout.paragraph(string(R.string.report_no_side_effect), ReportStyle.muted)
            return
        }
        layout.subTitle(string(R.string.report_by_symptom))
        layout.table(
            listOf(
                Column(string(R.string.symptom), 2.6f),
                Column(string(R.string.report_occurrences), 1f, Layout.Alignment.ALIGN_OPPOSITE),
                Column(string(R.string.report_avg_severity), 1.2f, Layout.Alignment.ALIGN_OPPOSITE),
                Column(string(R.string.report_max_severity), 1.3f),
            ),
            ReportMath.symptomStats(sideEffects.map { it.type to it.severity }).map {
                listOf(
                    symptomName(it.type),
                    it.count.toString(),
                    string(R.string.report_severity_value, format.oneDecimal(it.averageSeverity)),
                    string(Severity.fromLevel(it.maxSeverity).nameRes),
                )
            },
        )
        layout.paragraph(string(R.string.report_severity_help), ReportStyle.muted)

        layout.subTitle(string(R.string.report_all_entries))
        layout.table(
            listOf(
                Column(string(R.string.report_col_date), 1.25f),
                Column(string(R.string.symptom), 2f),
                Column(string(R.string.severity), 1.2f),
                Column(string(R.string.notes), 3.5f),
            ),
            sideEffects.map {
                listOf(format.date(it.timestamp), symptomName(it.type), string(Severity.fromLevel(it.severity).nameRes), it.notes.trim())
            },
        )
    }

    private fun habitsSection() {
        layout.sectionTitle(string(R.string.report_section_habits), keepWithNext = 70f)
        if (dailyLogs.isEmpty()) {
            layout.paragraph(string(R.string.report_no_habits), ReportStyle.muted)
            return
        }
        val water = dailyLogs.map { it.waterMl }
        val protein = dailyLogs.map { it.proteinG }
        val appetite = dailyLogs.map { it.appetite }
        val mood = dailyLogs.map { it.mood }
        val appetiteAverage = ReportMath.averageOfSet(appetite)
        val moodAverage = ReportMath.averageOfSet(mood)

        fun caption(values: List<Int>, vararg extra: String?): String =
            (listOf(quantity(R.plurals.report_days, ReportMath.countSet(values))) + extra.filterNotNull()).joinToString(" · ")

        layout.statCards(
            listOf(
                Stat(
                    string(R.string.water),
                    ReportMath.averageOfSet(water)?.let { string(R.string.report_ml_per_day, format.integer(it)) } ?: DASH,
                    caption(water, settings.waterGoalMl.takeIf { it > 0 }?.let { string(R.string.report_goal_value, "$it ml") }),
                ),
                Stat(
                    string(R.string.protein),
                    ReportMath.averageOfSet(protein)?.let { string(R.string.report_g_per_day, format.integer(it)) } ?: DASH,
                    caption(protein, settings.proteinGoalG.takeIf { it > 0 }?.let { string(R.string.report_goal_value, "$it g") }),
                ),
                Stat(
                    string(R.string.appetite),
                    appetiteAverage?.let { string(R.string.report_scale_5, format.oneDecimal(it)) } ?: DASH,
                    caption(appetite, appetiteAverage?.let { string(APPETITE_LABELS[(it.roundToInt() - 1).coerceIn(0, 4)]) }),
                ),
                Stat(
                    string(R.string.mood),
                    moodAverage?.let { string(R.string.report_scale_5, format.oneDecimal(it)) } ?: DASH,
                    caption(mood),
                ),
            )
        )
        layout.paragraph(string(R.string.report_habits_help), ReportStyle.muted)
    }

    /** Questions with blank lines, then the disclaimer at the bottom of the last page. */
    private fun questionsSection() {
        val noteTitle = string(R.string.report_disclaimer_title)
        val noteText = string(R.string.report_data_source, appName) + " " + string(R.string.disclaimer_long)
        val noteHeight = layout.noteHeight(noteTitle, noteText) + 12f

        layout.sectionTitle(
            string(R.string.report_section_questions),
            keepWithNext = MIN_QUESTION_LINES * QUESTION_LINE_SPACING + noteHeight + 16f,
        )
        layout.paragraph(string(R.string.report_questions_help), ReportStyle.muted)
        questions.filter { it.isNotBlank() }.forEach { layout.paragraph("•  " + it.trim(), ReportStyle.body, after = 3f) }
        // Fill the space left above the disclaimer.
        val lines = ((layout.remaining - noteHeight) / QUESTION_LINE_SPACING).toInt()
            .coerceIn(MIN_QUESTION_LINES, MAX_QUESTION_LINES)
        layout.ruledLines(lines, QUESTION_LINE_SPACING)
        layout.bottomNote(noteTitle, noteText)
    }

    // --- Page decoration (runs when the PDF is written, once the page count is known) ---

    private fun decoratePage(canvas: Canvas, page: Int, pageCount: Int) {
        val left = layout.left
        val right = layout.right
        val paint = ReportStyle.small
        canvas.drawRect(0f, 0f, ReportLayout.PAGE_WIDTH.toFloat(), 6f, ReportStyle.accentFill)
        if (page > 1) {
            canvas.drawText("$appName · ${string(R.string.report_title)}", left, 26f, paint)
            val name = settings.name.trim()
            if (name.isNotEmpty()) canvas.drawText(name, right - paint.measureText(name), 26f, paint)
            canvas.drawLine(left, 32f, right, 32f, ReportStyle.divider)
        }
        val footerLine = ReportLayout.CONTENT_BOTTOM + 20f
        canvas.drawLine(left, footerLine, right, footerLine, ReportStyle.divider)
        val baseline = footerLine + 12f
        canvas.drawText(string(R.string.report_footer, appName, generatedOn), left, baseline, paint)
        val pageText = string(R.string.report_page_of, page, pageCount)
        canvas.drawText(pageText, right - paint.measureText(pageText), baseline, paint)
    }

    // --- Text helpers ---

    private fun string(@StringRes id: Int, vararg args: Any): String =
        if (args.isEmpty()) context.getString(id) else context.getString(id, *args)

    private fun quantity(@PluralsRes id: Int, count: Int): String =
        context.resources.getQuantityString(id, count, count)

    private fun medicationName(name: String): String {
        val medication = Medication.fromName(name)
        return if (medication == Medication.OTHER && settings.customMedName.isNotBlank()) settings.customMedName
        else string(medication.nameRes)
    }

    private fun symptomName(type: String): String = string(SideEffectType.fromName(type).nameRes)

    private fun intervalLabel(days: Int): String = when (days) {
        1 -> string(R.string.every_day)
        7 -> string(R.string.every_week)
        14 -> string(R.string.every_two_weeks)
        else -> string(R.string.every_n_days, days)
    }

    private fun mg(dose: Double): String = string(R.string.report_mg, format.dose(dose))

    private fun percent(value: String): String = string(R.string.report_percent, value)

    private fun weightText(kg: Double): String = format.oneDecimal(unit.fromKg(kg)) + " " + unitSymbol

    private fun signedWeight(kg: Double): String = signed(unit.fromKg(kg), format::oneDecimal) + " " + unitSymbol

    /** "+1.2" or "−1.2"; no sign when the value rounds to zero. */
    private fun signed(value: Double, text: (Double) -> String): String {
        val magnitude = text(abs(value))
        return when {
            magnitude == text(0.0) -> magnitude
            value > 0 -> "+$magnitude"
            else -> "−$magnitude"
        }
    }
}

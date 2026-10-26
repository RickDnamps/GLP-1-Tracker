package com.rickdnamps.glp1tracker.report

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** Dates and numbers in the report's language, which may differ from the phone's. */
internal class ReportFormatter(locale: Locale, private val zone: ZoneId = ZoneId.systemDefault()) {
    private val symbols = DecimalFormatSymbols.getInstance(locale)
    private val oneDecimalFormat = DecimalFormat("0.0", symbols)
    private val compactFormat = DecimalFormat("0.#", symbols)
    private val doseFormat = DecimalFormat("0.##", symbols)
    private val integerFormat = DecimalFormat("0", symbols)
    private val mediumDate = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
    private val dayMonth = DateTimeFormatter.ofPattern("d MMM", locale)
    private val monthYear = DateTimeFormatter.ofPattern("MMM yyyy", locale)

    fun localDate(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    fun date(millis: Long): String = localDate(millis).format(mediumDate)

    /** Short axis label: "5 Oct", or "Oct 2026" for long spans. */
    fun axisDate(millis: Long, longSpan: Boolean): String =
        localDate(millis).format(if (longSpan) monthYear else dayMonth)

    fun oneDecimal(value: Double): String = oneDecimalFormat.format(value)

    /** At most one decimal, none when it is zero (axis labels: "70", "72.5"). */
    fun compact(value: Double): String = compactFormat.format(value)

    fun dose(mg: Double): String = doseFormat.format(mg)

    fun integer(value: Double): String = integerFormat.format(value)
}

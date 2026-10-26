package com.rickdnamps.glp1tracker.domain

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

object Format {
    private fun decimal(pattern: String) = DecimalFormat(pattern, DecimalFormatSymbols.getInstance(Locale.getDefault()))

    fun number(value: Double, maxDecimals: Int = 2): String =
        decimal("0." + "#".repeat(maxDecimals.coerceAtLeast(1))).format(value)

    fun oneDecimal(value: Double): String = decimal("0.0").format(value)

    fun dose(mg: Double): String = number(mg, 2)

    /** Parses user input accepting both "," and "." as decimal separator. */
    fun parse(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()

    fun toLocalDateTime(millis: Long): LocalDateTime =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())

    fun toMillis(dateTime: LocalDateTime): Long =
        dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    fun date(millis: Long): String = date(toLocalDateTime(millis).toLocalDate())

    fun date(date: LocalDate): String =
        date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))

    fun shortDate(millis: Long): String =
        toLocalDateTime(millis).format(DateTimeFormatter.ofPattern("d MMM"))

    fun dateTime(millis: Long): String =
        toLocalDateTime(millis).format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT))

    fun time(dateTime: LocalDateTime): String =
        dateTime.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))

    fun weekdayDate(dateTime: LocalDateTime): String =
        dateTime.format(DateTimeFormatter.ofPattern("EEEE d MMMM"))
            .replaceFirstChar { it.titlecase(Locale.getDefault()) }
}

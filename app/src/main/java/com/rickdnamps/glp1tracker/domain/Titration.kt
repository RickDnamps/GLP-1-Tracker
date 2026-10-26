package com.rickdnamps.glp1tracker.domain

import java.time.LocalDate

/**
 * Dose-escalation plan: an ordered list of steps; the last step (days = 0) is the maintenance dose.
 * Serialised as "dose:days;dose:days;…" (dose in mg with '.' separator).
 */
data class TitrationStep(val doseMg: Double, val days: Int)

data class TitrationStatus(
    val index: Int,
    val total: Int,
    val current: TitrationStep,
    val next: TitrationStep?,
    /** First day of the next step, null on the maintenance step. */
    val nextStepDate: LocalDate?,
)

object Titration {

    fun parse(text: String): List<TitrationStep> =
        text.split(';').mapNotNull { part ->
            val (dose, days) = part.split(':').takeIf { it.size == 2 } ?: return@mapNotNull null
            val d = dose.trim().toDoubleOrNull() ?: return@mapNotNull null
            val n = days.trim().toIntOrNull() ?: return@mapNotNull null
            if (d > 0 && n >= 0) TitrationStep(d, n) else null
        }

    fun serialize(steps: List<TitrationStep>): String =
        steps.joinToString(";") { "${it.doseMg}:${it.days}" }

    /** Usual escalation from the product monographs (4-week steps for weekly drugs). */
    fun preset(medication: Medication): List<TitrationStep> {
        fun weekly(vararg doses: Double) = doses.mapIndexed { i, d -> TitrationStep(d, if (i == doses.lastIndex) 0 else 28) }
        return when (medication) {
            Medication.OZEMPIC -> weekly(0.25, 0.5, 1.0, 2.0)
            Medication.WEGOVY, Medication.SEMAGLUTIDE_COMPOUNDED -> weekly(0.25, 0.5, 1.0, 1.7, 2.4)
            Medication.MOUNJARO, Medication.ZEPBOUND, Medication.TIRZEPATIDE_COMPOUNDED -> weekly(2.5, 5.0, 7.5, 10.0, 12.5, 15.0)
            Medication.TRULICITY -> weekly(0.75, 1.5, 3.0, 4.5)
            Medication.SAXENDA -> listOf(0.6, 1.2, 1.8, 2.4, 3.0).mapIndexed { i, d -> TitrationStep(d, if (i == 4) 0 else 7) }
            Medication.VICTOZA -> listOf(TitrationStep(0.6, 7), TitrationStep(1.2, 7), TitrationStep(1.8, 0))
            Medication.RYBELSUS -> listOf(TitrationStep(3.0, 30), TitrationStep(7.0, 30), TitrationStep(14.0, 0))
            Medication.WEGOVY_PILL -> listOf(TitrationStep(1.5, 30), TitrationStep(4.0, 30), TitrationStep(9.0, 30), TitrationStep(25.0, 0))
            Medication.OTHER -> emptyList()
        }
    }

    fun status(steps: List<TitrationStep>, start: LocalDate, today: LocalDate): TitrationStatus? {
        if (steps.isEmpty()) return null
        var stepStart = start
        steps.forEachIndexed { i, step ->
            val isLast = i == steps.lastIndex || step.days == 0
            val stepEnd = stepStart.plusDays(step.days.toLong())
            if (isLast || today.isBefore(stepEnd)) {
                return TitrationStatus(
                    index = i,
                    total = steps.size,
                    current = step,
                    next = if (isLast) null else steps[i + 1],
                    nextStepDate = if (isLast) null else stepEnd,
                )
            }
            stepStart = stepEnd
        }
        return null
    }
}

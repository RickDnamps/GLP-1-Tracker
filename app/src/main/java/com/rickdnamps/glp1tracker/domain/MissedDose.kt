package com.rickdnamps.glp1tracker.domain

import java.time.Duration
import java.time.LocalDateTime

/**
 * What to do after a missed dose, following the product monographs. Always shown with a
 * "check with your pharmacist" note — this is guidance, not medical advice.
 */
data class MissedDoseAdvice(
    val action: Action,
    /** Last moment the missed dose can still be taken (TAKE_NOW only). */
    val deadline: LocalDateTime?,
    /** Minimum hours that must separate two doses when changing the injection day. */
    val minGapHours: Int?,
) {
    enum class Action { TAKE_NOW, SKIP, ASK }
}

object MissedDose {

    private data class Rule(
        /** Missed dose may be taken up to this many hours after it was due… */
        val windowAfterDueHours: Long? = null,
        /** …or as long as the next scheduled dose is at least this many hours away. */
        val minHoursBeforeNext: Long? = null,
        val minGapHours: Int? = null,
        val alwaysSkip: Boolean = false,
    )

    private fun rule(m: Medication): Rule? = when (m) {
        Medication.OZEMPIC, Medication.SEMAGLUTIDE_COMPOUNDED -> Rule(windowAfterDueHours = 5 * 24, minGapHours = 48)
        Medication.WEGOVY -> Rule(minHoursBeforeNext = 48, minGapHours = 48)
        Medication.MOUNJARO, Medication.ZEPBOUND, Medication.TIRZEPATIDE_COMPOUNDED -> Rule(windowAfterDueHours = 4 * 24, minGapHours = 72)
        Medication.TRULICITY -> Rule(minHoursBeforeNext = 72, minGapHours = 72)
        Medication.SAXENDA -> Rule(windowAfterDueHours = 12)
        Medication.VICTOZA, Medication.RYBELSUS, Medication.WEGOVY_PILL -> Rule(alwaysSkip = true)
        Medication.OTHER -> null
    }

    /** [due] is the missed due date-time, [intervalDays] the dosing interval. */
    fun advise(medication: Medication, due: LocalDateTime, intervalDays: Int, now: LocalDateTime): MissedDoseAdvice {
        val r = rule(medication) ?: return MissedDoseAdvice(MissedDoseAdvice.Action.ASK, null, null)
        if (r.alwaysSkip) return MissedDoseAdvice(MissedDoseAdvice.Action.SKIP, null, r.minGapHours)
        val nextDue = due.plusDays(intervalDays.coerceAtLeast(1).toLong())
        val deadline = when {
            r.windowAfterDueHours != null -> due.plusHours(r.windowAfterDueHours)
            r.minHoursBeforeNext != null -> nextDue.minusHours(r.minHoursBeforeNext)
            else -> null
        }
        return if (deadline != null && now.isBefore(deadline)) {
            MissedDoseAdvice(MissedDoseAdvice.Action.TAKE_NOW, deadline, r.minGapHours)
        } else {
            MissedDoseAdvice(MissedDoseAdvice.Action.SKIP, null, r.minGapHours)
        }
    }

    fun hoursLeft(deadline: LocalDateTime, now: LocalDateTime): Long = Duration.between(now, deadline).toHours().coerceAtLeast(0)
}

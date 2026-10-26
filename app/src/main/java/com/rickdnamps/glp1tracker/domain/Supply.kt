package com.rickdnamps.glp1tracker.domain

import java.time.LocalDate

/** Pen in-use lifetime (days after first use) and remaining medication supply. */
object Supply {

    /** Typical in-use storage limits from the monographs; 0 = single-use pens or tablets. */
    fun defaultDiscardDays(medication: Medication): Int = when (medication) {
        Medication.OZEMPIC -> 56
        Medication.MOUNJARO, Medication.ZEPBOUND -> 30
        Medication.SAXENDA, Medication.VICTOZA -> 30
        Medication.SEMAGLUTIDE_COMPOUNDED, Medication.TIRZEPATIDE_COMPOUNDED -> 28
        else -> 0
    }

    /** Doses left in the current pen plus unopened pens. */
    fun dosesLeft(remainingInPen: Int, unopenedPens: Int, dosesPerPen: Int): Int =
        remainingInPen.coerceAtLeast(0) + unopenedPens.coerceAtLeast(0) * dosesPerPen.coerceAtLeast(0)

    /** Date of the last dose the current supply covers, counting from the next due date. */
    fun runsOutOn(nextDue: LocalDate, dosesLeft: Int, intervalDays: Int): LocalDate? =
        if (dosesLeft <= 0) null else nextDue.plusDays((dosesLeft - 1).toLong() * intervalDays.coerceAtLeast(1))

    fun discardDate(openedOn: LocalDate, discardDays: Int): LocalDate? =
        if (discardDays <= 0) null else openedOn.plusDays(discardDays.toLong())
}

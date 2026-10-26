package com.rickdnamps.glp1tracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class TreatmentLogicTest {

    @Test
    fun titrationRoundTripAndStatus() {
        val plan = Titration.preset(Medication.OZEMPIC)
        assertEquals(plan, Titration.parse(Titration.serialize(plan)))
        val start = LocalDate.of(2026, 1, 1)
        val s0 = Titration.status(plan, start, start.plusDays(10))!!
        assertEquals(0, s0.index)
        assertEquals(0.5, s0.next!!.doseMg, 0.0)
        assertEquals(start.plusDays(28), s0.nextStepDate)
        val s1 = Titration.status(plan, start, start.plusDays(28))!!
        assertEquals(1, s1.index)
        val last = Titration.status(plan, start, start.plusDays(400))!!
        assertEquals(3, last.index)
        assertNull(last.next)
    }

    @Test
    fun ozempicMissedDoseWindowIsFiveDays() {
        val due = LocalDateTime.of(2026, 3, 2, 9, 0)
        val within = MissedDose.advise(Medication.OZEMPIC, due, 7, due.plusDays(3))
        assertEquals(MissedDoseAdvice.Action.TAKE_NOW, within.action)
        assertEquals(due.plusDays(5), within.deadline)
        assertEquals(MissedDoseAdvice.Action.SKIP, MissedDose.advise(Medication.OZEMPIC, due, 7, due.plusDays(6)).action)
    }

    @Test
    fun wegovyNeedsTwoDaysBeforeNextDose() {
        val due = LocalDateTime.of(2026, 3, 2, 9, 0)
        assertEquals(MissedDoseAdvice.Action.TAKE_NOW, MissedDose.advise(Medication.WEGOVY, due, 7, due.plusDays(4)).action)
        assertEquals(MissedDoseAdvice.Action.SKIP, MissedDose.advise(Medication.WEGOVY, due, 7, due.plusDays(5).plusHours(1)).action)
        assertEquals(MissedDoseAdvice.Action.ASK, MissedDose.advise(Medication.OTHER, due, 7, due.plusDays(1)).action)
    }

    @Test
    fun supplyMath() {
        assertEquals(10, Supply.dosesLeft(2, 2, 4))
        val next = LocalDate.of(2026, 3, 2)
        assertEquals(next.plusDays(63), Supply.runsOutOn(next, 10, 7))
        assertNull(Supply.runsOutOn(next, 0, 7))
        assertEquals(LocalDate.of(2026, 4, 27), Supply.discardDate(LocalDate.of(2026, 3, 2), 56))
    }
}

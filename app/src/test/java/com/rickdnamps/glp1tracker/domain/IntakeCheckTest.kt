package com.rickdnamps.glp1tracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IntakeCheckTest {
    @Test
    fun flagsLowIntakeOnlyOnLoggedDays() {
        val days = List(5) { IntakeCheck.Day(calories = 900, proteinG = 40, entries = 3) } +
            IntakeCheck.Day(calories = 200, proteinG = 5, entries = 1) // ignored: a single entry
        val r = IntakeCheck.check(days, IntakeCheck.calorieFloor(Sex.FEMALE), proteinTargetG = 90)!!
        assertTrue(r.lowCalories)
        assertTrue(r.lowProtein)
        assertEquals(900, r.averageCalories)
    }

    @Test
    fun noWarningWhenEatingEnoughOrTooFewDays() {
        val ok = List(5) { IntakeCheck.Day(1_600, 100, 4) }
        val r = IntakeCheck.check(ok, IntakeCheck.calorieFloor(Sex.MALE), 120)!!
        assertFalse(r.any)
        assertNull(IntakeCheck.check(List(3) { IntakeCheck.Day(500, 10, 3) }, 1_200, 90))
    }
}

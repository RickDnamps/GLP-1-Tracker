package com.rickdnamps.glp1tracker.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class StepMathTest {
    @Test
    fun countsStepsWithinADay() {
        var s = StepMath.update(null, 100, 5_000)
        assertEquals(0, s.today)
        s = StepMath.update(s, 100, 8_200)
        assertEquals(3_200, s.today)
    }

    @Test
    fun startsANewDayAndKeepsHistory() {
        var s = StepMath.update(null, 100, 5_000)
        s = StepMath.update(s, 100, 9_000) // 4 000 on day 100
        s = StepMath.update(s, 101, 9_500) // first reading of day 101
        assertEquals(500, s.today)
        assertEquals(4_000L, s.history[100])
        assertEquals(listOf(99L to 0L, 100L to 4_000L, 101L to 500L), StepMath.lastDays(s, 101, 3))
    }

    @Test
    fun survivesAReboot() {
        var s = StepMath.update(null, 100, 5_000)
        s = StepMath.update(s, 100, 7_000) // 2 000 today
        s = StepMath.update(s, 100, 300) // reboot, then 300 steps
        assertEquals(2_300, s.today)
        s = StepMath.update(s, 101, 100) // reboot overnight
        assertEquals(100, s.today)
        assertEquals(2_300L, s.history[100])
    }
}

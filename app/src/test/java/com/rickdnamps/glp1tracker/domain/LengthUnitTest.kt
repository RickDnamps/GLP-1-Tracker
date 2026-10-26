package com.rickdnamps.glp1tracker.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class LengthUnitTest {
    @Test
    fun feetInchesRoundTrip() {
        assertEquals(5 to 10, LengthUnit.feetInches(178.0))
        assertEquals(177.8, LengthUnit.fromFeetInches(5, 10.0), 1e-9)
        assertEquals(6 to 0, LengthUnit.feetInches(182.7))
        assertEquals(36.0, LengthUnit.IN.fromCm(91.44), 1e-9)
        assertEquals(91.44, LengthUnit.IN.toCm(36.0), 1e-9)
        assertEquals(80.0, LengthUnit.CM.fromCm(80.0), 0.0)
    }
}

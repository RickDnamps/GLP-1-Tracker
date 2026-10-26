package com.rickdnamps.glp1tracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class VitalRangesTest {

    private fun bp(s: Double, d: Double) = VitalRanges.classify(VitalType.BLOOD_PRESSURE, s, d)

    @Test
    fun bloodPressureStages() {
        assertEquals(BpStage.NORMAL, VitalRanges.bpStage(118.0, 76.0))
        assertEquals(BpStage.ELEVATED, VitalRanges.bpStage(124.0, 78.0))
        assertEquals(BpStage.STAGE_1, VitalRanges.bpStage(132.0, 78.0))
        // The diastolic alone can raise the category.
        assertEquals(BpStage.STAGE_1, VitalRanges.bpStage(118.0, 84.0))
        assertEquals(BpStage.STAGE_2, VitalRanges.bpStage(142.0, 70.0))
        assertEquals(BpStage.STAGE_2, VitalRanges.bpStage(128.0, 92.0))
        assertEquals(BpStage.CRISIS, VitalRanges.bpStage(185.0, 100.0))
        assertEquals(BpStage.CRISIS, VitalRanges.bpStage(170.0, 125.0))
        assertEquals(BpStage.LOW, VitalRanges.bpStage(88.0, 58.0))
        assertEquals(BpStage.LOW, VitalRanges.bpStage(105.0, 55.0))
        // A high systolic wins over a low diastolic.
        assertEquals(BpStage.STAGE_2, VitalRanges.bpStage(150.0, 55.0))
    }

    @Test
    fun bloodPressureLevels() {
        assertEquals(VitalLevel.NORMAL, bp(119.0, 79.0))
        assertEquals(VitalLevel.ELEVATED, bp(120.0, 79.0))
        assertEquals(VitalLevel.ELEVATED, bp(139.0, 89.0)) // stage 1 stays "elevated"
        assertEquals(VitalLevel.HIGH, bp(140.0, 80.0))
        assertEquals(VitalLevel.HIGH, bp(130.0, 90.0))
        assertEquals(VitalLevel.LOW, bp(89.0, 60.0))
    }

    @Test
    fun heartRate() {
        assertEquals(VitalLevel.LOW, VitalRanges.classify(VitalType.HEART_RATE, 48.0))
        assertEquals(VitalLevel.NORMAL, VitalRanges.classify(VitalType.HEART_RATE, 50.0))
        assertEquals(VitalLevel.NORMAL, VitalRanges.classify(VitalType.HEART_RATE, 100.0))
        assertEquals(VitalLevel.HIGH, VitalRanges.classify(VitalType.HEART_RATE, 101.0))
    }

    @Test
    fun fastingGlucose() {
        fun g(v: Double) = VitalRanges.classify(VitalType.GLUCOSE, v)
        assertEquals(VitalLevel.LOW, g(3.9))
        assertEquals(VitalLevel.NORMAL, g(4.0))
        assertEquals(VitalLevel.NORMAL, g(6.0))
        assertEquals(VitalLevel.ELEVATED, g(6.1))
        assertEquals(VitalLevel.ELEVATED, g(6.9))
        assertEquals(VitalLevel.HIGH, g(7.0))
        // Classified at display precision: 6.96 shows as 7.0, so it is "high".
        assertEquals(VitalLevel.HIGH, g(6.96))
        assertEquals(VitalLevel.NORMAL, g(6.04))
    }

    @Test
    fun hba1c() {
        fun a(v: Double) = VitalRanges.classify(VitalType.HBA1C, v)
        assertEquals(VitalLevel.NORMAL, a(5.6))
        assertEquals(VitalLevel.ELEVATED, a(5.7))
        assertEquals(VitalLevel.ELEVATED, a(6.4))
        assertEquals(VitalLevel.HIGH, a(6.5))
    }

    @Test
    fun lipids() {
        assertEquals(VitalLevel.NORMAL, VitalRanges.classify(VitalType.LDL, 3.49))
        assertEquals(VitalLevel.ELEVATED, VitalRanges.classify(VitalType.LDL, 3.5))
        assertEquals(VitalLevel.HIGH, VitalRanges.classify(VitalType.LDL, 5.2))
        assertEquals(VitalLevel.NORMAL, VitalRanges.classify(VitalType.TRIGLYCERIDES, 1.69))
        assertEquals(VitalLevel.ELEVATED, VitalRanges.classify(VitalType.TRIGLYCERIDES, 1.7))
        assertEquals(VitalLevel.ELEVATED, VitalRanges.classify(VitalType.TRIGLYCERIDES, 2.2))
        assertEquals(VitalLevel.HIGH, VitalRanges.classify(VitalType.TRIGLYCERIDES, 2.3))
    }

    @Test
    fun validation() {
        val v = VitalRanges
        assertEquals(VitalInputError.MISSING_VALUE, v.validate(VitalType.HEART_RATE, null, null))
        assertEquals(VitalInputError.VALUE_OUT_OF_RANGE, v.validate(VitalType.HEART_RATE, 720.0, null))
        assertNull(v.validate(VitalType.HEART_RATE, 72.0, null))
        // Second value ignored for single-value types.
        assertNull(v.validate(VitalType.GLUCOSE, 5.4, 999.0))
        assertEquals(VitalInputError.MISSING_SECOND, v.validate(VitalType.BLOOD_PRESSURE, 128.0, null))
        assertEquals(VitalInputError.SECOND_OUT_OF_RANGE, v.validate(VitalType.BLOOD_PRESSURE, 128.0, 8.0))
        assertEquals(VitalInputError.SECOND_NOT_BELOW_FIRST, v.validate(VitalType.BLOOD_PRESSURE, 80.0, 120.0))
        assertNull(v.validate(VitalType.BLOOD_PRESSURE, 128.0, 82.0))
        assertEquals(VitalInputError.VALUE_OUT_OF_RANGE, v.validate(VitalType.GLUCOSE, 108.0, null))
    }

    @Test
    fun unitConversions() {
        val v = VitalRanges
        assertEquals(VitalConversion(6.0, "mg/dL"), v.suggestConversion(VitalType.GLUCOSE, 108.0))
        assertEquals(VitalConversion(2.59, "mg/dL"), v.suggestConversion(VitalType.LDL, 100.0))
        assertEquals(VitalConversion(1.69, "mg/dL"), v.suggestConversion(VitalType.TRIGLYCERIDES, 150.0))
        assertEquals(VitalConversion(6.5, "mmol/mol"), v.suggestConversion(VitalType.HBA1C, 48.0))
        assertEquals(VitalConversion(6.5, ""), v.suggestConversion(VitalType.HBA1C, 0.065))
        // Plausible values and units without an alternative get no suggestion.
        assertNull(v.suggestConversion(VitalType.GLUCOSE, 5.5))
        assertNull(v.suggestConversion(VitalType.HEART_RATE, 400.0))
        assertNull(v.suggestConversion(VitalType.GLUCOSE, 2000.0))
        assertNull(v.suggestConversion(VitalType.GLUCOSE, 0.0))
    }

    @Test
    fun simpleChange() {
        assertEquals(-12.0, VitalRanges.change(140.0, 128.0), 1e-9)
        assertEquals(-10.0, VitalRanges.percentChange(7.0, 6.3)!!, 1e-9)
        assertNull(VitalRanges.percentChange(0.0, 5.0))
    }

    @Test
    fun bloodPressureImproves() {
        val c = VitalRanges.change(VitalType.BLOOD_PRESSURE, VitalReading(142.0, 91.0), VitalReading(126.0, 79.0))
        assertEquals(-16.0, c.delta, 1e-9)
        assertEquals(-12.0, c.delta2!!, 1e-9)
        assertEquals(VitalTrend.BETTER, c.trend)
    }

    @Test
    fun trendsInsideTheSameCategory() {
        val v = VitalRanges
        // HbA1c 7.4 -> 6.8: still in the diabetes range, but going down is better.
        assertEquals(VitalTrend.BETTER, v.trend(VitalType.HBA1C, VitalReading(7.4), VitalReading(6.8)))
        assertEquals(VitalTrend.WORSE, v.trend(VitalType.HBA1C, VitalReading(6.6), VitalReading(7.2)))
        // 0.1 % is the smallest meaningful HbA1c change.
        assertEquals(VitalTrend.BETTER, v.trend(VitalType.HBA1C, VitalReading(6.9), VitalReading(6.8)))
        // Small blood pressure noise is stable.
        assertEquals(VitalTrend.STABLE, v.trend(VitalType.BLOOD_PRESSURE, VitalReading(150.0, 95.0), VitalReading(148.0, 94.0)))
        // Normal heart rate or glucose moving around inside the range is neutral.
        assertEquals(VitalTrend.STABLE, v.trend(VitalType.HEART_RATE, VitalReading(80.0), VitalReading(64.0)))
        assertEquals(VitalTrend.STABLE, v.trend(VitalType.GLUCOSE, VitalReading(5.8), VitalReading(4.6)))
        // Lower LDL is better even inside the normal range.
        assertEquals(VitalTrend.BETTER, v.trend(VitalType.LDL, VitalReading(3.1), VitalReading(2.4)))
        // Low values: going up is better.
        assertEquals(VitalTrend.BETTER, v.trend(VitalType.HEART_RATE, VitalReading(40.0), VitalReading(46.0)))
        // Category changes win over the size of the change.
        assertEquals(VitalTrend.WORSE, v.trend(VitalType.GLUCOSE, VitalReading(4.2), VitalReading(3.8)))
        assertEquals(VitalTrend.BETTER, v.trend(VitalType.GLUCOSE, VitalReading(7.1), VitalReading(6.0)))
        // Above normal to below normal: neither better nor worse.
        assertEquals(VitalTrend.STABLE, v.trend(VitalType.BLOOD_PRESSURE, VitalReading(125.0, 78.0), VitalReading(88.0, 58.0)))
    }

    @Test
    fun formatting() {
        val us = Locale.US
        val fr = Locale.CANADA_FRENCH
        assertEquals("128/82", VitalRanges.formatReading(VitalType.BLOOD_PRESSURE, 128.0, 82.0, us))
        assertEquals("6.0", VitalRanges.format(VitalType.GLUCOSE, 6.0, us))
        assertEquals("6,0", VitalRanges.format(VitalType.GLUCOSE, 6.0, fr))
        assertEquals("2.45", VitalRanges.format(VitalType.LDL, 2.45, us))
        assertEquals("3.0", VitalRanges.format(VitalType.LDL, 3.0, us))
        assertEquals("−12", VitalRanges.formatDelta(VitalType.BLOOD_PRESSURE, -12.0, us))
        assertEquals("+0.3", VitalRanges.formatDelta(VitalType.HBA1C, 0.3, us))
        assertEquals("±0.0", VitalRanges.formatDelta(VitalType.GLUCOSE, 0.01, us))
        assertEquals("6", VitalRanges.inputText(VitalType.GLUCOSE, 6.0, us))
        assertEquals("2,45", VitalRanges.inputText(VitalType.LDL, 2.45, fr))
    }

    @Test
    fun everyTypeHasConsistentRanges() {
        VitalType.entries.forEach { t ->
            val upper = VitalRanges.upperNormal(t)
            assertNotNull(VitalRanges.plausible(t))
            assertTrue("$t upper normal outside plausible range", upper in VitalRanges.plausible(t))
        }
    }
}

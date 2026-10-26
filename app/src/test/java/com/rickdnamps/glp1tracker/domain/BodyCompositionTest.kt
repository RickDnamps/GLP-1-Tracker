package com.rickdnamps.glp1tracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BodyCompositionTest {

    @Test
    fun estimateIsPlausibleForTypicalAdults() {
        val man = BodyCompositionMath.estimate(95.0, 178.0, 45, Sex.MALE, 480.0)!!
        assertTrue("fat ${man.bodyFatPct}", man.bodyFatPct!! in 20.0..40.0)
        assertTrue(man.waterPct!! in 40.0..65.0)
        assertTrue(man.boneKg!! in 2.0..4.5)
        assertTrue(man.bmrKcal!! in 1500.0..2200.0)
        assertNotNull(man.metabolicAge)
        val woman = BodyCompositionMath.estimate(80.0, 163.0, 50, Sex.FEMALE, 560.0)!!
        assertTrue("fat ${woman.bodyFatPct}", woman.bodyFatPct!! in 30.0..50.0)
        assertNull(BodyCompositionMath.estimate(80.0, 163.0, 50, Sex.FEMALE, 5000.0))
    }

    @Test
    fun moreMuscleMeansYoungerMetabolicAge() {
        val lean = BodyCompositionMath.metabolicAge(1900.0, 80.0, 175.0, 45, Sex.MALE)
        val soft = BodyCompositionMath.metabolicAge(1600.0, 80.0, 175.0, 45, Sex.MALE)
        assertTrue(lean < soft)
        assertTrue(soft <= 60)
    }

    @Test
    fun lossSplitAndLevels() {
        val split = BodyCompositionMath.lossSplit(100.0, 40.0, 90.0, 36.0)!!
        assertEquals(10.0, split.totalKg, 1e-9)
        assertEquals(7.6, split.fatKg, 1e-9)
        assertEquals(0.76, split.fatShare, 1e-9)
        assertNull(BodyCompositionMath.lossSplit(90.0, 30.0, 90.1, 30.0))
        assertEquals(CompLevel.HIGH, BodyCompositionMath.visceralLevel(12.0))
        assertEquals(CompLevel.HEALTHY, BodyCompositionMath.fatLevel(22.0, Sex.MALE))
        assertEquals(480.0, BodyCompositionMath.normalizeImpedance(4800.0)!!, 0.0)
    }
}

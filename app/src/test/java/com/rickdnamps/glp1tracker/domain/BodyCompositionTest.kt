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
    fun matchesTheRenphoAppOnTheReferenceReading() {
        // Renpho app, ES-CS20M, 304.4 lb, 6'2", 46 y, man: 38.6 % fat, 44.3 % water, 9.4 lb bone,
        // 177.6 lb muscle, 14.0 % protein, 39.7 % skeletal muscle, 32.5 % subcutaneous,
        // 2201 kcal, metabolic age 53. Impedance ≈ 509 Ω (as normalised by the app).
        val c = BodyCompositionMath.estimate(138.07, 187.96, 46, Sex.MALE, 509.0)!!
        assertEquals(38.6, c.bodyFatPct!!, 0.5)
        assertEquals(44.3, c.waterPct!!, 0.5)
        assertEquals(9.4 / 2.20462, c.boneKg!!, 0.15)
        assertEquals(177.6 / 2.20462, c.muscleKg!!, 1.0)
        assertEquals(14.0, c.proteinPct!!, 0.5)
        assertEquals(39.7, c.skeletalMusclePct!!, 0.5)
        assertEquals(32.5, c.subcutaneousFatPct!!, 0.5)
        assertEquals(2201.0, c.bmrKcal!!, 20.0)
        assertEquals(53, c.metabolicAge)
    }

    @Test
    fun completesAHealthConnectReadingFromItsFatPercentage() {
        // Renpho shares only weight and body fat with Health Connect; the rest is derived from the
        // same reading (304.4 lb, 38.6 %) and lands on the Renpho app's values above.
        val c = BodyCompositionMath.complete(BodyComp(bodyFatPct = 38.6), 138.07, 46, Sex.MALE)
        assertEquals(38.6, c.bodyFatPct!!, 0.001)
        assertEquals(44.3, c.waterPct!!, 0.3)
        assertEquals(9.4 / 2.20462, c.boneKg!!, 0.1)
        assertEquals(177.6 / 2.20462, c.muscleKg!!, 1.0)
        assertEquals(14.0, c.proteinPct!!, 0.3)
        assertEquals(39.7, c.skeletalMusclePct!!, 0.3)
        assertEquals(32.5, c.subcutaneousFatPct!!, 0.3)
        assertEquals(2201.0, c.bmrKcal!!, 10.0)
        assertEquals(53, c.metabolicAge)
        assertNull(c.visceralFat)

        // Values the source did provide are kept; no fat % → nothing to derive; no profile → no body age.
        val withBmr = BodyCompositionMath.complete(BodyComp(bodyFatPct = 38.6, bmrKcal = 2150.0, visceralFat = 18.0), 138.07, 46, Sex.MALE)
        assertEquals(2150.0, withBmr.bmrKcal!!, 0.001)
        assertEquals(18.0, withBmr.visceralFat!!, 0.001)
        assertEquals(BodyComp(waterPct = 50.0), BodyCompositionMath.complete(BodyComp(waterPct = 50.0), 80.0, 40, Sex.FEMALE))
        val noProfile = BodyCompositionMath.complete(BodyComp(bodyFatPct = 30.0), 80.0, null, null)
        assertNull(noProfile.metabolicAge)
        assertNotNull(noProfile.waterPct)
    }

    @Test
    fun levelsMatchTheRenphoAppOnTheReferenceReading() {
        // Renpho: 302.6 lb, 188 cm man, 46 y — muscle "Excellent", skeletal muscle / water /
        // protein "below", subcutaneous fat and metabolic age "above", bone "average".
        val m = BodyCompositionMath
        assertEquals(CompLevel.EXCELLENT, m.muscleMassLevel(79.65, 188.0, Sex.MALE))
        assertEquals(CompLevel.LOW, m.skeletalMuscleLevel(39.2, Sex.MALE))
        assertEquals(CompLevel.LOW, m.waterLevel(44.1, Sex.MALE))
        assertEquals(CompLevel.LOW, m.proteinLevel(13.9))
        assertEquals(CompLevel.HIGH, m.subcutaneousLevel(32.8, Sex.MALE))
        assertEquals(CompLevel.HIGH, m.metabolicAgeLevel(53, 46))
        assertEquals(CompLevel.HEALTHY, m.boneLevel(4.26, 137.3, Sex.MALE))
        assertEquals(CompLevel.VERY_HIGH, m.bmiLevel(39.3))
        assertEquals(CompLevel.HEALTHY, m.bmiLevel(22.0))
    }

    @Test
    fun metabolicAgeFollowsBodyFat() {
        // Healthy fat for the age → body age ≈ real age; leaner → younger; more fat → older.
        assertEquals(46, BodyCompositionMath.metabolicAge(16.0, 46, Sex.MALE))
        assertTrue(BodyCompositionMath.metabolicAge(10.0, 46, Sex.MALE) < 46)
        assertEquals(53, BodyCompositionMath.metabolicAge(38.6, 46, Sex.MALE))
        assertEquals(50, BodyCompositionMath.metabolicAge(28.0, 50, Sex.FEMALE))
        // Never runs away: capped at +15 years.
        assertEquals(61, BodyCompositionMath.metabolicAge(90.0, 46, Sex.MALE))
        // Large man (188 cm, 138 kg, ~450 Ω): about 8 years above his age, not pinned to the cap.
        val big = BodyCompositionMath.estimate(138.0, 188.0, 46, Sex.MALE, 450.0)!!
        assertTrue("age ${big.metabolicAge}", big.metabolicAge!! in 50..58)
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

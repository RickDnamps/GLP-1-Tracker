package com.rickdnamps.glp1tracker.scale

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScaleProtocolsTest {
    private fun bytes(vararg v: Int) = ByteArray(v.size) { v[it].toByte() }

    @Test
    fun standardWeightScaleSi() {
        // flags 0 (kg), raw 16000 * 0.005 = 80.0 kg
        assertEquals(80.0, StandardWeightScale.parse(bytes(0x00, 0x80, 0x3E))!!, 0.001)
    }

    @Test
    fun standardWeightScaleImperialAndInvalid() {
        // flags 1 (lb), raw 17637 * 0.01 = 176.37 lb = 80.0 kg
        assertEquals(80.0, StandardWeightScale.parse(bytes(0x01, 0xE5, 0x44))!!, 0.01)
        assertNull(StandardWeightScale.parse(bytes(0x00, 0xFF, 0xFF)))
    }

    @Test
    fun qnLiveThenStableWeight() {
        val qn = QnProtocol(hasT1 = true, hasT2 = false)
        // 0x10 frame, classic layout: weight 0x1F40 = 8000 / 100 = 80.00 kg
        val live = qn.onNotification(QnProtocol.T1_NOTIFY, bytes(0x10, 0x0B, 0x05, 0x1F, 0x40, 0x00, 0, 0, 0, 0, 0), 0)
        assertEquals(listOf(ScaleEvent.Live(80.0)), live)
        val stable = qn.onNotification(QnProtocol.T1_NOTIFY, bytes(0x10, 0x0B, 0x05, 0x1F, 0x40, 0x01, 0, 0, 0, 0, 0), 0)
        assertEquals(listOf(ScaleEvent.Stable(80.0)), stable)
        // Only one stable reading per session.
        assertTrue(qn.onNotification(QnProtocol.T1_NOTIFY, bytes(0x10, 0x0B, 0x05, 0x1F, 0x40, 0x01, 0, 0, 0, 0, 0), 0).isEmpty())
    }

    @Test
    fun qnInfoFrameTriggersConfigWithChecksum() {
        val qn = QnProtocol(hasT1 = true, hasT2 = false)
        val events = qn.onNotification(QnProtocol.T1_NOTIFY, bytes(0x12, 0x0B, 0x05, 0, 0, 0, 0, 0, 0, 0, 0x01), 1_000_000L)
        val writes = events.filterIsInstance<ScaleEvent.Write>().map { it.write }
        assertEquals(2, writes.size)
        val cfg = writes[0].value
        assertEquals(0x13, cfg[0].toInt())
        assertEquals(QnProtocol.T1_WRITE_CONFIG, writes[0].characteristic)
        var sum = 0
        for (i in 0 until cfg.lastIndex) sum = (sum + (cfg[i].toInt() and 0xFF)) and 0xFF
        assertEquals(sum.toByte(), cfg.last())
        assertEquals(QnProtocol.T1_WRITE_TIME, writes[1].characteristic)
    }

    @Test
    fun lefuStreamsThenStopsWithLastWeight() {
        val lefu = LefuProtocol()
        fun weight(raw: Int) = bytes(0x55, 0xAA, 0x14, 0x00, 0x07, 0, 0, 0, raw ushr 8, raw and 0xFF, 0, 0, 0)
        assertEquals(listOf(ScaleEvent.Live(79.5)), lefu.onNotification(LefuProtocol.RESULTS, weight(7950), 0))
        assertEquals(listOf(ScaleEvent.Live(80.1)), lefu.onNotification(LefuProtocol.RESULTS, weight(8010), 0))
        val stop = bytes(0x55, 0xAA, 0x11, 0x00, 0x01, 0x00, 0x00)
        assertEquals(listOf(ScaleEvent.Stable(80.1)), lefu.onNotification(LefuProtocol.RESULTS, stop, 0))
    }

    @Test
    fun lefuFrameChecksum() {
        val f = LefuProtocol.buildFrame(0x90, bytes(0x01, 0x00, 0x01, 0x00))
        assertEquals(listOf(0x55, 0xAA, 0x90, 0x00, 0x04, 0x01, 0x00, 0x01, 0x00, 0x95), f.map { it.toInt() and 0xFF })
    }

    @Test
    fun protocolSelectionPrefersVendorServices() {
        assertTrue(ScaleProtocol.forServices(setOf(LefuProtocol.SERVICE, StandardWeightScale.SERVICE)) is LefuProtocol)
        assertTrue(ScaleProtocol.forServices(setOf(QnProtocol.SERVICE_T2)) is QnProtocol)
        assertTrue(ScaleProtocol.forServices(setOf(StandardWeightScale.SERVICE)) is StandardWeightScale)
        assertNull(ScaleProtocol.forServices(setOf(uuid16(0x180F))))
    }

    @Test
    fun stabilityDetectorFiresOnceAfterWindow() {
        val d = StabilityDetector(windowMillis = 2_000)
        assertNull(d.onLive(80.0, 0))
        assertNull(d.onLive(80.05, 1_000))
        assertEquals(80.02, d.onLive(80.02, 2_100)!!, 0.0)
        assertNull(d.onLive(80.02, 5_000))
    }

    @Test
    fun lefuBioimpedanceHandshake() {
        val profile = ScaleUserProfile(male = true, birthYear = 1980, heightCm = 178.0, refWeightKg = 92.5)
        val lefu = LefuProtocol(profile)
        fun frame(cmd: Int, vararg payload: Int) = LefuProtocol.buildFrame(cmd, bytes(*payload))
        // Profile ack → user profile 0x96 with height in mm and reference weight in 0.01 kg
        val w1 = lefu.onNotification(LefuProtocol.RESULTS, frame(0x17, 0x01), 0).filterIsInstance<ScaleEvent.Write>()
        assertEquals(1, w1.size)
        val p96 = w1[0].write.value
        assertEquals(0x96, p96[2].toInt() and 0xFF)
        assertEquals(0x11, p96[5].toInt() and 0xFF) // male
        assertEquals(1780, ((p96[10].toInt() and 0xFF) shl 8) or (p96[11].toInt() and 0xFF))
        assertEquals(9250, ((p96[14].toInt() and 0xFF) shl 8) or (p96[15].toInt() and 0xFF))
        // History ack accepted → start BIA (0x90)
        val w2 = lefu.onNotification(LefuProtocol.RESULTS, frame(0x16, 0x01), 0).filterIsInstance<ScaleEvent.Write>()
        assertEquals(0x90, w2.single().write.value[2].toInt() and 0xFF)
        // Final BIA: weight 91.20 kg at [10..11], resistance 512 ohm at [12..13]
        val final = frame(0x18, 0, 0, 0, 0, 0, 0x23, 0xA0, 0x02, 0x00, 0, 0)
        val events = lefu.onNotification(LefuProtocol.RESULTS, final, 0)
        assertEquals(2, events.filterIsInstance<ScaleEvent.Write>().size)
        assertEquals(ScaleEvent.Stable(91.2, impedance = 512.0), events.filterIsInstance<ScaleEvent.Stable>().single())
    }

    @Test
    fun lefuReassemblesFragments() {
        val lefu = LefuProtocol()
        val full = LefuProtocol.buildFrame(0x14, bytes(0, 0, 0, 0x1F, 0x40, 0, 0))
        val first = bytes(0xAD, 0, 0) + full.copyOfRange(0, 6)
        val cont = bytes(0xAF, 0, 0) + full.copyOfRange(6, full.size)
        assertTrue(lefu.onNotification(LefuProtocol.RESULTS, first, 0).isEmpty())
        assertEquals(listOf(ScaleEvent.Live(80.0)), lefu.onNotification(LefuProtocol.RESULTS, cont, 0))
    }

    @Test
    fun qnDisplayUnitFollowsApp() {
        val info = bytes(0x12, 0x0B, 0x05, 0, 0, 0, 0, 0, 0, 0, 0x01)
        val lb = QnProtocol(hasT1 = true, hasT2 = false, displayLb = true)
        val cfgLb = lb.onNotification(QnProtocol.T1_NOTIFY, info, 0).filterIsInstance<ScaleEvent.Write>().first().write.value
        assertEquals(0x02, cfgLb[3].toInt())
        val kg = QnProtocol(hasT1 = true, hasT2 = false, displayLb = false)
        val cfgKg = kg.onNotification(QnProtocol.T1_NOTIFY, info, 0).filterIsInstance<ScaleEvent.Write>().first().write.value
        assertEquals(0x01, cfgKg[3].toInt())
    }
}

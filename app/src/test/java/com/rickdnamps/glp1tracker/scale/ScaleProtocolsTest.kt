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
}

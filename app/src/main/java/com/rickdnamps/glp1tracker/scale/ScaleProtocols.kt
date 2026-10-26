package com.rickdnamps.glp1tracker.scale

import com.rickdnamps.glp1tracker.domain.BodyComp
import com.rickdnamps.glp1tracker.domain.BodyCompositionMath
import java.util.UUID

fun uuid16(short: Int): UUID = UUID.fromString(String.format("%08x-0000-1000-8000-00805f9b34fb", short))

/** A GATT write a protocol wants to perform. */
data class GattWrite(val service: UUID, val characteristic: UUID, val value: ByteArray) {
    override fun equals(other: Any?) =
        other is GattWrite && other.service == service && other.characteristic == characteristic && other.value.contentEquals(value)
    override fun hashCode() = 31 * (31 * service.hashCode() + characteristic.hashCode()) + value.contentHashCode()
}

/** What a protocol extracted from a notification. */
sealed interface ScaleEvent {
    data class Live(val kg: Double) : ScaleEvent
    data class Stable(val kg: Double, val composition: BodyComp? = null, val impedance: Double? = null) : ScaleEvent
    data class Write(val write: GattWrite) : ScaleEvent
}

/**
 * Pure (Android-free) description of one scale protocol, so parsing can be unit-tested.
 * The GATT plumbing lives in [BleScaleManager].
 */
interface ScaleProtocol {
    val displayName: String
    /** (service, characteristic) pairs to subscribe to, in order. Missing ones are skipped. */
    val subscriptions: List<Pair<UUID, UUID>>
    /** Writes to send once all subscriptions are active. */
    fun onReady(nowMillis: Long): List<GattWrite> = emptyList()
    fun onNotification(characteristic: UUID, data: ByteArray, nowMillis: Long): List<ScaleEvent>

    companion object {
        /** Picks a protocol from the services discovered on the device (most specific first). */
        fun forServices(services: Set<UUID>): ScaleProtocol? = when {
            LefuProtocol.SERVICE in services -> LefuProtocol()
            QnProtocol.SERVICE_T1 in services || QnProtocol.SERVICE_T2 in services -> QnProtocol(
                hasT1 = QnProtocol.SERVICE_T1 in services,
                hasT2 = QnProtocol.SERVICE_T2 in services,
            )
            StandardWeightScale.SERVICE in services || StandardWeightScale.BODY_SERVICE in services ->
                StandardWeightScale(hasBodyComposition = StandardWeightScale.BODY_SERVICE in services)
            else -> null
        }

        /** Heuristic used in scan results to flag likely compatible scales. */
        fun looksCompatible(name: String?, advertisedServices: Collection<UUID>): Boolean {
            val n = name?.lowercase().orEmpty()
            val known = setOf(LefuProtocol.SERVICE, QnProtocol.SERVICE_T1, QnProtocol.SERVICE_T2, StandardWeightScale.SERVICE, uuid16(0x181B))
            return advertisedServices.any { it in known } ||
                listOf("renpho", "qn-scale", "es-cs20m", "es-32md", "es-26", "fitindex", "scale", "balance").any { it in n }
        }
    }
}

private fun u8(b: Byte) = b.toInt() and 0xFF
private fun u16be(d: ByteArray, i: Int) = (u8(d[i]) shl 8) or u8(d[i + 1])
private fun u16le(d: ByteArray, i: Int) = u8(d[i]) or (u8(d[i + 1]) shl 8)
private fun sum8(d: ByteArray, until: Int): Byte {
    var s = 0
    for (i in 0 until until) s = (s + u8(d[i])) and 0xFF
    return s.toByte()
}

private fun plausible(kg: Double) = kg in 5.0..300.0

/**
 * Bluetooth SIG Weight Scale (0x181D, Weight Measurement 0x2A9D) and Body Composition
 * (0x181B, Body Composition Measurement 0x2A9C) services. When both exist the weight is held
 * until the composition packet arrives (the manager finishes anyway after a short timeout).
 */
class StandardWeightScale(private val hasBodyComposition: Boolean = false) : ScaleProtocol {
    override val displayName = if (hasBodyComposition) "Bluetooth Body Composition" else "Bluetooth Weight Scale"
    override val subscriptions = listOf(SERVICE to MEASUREMENT, BODY_SERVICE to BODY_MEASUREMENT)

    private var weightKg: Double? = null
    private var done = false

    override fun onNotification(characteristic: UUID, data: ByteArray, nowMillis: Long): List<ScaleEvent> {
        if (done) return emptyList()
        return when (characteristic) {
            MEASUREMENT -> {
                val kg = parse(data) ?: return emptyList()
                weightKg = kg
                if (hasBodyComposition) listOf(ScaleEvent.Live(kg)) else { done = true; listOf(ScaleEvent.Stable(kg)) }
            }
            BODY_MEASUREMENT -> {
                val body = parseBody(data) ?: return emptyList()
                val kg = body.weightKg ?: weightKg ?: return emptyList()
                done = true
                listOf(ScaleEvent.Stable(kg, body.toComposition(kg), body.impedance))
            }
            else -> emptyList()
        }
    }

    data class Body(
        val fatPct: Double,
        val bmrKcal: Double? = null,
        val musclePct: Double? = null,
        val muscleKg: Double? = null,
        val fatFreeKg: Double? = null,
        val waterKg: Double? = null,
        val impedance: Double? = null,
        val weightKg: Double? = null,
    ) {
        fun toComposition(kg: Double) = BodyComp(
            bodyFatPct = fatPct,
            muscleKg = muscleKg ?: musclePct?.let { kg * it / 100 },
            waterPct = waterKg?.let { it / kg * 100 },
            bmrKcal = bmrKcal,
            impedance = impedance,
        )
    }

    companion object {
        val SERVICE = uuid16(0x181D)
        val MEASUREMENT = uuid16(0x2A9D)
        val BODY_SERVICE = uuid16(0x181B)
        val BODY_MEASUREMENT = uuid16(0x2A9C)

        /** Flags bit 0: 0 = SI (0.005 kg), 1 = imperial (0.01 lb). 0xFFFF = measurement unsuccessful. */
        fun parse(data: ByteArray): Double? {
            if (data.size < 3) return null
            val raw = u16le(data, 1)
            if (raw == 0xFFFF) return null
            val imperial = (u8(data[0]) and 0x01) != 0
            val kg = if (imperial) raw * 0.01 / 2.20462262 else raw * 0.005
            return kg.takeIf(::plausible)
        }

        /** Body Composition Measurement: uint16 flags, body fat (0.1 %), then optional fields in flag order. */
        fun parseBody(data: ByteArray): Body? {
            if (data.size < 4) return null
            val flags = u16le(data, 0)
            val imperial = flags and 0x0001 != 0
            fun mass(raw: Int) = if (imperial) raw * 0.01 / 2.20462262 else raw * 0.005
            val fatRaw = u16le(data, 2)
            if (fatRaw == 0xFFFF) return null
            var i = 4
            fun next(): Int? = if (i + 1 < data.size) u16le(data, i).also { i += 2 } else null
            if (flags and 0x0002 != 0) i += 7 // timestamp
            if (flags and 0x0004 != 0) i += 1 // user id
            val bmr = if (flags and 0x0008 != 0) next()?.let { it / 4.184 } else null // kJ → kcal
            val musclePct = if (flags and 0x0010 != 0) next()?.let { it * 0.1 } else null
            val muscleKg = if (flags and 0x0020 != 0) next()?.let(::mass) else null
            val ffm = if (flags and 0x0040 != 0) next()?.let(::mass) else null
            if (flags and 0x0080 != 0) next() // soft lean mass (unused)
            val water = if (flags and 0x0100 != 0) next()?.let(::mass) else null
            val impedance = if (flags and 0x0200 != 0) next()?.let { it * 0.1 } else null
            val weight = if (flags and 0x0400 != 0) next()?.let(::mass) else null
            return Body(fatRaw * 0.1, bmr, musclePct, muscleKg, ffm, water, impedance, weight?.takeIf(::plausible))
        }
    }
}

/**
 * QN / Yolanda vendor protocol used by many Renpho, FITINDEX and other low-cost scales
 * (service 0xFFE0 "type 1" or 0xFFF0 "type 2"). Frames: [opcode, length, protocolType, ..., checksum].
 */
class QnProtocol(private val hasT1: Boolean, private val hasT2: Boolean) : ScaleProtocol {
    override val displayName = "QN / Renpho"
    override val subscriptions = listOf(SERVICE_T1 to T1_NOTIFY, SERVICE_T1 to T1_INDICATE, SERVICE_T2 to T2_NOTIFY)

    private var protocolType: Byte = 0
    private var scaleFactor = 100.0
    private var configured = false
    private var published = false

    private fun target(t1Char: UUID): Pair<UUID, UUID> = if (hasT2) SERVICE_T2 to T2_WRITE else SERVICE_T1 to t1Char

    private fun frame(vararg bytes: Int): ByteArray {
        val out = ByteArray(bytes.size + 1)
        bytes.forEachIndexed { i, b -> out[i] = b.toByte() }
        out[out.lastIndex] = sum8(out, out.lastIndex)
        return out
    }

    private fun write(t1Char: UUID, value: ByteArray): ScaleEvent.Write {
        val (s, c) = target(t1Char)
        return ScaleEvent.Write(GattWrite(s, c, value))
    }

    private fun qnSeconds(nowMillis: Long) = (nowMillis / 1000L - QN_EPOCH_OFFSET).toInt()

    override fun onNotification(characteristic: UUID, data: ByteArray, nowMillis: Long): List<ScaleEvent> {
        if (characteristic != T1_NOTIFY && characteristic != T2_NOTIFY) return emptyList()
        if (data.size < 3) return emptyList()
        if (protocolType == 0.toByte()) protocolType = data[2]
        val pt = u8(protocolType)
        return when (u8(data[0])) {
            0x12 -> {
                if (data.size > 10) scaleFactor = if (data[10].toInt() == 1) 100.0 else 10.0
                if (configured) return emptyList()
                configured = true
                val t = qnSeconds(nowMillis)
                val time = byteArrayOf(0x02, t.toByte(), (t ushr 8).toByte(), (t ushr 16).toByte(), (t ushr 24).toByte())
                listOf(
                    // Unit config: 0x01 = kg (the app converts for display).
                    write(T1_WRITE_CONFIG, frame(0x13, 0x09, pt, 0x01, 0x10, 0x00, 0x00, 0x00)),
                    write(T1_WRITE_TIME, time),
                )
            }
            0x14 -> {
                val t = qnSeconds(nowMillis)
                listOf(write(T1_WRITE_CONFIG, frame(0x20, 0x08, pt, t and 0xFF, (t ushr 8) and 0xFF, (t ushr 16) and 0xFF, (t ushr 24) and 0xFF)))
            }
            0x21 -> listOf(
                write(T1_WRITE_CONFIG, frame(0xA0, 0x0D, 0x04, 0xFE, 0, 0, 0, 0, 0, 0, 0, 0)),
                write(T1_WRITE_CONFIG, frame(0xA0, 0x0D, 0x02, 0x01, 0x00, 0x08, 0x00, 0x21, 0x06, 0xB8, 0x04, 0x02)),
            )
            0x10 -> parseWeight(data)
            else -> emptyList()
        }
    }

    private fun parseWeight(data: ByteArray): List<ScaleEvent> {
        if (data.size < 10) return emptyList()
        // Two layouts: classic [3..4]=weight, [5]=stable; newer (ES-30M) [4]=state, [5..6]=weight.
        val altLayout = u8(data[4]) <= 0x02 && scaleFactor == 10.0 && data.size >= 11
        val stable: Boolean
        val raw: Int
        val r1: Int
        if (altLayout) {
            stable = u8(data[4]) == 0x02 || u8(data[4]) == 0x01
            raw = u16be(data, 5)
            r1 = u16be(data, 7)
        } else {
            stable = data[5].toInt() == 1
            raw = u16be(data, 3)
            r1 = u16be(data, 6)
        }
        var kg = raw / scaleFactor
        if (!plausible(kg)) kg /= 10.0
        if (!plausible(kg)) return emptyList()
        if (!stable) return listOf(ScaleEvent.Live(kg))
        if (published) return emptyList()
        published = true
        return listOf(ScaleEvent.Stable(kg, impedance = BodyCompositionMath.normalizeImpedance(r1.toDouble())))
    }

    companion object {
        /** QN epoch: seconds since 2000-01-01 (as used by the scale firmware). */
        const val QN_EPOCH_OFFSET = 946_702_800L
        val SERVICE_T1 = uuid16(0xFFE0)
        val T1_NOTIFY = uuid16(0xFFE1)
        val T1_INDICATE = uuid16(0xFFE2)
        val T1_WRITE_CONFIG = uuid16(0xFFE3)
        val T1_WRITE_TIME = uuid16(0xFFE4)
        val SERVICE_T2 = uuid16(0xFFF0)
        val T2_NOTIFY = uuid16(0xFFF1)
        val T2_WRITE = uuid16(0xFFF2)
    }
}

/**
 * "Lefu" protocol of the Renpho ES-CS20M / ES-32MD family (service 0x1A10).
 * Frames: 0x55 0xAA cmd 0x00 len payload... checksum. Weight frames (0x14) stream while
 * standing on the scale; a 0x11 frame with payload 0 marks the end of the measurement.
 */
class LefuProtocol : ScaleProtocol {
    override val displayName = "Renpho ES-CS20M"
    override val subscriptions = listOf(SERVICE to RESULTS)

    private var lastKg: Double? = null
    private var published = false

    override fun onReady(nowMillis: Long): List<GattWrite> {
        val ts = (nowMillis / 1000L).toInt()
        val payload = byteArrayOf(0x01, 0x00, 0x00, (ts ushr 24).toByte(), (ts ushr 16).toByte(), (ts ushr 8).toByte(), ts.toByte(), 0x01, 0x06)
        return listOf(GattWrite(SERVICE, COMMAND, buildFrame(0x97, payload)))
    }

    override fun onNotification(characteristic: UUID, data: ByteArray, nowMillis: Long): List<ScaleEvent> {
        if (characteristic != RESULTS || data.size < 3 || u8(data[0]) != 0x55) return emptyList()
        return when (u8(data[2])) {
            0x14 -> {
                if (data.size < 12) return emptyList()
                val kg = u16be(data, 8) / 100.0
                if (!plausible(kg)) return emptyList()
                lastKg = kg
                listOf(ScaleEvent.Live(kg))
            }
            0x18 -> {
                if (data.size < 12) return emptyList()
                val kg = u16be(data, 10) / 100.0
                if (plausible(kg)) lastKg = kg
                finish()
            }
            0x11 -> if (data.size >= 6 && data[5].toInt() == 0) finish() else emptyList()
            else -> emptyList()
        }
    }

    private fun finish(): List<ScaleEvent> {
        val kg = lastKg ?: return emptyList()
        if (published) return emptyList()
        published = true
        return listOf(ScaleEvent.Stable(kg))
    }

    companion object {
        val SERVICE = uuid16(0x1A10)
        val RESULTS = uuid16(0x2A10)
        val COMMAND = uuid16(0x2A11)

        fun buildFrame(cmd: Int, payload: ByteArray): ByteArray {
            val f = ByteArray(5 + payload.size + 1)
            f[0] = 0x55
            f[1] = 0xAA.toByte()
            f[2] = cmd.toByte()
            f[3] = 0x00
            f[4] = payload.size.toByte()
            payload.copyInto(f, 5)
            f[f.lastIndex] = sum8(f, f.lastIndex)
            return f
        }
    }
}

/**
 * Fallback for protocols that only stream live weights: a reading is considered stable once
 * it stays within 0.1 kg for [windowMillis].
 */
class StabilityDetector(private val windowMillis: Long = 2_500) {
    private var anchorKg = 0.0
    private var anchorTime = 0L
    private var hasAnchor = false
    private var fired = false

    fun onLive(kg: Double, nowMillis: Long): Double? {
        if (fired) return null
        if (!hasAnchor || kotlin.math.abs(kg - anchorKg) > 0.1) {
            hasAnchor = true
            anchorKg = kg
            anchorTime = nowMillis
            return null
        }
        if (nowMillis - anchorTime >= windowMillis) {
            fired = true
            return kg
        }
        return null
    }
}

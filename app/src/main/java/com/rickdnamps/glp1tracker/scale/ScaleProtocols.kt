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
    /** How long to wait after the last live reading before keeping it as the result. */
    val liveFallbackMs: Long get() = 4_000

    /** Writes to send once all subscriptions are active. */
    fun onReady(nowMillis: Long): List<GattWrite> = emptyList()
    fun onNotification(characteristic: UUID, data: ByteArray, nowMillis: Long): List<ScaleEvent>

    companion object {
        /** Picks a protocol from the services discovered on the device (most specific first). */
        /** [displayLb]: unit the scale's own display should use (it follows the app's setting). */
        fun forServices(services: Set<UUID>, profile: ScaleUserProfile? = null, displayLb: Boolean = false): ScaleProtocol? = when {
            LefuProtocol.SERVICE in services -> LefuProtocol(profile)
            QnProtocol.SERVICE_T1 in services || QnProtocol.SERVICE_T2 in services -> QnProtocol(
                hasT1 = QnProtocol.SERVICE_T1 in services,
                hasT2 = QnProtocol.SERVICE_T2 in services,
                displayLb = displayLb,
            )
            StandardWeightScale.SERVICE in services || StandardWeightScale.BODY_SERVICE in services ->
                StandardWeightScale(hasBodyComposition = StandardWeightScale.BODY_SERVICE in services)
            else -> null
        }

        /** Heuristic used in scan results to flag likely compatible scales. */
        fun looksCompatible(name: String?, advertisedServices: Collection<UUID>): Boolean {
            val n = name?.lowercase().orEmpty()
            val known = setOf(LefuProtocol.SERVICE, QnProtocol.SERVICE_T1, QnProtocol.SERVICE_T2, StandardWeightScale.SERVICE, uuid16(0x181B))
            // Some ES-32MD / ES-CS20M units advertise a "113360_…" placeholder name.
            return advertisedServices.any { it in known } || n.startsWith("113360_") ||
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
    private var pendingBody: Body? = null
    private var done = false

    // Composition is computed after the weight on many scales: give it time.
    override val liveFallbackMs: Long get() = if (hasBodyComposition) 10_000 else 4_000

    override fun onNotification(characteristic: UUID, data: ByteArray, nowMillis: Long): List<ScaleEvent> {
        if (done) return emptyList()
        return when (characteristic) {
            MEASUREMENT -> {
                val kg = parse(data) ?: return emptyList()
                weightKg = kg
                val body = pendingBody
                when {
                    body != null -> { done = true; listOf(ScaleEvent.Stable(kg, body.toComposition(kg), body.impedance)) }
                    hasBodyComposition -> listOf(ScaleEvent.Live(kg))
                    else -> { done = true; listOf(ScaleEvent.Stable(kg)) }
                }
            }
            BODY_MEASUREMENT -> {
                val body = parseBody(data) ?: return emptyList()
                val kg = body.weightKg ?: weightKg
                if (kg == null) {
                    // Composition arrived before the weight: keep it until the weight comes.
                    pendingBody = body
                    return emptyList()
                }
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
class QnProtocol(private val hasT1: Boolean, private val hasT2: Boolean, private val displayLb: Boolean = false) : ScaleProtocol {
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
                    // Unit config sets the scale's own display (0x01 kg, 0x02 lb); weights are always sent in kg.
                    write(T1_WRITE_CONFIG, frame(0x13, 0x09, pt, if (displayLb) 0x02 else 0x01, 0x10, 0x00, 0x00, 0x00)),
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

/** Profile sent to scales that measure body composition (bioimpedance). */
data class ScaleUserProfile(
    val male: Boolean,
    val birthYear: Int,
    val heightCm: Double,
    /** Last known weight; the scale needs it to accept the profile (0 = unknown). */
    val refWeightKg: Double,
    /** Birth month/day; mid-year when only the year is known. */
    val birthMonth: Int = 7,
    val birthDay: Int = 1,
)

/**
 * "Lefu" protocol of the Renpho ES-CS20M / ES-32MD family (service 0x1A10).
 * Frames: 0x55 0xAA cmd 0x00 len payload... checksum. Long frames are split in two
 * notifications prefixed 0xAD (first) / 0xAF (continuation) + 2 bytes.
 *
 * Weight only: subscribe, send 0x97; 0x14 frames stream the weight, 0x11 with payload 0 ends.
 * With a [profile] the scale also measures impedance: 0x97 → ack 0x17 → profile 0x96 → ack 0x16
 * (accepted) → start BIA 0x90 → interim 0x19 (answer 0x99 + 0x96) → final 0x18 with resistance.
 */
class LefuProtocol(private val profile: ScaleUserProfile? = null) : ScaleProtocol {
    override val displayName = "Renpho ES-CS20M"
    override val subscriptions = listOf(SERVICE to RESULTS)

    /** Bioimpedance takes a few seconds after the weight settles. */
    override val liveFallbackMs: Long get() = if (profile != null) 12_000 else 4_000

    private enum class Phase { WAIT_PROFILE_ACK, WAIT_WEIGHT_FOR_PROFILE, WAIT_HIST_ACK, MEASURING }

    private var phase = Phase.WAIT_PROFILE_ACK
    private var lastKg: Double? = null
    private var fragment: ByteArray? = null
    private var published = false

    override fun onReady(nowMillis: Long): List<GattWrite> {
        val ts = (nowMillis / 1000L).toInt()
        val payload = byteArrayOf(0x01, 0x00, 0x00, (ts ushr 24).toByte(), (ts ushr 16).toByte(), (ts ushr 8).toByte(), ts.toByte(), 0x01, 0x06)
        return listOf(command(0x97, payload))
    }

    private fun command(cmd: Int, payload: ByteArray) = GattWrite(SERVICE, COMMAND, buildFrame(cmd, payload))

    private fun write(cmd: Int, payload: ByteArray) = ScaleEvent.Write(command(cmd, payload))

    override fun onNotification(characteristic: UUID, data: ByteArray, nowMillis: Long): List<ScaleEvent> {
        if (characteristic != RESULTS || data.isEmpty()) return emptyList()
        return when (u8(data[0])) {
            FRAG_FIRST -> {
                fragment = data.copyOfRange(minOf(3, data.size), data.size)
                emptyList()
            }
            FRAG_CONT -> {
                val first = fragment ?: return emptyList()
                fragment = null
                onFrame(first + data.copyOfRange(minOf(3, data.size), data.size))
            }
            else -> onFrame(data)
        }
    }

    private fun onFrame(data: ByteArray): List<ScaleEvent> {
        if (data.size < 3 || u8(data[0]) != 0x55) return emptyList()
        return when (u8(data[2])) {
            0x14 -> {
                if (data.size < 12) return emptyList()
                val kg = u16be(data, 8) / 100.0
                if (!plausible(kg)) return emptyList()
                lastKg = kg
                val p = profile
                if (phase == Phase.WAIT_WEIGHT_FOR_PROFILE && p != null) {
                    // No previous weigh-in (new install): the scale rejects a profile without a
                    // reference weight, so send it as soon as the first real weight arrives.
                    phase = Phase.WAIT_HIST_ACK
                    return listOf(ScaleEvent.Live(kg), write(0x96, profilePayload(p, (kg * 100).toInt())))
                }
                listOf(ScaleEvent.Live(kg))
            }
            0x17 -> {
                val p = profile
                if (phase != Phase.WAIT_PROFILE_ACK || p == null) return emptyList()
                val reference = p.refWeightKg.takeIf { plausible(it) } ?: lastKg
                if (reference == null) {
                    phase = Phase.WAIT_WEIGHT_FOR_PROFILE
                    return emptyList()
                }
                phase = Phase.WAIT_HIST_ACK
                listOf(write(0x96, profilePayload(p, (reference * 100).toInt())))
            }
            0x16 -> {
                if (phase != Phase.WAIT_HIST_ACK) return emptyList()
                phase = Phase.MEASURING
                // Accepted (byte 5 = 1): arm the impedance measurement. Rejected: weight only.
                if (data.size >= 6 && u8(data[5]) == 0x01) listOf(write(0x90, byteArrayOf(0x01, 0x00, 0x01, 0x00))) else emptyList()
            }
            0x19 -> {
                val p = profile ?: return emptyList()
                val interim = if (data.size >= 13) u16be(data, 11) else ((lastKg ?: 0.0) * 100).toInt()
                listOf(write(0x99, byteArrayOf(0x01)), write(0x96, profilePayload(p, interim)))
            }
            0x18 -> {
                if (data.size < 12) return emptyList()
                val kg = u16be(data, 10) / 100.0
                if (plausible(kg)) lastKg = kg
                val r1 = if (data.size >= 14) u16be(data, 12) else 0
                val saves = profile?.let { p ->
                    val save = write(0x96, profilePayload(p, ((lastKg ?: 0.0) * 100).toInt()))
                    listOf(save, save)
                }.orEmpty()
                saves + finish(BodyCompositionMath.normalizeImpedance(r1.toDouble()))
            }
            0x11 -> if (data.size >= 6 && data[5].toInt() == 0) finish(null) else emptyList()
            else -> emptyList()
        }
    }

    private fun finish(impedance: Double?): List<ScaleEvent> {
        val kg = lastKg ?: return emptyList()
        if (published) return emptyList()
        published = true
        return listOf(ScaleEvent.Stable(kg, impedance = impedance))
    }

    companion object {
        val SERVICE = uuid16(0x1A10)
        val RESULTS = uuid16(0x2A10)
        val COMMAND = uuid16(0x2A11)
        private const val FRAG_FIRST = 0xAD
        private const val FRAG_CONT = 0xAF

        /**
         * 0x96 user profile: sex (0x11 male / 0x21 female), birth year (BE16), month, day,
         * height in mm (BE16), 0, 0, reference weight in 0.01 kg (BE16), mode (0xAA standard), 0x01, 0x05.
         */
        fun profilePayload(p: ScaleUserProfile, weightRaw: Int): ByteArray {
            val heightMm = (p.heightCm * 10).toInt().coerceIn(0, 0xFFFF)
            val w = weightRaw.coerceIn(0, 0xFFFF)
            val year = p.birthYear
            return byteArrayOf(
                (if (p.male) 0x11 else 0x21).toByte(),
                (year ushr 8).toByte(), year.toByte(),
                p.birthMonth.coerceIn(1, 12).toByte(), p.birthDay.coerceIn(1, 31).toByte(),
                (heightMm ushr 8).toByte(), heightMm.toByte(),
                0x00, 0x00,
                (w ushr 8).toByte(), w.toByte(),
                0xAA.toByte(), 0x01, 0x05,
            )
        }

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

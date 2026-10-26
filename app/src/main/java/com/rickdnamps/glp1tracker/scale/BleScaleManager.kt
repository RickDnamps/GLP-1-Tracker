package com.rickdnamps.glp1tracker.scale

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.rickdnamps.glp1tracker.domain.BodyComp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

data class FoundScale(val address: String, val name: String, val rssi: Int, val compatible: Boolean)

sealed interface ScaleState {
    data object Idle : ScaleState
    /**
     * Scanning. [waitingFor] = remembered scale (connects as soon as it wakes up); otherwise the first
     * compatible scale is picked automatically unless [manual]. [seen] counts nearby Bluetooth devices.
     */
    data class Scanning(
        val devices: List<FoundScale>,
        val waitingFor: String?,
        val manual: Boolean = false,
        val seen: Int = 0,
    ) : ScaleState
    data class Connecting(val name: String, val attempt: Int = 1) : ScaleState
    data class Measuring(val name: String, val protocol: String, val liveKg: Double?) : ScaleState
    data class Done(
        val name: String,
        val address: String,
        val kg: Double,
        val composition: BodyComp? = null,
        val impedance: Double? = null,
    ) : ScaleState
    data class Error(val reason: Reason) : ScaleState

    enum class Reason { BLUETOOTH_OFF, NO_PERMISSION, UNSUPPORTED, CONNECTION_LOST, SCAN_FAILED, TIMEOUT }
}

/**
 * Talks to Bluetooth LE bathroom scales. All Bluetooth callbacks are funnelled to the main
 * thread; GATT operations are queued because Android allows only one in flight at a time.
 * Everything is written to [log] so a failing scale can be diagnosed from a copy of it.
 */
@SuppressLint("MissingPermission") // every entry point checks hasPermissions() first
class BleScaleManager(context: Context) {
    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val adapter get() = appContext.getSystemService(BluetoothManager::class.java)?.adapter

    private val _state = MutableStateFlow<ScaleState>(ScaleState.Idle)
    val state: StateFlow<ScaleState> = _state.asStateFlow()

    private val _log = MutableStateFlow<List<String>>(emptyList())
    /** Technical log of the current session (scan, connection, frames), newest last. */
    val log: StateFlow<List<String>> = _log.asStateFlow()

    /** Profile for scales measuring body composition (sex, birth date, height, last weight). */
    var userProfile: ScaleUserProfile? = null

    /** Keep the scale's display in pounds when the app uses pounds. */
    var displayLb: Boolean = false

    private var gatt: BluetoothGatt? = null
    private var protocol: ScaleProtocol? = null
    private var stability = StabilityDetector()
    private var deviceName = ""
    private var deviceAddress = ""
    private val found = linkedMapOf<String, FoundScale>()
    private val seen = hashSetOf<String>()
    private var targetAddress: String? = null
    private var targetName: String? = null
    private var autoPick = true
    private var scanning = false
    private val scanStarts = ArrayDeque<Long>()
    private var connectAttempt = 0
    private val queue = ArrayDeque<() -> Boolean>()
    private var busy = false
    private var lastLiveKg: Double? = null
    private var steadyKg: Double? = null

    fun hasPermissions(): Boolean = requiredPermissions().all {
        ContextCompat.checkSelfPermission(appContext, it) == PackageManager.PERMISSION_GRANTED
    }

    fun isBluetoothOn(): Boolean = adapter?.isEnabled == true

    /**
     * Scans for scales. With [address] it waits for that scale and connects as soon as it wakes up;
     * otherwise it connects to the first compatible scale it sees, unless [manualPick].
     */
    fun start(address: String?, manualPick: Boolean = false, savedName: String? = null) {
        main.removeCallbacks(delayedStop)
        main.removeCallbacks(delayedStart)
        stop()
        _log.value = emptyList()
        log("Start: Android ${Build.VERSION.SDK_INT}, ${Build.MANUFACTURER} ${Build.MODEL}, saved scale=${address ?: "none"}, " +
            "profile=${if (userProfile != null) "yes" else "no"}, lb=$displayLb")
        if (!hasPermissions()) {
            log("Missing permission")
            _state.value = ScaleState.Error(ScaleState.Reason.NO_PERMISSION)
            return
        }
        if (adapter?.isEnabled != true) {
            log("Bluetooth off")
            _state.value = ScaleState.Error(ScaleState.Reason.BLUETOOTH_OFF)
            return
        }
        targetAddress = address
        targetName = savedName
        autoPick = !manualPick
        found.clear()
        seen.clear()
        _state.value = ScaleState.Scanning(emptyList(), address, manualPick)
        // Android silently ignores a 6th scan started within 30 s: wait instead of failing quietly.
        val now = System.currentTimeMillis()
        while (scanStarts.isNotEmpty() && now - scanStarts.first() > 30_000) scanStarts.removeFirst()
        if (scanStarts.size >= 5) {
            val wait = 30_000 - (now - scanStarts.first()) + 500
            log("Too many scans in 30 s, starting in ${wait / 1000} s")
            main.postDelayed(delayedStart, wait)
        } else {
            startScan()
        }
    }

    private val delayedStart = Runnable { if (_state.value is ScaleState.Scanning) startScan() }

    private fun startScan() {
        val scanner = adapter?.takeIf { it.isEnabled }?.bluetoothLeScanner
        if (scanner == null) {
            _state.value = ScaleState.Error(ScaleState.Reason.BLUETOOTH_OFF)
            return
        }
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        runCatching { scanner.startScan(null, settings, scanCallback) }
            .onSuccess {
                scanStarts.addLast(System.currentTimeMillis())
                scanning = true
                log("Scanning…")
                main.postDelayed(scanTimeout, SCAN_TIMEOUT_MS)
            }
            .onFailure {
                log("startScan failed: ${it.message}")
                _state.value = ScaleState.Error(ScaleState.Reason.SCAN_FAILED)
            }
    }

    fun connect(address: String, name: String) {
        main.removeCallbacks(delayedStop)
        stopScan()
        deviceName = name
        deviceAddress = address
        connectAttempt = 0
        openGatt()
    }

    private fun openGatt() {
        main.removeCallbacks(liveFallback)
        main.removeCallbacks(steadyFinish)
        main.removeCallbacks(connectTimeout)
        gatt?.let { runCatching { it.disconnect(); it.close() } }
        gatt = null
        queue.clear()
        busy = false
        lastLiveKg = null
        steadyKg = null
        protocol = null
        stability = StabilityDetector()
        val device = runCatching { adapter?.getRemoteDevice(deviceAddress) }.getOrNull()
        if (device == null) {
            _state.value = ScaleState.Error(ScaleState.Reason.CONNECTION_LOST)
            return
        }
        connectAttempt++
        log("Connect $deviceName $deviceAddress (attempt $connectAttempt/$MAX_ATTEMPTS)")
        _state.value = ScaleState.Connecting(deviceName, connectAttempt)
        main.postDelayed(connectTimeout, CONNECT_TIMEOUT_MS)
        gatt = device.connectGatt(appContext, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    /** Retries the connection while the scale is probably still awake; false when out of attempts. */
    private fun retry(why: String): Boolean {
        if (connectAttempt >= MAX_ATTEMPTS) return false
        log("$why → retrying")
        main.postDelayed({ if (_state.value is ScaleState.Connecting || _state.value is ScaleState.Measuring) openGatt() }, 700)
        _state.value = ScaleState.Connecting(deviceName, connectAttempt + 1)
        return true
    }

    private val connectTimeout = Runnable {
        if (_state.value is ScaleState.Connecting) {
            log("Connection timeout")
            gatt?.let { runCatching { it.disconnect(); it.close() } }
            gatt = null
            if (!retry("Timeout")) _state.value = ScaleState.Error(ScaleState.Reason.CONNECTION_LOST)
        }
    }

    fun stop() {
        stopScan()
        main.removeCallbacks(delayedStart)
        main.removeCallbacks(connectTimeout)
        main.removeCallbacks(liveFallback)
        main.removeCallbacks(steadyFinish)
        queue.clear()
        busy = false
        gatt?.let { runCatching { it.disconnect(); it.close() } }
        gatt = null
        if (_state.value !is ScaleState.Done) _state.value = ScaleState.Idle
    }

    fun reset() {
        stop()
        _state.value = ScaleState.Idle
    }

    private val delayedStop = Runnable { stop() }

    /** Stops searching after a while so a forgotten sheet does not drain the battery. */
    private val scanTimeout = Runnable {
        if (scanning) {
            stopScan()
            log("Scan timeout: ${seen.size} devices seen, none connected")
            _state.value = ScaleState.Error(ScaleState.Reason.TIMEOUT)
        }
    }

    private fun stopScan() {
        main.removeCallbacks(scanTimeout)
        if (scanning) runCatching { adapter?.bluetoothLeScanner?.stopScan(scanCallback) }
        scanning = false
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            main.post { onScan(result) }
        }

        override fun onScanFailed(errorCode: Int) {
            main.post {
                log("Scan failed: error $errorCode")
                _state.value = ScaleState.Error(ScaleState.Reason.SCAN_FAILED)
            }
        }
    }

    private fun onScan(result: ScanResult) {
        if (!scanning) return
        val address = result.device.address
        val record = result.scanRecord
        val name = record?.deviceName ?: runCatching { result.device.name }.getOrNull()
        val services = record?.serviceUuids?.map { it.uuid }.orEmpty()
        val compatible = ScaleProtocol.looksCompatible(name, services)
        val target = targetAddress
        if (seen.add(address) && (compatible || address == target || !name.isNullOrBlank())) {
            log(
                "Seen ${name ?: "(no name)"} $address ${result.rssi} dBm" +
                    (if (services.isNotEmpty()) " services=" + services.joinToString(",") { shortUuid(it) } else "") +
                    (if (compatible || address == target) " adv=" + hex(record?.bytes, 62) else "") +
                    (if (compatible) " [compatible]" else "")
            )
        }
        if (target != null) {
            if (address == target) {
                log("Saved scale found")
                connect(address, name ?: targetName ?: address)
            } else if (compatible && name != null && name == targetName) {
                // Some scales change their Bluetooth address: same model name is good enough.
                log("Saved scale found under a new address")
                connect(address, name)
            } else {
                publishScanning()
            }
            return
        }
        if (!name.isNullOrBlank() || compatible) {
            found[address] = FoundScale(address, name ?: address, result.rssi, compatible)
        }
        if (autoPick && compatible) {
            log("Auto-selecting ${name ?: address}")
            connect(address, name ?: address)
            return
        }
        publishScanning()
    }

    private fun publishScanning() {
        val current = _state.value as? ScaleState.Scanning ?: return
        _state.value = current.copy(
            devices = found.values.sortedWith(compareByDescending<FoundScale> { it.compatible }.thenByDescending { it.rssi }),
            seen = seen.size,
        )
    }

    // --- GATT ---

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            main.post {
                if (g != gatt) return@post
                log("Connection state=${stateName(newState)} status=$status")
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    main.removeCallbacks(connectTimeout)
                    // Some scales drop the link if discovery starts too early.
                    main.postDelayed({
                        if (g == gatt) {
                            log("Discovering services")
                            if (!g.discoverServices()) log("discoverServices() refused")
                        }
                    }, 400)
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    runCatching { g.close() }
                    gatt = null
                    onDisconnected()
                }
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            main.post { if (g == gatt) onServices(g, status) }
        }

        override fun onDescriptorWrite(g: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            main.post {
                if (status != BluetoothGatt.GATT_SUCCESS) log("Subscribe ${shortUuid(descriptor.characteristic.uuid)} failed: status $status")
                next()
            }
        }

        override fun onCharacteristicWrite(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            main.post {
                if (status != BluetoothGatt.GATT_SUCCESS) log("Write ${shortUuid(characteristic.uuid)} failed: status $status")
                next()
            }
        }

        override fun onCharacteristicChanged(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            val copy = value.copyOf()
            main.post { onData(characteristic.uuid, copy) }
        }

        @Deprecated("Used before Android 13")
        override fun onCharacteristicChanged(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            @Suppress("DEPRECATION")
            val copy = characteristic.value?.copyOf() ?: return
            main.post { onData(characteristic.uuid, copy) }
        }
    }

    private fun onDisconnected() {
        main.removeCallbacks(connectTimeout)
        main.removeCallbacks(liveFallback)
        main.removeCallbacks(steadyFinish)
        queue.clear()
        busy = false
        if (_state.value is ScaleState.Done) return
        // The user stepped off before the final frame: keep the steady weight rather than failing.
        val kept = steadyKg
        if (kept != null) {
            log("Disconnected after a steady reading: keeping ${kept} kg")
            finish(kept)
            return
        }
        if (!retry("Disconnected")) _state.value = ScaleState.Error(ScaleState.Reason.CONNECTION_LOST)
    }

    private fun onServices(g: BluetoothGatt, status: Int) {
        log(
            "Services (status $status): " + g.services.joinToString(" ") { sv ->
                shortUuid(sv.uuid) + "[" + sv.characteristics.joinToString(",") { c -> shortUuid(c.uuid) + ":" + props(c.properties) } + "]"
            }
        )
        val services = g.services.map { it.uuid }.toSet()
        if (services.isEmpty() && retry("No services")) return
        val p = ScaleProtocol.forServices(services, userProfile, displayLb)
        if (p == null) {
            log("No supported protocol")
            stop()
            _state.value = ScaleState.Error(ScaleState.Reason.UNSUPPORTED)
            return
        }
        log("Protocol: ${p.displayName}")
        protocol = p
        _state.value = ScaleState.Measuring(deviceName, p.displayName, null)
        p.subscriptions.forEach { (s, c) ->
            val ch = g.getService(s)?.getCharacteristic(c) ?: return@forEach
            enqueue { enableNotifications(g, ch) }
        }
        p.onReady(System.currentTimeMillis()).forEach { w -> enqueue { write(g, w) } }
        next()
    }

    private fun onData(uuid: UUID, data: ByteArray) {
        log("← ${shortUuid(uuid)} ${hex(data)}")
        val p = protocol ?: return
        val g = gatt ?: return
        for (event in p.onNotification(uuid, data, System.currentTimeMillis())) {
            when (event) {
                is ScaleEvent.Write -> enqueue { write(g, event.write) }
                is ScaleEvent.Live -> {
                    lastLiveKg = event.kg
                    _state.value = ScaleState.Measuring(deviceName, p.displayName, event.kg)
                    // A steady weight is only a fallback: the protocol's own final frame carries the
                    // body-composition data, so give it time before keeping the steady value.
                    stability.onLive(event.kg, System.currentTimeMillis())?.let { kg ->
                        steadyKg = kg
                        log("Steady at $kg kg")
                        main.postDelayed(steadyFinish, p.liveFallbackMs)
                    }
                    // If the stream stops without a final packet, keep the reading.
                    main.removeCallbacks(liveFallback)
                    main.postDelayed(liveFallback, p.liveFallbackMs)
                }
                is ScaleEvent.Stable -> finish(event.kg, event.composition, event.impedance)
            }
        }
        if (!busy) next()
    }

    private val liveFallback = Runnable { (steadyKg ?: lastLiveKg)?.let { log("No final frame: keeping last reading"); finish(it) } }
    private val steadyFinish = Runnable { steadyKg?.let { log("No final frame after steady weight"); finish(it) } }

    private fun finish(kg: Double, composition: BodyComp? = null, impedance: Double? = null) {
        if (_state.value is ScaleState.Done) return
        main.removeCallbacks(liveFallback)
        main.removeCallbacks(steadyFinish)
        log("Result: $kg kg" + (impedance?.let { ", impedance $it Ω" } ?: "") + (if (composition != null) ", composition" else ""))
        _state.value = ScaleState.Done(deviceName, deviceAddress, kg, composition, impedance)
        // Give the scale a moment to receive pending acknowledgements before disconnecting.
        main.postDelayed(delayedStop, 1_500)
    }

    private fun enqueue(op: () -> Boolean) {
        queue.addLast(op)
    }

    /** Runs the next queued GATT operation; ops returning false (not started) are skipped. */
    private fun next() {
        busy = false
        while (queue.isNotEmpty()) {
            val op = queue.removeFirst()
            if (runCatching(op).getOrDefault(false)) {
                busy = true
                return
            }
        }
    }

    private fun enableNotifications(g: BluetoothGatt, ch: BluetoothGattCharacteristic): Boolean {
        log("Subscribe ${shortUuid(ch.uuid)}")
        if (!g.setCharacteristicNotification(ch, true)) return false
        val cccd = ch.getDescriptor(CCCD) ?: return false
        val value = if (ch.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) {
            BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
        } else {
            BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        }
        return if (Build.VERSION.SDK_INT >= 33) {
            g.writeDescriptor(cccd, value) == BluetoothGatt.GATT_SUCCESS
        } else {
            @Suppress("DEPRECATION")
            cccd.value = value
            @Suppress("DEPRECATION")
            g.writeDescriptor(cccd)
        }
    }

    private fun write(g: BluetoothGatt, w: GattWrite): Boolean {
        val ch = g.getService(w.service)?.getCharacteristic(w.characteristic) ?: return false
        log("→ ${shortUuid(w.characteristic)} ${hex(w.value)}")
        val type = if (ch.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) {
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        } else {
            BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        }
        return if (Build.VERSION.SDK_INT >= 33) {
            g.writeCharacteristic(ch, w.value, type) == BluetoothGatt.GATT_SUCCESS
        } else {
            ch.writeType = type
            @Suppress("DEPRECATION")
            ch.value = w.value
            @Suppress("DEPRECATION")
            g.writeCharacteristic(ch)
        }
    }

    private fun log(message: String) {
        val time = LocalTime.now().format(TIME)
        _log.value = (_log.value + "$time $message").takeLast(MAX_LOG_LINES)
    }

    companion object {
        private val CCCD: UUID = uuid16(0x2902)
        private const val SCAN_TIMEOUT_MS = 180_000L
        private const val CONNECT_TIMEOUT_MS = 12_000L
        private const val MAX_ATTEMPTS = 3
        private const val MAX_LOG_LINES = 400
        private val TIME = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")

        fun requiredPermissions(): Array<String> =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
            }

        private fun hex(bytes: ByteArray?, max: Int = 64): String {
            if (bytes == null) return "-"
            val shown = bytes.take(max).joinToString(" ") { "%02X".format(it.toInt() and 0xFF) }
            return if (bytes.size > max) "$shown …" else shown
        }

        /** 0000xxxx-0000-1000-8000-00805f9b34fb → "xxxx"; other UUIDs unchanged. */
        private fun shortUuid(u: UUID): String {
            val s = u.toString()
            return if (s.endsWith("-0000-1000-8000-00805f9b34fb") && s.startsWith("0000")) s.substring(4, 8) else s
        }

        private fun props(p: Int): String = buildString {
            if (p and BluetoothGattCharacteristic.PROPERTY_READ != 0) append('R')
            if (p and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) append('W')
            if (p and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0) append('w')
            if (p and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) append('N')
            if (p and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) append('I')
        }

        private fun stateName(s: Int) = when (s) {
            BluetoothProfile.STATE_CONNECTED -> "connected"
            BluetoothProfile.STATE_CONNECTING -> "connecting"
            BluetoothProfile.STATE_DISCONNECTED -> "disconnected"
            BluetoothProfile.STATE_DISCONNECTING -> "disconnecting"
            else -> s.toString()
        }
    }
}

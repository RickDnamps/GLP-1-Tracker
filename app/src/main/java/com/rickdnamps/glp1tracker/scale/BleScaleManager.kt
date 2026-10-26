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
import java.util.UUID

data class FoundScale(val address: String, val name: String, val rssi: Int, val compatible: Boolean)

sealed interface ScaleState {
    data object Idle : ScaleState
    /** Scanning; [devices] is filled when choosing a new scale, empty while waiting for a known one. */
    data class Scanning(val devices: List<FoundScale>, val waitingFor: String?) : ScaleState
    data class Connecting(val name: String) : ScaleState
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
 */
@SuppressLint("MissingPermission") // every entry point checks hasPermissions() first
class BleScaleManager(context: Context) {
    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val adapter get() = appContext.getSystemService(BluetoothManager::class.java)?.adapter

    private val _state = MutableStateFlow<ScaleState>(ScaleState.Idle)
    val state: StateFlow<ScaleState> = _state.asStateFlow()

    private var gatt: BluetoothGatt? = null
    private var protocol: ScaleProtocol? = null
    private var stability = StabilityDetector()
    private var deviceName = ""
    private var deviceAddress = ""
    private val found = linkedMapOf<String, FoundScale>()
    private var targetAddress: String? = null
    private var scanning = false
    private val queue = ArrayDeque<() -> Boolean>()
    private var busy = false

    fun hasPermissions(): Boolean = requiredPermissions().all {
        ContextCompat.checkSelfPermission(appContext, it) == PackageManager.PERMISSION_GRANTED
    }

    fun isBluetoothOn(): Boolean = adapter?.isEnabled == true

    /** Scans for scales. With [address] it waits for that scale and connects as soon as it wakes up. */
    fun start(address: String?) {
        main.removeCallbacks(delayedStop)
        stop()
        if (!hasPermissions()) { _state.value = ScaleState.Error(ScaleState.Reason.NO_PERMISSION); return }
        val scanner = adapter?.takeIf { it.isEnabled }?.bluetoothLeScanner
        if (scanner == null) { _state.value = ScaleState.Error(ScaleState.Reason.BLUETOOTH_OFF); return }
        targetAddress = address
        found.clear()
        _state.value = ScaleState.Scanning(emptyList(), address)
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        runCatching { scanner.startScan(null, settings, scanCallback) }
            .onSuccess {
                scanning = true
                main.postDelayed(scanTimeout, SCAN_TIMEOUT_MS)
            }
            .onFailure { _state.value = ScaleState.Error(ScaleState.Reason.SCAN_FAILED) }
    }

    fun connect(address: String, name: String) {
        main.removeCallbacks(delayedStop)
        main.removeCallbacks(liveFallback)
        lastLiveKg = null
        stopScan()
        val device = runCatching { adapter?.getRemoteDevice(address) }.getOrNull() ?: return
        deviceName = name
        deviceAddress = address
        protocol = null
        stability = StabilityDetector()
        _state.value = ScaleState.Connecting(name)
        gatt = device.connectGatt(appContext, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    fun stop() {
        stopScan()
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
            main.post { _state.value = ScaleState.Error(ScaleState.Reason.SCAN_FAILED) }
        }
    }

    private fun onScan(result: ScanResult) {
        if (!scanning) return
        val address = result.device.address
        val name = result.scanRecord?.deviceName ?: runCatching { result.device.name }.getOrNull()
        val target = targetAddress
        if (target != null) {
            if (address == target) connect(address, name ?: address)
            return
        }
        if (name.isNullOrBlank()) return
        val services = result.scanRecord?.serviceUuids?.map { it.uuid }.orEmpty()
        found[address] = FoundScale(address, name, result.rssi, ScaleProtocol.looksCompatible(name, services))
        _state.value = ScaleState.Scanning(
            found.values.sortedWith(compareByDescending<FoundScale> { it.compatible }.thenByDescending { it.rssi }),
            null,
        )
    }

    // --- GATT ---

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            main.post {
                if (g != gatt) return@post
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    g.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    g.close()
                    gatt = null
                    if (_state.value !is ScaleState.Done) _state.value = ScaleState.Error(ScaleState.Reason.CONNECTION_LOST)
                }
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            main.post { if (g == gatt) onServices(g) }
        }

        override fun onDescriptorWrite(g: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            main.post { next() }
        }

        override fun onCharacteristicWrite(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            main.post { next() }
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

    private fun onServices(g: BluetoothGatt) {
        val services = g.services.map { it.uuid }.toSet()
        val p = ScaleProtocol.forServices(services)
        if (p == null) {
            stop()
            _state.value = ScaleState.Error(ScaleState.Reason.UNSUPPORTED)
            return
        }
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
        val p = protocol ?: return
        val g = gatt ?: return
        for (event in p.onNotification(uuid, data, System.currentTimeMillis())) {
            when (event) {
                is ScaleEvent.Write -> enqueue { write(g, event.write) }
                is ScaleEvent.Live -> {
                    lastLiveKg = event.kg
                    _state.value = ScaleState.Measuring(deviceName, p.displayName, event.kg)
                    stability.onLive(event.kg, System.currentTimeMillis())?.let { finish(it) }
                    // If the stream stops without a "stable" packet, keep the last reading.
                    main.removeCallbacks(liveFallback)
                    main.postDelayed(liveFallback, p.liveFallbackMs)
                }
                is ScaleEvent.Stable -> finish(event.kg, event.composition, event.impedance)
            }
        }
        if (!busy) next()
    }

    private var lastLiveKg: Double? = null
    private val liveFallback = Runnable { lastLiveKg?.let { finish(it) } }

    private fun finish(kg: Double, composition: BodyComp? = null, impedance: Double? = null) {
        if (_state.value is ScaleState.Done) return
        main.removeCallbacks(liveFallback)
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

    companion object {
        private val CCCD: UUID = uuid16(0x2902)
        private const val SCAN_TIMEOUT_MS = 120_000L

        fun requiredPermissions(): Array<String> =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
            }
    }
}

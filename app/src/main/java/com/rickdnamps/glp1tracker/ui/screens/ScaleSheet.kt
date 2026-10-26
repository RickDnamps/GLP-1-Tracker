package com.rickdnamps.glp1tracker.ui.screens

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.automirrored.outlined.BluetoothSearching
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Scale
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rickdnamps.glp1tracker.R
import com.rickdnamps.glp1tracker.data.UserSettings
import com.rickdnamps.glp1tracker.scale.BleScaleManager
import com.rickdnamps.glp1tracker.scale.FoundScale
import com.rickdnamps.glp1tracker.scale.ScaleState
import com.rickdnamps.glp1tracker.ui.AppViewModel
import com.rickdnamps.glp1tracker.ui.weightText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScaleSheet(vm: AppViewModel, settings: UserSettings, onDismiss: () -> Unit) {
    val scale = vm.scale
    val state by scale.state.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val savedAddress = settings.scaleAddress.ifBlank { null }

    val startScan = { scale.start(savedAddress) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        startScan()
    }
    val enableBluetooth = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        startScan()
    }

    LaunchedEffect(Unit) {
        if (scale.hasPermissions()) startScan() else permissionLauncher.launch(BleScaleManager.requiredPermissions())
    }
    DisposableEffect(Unit) { onDispose { scale.reset() } }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Scale, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.scale_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            }

            when (val s = state) {
                ScaleState.Idle -> CircularProgressIndicator()
                is ScaleState.Scanning -> if (s.waitingFor != null) {
                    Icon(Icons.AutoMirrored.Outlined.BluetoothSearching, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(
                        stringResource(R.string.scale_step_on, settings.scaleName.ifBlank { s.waitingFor }),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    TextButton(onClick = { scale.start(null) }) { Text(stringResource(R.string.scale_choose_other)) }
                } else {
                    Text(stringResource(R.string.scale_pick), style = MaterialTheme.typography.bodyLarge)
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    if (s.devices.isEmpty()) {
                        Text(
                            stringResource(R.string.scale_wake_hint) +
                                if (android.os.Build.VERSION.SDK_INT <= 30) "\n" + stringResource(R.string.scale_location_hint) else "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Column(Modifier.fillMaxWidth()) {
                        s.devices.forEach { d -> DeviceRow(d) { scale.connect(d.address, d.name) } }
                    }
                }
                is ScaleState.Connecting -> {
                    CircularProgressIndicator()
                    Text(stringResource(R.string.scale_connecting, s.name))
                }
                is ScaleState.Measuring -> {
                    Text(
                        s.liveKg?.let { weightText(it, settings) } ?: "—",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(stringResource(R.string.scale_measuring, s.name), style = MaterialTheme.typography.bodyMedium)
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
                is ScaleState.Done -> {
                    Icon(Icons.Outlined.CheckCircle, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(
                        weightText(s.kg, settings),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = startScan) { Text(stringResource(R.string.scale_again)) }
                        Button(onClick = { vm.saveScaleWeight(s.kg, s.address, s.name); onDismiss() }) {
                            Text(stringResource(R.string.save))
                        }
                    }
                }
                is ScaleState.Error -> {
                    Icon(Icons.Outlined.ErrorOutline, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
                    Text(
                        stringResource(
                            when (s.reason) {
                                ScaleState.Reason.BLUETOOTH_OFF -> R.string.scale_err_bt_off
                                ScaleState.Reason.NO_PERMISSION -> R.string.scale_err_permission
                                ScaleState.Reason.UNSUPPORTED -> R.string.scale_err_unsupported
                                ScaleState.Reason.CONNECTION_LOST -> R.string.scale_err_lost
                                ScaleState.Reason.SCAN_FAILED -> R.string.scale_err_scan
                                ScaleState.Reason.TIMEOUT -> R.string.scale_err_timeout
                            }
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    when (s.reason) {
                        ScaleState.Reason.BLUETOOTH_OFF -> Button(onClick = {
                            runCatching { enableBluetooth.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)) }
                        }) { Text(stringResource(R.string.scale_enable_bt)) }
                        ScaleState.Reason.NO_PERMISSION -> Button(onClick = {
                            permissionLauncher.launch(BleScaleManager.requiredPermissions())
                        }) { Text(stringResource(R.string.allow)) }
                        else -> Button(onClick = startScan) { Text(stringResource(R.string.retry)) }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            HorizontalDivider()
            Text(
                stringResource(R.string.scale_supported),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (savedAddress != null) {
                TextButton(onClick = { vm.forgetScale(); scale.start(null) }) {
                    Text(stringResource(R.string.scale_forget, settings.scaleName.ifBlank { savedAddress }))
                }
            }
        }
    }
}

@Composable
private fun DeviceRow(device: FoundScale, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Bluetooth, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(device.name, style = MaterialTheme.typography.bodyLarge)
            Text(device.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (device.compatible) {
            AssistChip(onClick = onClick, label = { Text(stringResource(R.string.scale_compatible)) })
        }
    }
}

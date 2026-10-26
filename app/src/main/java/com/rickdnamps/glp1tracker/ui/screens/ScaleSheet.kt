package com.rickdnamps.glp1tracker.ui.screens

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
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
import com.rickdnamps.glp1tracker.scale.ScaleUserProfile
import androidx.compose.runtime.remember
import com.rickdnamps.glp1tracker.ui.AppViewModel
import com.rickdnamps.glp1tracker.ui.weightText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScaleSheet(vm: AppViewModel, settings: UserSettings, onDismiss: () -> Unit) {
    val scale = vm.scale
    val state by scale.state.collectAsStateWithLifecycle()
    val log by scale.log.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val savedAddress = settings.scaleAddress.ifBlank { null }
    var showDevices by rememberSaveable { mutableStateOf(false) }
    var showLog by rememberSaveable { mutableStateOf(false) }

    // Profile lets body-composition scales (e.g. Renpho ES-CS20M) measure impedance.
    val profile = remember(settings) {
        val male = when (settings.sex) { 1 -> true; 2 -> false; else -> null }
        if (male != null && settings.birthYear > 0 && settings.heightCm > 0) {
            val born = settings.birthDateOrNull
            ScaleUserProfile(
                male, settings.birthYear, settings.heightCm, vm.state.value.weights.lastOrNull()?.weightKg ?: 0.0,
                birthMonth = born?.monthValue ?: 7, birthDay = born?.dayOfMonth ?: 1,
            )
        } else null
    }
    val startScan: (Boolean) -> Unit = { manual ->
        scale.userProfile = profile
        scale.displayLb = settings.weightUnit == com.rickdnamps.glp1tracker.domain.WeightUnit.LB
        scale.start(if (manual) null else savedAddress, manualPick = manual, savedName = settings.scaleName.ifBlank { null })
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        startScan(false)
    }
    val enableBluetooth = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        startScan(false)
    }

    LaunchedEffect(Unit) {
        if (scale.hasPermissions()) startScan(false) else permissionLauncher.launch(BleScaleManager.requiredPermissions())
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
                is ScaleState.Scanning -> if (s.manual) {
                    Text(stringResource(R.string.scale_pick), style = MaterialTheme.typography.bodyLarge)
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    SeenCount(s.seen)
                    Column(Modifier.fillMaxWidth()) {
                        s.devices.forEach { d -> DeviceRow(d) { scale.connect(d.address, d.name) } }
                    }
                } else {
                    PulsingIcon()
                    Text(
                        stringResource(R.string.scale_step_now),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        if (s.waitingFor != null) stringResource(R.string.scale_searching_for, settings.scaleName.ifBlank { s.waitingFor })
                        else stringResource(R.string.scale_searching),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                    )
                    SeenCount(s.seen)
                    HowToCard()
                    if (s.waitingFor != null) {
                        TextButton(onClick = { startScan(true) }) { Text(stringResource(R.string.scale_choose_other)) }
                    } else {
                        TextButton(onClick = { showDevices = !showDevices }) { Text(stringResource(R.string.scale_not_listed)) }
                        if (showDevices) {
                            if (s.devices.isEmpty()) {
                                Text(
                                    stringResource(R.string.scale_wake_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Column(Modifier.fillMaxWidth()) {
                                s.devices.forEach { d -> DeviceRow(d) { scale.connect(d.address, d.name) } }
                            }
                        }
                    }
                    if (android.os.Build.VERSION.SDK_INT <= 30) {
                        Text(
                            stringResource(R.string.scale_location_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                is ScaleState.Connecting -> {
                    CircularProgressIndicator()
                    Text(stringResource(R.string.scale_connecting, s.name), style = MaterialTheme.typography.titleMedium)
                    if (s.attempt > 1) {
                        Text(
                            stringResource(R.string.scale_attempt, s.attempt, 3),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        stringResource(R.string.scale_stay_on),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                }
                is ScaleState.Measuring -> {
                    Text(
                        s.liveKg?.let { weightText(it, settings) } ?: "—",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(stringResource(R.string.scale_measuring, s.name), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                    Text(s.protocol, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        stringResource(if (profile != null) R.string.scale_barefoot else R.string.comp_profile_needed),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
                is ScaleState.Done -> {
                    // Preview of the composition that will be saved (scale values or impedance estimate).
                    val preview = s.composition ?: s.impedance?.let { imp ->
                        val sex = settings.sexOrNull
                        val age = settings.ageOrNull
                        if (sex != null && age != null && settings.heightCm > 0) {
                            com.rickdnamps.glp1tracker.domain.BodyCompositionMath.estimate(s.kg, settings.heightCm, age, sex, imp)
                        } else null
                    }
                    Icon(Icons.Outlined.CheckCircle, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(
                        weightText(s.kg, settings),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    preview?.let { c ->
                        val f = com.rickdnamps.glp1tracker.domain.Format
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            c.bodyFatPct?.let { Text(stringResource(R.string.comp_fat) + "\n" + f.oneDecimal(it) + " %", style = MaterialTheme.typography.bodyMedium) }
                            c.muscleKg?.let { Text(stringResource(R.string.comp_muscle) + "\n" + weightText(it, settings), style = MaterialTheme.typography.bodyMedium) }
                            c.waterPct?.let { Text(stringResource(R.string.comp_water) + "\n" + f.oneDecimal(it) + " %", style = MaterialTheme.typography.bodyMedium) }
                        }
                    }
                    if (s.composition == null && s.impedance != null && (settings.sexOrNull == null || settings.ageOrNull == null)) {
                        Text(
                            stringResource(R.string.comp_profile_needed),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { startScan(false) }) { Text(stringResource(R.string.scale_again)) }
                        Button(onClick = { vm.saveScaleWeight(s.kg, s.address, s.name, s.composition, s.impedance); onDismiss() }) {
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
                        textAlign = TextAlign.Center,
                    )
                    when (s.reason) {
                        ScaleState.Reason.BLUETOOTH_OFF -> Button(onClick = {
                            runCatching { enableBluetooth.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)) }
                        }) { Text(stringResource(R.string.scale_enable_bt)) }
                        ScaleState.Reason.NO_PERMISSION -> Button(onClick = {
                            permissionLauncher.launch(BleScaleManager.requiredPermissions())
                        }) { Text(stringResource(R.string.allow)) }
                        else -> Button(onClick = { startScan(false) }) { Text(stringResource(R.string.retry)) }
                    }
                    if (s.reason == ScaleState.Reason.CONNECTION_LOST || s.reason == ScaleState.Reason.TIMEOUT) OtherAppTip()
                }
            }

            Spacer(Modifier.height(4.dp))
            HorizontalDivider()
            Text(
                stringResource(R.string.scale_hc_tip),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(R.string.scale_supported),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (savedAddress != null) {
                TextButton(onClick = { vm.forgetScale(); startScan(true) }) {
                    Text(stringResource(R.string.scale_forget, settings.scaleName.ifBlank { savedAddress }))
                }
            }
            TextButton(onClick = { showLog = !showLog }) {
                Icon(if (showLog) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.scale_tech))
            }
            if (showLog) TechLog(log)
        }
    }
}

/** The three things to do, shown while searching. */
@Composable
private fun HowToCard() {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(R.string.scale_how_1, R.string.scale_how_2, R.string.scale_how_3).forEachIndexed { i, res ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("${i + 1}", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelLarge)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(res), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
        }
    }
    OtherAppTip()
}

/** Scales accept a single connection: the maker's app grabs it first when it runs. */
@Composable
private fun OtherAppTip() {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Info, null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
            Spacer(Modifier.width(10.dp))
            Text(
                stringResource(R.string.scale_tip_other_app),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
}

@Composable
private fun SeenCount(seen: Int) {
    if (seen > 0) {
        Text(
            pluralStringResource(R.plurals.scale_seen, seen, seen),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PulsingIcon() {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val scaleFactor by pulse.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "scale",
    )
    Box(
        Modifier
            .size(84.dp)
            .graphicsLayer { scaleX = scaleFactor; scaleY = scaleFactor }
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.AutoMirrored.Outlined.BluetoothSearching,
            null,
            Modifier.size(44.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

/** Raw log to copy and send when a scale misbehaves. */
@Composable
private fun TechLog(lines: List<String>) {
    val clipboard = LocalClipboardManager.current
    val text = lines.joinToString("\n")
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.scale_tech_help),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(onClick = { clipboard.setText(AnnotatedString(text)) }, enabled = lines.isNotEmpty()) {
            Icon(Icons.Outlined.ContentCopy, null)
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.copy_log))
        }
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                lines.takeLast(60).joinToString("\n").ifEmpty { "—" },
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(10.dp),
            )
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

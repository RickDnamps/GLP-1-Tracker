package com.rickdnamps.glp1tracker.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.rickdnamps.glp1tracker.ui.AppLock
import com.rickdnamps.glp1tracker.ui.findActivity
import com.rickdnamps.glp1tracker.ui.theme.Accent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Vaccines
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.rickdnamps.glp1tracker.R
import com.rickdnamps.glp1tracker.data.UserSettings
import com.rickdnamps.glp1tracker.domain.Format
import com.rickdnamps.glp1tracker.domain.Medication
import com.rickdnamps.glp1tracker.domain.Supply
import com.rickdnamps.glp1tracker.domain.WeightUnit
import com.rickdnamps.glp1tracker.health.HealthConnect
import com.rickdnamps.glp1tracker.notifications.Notifications
import com.rickdnamps.glp1tracker.notifications.ReminderScheduler
import com.rickdnamps.glp1tracker.ui.AppViewModel
import com.rickdnamps.glp1tracker.ui.UiState
import com.rickdnamps.glp1tracker.ui.components.BirthDateDialog
import com.rickdnamps.glp1tracker.ui.components.ChoiceDialog
import com.rickdnamps.glp1tracker.ui.components.ConfirmDialog
import com.rickdnamps.glp1tracker.ui.components.DatePickerDialogFor
import com.rickdnamps.glp1tracker.ui.components.HeightInputDialog
import com.rickdnamps.glp1tracker.ui.heightText
import com.rickdnamps.glp1tracker.domain.LengthUnit
import com.rickdnamps.glp1tracker.ui.components.NumberInputDialog
import com.rickdnamps.glp1tracker.ui.components.SectionCard
import com.rickdnamps.glp1tracker.ui.components.SettingRow
import com.rickdnamps.glp1tracker.ui.components.TextInputDialog
import com.rickdnamps.glp1tracker.ui.components.TimePickerDialogFor
import com.rickdnamps.glp1tracker.ui.medicationName
import com.rickdnamps.glp1tracker.ui.weightText
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.launch

private enum class SettingDialog {
    NAME, HEIGHT, START_WEIGHT, GOAL_WEIGHT, MEDICATION, DOSE, DOSE_CUSTOM, INTERVAL, INTERVAL_CUSTOM, DAY, TIME,
    CUSTOM_NAME, CUSTOM_HALF_LIFE, WATER_GOAL, STEP_GOAL, PROTEIN_GOAL, PEN_TOTAL, PEN_REMAINING,
    SEX, BIRTH_YEAR, REPORT_PERIOD, STOCK_PENS, DISCARD_DAYS, PEN_OPENED, WEIGH_IN_DAY, WEIGH_IN_TIME, CALORIE_GOAL, FIBER_GOAL,
}

private const val CUSTOM = -1.0

@Composable
fun SettingsScreen(
    state: UiState,
    vm: AppViewModel,
    contentPadding: PaddingValues,
    onOpenScale: () -> Unit,
    onOpenTitration: () -> Unit,
) {
    val s = state.settings
    val context = LocalContext.current
    var dialog by remember { mutableStateOf<SettingDialog?>(null) }
    var pendingRestore by remember { mutableStateOf<Uri?>(null) }

    val scope = rememberCoroutineScope()
    val healthConnect = vm.healthConnect
    var hcAvailable by remember { mutableStateOf(healthConnect.isAvailable()) }

    // Permission states are re-read whenever the screen resumes (user may come back from system settings).
    var canNotify by remember { mutableStateOf(Notifications.canPost(context)) }
    var canExact by remember { mutableStateOf(ReminderScheduler.canScheduleExact(context)) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            canNotify = Notifications.canPost(context)
            canExact = ReminderScheduler.canScheduleExact(context)
            hcAvailable = healthConnect.isAvailable()
            vm.reschedule() // the exact-alarm permission may have changed
        }
    }
    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { canNotify = it }

    val hcPermission = rememberLauncherForActivityResult(HealthConnect.permissionContract()) { granted ->
        if (granted.containsAll(healthConnect.permissions)) {
            vm.updateSettings { it.copy(healthConnectAutoImport = true) }
            vm.importFromHealthConnect(manual = true)
        }
    }

    val today = LocalDate.now()
    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let(vm::exportCsv)
    }
    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(vm::exportBackup)
    }
    var reportFrom by rememberSaveable { mutableStateOf(0L) }
    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri?.let { vm.exportDoctorReport(it, fromMillis = reportFrom) }
    }
    val folderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let(vm::setAutoBackupFolder)
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        pendingRestore = uri
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionCard(title = stringResource(R.string.profile), icon = Icons.Outlined.Person) {
                SettingRow(stringResource(R.string.first_name), s.name.ifBlank { "—" }) { dialog = SettingDialog.NAME }
                Text(stringResource(R.string.weight_unit), style = MaterialTheme.typography.bodyLarge)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    WeightUnit.entries.forEachIndexed { i, u ->
                        SegmentedButton(
                            selected = s.weightUnit == u,
                            onClick = { vm.updateSettings { it.copy(weightUnit = u) } },
                            shape = SegmentedButtonDefaults.itemShape(i, WeightUnit.entries.size),
                        ) { Text(stringResource(u.symbolRes)) }
                    }
                }
                Text(stringResource(R.string.length_unit), style = MaterialTheme.typography.bodyLarge)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf(LengthUnit.CM to R.string.unit_cm, LengthUnit.IN to R.string.unit_ft_in).forEachIndexed { i, (u, label) ->
                        SegmentedButton(
                            selected = s.lengthUnit == u,
                            onClick = { vm.updateSettings { it.copy(lengthUnitValue = u.ordinal) } },
                            shape = SegmentedButtonDefaults.itemShape(i, 2),
                        ) { Text(stringResource(label)) }
                    }
                }
                SettingRow(stringResource(R.string.height), if (s.heightCm > 0) heightText(s.heightCm, s) else "—") {
                    dialog = SettingDialog.HEIGHT
                }
                SettingRow(
                    stringResource(R.string.sex),
                    when (s.sex) { 1 -> stringResource(R.string.sex_male); 2 -> stringResource(R.string.sex_female); else -> "—" },
                ) { dialog = SettingDialog.SEX }
                SettingRow(
                    stringResource(R.string.birth_date),
                    when {
                        s.birthDateOrNull != null -> Format.date(s.birthDateOrNull!!) + " · " + stringResource(R.string.age_years, s.ageOrNull ?: 0)
                        s.birthYear > 0 -> s.birthYear.toString()
                        else -> stringResource(R.string.birth_date_tap)
                    },
                ) { dialog = SettingDialog.BIRTH_YEAR }
                SettingRow(
                    stringResource(R.string.start_weight),
                    if (s.startWeightKg > 0) weightText(s.startWeightKg, s) else stringResource(R.string.first_entry),
                ) { dialog = SettingDialog.START_WEIGHT }
                SettingRow(stringResource(R.string.goal_weight), if (s.goalWeightKg > 0) weightText(s.goalWeightKg, s) else "—") {
                    dialog = SettingDialog.GOAL_WEIGHT
                }
            }
        }
        item {
            SectionCard(title = stringResource(R.string.appearance), icon = Icons.Outlined.Palette) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf(R.string.theme_system, R.string.theme_light, R.string.theme_dark).forEachIndexed { i, label ->
                        SegmentedButton(
                            selected = s.themeMode == i,
                            onClick = { vm.updateSettings { it.copy(themeMode = i) } },
                            shape = SegmentedButtonDefaults.itemShape(i, 3),
                        ) { Text(stringResource(label), maxLines = 1) }
                    }
                }
                Text(stringResource(R.string.accent_color), style = MaterialTheme.typography.bodyLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Accent.entries.forEach { a ->
                        val selected = !s.dynamicColor && Accent.fromName(s.accent) == a
                        Box(
                            Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(a.light.first, a.light.third)))
                                .border(if (selected) 3.dp else 0.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                .clickable { vm.updateSettings { it.copy(accent = a.name, dynamicColor = false) } }
                                .semantics { contentDescription = context.getString(a.label) },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (selected) Icon(Icons.Filled.Check, null, tint = Color.White)
                        }
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    SwitchRow(stringResource(R.string.dynamic_color), s.dynamicColor) { on -> vm.updateSettings { it.copy(dynamicColor = on) } }
                }
            }
        }
        item {
            SectionCard(title = stringResource(R.string.privacy), icon = Icons.Outlined.Lock) {
                val lockAvailable = remember { AppLock.isAvailable(context) }
                val lockTitle = stringResource(R.string.lock_title)
                val lockSubtitle = stringResource(R.string.lock_enable_subtitle)
                val cancel = stringResource(R.string.cancel)
                SwitchRow(stringResource(R.string.app_lock), s.appLock) { on ->
                    if (!on) {
                        vm.updateSettings { it.copy(appLock = false) }
                    } else {
                        context.findActivity()?.let { activity ->
                            AppLock.prompt(activity, lockTitle, lockSubtitle, cancel) {
                                vm.updateSettings { it.copy(appLock = true) }
                            }
                        }
                    }
                }
                Text(
                    stringResource(if (lockAvailable) R.string.app_lock_help else R.string.app_lock_unavailable),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            SectionCard(title = stringResource(R.string.treatment), icon = Icons.Outlined.Vaccines) {
                SettingRow(stringResource(R.string.medication), medicationName(s.medication, s)) { dialog = SettingDialog.MEDICATION }
                if (s.medication == Medication.OTHER) {
                    SettingRow(stringResource(R.string.custom_med_name), s.customMedName.ifBlank { "—" }) { dialog = SettingDialog.CUSTOM_NAME }
                    SettingRow(stringResource(R.string.custom_half_life), Format.number(s.customHalfLifeHours, 1) + " h") {
                        dialog = SettingDialog.CUSTOM_HALF_LIFE
                    }
                }
                SettingRow(stringResource(R.string.current_dose), Format.dose(s.doseMg) + " mg") { dialog = SettingDialog.DOSE }
                SettingRow(stringResource(R.string.frequency), intervalLabel(s.intervalDays)) { dialog = SettingDialog.INTERVAL }
                if (s.intervalDays == 7) {
                    SettingRow(stringResource(R.string.dose_day), dayName(s.injectionDay)) { dialog = SettingDialog.DAY }
                }
                SettingRow(stringResource(R.string.reminder_time), Format.time(today.atTime(s.reminderTime))) { dialog = SettingDialog.TIME }
                SettingRow(
                    stringResource(R.string.titration_title),
                    stringResource(if (s.titrationEnabled) R.string.on else R.string.off),
                    onClick = onOpenTitration,
                )
            }
        }
        item {
            SectionCard(title = stringResource(R.string.reminders), icon = Icons.Outlined.Notifications) {
                SwitchRow(stringResource(R.string.dose_reminders), s.remindersEnabled) { on ->
                    vm.updateSettings { it.copy(remindersEnabled = on) }
                    if (on && !canNotify && Build.VERSION.SDK_INT >= 33) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                SwitchRow(stringResource(R.string.reminder_eve), s.doseEveReminder) { on -> vm.updateSettings { it.copy(doseEveReminder = on) } }
                SwitchRow(stringResource(R.string.reminder_weigh_in), s.weighInReminder) { on -> vm.updateSettings { it.copy(weighInReminder = on) } }
                if (s.weighInReminder) {
                    SettingRow(stringResource(R.string.weigh_in_day), dayName(s.weighInDay)) { dialog = SettingDialog.WEIGH_IN_DAY }
                    SettingRow(stringResource(R.string.weigh_in_time), Format.time(today.atTime(s.weighInHour, 0))) { dialog = SettingDialog.WEIGH_IN_TIME }
                }
                SwitchRow(stringResource(R.string.reminder_water), s.waterReminders) { on -> vm.updateSettings { it.copy(waterReminders = on) } }
                SwitchRow(stringResource(R.string.reminder_journal), s.journalReminder) { on -> vm.updateSettings { it.copy(journalReminder = on) } }
                if (!canNotify && Build.VERSION.SDK_INT >= 33) {
                    SettingRow(stringResource(R.string.allow_notifications), stringResource(R.string.allow)) {
                        notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                if (!canExact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    SettingRow(stringResource(R.string.allow_exact_alarms), stringResource(R.string.allow)) {
                        context.startActivity(
                            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + context.packageName))
                        )
                    }
                }
            }
        }
        item {
            SectionCard(title = stringResource(R.string.pen_title), icon = Icons.Outlined.Vaccines) {
                SwitchRow(stringResource(R.string.pen_tracking), s.penTrackingEnabled) { on ->
                    vm.updateSettings {
                        it.copy(
                            penTrackingEnabled = on,
                            penDiscardDays = if (on && it.penDiscardDays == 0) Supply.defaultDiscardDays(it.medication) else it.penDiscardDays,
                        )
                    }
                }
                if (s.penTrackingEnabled) {
                    SettingRow(stringResource(R.string.doses_per_pen), s.penDosesTotal.toString()) { dialog = SettingDialog.PEN_TOTAL }
                    SettingRow(stringResource(R.string.doses_remaining), s.penDosesRemaining.toString()) { dialog = SettingDialog.PEN_REMAINING }
                    SettingRow(stringResource(R.string.stock_pens_setting), s.stockPens.toString()) { dialog = SettingDialog.STOCK_PENS }
                    SettingRow(
                        stringResource(R.string.pen_opened_on),
                        s.penOpenedAt.takeIf { it > 0 }?.let { Format.date(it) } ?: "—",
                    ) { dialog = SettingDialog.PEN_OPENED }
                    SettingRow(
                        stringResource(R.string.pen_discard_after),
                        if (s.penDiscardDays > 0) stringResource(R.string.every_n_days_short, s.penDiscardDays) else "—",
                    ) { dialog = SettingDialog.DISCARD_DAYS }
                }
            }
        }
        item {
            SectionCard(title = stringResource(R.string.connections), icon = Icons.Outlined.MonitorHeart) {
                SettingRow(stringResource(R.string.scale_title), s.scaleName.ifBlank { stringResource(R.string.none) }, onClick = onOpenScale)
                if (s.scaleAddress.isNotBlank()) {
                    SettingRow(stringResource(R.string.scale_forget, s.scaleName.ifBlank { s.scaleAddress }), null) { vm.forgetScale() }
                }
                if (hcAvailable) {
                    SwitchRow(stringResource(R.string.hc_import), s.healthConnectAutoImport) { on ->
                        if (!on) {
                            vm.updateSettings { it.copy(healthConnectAutoImport = false) }
                        } else {
                            scope.launch {
                                if (runCatching { healthConnect.hasPermissions() }.getOrDefault(false)) {
                                    vm.updateSettings { it.copy(healthConnectAutoImport = true) }
                                    vm.importFromHealthConnect(manual = true)
                                } else {
                                    hcPermission.launch(healthConnect.requestedPermissions)
                                }
                            }
                        }
                    }
                    if (s.healthConnectAutoImport) {
                        SettingRow(stringResource(R.string.hc_import_now), null) { vm.importFromHealthConnect(manual = true) }
                    }
                } else {
                    SettingRow(stringResource(R.string.hc_unavailable), stringResource(R.string.install)) {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.apps.healthdata"))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }
                }
                Text(
                    stringResource(R.string.hc_help),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            SectionCard(title = stringResource(R.string.daily_goals), icon = Icons.Outlined.LocalDrink) {
                SettingRow(stringResource(R.string.water_goal), "${s.waterGoalMl} ml") { dialog = SettingDialog.WATER_GOAL }
                SettingRow(stringResource(R.string.step_goal), s.stepGoal.toString()) { dialog = SettingDialog.STEP_GOAL }
                SettingRow(stringResource(R.string.protein_goal), "${s.proteinGoalG} g") { dialog = SettingDialog.PROTEIN_GOAL }
                SettingRow(stringResource(R.string.calorie_goal), "${s.calorieGoal} kcal") { dialog = SettingDialog.CALORIE_GOAL }
                SettingRow(stringResource(R.string.fiber_goal), "${s.fiberGoalG} g") { dialog = SettingDialog.FIBER_GOAL }
            }
        }
        item {
            SectionCard(title = stringResource(R.string.data), icon = Icons.Outlined.Save) {
                SettingRow(stringResource(R.string.report_export), null) { dialog = SettingDialog.REPORT_PERIOD }
                SettingRow(stringResource(R.string.export_csv), null) { csvLauncher.launch("glp1-tracker-$today.csv") }
                SettingRow(stringResource(R.string.backup_json), null) { backupLauncher.launch("glp1-tracker-backup-$today.json") }
                SettingRow(
                    stringResource(R.string.auto_backup),
                    if (s.autoBackupUri.isBlank()) stringResource(R.string.off)
                    else s.lastAutoBackupAt.takeIf { it > 0 }?.let { Format.date(it) } ?: stringResource(R.string.on),
                ) { folderLauncher.launch(null) }
                if (s.autoBackupUri.isNotBlank()) {
                    if (System.currentTimeMillis() - s.lastAutoBackupAt > 14 * 86_400_000L) {
                        Text(
                            stringResource(R.string.auto_backup_stale),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    SettingRow(stringResource(R.string.auto_backup_off), null) { vm.disableAutoBackup() }
                }
                SettingRow(stringResource(R.string.restore_json), null) {
                    restoreLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/plain"))
                }
                Text(
                    stringResource(R.string.data_privacy),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            SectionCard(title = stringResource(R.string.about), icon = Icons.Outlined.Info) {
                val version = remember {
                    runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: ""
                }
                Text(stringResource(R.string.version, version), style = MaterialTheme.typography.bodyMedium)
                Text(
                    stringResource(R.string.disclaimer_long),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    SettingDialogs(dialog, s, vm, onDialog = { dialog = it }, onReport = { months ->
        reportFrom = if (months == 0) 0L else System.currentTimeMillis() - months * 30L * 86_400_000L
        pdfLauncher.launch("glp1-tracker-report-$today.pdf")
    })

    pendingRestore?.let { uri ->
        ConfirmDialog(
            title = stringResource(R.string.restore_json),
            text = stringResource(R.string.restore_confirm),
            confirmLabel = stringResource(R.string.restore),
            onDismiss = { pendingRestore = null },
            onConfirm = { vm.importBackup(uri); pendingRestore = null },
        )
    }
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
fun intervalLabel(days: Int): String = when (days) {
    1 -> stringResource(R.string.every_day)
    7 -> stringResource(R.string.every_week)
    14 -> stringResource(R.string.every_two_weeks)
    else -> stringResource(R.string.every_n_days, days)
}

fun dayName(day: DayOfWeek): String =
    day.getDisplayName(TextStyle.FULL, Locale.getDefault()).replaceFirstChar { it.titlecase(Locale.getDefault()) }

@Composable
private fun SettingDialogs(
    dialog: SettingDialog?,
    s: UserSettings,
    vm: AppViewModel,
    onDialog: (SettingDialog?) -> Unit,
    onReport: (months: Int) -> Unit,
) {
    val close = { onDialog(null) }
    val unit = stringResource(s.weightUnit.symbolRes)
    fun save(transform: (UserSettings) -> UserSettings) {
        vm.updateSettings(transform)
        onDialog(null)
    }
    when (dialog) {
        null -> Unit
        SettingDialog.NAME -> TextInputDialog(stringResource(R.string.first_name), s.name, close) { v -> save { it.copy(name = v) } }
        SettingDialog.HEIGHT -> HeightInputDialog(stringResource(R.string.height), s.heightCm, s.lengthUnit == LengthUnit.IN, close) { cm ->
            save { it.copy(heightCm = cm) }
        }
        SettingDialog.START_WEIGHT -> NumberInputDialog(
            stringResource(R.string.start_weight), s.weightUnit.fromKg(s.startWeightKg), unit, close,
            { v -> save { it.copy(startWeightKg = it.weightUnit.toKg(v)) } },
            supportingText = stringResource(R.string.start_weight_help),
        )
        SettingDialog.GOAL_WEIGHT -> NumberInputDialog(
            stringResource(R.string.goal_weight), s.weightUnit.fromKg(s.goalWeightKg), unit, close,
            { v -> save { it.copy(goalWeightKg = it.weightUnit.toKg(v)) } },
        )
        SettingDialog.MEDICATION -> ChoiceDialog(
            title = stringResource(R.string.medication),
            options = Medication.entries,
            selected = s.medication,
            label = { medicationName(it, s) },
            onDismiss = close,
            onSelect = { med ->
                save {
                    it.copy(
                        medication = med,
                        intervalDays = med.defaultIntervalDays,
                        penDiscardDays = Supply.defaultDiscardDays(med),
                        doseMg = if (it.doseMg in med.commonDosesMg || med.commonDosesMg.isEmpty()) it.doseMg else med.commonDosesMg.first(),
                    )
                }
            },
        )
        SettingDialog.DOSE -> {
            val options = s.medication.commonDosesMg + CUSTOM
            val customLabel = stringResource(R.string.other_value)
            ChoiceDialog(
                title = stringResource(R.string.current_dose),
                options = options,
                selected = s.doseMg.takeIf { it in s.medication.commonDosesMg },
                label = { if (it == CUSTOM) customLabel else Format.dose(it) + " mg" },
                onDismiss = close,
                onSelect = { d -> if (d == CUSTOM) onDialog(SettingDialog.DOSE_CUSTOM) else save { it.copy(doseMg = d) } },
            )
        }
        SettingDialog.DOSE_CUSTOM -> NumberInputDialog(stringResource(R.string.current_dose), s.doseMg, "mg", close, { v ->
            if (v > 0) save { it.copy(doseMg = v) }
        })
        SettingDialog.INTERVAL -> {
            val customLabel = stringResource(R.string.other_value)
            ChoiceDialog(
                title = stringResource(R.string.frequency),
                options = listOf(1, 7, 14, -1),
                selected = s.intervalDays.takeIf { it in listOf(1, 7, 14) },
                label = { if (it == -1) customLabel else intervalLabel(it) },
                onDismiss = close,
                onSelect = { d -> if (d == -1) onDialog(SettingDialog.INTERVAL_CUSTOM) else save { it.copy(intervalDays = d) } },
            )
        }
        SettingDialog.INTERVAL_CUSTOM -> NumberInputDialog(
            stringResource(R.string.frequency), s.intervalDays.toDouble(), stringResource(R.string.days_unit), close,
            { v -> if (v >= 1) save { it.copy(intervalDays = v.toInt()) } },
            allowDecimals = false,
        )
        SettingDialog.DAY -> ChoiceDialog(
            title = stringResource(R.string.dose_day),
            options = DayOfWeek.entries,
            selected = s.injectionDay,
            label = { dayName(it) },
            onDismiss = close,
            onSelect = { d -> save { it.copy(injectionDay = d) } },
        )
        SettingDialog.TIME -> TimePickerDialogFor(s.reminderTime, close) { t ->
            save { it.copy(reminderHour = t.hour, reminderMinute = t.minute) }
        }
        SettingDialog.CUSTOM_NAME -> TextInputDialog(stringResource(R.string.custom_med_name), s.customMedName, close) { v ->
            save { it.copy(customMedName = v) }
        }
        SettingDialog.CUSTOM_HALF_LIFE -> NumberInputDialog(
            stringResource(R.string.custom_half_life), s.customHalfLifeHours, "h", close,
            { v -> if (v > 0) save { it.copy(customHalfLifeHours = v) } },
            supportingText = stringResource(R.string.custom_half_life_help),
        )
        SettingDialog.WATER_GOAL -> NumberInputDialog(stringResource(R.string.water_goal), s.waterGoalMl.toDouble(), "ml", close, { v ->
            save { it.copy(waterGoalMl = v.toInt()) }
        }, allowDecimals = false)
        SettingDialog.STEP_GOAL -> NumberInputDialog(stringResource(R.string.step_goal), s.stepGoal.toDouble(), "", close, { v ->
            if (v.toInt() in 500..100_000) save { it.copy(stepGoal = v.toInt()) }
        }, allowDecimals = false)
        SettingDialog.PROTEIN_GOAL -> NumberInputDialog(stringResource(R.string.protein_goal), s.proteinGoalG.toDouble(), "g", close, { v ->
            save { it.copy(proteinGoalG = v.toInt()) }
        }, allowDecimals = false)
        SettingDialog.PEN_TOTAL -> NumberInputDialog(stringResource(R.string.doses_per_pen), s.penDosesTotal.toDouble(), "", close, { v ->
            if (v >= 1) save { it.copy(penDosesTotal = v.toInt(), penDosesRemaining = it.penDosesRemaining.coerceAtMost(v.toInt())) }
        }, allowDecimals = false)
        SettingDialog.SEX -> {
            val male = stringResource(R.string.sex_male)
            val female = stringResource(R.string.sex_female)
            ChoiceDialog(
                title = stringResource(R.string.sex),
                options = listOf(1, 2),
                selected = s.sex.takeIf { it > 0 },
                label = { if (it == 1) male else female },
                onDismiss = close,
                onSelect = { v -> save { it.copy(sex = v) } },
            )
        }
        SettingDialog.BIRTH_YEAR -> BirthDateDialog(
            initial = s.birthDateOrNull ?: s.birthYear.takeIf { it > 0 }?.let { LocalDate.of(it, 7, 1) },
            onDismiss = close,
        ) { d ->
            save { it.copy(birthYear = d.year, birthMonth = d.monthValue, birthDay = d.dayOfMonth) }
        }
        SettingDialog.REPORT_PERIOD -> {
            val labels = mapOf(
                3 to stringResource(R.string.report_period_3m),
                6 to stringResource(R.string.report_period_6m),
                12 to stringResource(R.string.report_period_12m),
                0 to stringResource(R.string.report_period_all),
            )
            ChoiceDialog(
                title = stringResource(R.string.report_export),
                options = listOf(3, 6, 12, 0),
                selected = null,
                label = { labels.getValue(it) },
                onDismiss = close,
                onSelect = { months -> onDialog(null); onReport(months) },
            )
        }
        SettingDialog.STOCK_PENS -> NumberInputDialog(stringResource(R.string.stock_pens_setting), s.stockPens.toDouble(), "", close, { v ->
            save { it.copy(stockPens = v.toInt()) }
        }, allowDecimals = false)
        SettingDialog.DISCARD_DAYS -> NumberInputDialog(stringResource(R.string.pen_discard_after), s.penDiscardDays.toDouble(), stringResource(R.string.days_unit), close, { v ->
            save { it.copy(penDiscardDays = v.toInt()) }
        }, allowDecimals = false, supportingText = stringResource(R.string.pen_discard_help))
        SettingDialog.PEN_OPENED -> DatePickerDialogFor(
            s.penOpenedAt.takeIf { it > 0 }?.let { Format.toLocalDateTime(it).toLocalDate() } ?: LocalDate.now(),
            close,
        ) { d -> save { it.copy(penOpenedAt = Format.toMillis(d.atTime(12, 0))) } }
        SettingDialog.WEIGH_IN_DAY -> ChoiceDialog(
            title = stringResource(R.string.weigh_in_day),
            options = DayOfWeek.entries,
            selected = s.weighInDay,
            label = { dayName(it) },
            onDismiss = close,
            onSelect = { d -> save { it.copy(weighInDayValue = d.value) } },
        )
        SettingDialog.WEIGH_IN_TIME -> TimePickerDialogFor(java.time.LocalTime.of(s.weighInHour, 0), close) { t ->
            save { it.copy(weighInHour = t.hour) }
        }
        SettingDialog.CALORIE_GOAL -> NumberInputDialog(stringResource(R.string.calorie_goal), s.calorieGoal.toDouble(), "kcal", close, { v ->
            save { it.copy(calorieGoal = v.toInt()) }
        }, allowDecimals = false)
        SettingDialog.FIBER_GOAL -> NumberInputDialog(stringResource(R.string.fiber_goal), s.fiberGoalG.toDouble(), "g", close, { v ->
            save { it.copy(fiberGoalG = v.toInt()) }
        }, allowDecimals = false)
        SettingDialog.PEN_REMAINING -> NumberInputDialog(stringResource(R.string.doses_remaining), s.penDosesRemaining.toDouble(), "", close, { v ->
            save { it.copy(penDosesRemaining = v.toInt().coerceAtMost(it.penDosesTotal)) }
        }, allowDecimals = false)
    }
}


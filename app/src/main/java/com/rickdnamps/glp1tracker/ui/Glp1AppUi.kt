package com.rickdnamps.glp1tracker.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Vaccines
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rickdnamps.glp1tracker.R
import com.rickdnamps.glp1tracker.notifications.Notifications
import com.rickdnamps.glp1tracker.ui.screens.Editor
import com.rickdnamps.glp1tracker.ui.screens.HomeScreen
import com.rickdnamps.glp1tracker.ui.screens.InjectionEditor
import com.rickdnamps.glp1tracker.ui.screens.InjectionsScreen
import com.rickdnamps.glp1tracker.ui.screens.JournalScreen
import com.rickdnamps.glp1tracker.ui.screens.OnboardingScreen
import com.rickdnamps.glp1tracker.ui.screens.ProgressScreen
import com.rickdnamps.glp1tracker.ui.screens.ScaleSheet
import com.rickdnamps.glp1tracker.ui.screens.SettingsScreen
import com.rickdnamps.glp1tracker.ui.screens.SideEffectEditor
import com.rickdnamps.glp1tracker.ui.screens.WeightEditor

private enum class Tab(val route: String, @StringRes val label: Int, val icon: ImageVector) {
    HOME("home", R.string.tab_home, Icons.Outlined.Home),
    DOSES("doses", R.string.tab_doses, Icons.Outlined.Vaccines),
    PROGRESS("progress", R.string.tab_progress, Icons.AutoMirrored.Outlined.TrendingDown),
    JOURNAL("journal", R.string.tab_journal, Icons.AutoMirrored.Outlined.EventNote),
    SETTINGS("settings", R.string.tab_settings, Icons.Outlined.Settings),
}

@Composable
fun Glp1AppUi(vm: AppViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    when {
        !state.loaded -> Box(Modifier.fillMaxSize())
        !state.settings.onboardingDone -> OnboardingScreen(onFinish = vm::completeOnboarding)
        else -> MainScaffold(state, vm)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScaffold(state: UiState, vm: AppViewModel) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentTab = Tab.entries.firstOrNull { tab -> backStack?.destination?.hierarchy?.any { it.route == tab.route } == true } ?: Tab.HOME
    val snackbar = remember { SnackbarHostState() }
    var editor by remember { mutableStateOf<Editor?>(null) }

    LaunchedEffect(Unit) {
        vm.messages.collect { snackbar.showSnackbar(context.getString(it)) }
    }
    LaunchedEffect(Unit) {
        vm.importedCounts.collect { count ->
            snackbar.showSnackbar(context.resources.getQuantityString(R.plurals.hc_imported, count, count))
        }
    }
    // Pull new weights from Health Connect (Renpho, Withings, Fitbit…) when the app opens.
    LaunchedEffect(Unit) {
        if (state.settings.healthConnectAutoImport) vm.importFromHealthConnect(manual = false)
    }

    // Ask once for the notification permission needed by dose reminders (Android 13+).
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 && state.settings.remindersEnabled && !Notifications.canPost(context)) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(if (currentTab == Tab.HOME) R.string.app_name else currentTab.label)) })
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            val fab: Pair<Int, Editor>? = when (currentTab) {
                Tab.DOSES -> R.string.add_dose to Editor.Injection()
                Tab.PROGRESS -> R.string.add_weight to Editor.Weight()
                Tab.JOURNAL -> R.string.add_side_effect to Editor.SideEffect()
                else -> null
            }
            if (fab != null) {
                ExtendedFloatingActionButton(
                    onClick = { editor = fab.second },
                    icon = { Icon(Icons.Outlined.Add, null) },
                    text = { Text(stringResource(fab.first)) },
                )
            }
        },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, null) },
                        label = { Text(stringResource(tab.label), maxLines = 1) },
                    )
                }
            }
        },
    ) { padding ->
        val open: (Editor) -> Unit = { editor = it }
        NavHost(navController, startDestination = Tab.HOME.route) {
            composable(Tab.HOME.route) { HomeScreen(state, vm, padding, open) }
            composable(Tab.DOSES.route) { InjectionsScreen(state, padding, open) }
            composable(Tab.PROGRESS.route) { ProgressScreen(state, padding, open) { vm.importFromHealthConnect(manual = true) } }
            composable(Tab.JOURNAL.route) { JournalScreen(state, vm, padding, open) }
            composable(Tab.SETTINGS.route) { SettingsScreen(state, vm, padding, onOpenScale = { editor = Editor.Scale }) }
        }
    }

    val close = { editor = null }
    when (val e = editor) {
        null -> Unit
        is Editor.Injection -> InjectionEditor(
            item = e.item,
            settings = state.settings,
            injections = state.injections,
            onDismiss = close,
            onSave = { vm.saveInjection(it); close() },
            onDelete = { vm.deleteInjection(it); close() },
        )
        is Editor.Weight -> WeightEditor(
            item = e.item,
            settings = state.settings,
            lastWeightKg = state.weights.lastOrNull()?.weightKg,
            onDismiss = close,
            onSave = { vm.saveWeight(it); close() },
            onDelete = { vm.deleteWeight(it); close() },
        )
        Editor.Scale -> ScaleSheet(vm, state.settings, onDismiss = close)
        is Editor.SideEffect -> SideEffectEditor(
            item = e.item,
            onDismiss = close,
            onSave = { vm.saveSideEffect(it); close() },
            onDelete = { vm.deleteSideEffect(it); close() },
        )
    }
}

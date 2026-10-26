package com.rickdnamps.glp1tracker

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rickdnamps.glp1tracker.ui.AppViewModel
import com.rickdnamps.glp1tracker.ui.Glp1AppUi
import com.rickdnamps.glp1tracker.ui.theme.Accent
import com.rickdnamps.glp1tracker.ui.theme.Glp1Theme

/** FragmentActivity (not ComponentActivity) because the biometric app lock needs it. */
class MainActivity : FragmentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            val s = state.settings
            Glp1Theme(themeMode = s.themeMode, accent = Accent.fromName(s.accent), dynamicColor = s.dynamicColor) {
                Glp1AppUi(viewModel)
            }
        }
    }
}

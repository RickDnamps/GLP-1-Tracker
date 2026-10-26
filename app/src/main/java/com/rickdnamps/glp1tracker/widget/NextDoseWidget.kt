package com.rickdnamps.glp1tracker.widget

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import com.rickdnamps.glp1tracker.Glp1App
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/** Home-screen widget: countdown to the next planned dose, with a "Done" button once it is due. */
class NextDoseWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(SMALL, SMALL_WIDE, MEDIUM))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = (context.applicationContext as Glp1App).repository
        // The first frame comes from a snapshot; the flows then keep it current while the Glance
        // session is alive (an update requested during a running session only recomposes, it does
        // not call provideGlance again).
        val initialSettings = repository.settingsRepository.current()
        val initialInjections = repository.injections.first()
        provideContent {
            val settings by repository.settings.collectAsState(initial = initialSettings)
            val injections by repository.injections.collectAsState(initial = initialInjections)
            val ui = remember(settings, injections) {
                NextDoseUi.from(context, settings, injections, LocalDate.now())
            }
            GlanceTheme(colors = WidgetTheme.colors) {
                NextDoseContent(ui)
            }
        }
    }

    internal companion object {
        /** 2x1: countdown + medication only. */
        val SMALL = DpSize(110.dp, 40.dp)

        /** Wide single row (wide 2x1, 3x1, 4x1): adds a compact "Done" button. */
        val SMALL_WIDE = DpSize(180.dp, 40.dp)

        /** 3x2 / 4x2 and up: full card. */
        val MEDIUM = DpSize(180.dp, 110.dp)
    }
}

class NextDoseWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NextDoseWidget()
}

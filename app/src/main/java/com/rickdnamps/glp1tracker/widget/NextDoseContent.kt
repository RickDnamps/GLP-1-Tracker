package com.rickdnamps.glp1tracker.widget

import android.content.Intent
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.ButtonDefaults
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionSendBroadcast
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.rickdnamps.glp1tracker.MainActivity
import com.rickdnamps.glp1tracker.R
import com.rickdnamps.glp1tracker.notifications.ReminderReceiver

private const val CHECK_MARK = "✓"

/** Teal card normally, error colours once the dose is late (like the Home card). */
private data class Palette(
    val container: ColorProvider,
    val content: ColorProvider,
    val button: ColorProvider,
    val onButton: ColorProvider,
)

@Composable
private fun palette(late: Boolean): Palette {
    val c = GlanceTheme.colors
    return if (late) {
        Palette(c.errorContainer, c.onErrorContainer, c.error, c.onError)
    } else {
        Palette(c.primaryContainer, c.onPrimaryContainer, c.primary, c.onPrimary)
    }
}

@Composable
internal fun NextDoseContent(ui: NextDoseUi) {
    val context = LocalContext.current
    val size = LocalSize.current
    val colors = palette(ui.isLate)
    // Same broadcast as the notification "Done" action: logs the planned dose if it is due.
    val markDone = actionSendBroadcast(
        Intent(context, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION_MARK_DONE)
    )
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(colors.container)
            .widgetCorners()
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        val medium = size.width >= NextDoseWidget.MEDIUM.width && size.height >= NextDoseWidget.MEDIUM.height
        when {
            !ui.configured -> SetupLayout(colors)
            medium -> MediumLayout(ui, colors, markDone)
            else -> SmallLayout(
                ui = ui,
                colors = colors,
                markDone = markDone,
                showDone = ui.canMarkDone && size.width >= NextDoseWidget.SMALL_WIDE.width,
            )
        }
    }
}

/** System widget radius on Android 12+; Glance ignores corner radius on older versions. */
private fun GlanceModifier.widgetCorners(): GlanceModifier =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        cornerRadius(android.R.dimen.system_app_widget_background_radius)
    } else {
        this
    }

/** 3x2 / 4x2: title, big countdown, date, medication, site and a full-width "Done" button. */
@Composable
private fun MediumLayout(ui: NextDoseUi, colors: Palette, markDone: Action) {
    val context = LocalContext.current
    Column(modifier = GlanceModifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                provider = ImageProvider(R.drawable.ic_notification),
                contentDescription = null,
                modifier = GlanceModifier.size(16.dp),
                colorFilter = ColorFilter.tint(colors.content),
            )
            Spacer(GlanceModifier.width(6.dp))
            Text(
                text = context.getString(R.string.widget_name),
                style = TextStyle(color = colors.content, fontSize = 13.sp, fontWeight = FontWeight.Medium),
                maxLines = 1,
            )
        }
        Spacer(GlanceModifier.height(4.dp))
        Text(
            text = ui.countdown,
            style = TextStyle(color = colors.content, fontSize = 24.sp, fontWeight = FontWeight.Bold),
            maxLines = 2,
        )
        // Details take whatever height is left, so a short widget never pushes the button out.
        Column(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
            Text(
                text = ui.dueText,
                style = TextStyle(color = colors.content, fontSize = 13.sp),
                maxLines = 2,
            )
            Spacer(GlanceModifier.height(6.dp))
            Text(
                text = ui.doseText,
                style = TextStyle(color = colors.content, fontSize = 13.sp, fontWeight = FontWeight.Medium),
                maxLines = 1,
            )
            ui.siteText?.let {
                Text(
                    text = it,
                    style = TextStyle(color = colors.content, fontSize = 12.sp),
                    maxLines = 1,
                )
            }
        }
        if (ui.canMarkDone) {
            Spacer(GlanceModifier.height(8.dp))
            Button(
                text = context.getString(R.string.widget_done),
                onClick = markDone,
                modifier = GlanceModifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(backgroundColor = colors.button, contentColor = colors.onButton),
                maxLines = 1,
            )
        }
    }
}

/** 2x1 and single rows: countdown + medication, and a round check button when it fits. */
@Composable
private fun SmallLayout(ui: NextDoseUi, colors: Palette, markDone: Action, showDone: Boolean) {
    val context = LocalContext.current
    Row(
        modifier = GlanceModifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = ui.countdown,
                style = TextStyle(color = colors.content, fontSize = 16.sp, fontWeight = FontWeight.Bold),
                maxLines = 2,
            )
            Text(
                text = ui.doseText,
                style = TextStyle(color = colors.content, fontSize = 12.sp),
                maxLines = 1,
            )
        }
        if (showDone) {
            Spacer(GlanceModifier.width(8.dp))
            Box(
                modifier = GlanceModifier
                    .size(36.dp)
                    .background(colors.button)
                    .cornerRadius(18.dp)
                    .clickable(markDone)
                    .semantics { contentDescription = context.getString(R.string.widget_done_description) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = CHECK_MARK,
                    style = TextStyle(color = colors.onButton, fontSize = 18.sp, fontWeight = FontWeight.Bold),
                )
            }
        }
    }
}

/** Before onboarding there is no plan to show: invite the user to open the app. */
@Composable
private fun SetupLayout(colors: Palette) {
    val context = LocalContext.current
    Column(
        modifier = GlanceModifier.fillMaxSize().padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = context.getString(R.string.widget_name),
            style = TextStyle(color = colors.content, fontSize = 14.sp, fontWeight = FontWeight.Bold),
            maxLines = 1,
        )
        Text(
            text = context.getString(R.string.widget_setup),
            style = TextStyle(color = colors.content, fontSize = 12.sp),
            maxLines = 3,
        )
    }
}

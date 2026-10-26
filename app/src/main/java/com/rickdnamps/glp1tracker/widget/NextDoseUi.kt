package com.rickdnamps.glp1tracker.widget

import android.content.Context
import com.rickdnamps.glp1tracker.R
import com.rickdnamps.glp1tracker.data.InjectionEntity
import com.rickdnamps.glp1tracker.data.UserSettings
import com.rickdnamps.glp1tracker.domain.Format
import com.rickdnamps.glp1tracker.domain.InjectionSite
import com.rickdnamps.glp1tracker.domain.Medication
import com.rickdnamps.glp1tracker.ui.daysUntil
import com.rickdnamps.glp1tracker.ui.nextDue
import com.rickdnamps.glp1tracker.ui.siteLastUse
import java.time.LocalDate

/** Everything the widget shows, already formatted, using the same rules as the Home "Next dose" card. */
internal data class NextDoseUi(
    val configured: Boolean,
    val countdown: String = "",
    val dueText: String = "",
    val doseText: String = "",
    /** Null for oral medications. */
    val siteText: String? = null,
    val isLate: Boolean = false,
    /** Due today or overdue: ReminderReceiver only logs a dose in that case, so only then show "Done". */
    val canMarkDone: Boolean = false,
) {
    companion object {
        /** [injections] newest first, as emitted by TrackerRepository.injections. */
        fun from(
            context: Context,
            settings: UserSettings,
            injections: List<InjectionEntity>,
            today: LocalDate,
        ): NextDoseUi {
            if (!settings.onboardingDone) return NextDoseUi(configured = false)
            val res = context.resources
            val due = nextDue(settings, injections, today)
            val days = daysUntil(due, today)
            val countdown = when {
                days < 0 -> (-days).toInt().let { res.getQuantityString(R.plurals.widget_late_by_days, it, it) }
                days == 0L -> context.getString(R.string.widget_today)
                days == 1L -> context.getString(R.string.widget_tomorrow)
                else -> days.toInt().let { res.getQuantityString(R.plurals.widget_in_days, it, it) }
            }
            val site = if (settings.medication.isOral) null else InjectionSite.suggest(siteLastUse(injections))
            return NextDoseUi(
                configured = true,
                countdown = countdown,
                dueText = context.getString(R.string.widget_due_on, Format.weekdayDate(due), Format.time(due)),
                doseText = context.getString(
                    R.string.widget_dose,
                    medicationName(context, settings),
                    Format.dose(settings.doseMg),
                ),
                siteText = site?.let { context.getString(R.string.widget_site, context.getString(it.nameRes)) },
                isLate = days < 0,
                canMarkDone = days <= 0,
            )
        }

        /** Same rule as ui/Stats.kt `medicationName`, without Compose. */
        private fun medicationName(context: Context, s: UserSettings): String =
            if (s.medication == Medication.OTHER && s.customMedName.isNotBlank()) s.customMedName
            else context.getString(s.medication.nameRes)
    }
}

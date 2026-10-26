package com.rickdnamps.glp1tracker.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Lets the rest of the app re-render the home-screen widget after its data changed. */
object WidgetUpdater {
    private const val TAG = "WidgetUpdater"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Re-renders every placed "Next dose" widget; does nothing when none is placed. */
    suspend fun refresh(context: Context) {
        runCatching { NextDoseWidget().updateAll(context.applicationContext) }
            .onFailure {
                if (it is CancellationException) throw it
                Log.w(TAG, "Widget refresh failed", it)
            }
    }

    /** Fire-and-forget [refresh] for callers that cannot suspend (e.g. Glp1App.onDataChanged). */
    fun requestRefresh(context: Context) {
        val appContext = context.applicationContext
        scope.launch { refresh(appContext) }
    }
}

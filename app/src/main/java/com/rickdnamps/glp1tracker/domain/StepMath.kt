package com.rickdnamps.glp1tracker.domain

/**
 * Turns readings of the phone's step counter (steps since the last reboot) into steps per day.
 * Pure so it can be unit-tested; [com.rickdnamps.glp1tracker.steps.PhoneSteps] stores the state.
 */
object StepMath {

    /**
     * [day] = epoch day of the last reading, [base] = counter value that means "0 steps today",
     * [last] = last counter value, [history] = finished days (epoch day → steps).
     */
    data class State(val day: Long, val base: Long, val last: Long, val history: Map<Long, Long> = emptyMap()) {
        val today: Long get() = (last - base).coerceAtLeast(0)
    }

    private const val KEEP_DAYS = 30

    fun update(state: State?, day: Long, counter: Long): State {
        if (state == null) return State(day, counter, counter)
        var base = state.base
        var history = state.history
        val rebooted = counter < state.last
        if (day != state.day) {
            // Close the previous day with what was counted up to its last reading. Steps taken
            // between that reading and now cannot be split, so they count for today.
            if (day > state.day) history = (history + (state.day to state.today)).filterKeys { it > day - KEEP_DAYS }
            base = if (rebooted) 0 else state.last
        } else if (rebooted) {
            // The counter restarted at 0: keep what was already counted today.
            base = -(state.last - state.base)
        }
        return State(day, base, counter, history)
    }

    /** The last [days] days ending today (oldest first); missing days are 0. */
    fun lastDays(state: State, today: Long, days: Int = 7): List<Pair<Long, Long>> =
        ((today - days + 1)..today).map { d -> d to if (d == state.day) state.today else state.history[d] ?: 0L }
}

package com.rickdnamps.glp1tracker.domain

import java.time.LocalTime

/** "Dark in the evening": whether [time] falls in the night window (may cross midnight). */
object ThemeSchedule {
    fun isNight(time: LocalTime, startMinutes: Int, endMinutes: Int): Boolean {
        val now = time.hour * 60 + time.minute
        return when {
            startMinutes == endMinutes -> false
            startMinutes < endMinutes -> now in startMinutes until endMinutes
            else -> now >= startMinutes || now < endMinutes
        }
    }
}

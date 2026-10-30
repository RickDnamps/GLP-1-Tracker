package com.rickdnamps.glp1tracker.domain

/**
 * Gentle "you may be eating too little" check (estimates, not advice). Dietitians commonly cite
 * about 1,200 kcal/day (women) and 1,500 kcal/day (men) as the level below which intake on a
 * GLP-1 should be followed closely; protein is compared with the low end of the daily range.
 */
object IntakeCheck {

    data class Day(val calories: Int, val proteinG: Int, val entries: Int)

    data class Result(val lowCalories: Boolean, val lowProtein: Boolean, val averageCalories: Int, val averageProteinG: Int) {
        val any: Boolean get() = lowCalories || lowProtein
    }

    /** A day counts only with at least 2 food entries, so a forgotten log never triggers it. */
    private const val MIN_ENTRIES = 2
    private const val MIN_LOGGED_DAYS = 4

    fun calorieFloor(sex: Sex?): Int = if (sex == Sex.MALE) 1_500 else 1_200

    /** [days]: the last full days (not today). Null when there are too few logged days to judge. */
    fun check(days: List<Day>, calorieFloor: Int, proteinTargetG: Int): Result? {
        val logged = days.filter { it.entries >= MIN_ENTRIES }
        if (logged.size < MIN_LOGGED_DAYS) return null
        val kcal = logged.map { it.calories }.average()
        val protein = logged.map { it.proteinG }.average()
        return Result(
            lowCalories = kcal < calorieFloor,
            lowProtein = proteinTargetG > 0 && protein < proteinTargetG * 0.6,
            averageCalories = kcal.toInt(),
            averageProteinG = protein.toInt(),
        )
    }
}

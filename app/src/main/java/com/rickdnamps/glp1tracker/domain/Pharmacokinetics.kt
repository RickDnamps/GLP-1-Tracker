package com.rickdnamps.glp1tracker.domain

import kotlin.math.exp
import kotlin.math.ln

/**
 * One-compartment model with first-order absorption (Bateman function), summed over
 * every logged dose. It gives a rough *estimate* of the amount of drug in the body,
 * useful to visualise peaks and troughs — it is not a medical measurement.
 */
object Pharmacokinetics {

    data class Params(val halfLifeHours: Double, val tmaxHours: Double, val bioavailability: Double = 1.0)

    data class Dose(val timeMillis: Long, val doseMg: Double, val params: Params)

    private const val MS_PER_HOUR = 3_600_000.0

    /** The bisection is costly and depends only on the parameters, so cache it. */
    private val kaCache = java.util.concurrent.ConcurrentHashMap<Params, Double>()

    /** Solves tmax = ln(ka/ke)/(ka-ke) for the absorption rate ka (> ke) by bisection. */
    fun absorptionRate(ke: Double, tmaxHours: Double): Double {
        // tmax tends to 1/ke when ka -> ke; beyond that no solution exists.
        val tmax = tmaxHours.coerceAtMost(0.95 / ke)
        var lo = ke * 1.000001
        var hi = 100.0
        repeat(100) {
            val mid = (lo + hi) / 2
            val t = ln(mid / ke) / (mid - ke)
            if (t > tmax) lo = mid else hi = mid
        }
        return (lo + hi) / 2
    }

    fun amountAt(dose: Dose, atMillis: Long): Double {
        val t = (atMillis - dose.timeMillis) / MS_PER_HOUR
        if (t < 0) return 0.0
        val ke = ln(2.0) / dose.params.halfLifeHours
        val ka = kaCache.getOrPut(dose.params) { absorptionRate(ke, dose.params.tmaxHours) }
        return dose.doseMg * dose.params.bioavailability * ka / (ka - ke) * (exp(-ke * t) - exp(-ka * t))
    }

    fun levelAt(doses: List<Dose>, atMillis: Long): Double =
        doses.sumOf { amountAt(it, atMillis) }

    fun curve(doses: List<Dose>, fromMillis: Long, toMillis: Long, points: Int = 120): List<Pair<Long, Double>> {
        if (toMillis <= fromMillis || points < 2) return emptyList()
        val step = (toMillis - fromMillis) / (points - 1)
        return (0 until points).map { i ->
            val t = fromMillis + i * step
            t to levelAt(doses, t)
        }
    }
}

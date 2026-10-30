package com.rickdnamps.glp1tracker.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.BodyWaterMassRecord
import androidx.health.connect.client.records.BoneMassRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.rickdnamps.glp1tracker.data.WeightEntity
import com.rickdnamps.glp1tracker.data.composition
import com.rickdnamps.glp1tracker.data.withComposition
import com.rickdnamps.glp1tracker.domain.BodyComp
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import kotlin.math.abs
import kotlin.reflect.KClass

/**
 * Imports weigh-ins and body composition from Health Connect, where Renpho, Withings, Eufy,
 * Fitbit, Samsung Health, Google Fit… publish their measurements. Read-only.
 */
class HealthConnect(context: Context) {
    private val appContext = context.applicationContext

    /** Permissions the import needs. */
    val permissions = setOf(HealthPermission.getReadPermission(WeightRecord::class))

    private val compositionTypes: List<KClass<out Record>> = listOf(
        BodyFatRecord::class, LeanBodyMassRecord::class, BoneMassRecord::class,
        BodyWaterMassRecord::class, BasalMetabolicRateRecord::class,
    )

    /** Also asks for body composition and for history older than 30 days (optional). */
    val requestedPermissions: Set<String> =
        permissions + compositionTypes.map { HealthPermission.getReadPermission(it) } + READ_HISTORY

    private val stepsPermission = HealthPermission.getReadPermission(StepsRecord::class)
    private val sleepPermission = HealthPermission.getReadPermission(SleepSessionRecord::class)

    /** Steps and sleep for the home screen (optional, asked separately from the weight import). */
    val activityPermissions: Set<String> = setOf(stepsPermission, sleepPermission)

    /** Today's steps, the last 7 days (oldest first) and last night's sleep; null when not allowed or no data. */
    data class Activity(
        val stepsToday: Long?,
        val stepsByDay: List<Pair<LocalDate, Long>>,
        val sleepLastNightMinutes: Long?,
    )

    suspend fun hasActivityPermission(): Boolean =
        isAvailable() && client().permissionController.getGrantedPermissions().any { it in activityPermissions }

    suspend fun activity(today: LocalDate = LocalDate.now(), zone: ZoneId = ZoneId.systemDefault()): Activity {
        val granted = client().permissionController.getGrantedPermissions()
        // Aggregates merge every source (phone, watch, apps) without double counting.
        val steps = if (stepsPermission in granted) {
            runCatching {
                client().aggregateGroupByPeriod(
                    AggregateGroupByPeriodRequest(
                        metrics = setOf(StepsRecord.COUNT_TOTAL),
                        timeRangeFilter = TimeRangeFilter.between(today.minusDays(6).atStartOfDay(), today.plusDays(1).atStartOfDay()),
                        timeRangeSlicer = Period.ofDays(1),
                    )
                ).map { it.startTime.toLocalDate() to (it.result[StepsRecord.COUNT_TOTAL] ?: 0L) }
            }.getOrNull()
        } else null
        // Last night: sleep ending between yesterday 18:00 and today 18:00.
        val sleep = if (sleepPermission in granted) {
            runCatching {
                client().aggregate(
                    AggregateRequest(
                        metrics = setOf(SleepSessionRecord.SLEEP_DURATION_TOTAL),
                        timeRangeFilter = TimeRangeFilter.between(
                            today.minusDays(1).atTime(18, 0).atZone(zone).toInstant(),
                            today.atTime(18, 0).atZone(zone).toInstant(),
                        ),
                    )
                )[SleepSessionRecord.SLEEP_DURATION_TOTAL]?.toMinutes()?.takeIf { it > 0 }
            }.getOrNull()
        } else null
        return Activity(
            stepsToday = steps?.firstOrNull { it.first == today }?.second,
            stepsByDay = steps.orEmpty(),
            sleepLastNightMinutes = sleep,
        )
    }

    fun isAvailable(): Boolean =
        HealthConnectClient.getSdkStatus(appContext) == HealthConnectClient.SDK_AVAILABLE

    fun needsUpdate(): Boolean =
        HealthConnectClient.getSdkStatus(appContext) == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED

    private fun client() = HealthConnectClient.getOrCreate(appContext)

    suspend fun hasPermissions(): Boolean =
        isAvailable() && client().permissionController.getGrantedPermissions().containsAll(permissions)

    private suspend fun <T : Record> readAll(type: KClass<T>, since: Instant): List<T> {
        val out = mutableListOf<T>()
        var pageToken: String? = null
        do {
            val response = client().readRecords(
                ReadRecordsRequest(recordType = type, timeRangeFilter = TimeRangeFilter.after(since), pageToken = pageToken)
            )
            out += response.records
            pageToken = response.pageToken
        } while (pageToken != null)
        return out
    }

    /** A composition value measured at [time]. */
    private data class Sample(val time: Long, val apply: (BodyComp, Double) -> BodyComp)

    data class ImportResult(val newWeights: List<WeightEntity>, val updated: List<WeightEntity>)

    /**
     * New weigh-ins newer than [since] (skipping ones already present: ±2 min and ±0.05 kg),
     * plus body-composition values attached to the matching weigh-in (±10 min).
     */
    suspend fun import(since: Instant, existing: List<WeightEntity>): ImportResult {
        val own = appContext.packageName
        val weights = readAll(WeightRecord::class, since)
            .filter { it.metadata.dataOrigin.packageName != own && it.weight.inKilograms in 20.0..400.0 }

        // Optional types: a missing permission simply yields no data.
        val samples = mutableListOf<Sample>()
        runCatching { readAll(BodyFatRecord::class, since) }.getOrDefault(emptyList()).forEach { r ->
            samples += Sample(r.time.toEpochMilli()) { c, _ ->
                c.copy(bodyFatPct = c.bodyFatPct ?: r.percentage.value.takeIf { it in 2.0..75.0 })
            }
        }
        runCatching { readAll(LeanBodyMassRecord::class, since) }.getOrDefault(emptyList()).forEach { r ->
            samples += Sample(r.time.toEpochMilli()) { c, w ->
                c.copy(bodyFatPct = c.bodyFatPct ?: ((w - r.mass.inKilograms) / w * 100).takeIf { it in 2.0..70.0 })
            }
        }
        runCatching { readAll(BoneMassRecord::class, since) }.getOrDefault(emptyList()).forEach { r ->
            samples += Sample(r.time.toEpochMilli()) { c, _ -> c.copy(boneKg = c.boneKg ?: r.mass.inKilograms.takeIf { it in 0.5..10.0 }) }
        }
        runCatching { readAll(BodyWaterMassRecord::class, since) }.getOrDefault(emptyList()).forEach { r ->
            samples += Sample(r.time.toEpochMilli()) { c, w ->
                c.copy(waterPct = c.waterPct ?: (r.mass.inKilograms / w * 100).takeIf { it in 20.0..80.0 })
            }
        }
        runCatching { readAll(BasalMetabolicRateRecord::class, since) }.getOrDefault(emptyList()).forEach { r ->
            samples += Sample(r.time.toEpochMilli()) { c, _ ->
                c.copy(bmrKcal = c.bmrKcal ?: r.basalMetabolicRate.inKilocaloriesPerDay.takeIf { it in 500.0..5000.0 })
            }
        }

        fun enrich(w: WeightEntity): WeightEntity {
            val near = samples.filter { abs(it.time - w.timestamp) <= 10 * 60_000 }
            if (near.isEmpty()) return w
            var c = w.composition
            near.forEach { c = it.apply(c, w.weightKg) }
            return w.withComposition(c)
        }

        val newWeights = weights
            .map { WeightEntity(timestamp = it.time.toEpochMilli(), weightKg = it.weight.inKilograms, notes = "Health Connect") }
            .filter { candidate ->
                existing.none { e -> abs(e.timestamp - candidate.timestamp) < 120_000 && abs(e.weightKg - candidate.weightKg) < 0.05 }
            }
            .distinctBy { it.timestamp / 60_000 }
            .map(::enrich)

        // Existing weigh-ins (e.g. from the Bluetooth scale) can receive composition too.
        val updated = existing
            .filter { it.timestamp >= since.toEpochMilli() }
            .mapNotNull { e -> enrich(e).takeIf { it != e } }

        return ImportResult(newWeights, updated)
    }

    companion object {
        private const val READ_HISTORY = "android.permission.health.READ_HEALTH_DATA_HISTORY"

        fun permissionContract() = PermissionController.createRequestPermissionResultContract()
    }
}

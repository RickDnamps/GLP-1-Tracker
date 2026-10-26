package com.rickdnamps.glp1tracker.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.rickdnamps.glp1tracker.data.WeightEntity
import java.time.Instant
import kotlin.math.abs

/**
 * Imports weights from Health Connect, where Renpho, Withings, Eufy, Fitbit, Samsung Health,
 * Google Fit… publish their measurements. Read-only: nothing is written back.
 */
class HealthConnect(context: Context) {
    private val appContext = context.applicationContext

    val permissions = setOf(HealthPermission.getReadPermission(WeightRecord::class))

    fun isAvailable(): Boolean =
        HealthConnectClient.getSdkStatus(appContext) == HealthConnectClient.SDK_AVAILABLE

    fun needsUpdate(): Boolean =
        HealthConnectClient.getSdkStatus(appContext) == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED

    private fun client() = HealthConnectClient.getOrCreate(appContext)

    suspend fun hasPermissions(): Boolean =
        isAvailable() && client().permissionController.getGrantedPermissions().containsAll(permissions)

    /** Returns weights newer than [since] that are not already present (±2 min and ±0.05 kg). */
    suspend fun newWeights(since: Instant, existing: List<WeightEntity>): List<WeightEntity> {
        val records = mutableListOf<WeightRecord>()
        var pageToken: String? = null
        do {
            val response = client().readRecords(
                ReadRecordsRequest(
                    recordType = WeightRecord::class,
                    timeRangeFilter = TimeRangeFilter.after(since),
                    pageToken = pageToken,
                )
            )
            records += response.records
            pageToken = response.pageToken
        } while (pageToken != null)

        return records
            .filter { it.metadata.dataOrigin.packageName != appContext.packageName }
            .map { r ->
                WeightEntity(
                    timestamp = r.time.toEpochMilli(),
                    weightKg = r.weight.inKilograms,
                    notes = "Health Connect",
                )
            }
            .filter { candidate ->
                existing.none { e ->
                    abs(e.timestamp - candidate.timestamp) < 120_000 && abs(e.weightKg - candidate.weightKg) < 0.05
                }
            }
            .distinctBy { it.timestamp / 60_000 }
    }

    companion object {
        fun permissionContract() = PermissionController.createRequestPermissionResultContract()
    }
}

package com.healthtracker.app.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.healthtracker.app.data.DailyActivity
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId

/** Reads steps and sleep from Health Connect (Samsung Health syncs into it). */
class HealthConnectManager(private val context: Context) {

    val permissions: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
    )

    fun isAvailable(): Boolean =
        HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    fun needsUpdate(): Boolean =
        HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED

    private val client: HealthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }

    suspend fun hasPermissions(): Boolean = runCatching {
        isAvailable() && client.permissionController.getGrantedPermissions().containsAll(permissions)
    }.getOrDefault(false)

    /** Steps for each day, and sleep that ended on each day's morning (or afternoon nap). */
    suspend fun readDays(days: List<LocalDate>): List<DailyActivity> {
        if (!hasPermissions()) return emptyList()
        val zone = ZoneId.systemDefault()
        return days.map { d ->
            val start = d.atStartOfDay(zone).toInstant()
            val end = d.plusDays(1).atStartOfDay(zone).toInstant()
            val steps = runCatching {
                client.aggregate(
                    AggregateRequest(
                        metrics = setOf(StepsRecord.COUNT_TOTAL),
                        timeRangeFilter = TimeRangeFilter.between(start, end),
                    )
                )[StepsRecord.COUNT_TOTAL]?.toInt()
            }.getOrNull()
            val sleep = runCatching {
                val from = d.minusDays(1).atTime(12, 0).atZone(zone).toInstant()
                val to = d.atTime(18, 0).atZone(zone).toInstant()
                val records = client.readRecords(
                    ReadRecordsRequest(SleepSessionRecord::class, TimeRangeFilter.between(from, to))
                ).records
                records.filter { !it.endTime.isBefore(start) && !it.endTime.isAfter(to) }
                    .sumOf { Duration.between(it.startTime, it.endTime).toMinutes() }.toInt()
            }.getOrNull()
            DailyActivity(d.toEpochDay(), steps, sleep?.takeIf { it > 0 })
        }
    }
}

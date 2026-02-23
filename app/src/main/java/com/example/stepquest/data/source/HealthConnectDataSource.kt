package com.example.stepquest.data.source

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.example.stepquest.data.local.DailyStepsEntity
import java.time.LocalDate
import java.time.Period

class HealthConnectDataSource(private val context: Context) {

    private val stepsPermission = HealthPermission.getReadPermission(StepsRecord::class)

    fun isAvailable(): Boolean =
        HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    suspend fun hasPermission(): Boolean {
        if (!isAvailable()) return false
        val client = HealthConnectClient.getOrCreate(context)
        return stepsPermission in client.permissionController.getGrantedPermissions()
    }

    suspend fun fetchLast7Days(): List<DailyStepsEntity> {
        val client = HealthConnectClient.getOrCreate(context)
        val today = LocalDate.now()
        val sevenDaysAgo = today.minusDays(7)

        val response = client.aggregateGroupByPeriod(
            AggregateGroupByPeriodRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(
                    sevenDaysAgo.atStartOfDay(),
                    today.plusDays(1).atStartOfDay()
                ),
                timeRangeSlicer = Period.ofDays(1)
            )
        )

        return response.map { result ->
            DailyStepsEntity(
                date = result.startTime.toLocalDate().toString(),
                steps = result.result[StepsRecord.COUNT_TOTAL] ?: 0L
            )
        }
    }
}

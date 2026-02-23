package com.example.stepquest.data.source

import android.content.Context
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.fitness.FitnessLocal
import com.google.android.gms.fitness.LocalRecordingClient
import com.google.android.gms.fitness.data.LocalDataType
import com.google.android.gms.fitness.data.LocalField
import com.google.android.gms.fitness.request.LocalDataReadRequest
import kotlinx.coroutines.tasks.await
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class RecordingApiDataSource(private val context: Context) {

    fun isPlayServicesAvailable(): Boolean {
        val availability = GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(context, LocalRecordingClient.LOCAL_RECORDING_CLIENT_MIN_VERSION_CODE)
        return availability == ConnectionResult.SUCCESS
    }

    suspend fun subscribe(): Boolean {
        return try {
            val client = FitnessLocal.getLocalRecordingClient(context)
            client.subscribe(LocalDataType.TYPE_STEP_COUNT_DELTA).await()
            true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to subscribe to Recording API", e)
            false
        }
    }

    suspend fun readTodaySteps(): Long {
        val client = FitnessLocal.getLocalRecordingClient(context)
        val now = LocalDateTime.now()
        val startOfDay = now.toLocalDate().atStartOfDay()
        val zone = ZoneId.systemDefault()
        val request = LocalDataReadRequest.Builder()
            .aggregate(LocalDataType.TYPE_STEP_COUNT_DELTA)
            .bucketByTime(1, TimeUnit.DAYS)
            .setTimeRange(
                startOfDay.atZone(zone).toEpochSecond(),
                now.atZone(zone).toEpochSecond(),
                TimeUnit.SECONDS
            ).build()
        val response = client.readData(request).await()
        return response.buckets.flatMap { it.dataSets }
            .flatMap { it.dataPoints }
            .sumOf { it.getValue(LocalField.FIELD_STEPS).asInt().toLong() }
    }

    companion object {
        private const val TAG = "RecordingApiDataSource"
    }
}

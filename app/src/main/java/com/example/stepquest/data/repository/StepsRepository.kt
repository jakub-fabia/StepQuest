package com.example.stepquest.data.repository

import android.net.Uri
import com.example.stepquest.data.local.DailyStepsDao
import com.example.stepquest.data.local.DailyStepsEntity
import com.example.stepquest.data.source.CsvDataSource
import com.example.stepquest.data.source.HealthConnectDataSource
import com.example.stepquest.data.source.RecordingApiDataSource
import java.time.LocalDate

class StepsRepository(
    private val dao: DailyStepsDao,
    private val healthConnectDataSource: HealthConnectDataSource,
    private val recordingApiDataSource: RecordingApiDataSource,
    private val csvDataSource: CsvDataSource
) {

    suspend fun syncFromHealthConnect() {
        val entities = healthConnectDataSource.fetchLast7Days()
        dao.upsertAll(entities)
    }

    suspend fun syncTodayFromRecordingApi() {
        val todaySteps = recordingApiDataSource.readTodaySteps()
        dao.upsert(DailyStepsEntity(LocalDate.now().toString(), todaySteps))
    }

    suspend fun importCsv(uri: Uri): Int {
        val parsed = csvDataSource.parse(uri)
        var importedCount = 0
        for (entity in parsed) {
            val existing = dao.getByDate(entity.date)
            if (existing == null || existing.steps == 0L) {
                dao.upsert(entity)
                importedCount++
            }
        }
        return importedCount
    }

    suspend fun exportCsv(uri: Uri): Int {
        val entities = dao.getAll()
        if (entities.isEmpty()) return 0
        csvDataSource.write(uri, entities)
        return entities.size
    }

    suspend fun getAll(): List<DailyStepsEntity> = dao.getAll()

    suspend fun sumStepsInRange(startDate: String, endDate: String): Long =
        dao.sumStepsInRange(startDate, endDate)
}

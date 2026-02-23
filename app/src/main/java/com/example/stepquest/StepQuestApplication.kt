package com.example.stepquest

import android.app.Application
import android.content.Context
import com.example.stepquest.data.local.GoalPreferences
import com.example.stepquest.data.local.StepsDatabase
import com.example.stepquest.data.repository.StepsRepository
import com.example.stepquest.data.source.CsvDataSource
import com.example.stepquest.data.source.HealthConnectDataSource
import com.example.stepquest.data.source.RecordingApiDataSource

class StepQuestApplication : Application() {

    val database by lazy { StepsDatabase.getInstance(this) }
    val goalPreferences by lazy { GoalPreferences(this) }
    val healthConnectDataSource by lazy { HealthConnectDataSource(this) }
    val recordingApiDataSource by lazy { RecordingApiDataSource(this) }
    val csvDataSource by lazy { CsvDataSource(contentResolver) }
    val stepsRepository by lazy {
        StepsRepository(
            dao = database.dailyStepsDao(),
            healthConnectDataSource = healthConnectDataSource,
            recordingApiDataSource = recordingApiDataSource,
            csvDataSource = csvDataSource
        )
    }

    companion object {
        fun get(context: Context): StepQuestApplication =
            context.applicationContext as StepQuestApplication
    }
}

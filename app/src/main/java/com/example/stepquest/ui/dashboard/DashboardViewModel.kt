package com.example.stepquest.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.stepquest.StepQuestApplication
import com.example.stepquest.data.local.GoalPreferences
import com.example.stepquest.data.repository.StepsRepository
import com.example.stepquest.data.source.HealthConnectDataSource
import com.example.stepquest.data.source.RecordingApiDataSource
import com.example.stepquest.domain.model.DashboardError
import com.example.stepquest.domain.model.DashboardRow
import com.example.stepquest.domain.model.DashboardState
import com.example.stepquest.domain.usecase.GoalCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

class DashboardViewModel(
    private val stepsRepository: StepsRepository,
    private val goalPreferences: GoalPreferences,
    private val healthConnectDataSource: HealthConnectDataSource,
    private val recordingApiDataSource: RecordingApiDataSource
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    var recordingApiAvailable = false
        private set

    fun setupRecordingApi(
        hasActivityRecognitionPermission: Boolean,
        onNeedPermission: () -> Unit
    ) {
        if (!recordingApiDataSource.isPlayServicesAvailable()) {
            recordingApiAvailable = false
            return
        }
        if (hasActivityRecognitionPermission) {
            viewModelScope.launch {
                recordingApiAvailable = recordingApiDataSource.subscribe()
            }
        } else {
            onNeedPermission()
        }
    }

    fun onRecordingApiPermissionResult(granted: Boolean) {
        if (granted) {
            viewModelScope.launch {
                recordingApiAvailable = recordingApiDataSource.subscribe()
            }
        } else {
            recordingApiAvailable = false
        }
    }

    fun checkAndLoadSteps() {
        viewModelScope.launch {
            val hcAvailable = healthConnectDataSource.isAvailable()
            val hasPermission = if (hcAvailable) healthConnectDataSource.hasPermission() else false
            _state.update { it.copy(showGrantPermission = hcAvailable && !hasPermission) }
            doRefresh()
        }
    }

    fun performFullRefresh() {
        viewModelScope.launch { doRefresh() }
    }

    fun autoRefreshToday() {
        viewModelScope.launch {
            try {
                if (recordingApiAvailable) {
                    stepsRepository.syncTodayFromRecordingApi()
                }
                loadDashboardFromRoom()
            } catch (_: Exception) {
                // Silent on auto-refresh errors
            }
        }
    }

    fun onHcPermissionResult(granted: Boolean) {
        if (granted) {
            performFullRefresh()
        } else {
            _state.update {
                it.copy(error = DashboardError.PERMISSION_DENIED, showGrantPermission = true)
            }
        }
    }

    fun getYearlyGoal(): Long = goalPreferences.getYearlyGoal()

    fun setYearlyGoal(goal: Long) {
        goalPreferences.setYearlyGoal(goal)
        performFullRefresh()
    }

    private suspend fun doRefresh() {
        _state.update { it.copy(isRefreshing = true) }
        try {
            if (healthConnectDataSource.isAvailable() && healthConnectDataSource.hasPermission()) {
                _state.update { it.copy(showGrantPermission = false) }
                stepsRepository.syncFromHealthConnect()
            }
            if (recordingApiAvailable) {
                stepsRepository.syncTodayFromRecordingApi()
            }
            loadDashboardFromRoom()
            _state.update { it.copy(error = null) }
        } catch (e: Exception) {
            _state.update { it.copy(error = DashboardError.READING_ERROR) }
        } finally {
            _state.update { it.copy(isRefreshing = false) }
        }
    }

    private suspend fun loadDashboardFromRoom() {
        val today = LocalDate.now()
        val todayStr = today.toString()
        val last7Start = today.minusDays(6).toString()
        val startOfMonth = today.withDayOfMonth(1).toString()
        val last30Start = today.minusDays(29).toString()
        val startOfYear = today.withDayOfYear(1).toString()

        val todaySteps = stepsRepository.sumStepsInRange(todayStr, todayStr)
        val last7Steps = stepsRepository.sumStepsInRange(last7Start, todayStr)
        val monthSteps = stepsRepository.sumStepsInRange(startOfMonth, todayStr)
        val last30Steps = stepsRepository.sumStepsInRange(last30Start, todayStr)
        val yearSteps = stepsRepository.sumStepsInRange(startOfYear, todayStr)

        val goals = GoalCalculator.deriveGoals(goalPreferences.getYearlyGoal(), today)

        val expectedSteps = goals.daily * today.dayOfYear
        val paceSteps = yearSteps - expectedSteps

        _state.update {
            it.copy(
                today = DashboardRow(todaySteps, goals.daily, GoalCalculator.calculatePercent(todaySteps, goals.daily)),
                last7 = DashboardRow(last7Steps, goals.last7, GoalCalculator.calculatePercent(last7Steps, goals.last7)),
                month = DashboardRow(monthSteps, goals.monthly, GoalCalculator.calculatePercent(monthSteps, goals.monthly)),
                last30 = DashboardRow(last30Steps, goals.last30, GoalCalculator.calculatePercent(last30Steps, goals.last30)),
                year = DashboardRow(yearSteps, goals.yearly, GoalCalculator.calculatePercent(yearSteps, goals.yearly)),
                paceSteps = paceSteps
            )
        }
    }

    class Factory(private val app: StepQuestApplication) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(
                app.stepsRepository,
                app.goalPreferences,
                app.healthConnectDataSource,
                app.recordingApiDataSource
            ) as T
        }
    }
}

package com.example.stepquest.ui.dailysteps

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.stepquest.StepQuestApplication
import com.example.stepquest.data.local.GoalPreferences
import com.example.stepquest.data.repository.StepsRepository
import com.example.stepquest.domain.model.DailyStepsListState
import com.example.stepquest.domain.model.toDailySteps
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ImportEvent {
    data class Success(val count: Int) : ImportEvent
    data object NoData : ImportEvent
    data object Error : ImportEvent
}

sealed interface ExportEvent {
    data class Success(val count: Int) : ExportEvent
    data object NoData : ExportEvent
    data object Error : ExportEvent
}

class DailyStepsViewModel(
    private val stepsRepository: StepsRepository,
    private val goalPreferences: GoalPreferences
) : ViewModel() {

    private val _state = MutableStateFlow<DailyStepsListState>(DailyStepsListState.Loading)
    val state: StateFlow<DailyStepsListState> = _state.asStateFlow()

    private val _importEvents = MutableSharedFlow<ImportEvent>()
    val importEvents: SharedFlow<ImportEvent> = _importEvents.asSharedFlow()

    private val _exportEvents = MutableSharedFlow<ExportEvent>()
    val exportEvents: SharedFlow<ExportEvent> = _exportEvents.asSharedFlow()

    fun loadDailySteps() {
        viewModelScope.launch {
            _state.value = DailyStepsListState.Loading
            try {
                val entities = stepsRepository.getAll()
                val dailySteps = entities.map { it.toDailySteps() }
                if (dailySteps.isEmpty()) {
                    _state.value = DailyStepsListState.Empty
                } else {
                    val dailyGoal = goalPreferences.getYearlyGoal() / 365
                    _state.value = DailyStepsListState.Success(dailySteps, dailyGoal)
                }
            } catch (e: Exception) {
                _state.value = DailyStepsListState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun importCsv(uri: Uri) {
        viewModelScope.launch {
            try {
                val importedCount = stepsRepository.importCsv(uri)
                if (importedCount > 0) {
                    _importEvents.emit(ImportEvent.Success(importedCount))
                    loadDailySteps()
                } else {
                    _importEvents.emit(ImportEvent.NoData)
                }
            } catch (e: Exception) {
                _importEvents.emit(ImportEvent.Error)
            }
        }
    }

    fun exportCsv(uri: Uri) {
        viewModelScope.launch {
            try {
                val count = stepsRepository.exportCsv(uri)
                if (count > 0) {
                    _exportEvents.emit(ExportEvent.Success(count))
                } else {
                    _exportEvents.emit(ExportEvent.NoData)
                }
            } catch (e: Exception) {
                _exportEvents.emit(ExportEvent.Error)
            }
        }
    }

    class Factory(private val app: StepQuestApplication) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DailyStepsViewModel(app.stepsRepository, app.goalPreferences) as T
        }
    }
}

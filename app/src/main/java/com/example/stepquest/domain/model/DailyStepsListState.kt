package com.example.stepquest.domain.model

sealed interface DailyStepsListState {
    data object Loading : DailyStepsListState
    data class Success(val items: List<DailySteps>, val dailyGoal: Long) : DailyStepsListState
    data object Empty : DailyStepsListState
    data class Error(val message: String) : DailyStepsListState
}

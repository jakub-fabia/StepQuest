package com.example.stepquest.ui.stepslist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.stepquest.StepQuestApplication
import com.example.stepquest.data.local.DailyStepsEntity
import com.example.stepquest.data.local.GoalPreferences
import com.example.stepquest.data.repository.StepsRepository
import com.example.stepquest.domain.model.StepsListItem
import com.example.stepquest.domain.model.StepsListState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.Month
import java.time.temporal.TemporalAdjusters

class StepsListViewModel(
    private val stepsRepository: StepsRepository,
    private val goalPreferences: GoalPreferences
) : ViewModel() {

    private val _state = MutableStateFlow<StepsListState>(StepsListState.Loading)
    val state: StateFlow<StepsListState> = _state.asStateFlow()

    fun loadData(mode: String) {
        viewModelScope.launch {
            _state.value = StepsListState.Loading
            try {
                val all = stepsRepository.getAll()
                if (all.isEmpty()) {
                    _state.value = StepsListState.Empty
                    return@launch
                }
                val yearlyGoal = goalPreferences.getYearlyGoal()
                val items = if (mode == MODE_WEEKLY) {
                    buildWeeklyItems(all, yearlyGoal)
                } else {
                    buildMonthlyItems(all, yearlyGoal)
                }
                _state.value = StepsListState.Success(items)
            } catch (e: Exception) {
                _state.value = StepsListState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun buildWeeklyItems(entities: List<DailyStepsEntity>, yearlyGoal: Long): List<StepsListItem> {
        val weeklyGoal = yearlyGoal / 52
        val dateFormatter = DateTimeFormatter.ofPattern("MMM d")

        return entities
            .groupBy { LocalDate.parse(it.date).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) }
            .toSortedMap(compareByDescending { it })
            .map { (weekStart, days) ->
                val total = days.sumOf { it.steps }
                val weekEnd = weekStart.plusDays(6)
                val label = "${weekStart.format(dateFormatter)} – ${weekEnd.format(dateFormatter)}"
                StepsListItem(
                    label = label,
                    stepsText = "%,d".format(total),
                    highlightGreen = total >= weeklyGoal
                )
            }
    }

    private fun buildMonthlyItems(entities: List<DailyStepsEntity>, yearlyGoal: Long): List<StepsListItem> {
        val dailyGoal = yearlyGoal / 365
        val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")

        return entities
            .groupBy { LocalDate.parse(it.date).withDayOfMonth(1) }
            .toSortedMap(compareByDescending { it })
            .map { (monthStart, days) ->
                val total = days.sumOf { it.steps }
                val daysInMonth = monthStart.lengthOfMonth()
                val monthlyGoal = dailyGoal * daysInMonth
                val percent = if (monthlyGoal > 0) ((total.toFloat() / monthlyGoal) * 100).toInt() else 0
                val label = monthStart.format(monthFormatter)
                StepsListItem(
                    label = label,
                    stepsText = "%,d · %d%%".format(total, percent),
                    highlightGreen = total >= monthlyGoal
                )
            }
    }

    class Factory(private val app: StepQuestApplication) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StepsListViewModel(app.stepsRepository, app.goalPreferences) as T
        }
    }

    companion object {
        const val MODE_WEEKLY = "weekly"
        const val MODE_MONTHLY = "monthly"
    }
}

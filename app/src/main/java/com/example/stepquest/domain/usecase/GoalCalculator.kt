package com.example.stepquest.domain.usecase

import java.time.LocalDate
import java.time.Month

data class DerivedGoals(
    val daily: Long,
    val last7: Long,
    val monthly: Long,
    val last30: Long,
    val yearly: Long
)

object GoalCalculator {
    fun deriveGoals(yearlyGoal: Long, today: LocalDate = LocalDate.now()): DerivedGoals {
        val daily = yearlyGoal / 365
        return DerivedGoals(
            daily = daily,
            last7 = daily * 7,
            monthly = daily * today.lengthOfMonth(),
            last30 = daily * 30,
            yearly = yearlyGoal
        )
    }

    fun calculatePercent(steps: Long, goal: Long): Int =
        if (goal > 0) ((steps.toFloat() / goal) * 100).toInt().coerceAtLeast(0) else 0
}

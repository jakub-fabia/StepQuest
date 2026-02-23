package com.example.stepquest.domain.model

data class DashboardRow(
    val steps: Long,
    val goal: Long,
    val percent: Int
)

enum class DashboardError {
    PERMISSION_DENIED,
    READING_ERROR
}

data class DashboardState(
    val today: DashboardRow = DashboardRow(0, 0, 0),
    val last7: DashboardRow = DashboardRow(0, 0, 0),
    val month: DashboardRow = DashboardRow(0, 0, 0),
    val last30: DashboardRow = DashboardRow(0, 0, 0),
    val year: DashboardRow = DashboardRow(0, 0, 0),
    val paceSteps: Long = 0,
    val isRefreshing: Boolean = false,
    val showGrantPermission: Boolean = false,
    val error: DashboardError? = null
)

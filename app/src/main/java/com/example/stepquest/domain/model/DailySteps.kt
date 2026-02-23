package com.example.stepquest.domain.model

import com.example.stepquest.data.local.DailyStepsEntity
import java.time.LocalDate

data class DailySteps(val date: LocalDate, val steps: Long)

fun DailyStepsEntity.toDailySteps() = DailySteps(
    date = LocalDate.parse(date),
    steps = steps
)

fun DailySteps.toEntity() = DailyStepsEntity(
    date = date.toString(),
    steps = steps
)

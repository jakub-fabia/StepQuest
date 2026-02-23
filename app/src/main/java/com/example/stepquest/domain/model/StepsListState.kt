package com.example.stepquest.domain.model

sealed interface StepsListState {
    data object Loading : StepsListState
    data class Success(val items: List<StepsListItem>) : StepsListState
    data object Empty : StepsListState
    data class Error(val message: String) : StepsListState
}

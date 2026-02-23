package com.example.stepquest.data.local

import android.content.Context

class GoalPreferences(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getYearlyGoal(): Long = prefs.getLong(KEY_YEARLY_GOAL, DEFAULT_YEARLY_GOAL)

    fun setYearlyGoal(goal: Long) {
        prefs.edit().putLong(KEY_YEARLY_GOAL, goal).apply()
    }

    companion object {
        private const val PREFS_NAME = "stepquest_prefs"
        private const val KEY_YEARLY_GOAL = "yearly_goal"
        const val DEFAULT_YEARLY_GOAL = 3_000_000L
    }
}

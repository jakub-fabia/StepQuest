package com.example.stepquest.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.widget.RemoteViews
import com.example.stepquest.StepQuestApplication
import com.example.stepquest.MainActivity
import com.example.stepquest.R
import com.example.stepquest.domain.usecase.GoalCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class StepsWidgetProvider : AppWidgetProvider() {

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        scheduleAlarm(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        cancelAlarm(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        scheduleAlarm(context)
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                for (id in appWidgetIds) {
                    performUpdate(context, appWidgetManager, id)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_WIDGET_UPDATE) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val mgr = AppWidgetManager.getInstance(context)
                    val ids = mgr.getAppWidgetIds(ComponentName(context, StepsWidgetProvider::class.java))
                    for (id in ids) {
                        performUpdate(context, mgr, id)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private suspend fun performUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val data = fetchWidgetData(context)
        val views = RemoteViews(context.packageName, R.layout.widget_steps)

        views.setTextViewText(R.id.widget_step_count, "%,d".format(data.todaySteps))

        views.setTextViewText(R.id.widget_label_today, "Today · %d%%".format(data.todayPercent))
        views.setProgressBar(R.id.widget_bar_today, 100, data.todayPercent.coerceAtMost(100), false)

        views.setTextViewText(R.id.widget_label_7days, "7 days · %d%%".format(data.last7Percent))
        views.setProgressBar(R.id.widget_bar_7days, 100, data.last7Percent.coerceAtMost(100), false)

        views.setTextViewText(R.id.widget_label_30days, "30 days · %d%%".format(data.last30Percent))
        views.setProgressBar(R.id.widget_bar_30days, 100, data.last30Percent.coerceAtMost(100), false)

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, launchIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private data class WidgetData(
        val todaySteps: Long,
        val todayPercent: Int,
        val last7Percent: Int,
        val last30Percent: Int
    )

    private suspend fun fetchWidgetData(context: Context): WidgetData {
        val app = StepQuestApplication.get(context)
        val repository = app.stepsRepository
        val goalPreferences = app.goalPreferences

        val today = LocalDate.now()
        val todayStr = today.toString()

        val todaySteps = repository.sumStepsInRange(todayStr, todayStr)
        val last7Steps = repository.sumStepsInRange(today.minusDays(6).toString(), todayStr)
        val last30Steps = repository.sumStepsInRange(today.minusDays(29).toString(), todayStr)

        val yearlyGoal = goalPreferences.getYearlyGoal()
        val goals = GoalCalculator.deriveGoals(yearlyGoal, today)

        return WidgetData(
            todaySteps = todaySteps,
            todayPercent = GoalCalculator.calculatePercent(todaySteps, goals.daily),
            last7Percent = GoalCalculator.calculatePercent(last7Steps, goals.last7),
            last30Percent = GoalCalculator.calculatePercent(last30Steps, goals.last30)
        )
    }

    companion object {
        private const val ACTION_WIDGET_UPDATE = "com.example.stepquest.ACTION_WIDGET_UPDATE"
        private const val ALARM_INTERVAL_MS = 5 * 60 * 1000L // 5 minutes

        fun requestUpdate(context: Context) {
            val intent = Intent(ACTION_WIDGET_UPDATE).apply {
                component = ComponentName(context, StepsWidgetProvider::class.java)
            }
            context.sendBroadcast(intent)
        }

        fun scheduleAlarm(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pi = getAlarmPendingIntent(context)
            alarmManager.setRepeating(
                AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + ALARM_INTERVAL_MS,
                ALARM_INTERVAL_MS,
                pi
            )
        }

        fun cancelAlarm(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(getAlarmPendingIntent(context))
        }

        private fun getAlarmPendingIntent(context: Context): PendingIntent {
            val intent = Intent(ACTION_WIDGET_UPDATE).apply {
                component = ComponentName(context, StepsWidgetProvider::class.java)
            }
            return PendingIntent.getBroadcast(
                context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
    }
}

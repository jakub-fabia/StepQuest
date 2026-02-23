package com.example.stepquest.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.SystemClock
import android.widget.RemoteViews
import androidx.core.graphics.ColorUtils
import com.example.stepquest.StepQuestApplication
import com.example.stepquest.MainActivity
import com.example.stepquest.R
import com.example.stepquest.domain.usecase.GoalCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.abs

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
        val paceSteps = fetchPaceSteps(context)
        val views = RemoteViews(context.packageName, R.layout.widget_steps)

        views.setTextViewText(R.id.widget_pace_number, "%,d".format(abs(paceSteps)))
        views.setTextViewText(
            R.id.widget_pace_subtitle,
            context.getString(
                when {
                    paceSteps > 0 -> R.string.pace_steps_ahead
                    paceSteps < 0 -> R.string.pace_steps_behind
                    else -> R.string.pace_on_pace
                }
            )
        )

        val fraction = (abs(paceSteps).toFloat() / PACE_MAX_MAGNITUDE).coerceAtMost(1f)
        val targetColor = if (paceSteps >= 0) PACE_GREEN else PACE_RED

        val widgetBg = context.getColor(R.color.widget_bg)
        val bgColor = ColorUtils.blendARGB(widgetBg, targetColor, fraction * 0.35f)
        views.setColorStateList(R.id.widget_bg, "setBackgroundTintList", ColorStateList.valueOf(bgColor))

        val widgetText = context.getColor(R.color.widget_text_primary)
        val textColor = ColorUtils.blendARGB(widgetText, targetColor, fraction.coerceAtLeast(0.15f))
        views.setTextColor(R.id.widget_pace_number, textColor)

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, launchIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private suspend fun fetchPaceSteps(context: Context): Long {
        val app = StepQuestApplication.get(context)
        val repository = app.stepsRepository
        val goalPreferences = app.goalPreferences

        val today = LocalDate.now()
        val startOfYear = today.withDayOfYear(1).toString()
        val todayStr = today.toString()

        val yearSteps = repository.sumStepsInRange(startOfYear, todayStr)
        val goals = GoalCalculator.deriveGoals(goalPreferences.getYearlyGoal(), today)
        val expectedSteps = goals.daily * today.dayOfYear

        return yearSteps - expectedSteps
    }

    companion object {
        private const val ACTION_WIDGET_UPDATE = "com.example.stepquest.ACTION_WIDGET_UPDATE"
        private const val ALARM_INTERVAL_MS = 5 * 60 * 1000L // 5 minutes
        private const val PACE_MAX_MAGNITUDE = 100_000f
        private val PACE_GREEN = 0xFF4CAF50.toInt()
        private val PACE_RED = 0xFFF44336.toInt()

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

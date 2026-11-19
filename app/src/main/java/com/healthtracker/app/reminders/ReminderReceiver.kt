package com.healthtracker.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import com.healthtracker.app.container
import com.healthtracker.app.domain.Calc
import com.healthtracker.app.domain.Fmt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZonedDateTime

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_FIRE = "com.healthtracker.app.FIRE_"
        const val ACTION_ADD_WATER = "com.healthtracker.app.ADD_WATER"
        const val ACTION_WEIGHT_REPLY = "com.healthtracker.app.WEIGHT_REPLY"
        const val EXTRA_TYPE = "type"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val ctx = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                handle(ctx, intent)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun handle(ctx: Context, intent: Intent) {
        val c = ctx.container
        val s = c.settings.current()
        when (intent.action) {
            ACTION_ADD_WATER -> {
                c.repo.addWater(250)
                val total = c.repo.waterToday()
                if (total >= s.waterGoalMl) Notifications.waterDone(ctx, s.waterGoalMl)
                else Notifications.water(ctx, total, s.waterGoalMl)
            }

            ACTION_WEIGHT_REPLY -> {
                val text = RemoteInput.getResultsFromIntent(intent)?.getCharSequence(Notifications.KEY_WEIGHT)?.toString()
                val kg = text?.let { Fmt.parseNumber(it) }
                if (kg != null && kg in 30.0..350.0) {
                    c.repo.logWeight(kg)
                    val trend = Calc.trendOn(c.db.dao().allWeights(), LocalDate.now().toEpochDay())
                    Notifications.weightSaved(ctx, kg, trend)
                } else {
                    Notifications.weighIn(ctx, s.displayName, followUp = false, retry = true)
                }
            }

            else -> {
                if (!s.onboarded) return
                when (intent.getIntExtra(EXTRA_TYPE, 0)) {
                    ReminderScheduler.T_WATER -> {
                        val total = c.repo.waterToday()
                        if (total < s.waterGoalMl) Notifications.water(ctx, total, s.waterGoalMl)
                    }
                    ReminderScheduler.T_WEIGH ->
                        if (c.repo.weightToday() == null) Notifications.weighIn(ctx, s.displayName, followUp = false)
                    ReminderScheduler.T_WEIGH_FOLLOW ->
                        if (c.repo.weightToday() == null) Notifications.weighIn(ctx, s.displayName, followUp = true)
                    ReminderScheduler.T_MEAL -> {
                        val threeHours = System.currentTimeMillis() - 3 * 60 * 60 * 1000L
                        if (!c.repo.foodLoggedSince(threeHours)) Notifications.meal(ctx, ReminderScheduler.mealFor(ZonedDateTime.now()))
                    }
                    ReminderScheduler.T_WEEKLY -> Notifications.weekly(ctx)
                }
                ReminderScheduler.scheduleAll(ctx, s)
            }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val ctx = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ReminderScheduler.scheduleAll(ctx, ctx.container.settings.current())
            } finally {
                pending.finish()
            }
        }
    }
}

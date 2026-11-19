package com.healthtracker.app.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import com.healthtracker.app.MainActivity
import com.healthtracker.app.R
import com.healthtracker.app.domain.Fmt

object Notifications {
    const val CH_WATER = "water"
    const val CH_WATER_ALERT = "water_alert"
    const val CH_PAUSE = "pause_status"
    const val CH_WEIGH = "weigh"
    const val CH_MEALS = "meals"
    const val CH_WEEKLY = "weekly"

    const val ID_WATER = 101
    const val ID_WEIGH = 102
    const val ID_MEAL = 103
    const val ID_WEEKLY = 104
    const val ID_PAUSE_OFF = 105

    const val KEY_WEIGHT = "weight_kg"
    const val EXTRA_TAB = "tab"

    private const val BRAND = 0xFF1C5CAB.toInt()

    fun createChannels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannels(
            listOf(
                NotificationChannel(CH_WATER, "Water updates", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Quiet updates after you log water"
                },
                NotificationChannel(CH_WATER_ALERT, "Water alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Pop-up water reminders, including on the lock screen"
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                    enableVibration(true)
                },
                NotificationChannel(CH_PAUSE, "Food-app pause status", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Tells you when the food-app pause has been switched off"
                },
                NotificationChannel(CH_WEIGH, "Morning weigh-in", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Daily reminder to log your weight"
                },
                NotificationChannel(CH_MEALS, "Meal logging", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Reminders to log breakfast, lunch and dinner"
                },
                NotificationChannel(CH_WEEKLY, "Weekly review", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Your Sunday summary"
                },
            )
        )
    }

    fun canPost(ctx: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        return NotificationManagerCompat.from(ctx).areNotificationsEnabled()
    }

    fun openApp(ctx: Context, tab: String? = null, request: Int = 0): PendingIntent {
        val i = Intent(ctx, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if (tab != null) i.putExtra(EXTRA_TAB, tab)
        return PendingIntent.getActivity(ctx, 300 + request, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun base(ctx: Context, channel: String) =
        NotificationCompat.Builder(ctx, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(BRAND)
            .setAutoCancel(true)

    private fun post(ctx: Context, id: Int, n: android.app.Notification) {
        if (!canPost(ctx)) return
        try {
            NotificationManagerCompat.from(ctx).notify(id, n)
        } catch (_: SecurityException) {
        }
    }

    fun water(ctx: Context, drunkMl: Int, goalMl: Int) {
        val add = PendingIntent.getBroadcast(
            ctx, 201,
            Intent(ctx, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION_ADD_WATER),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val left = (goalMl - drunkMl).coerceAtLeast(0)
        val n = base(ctx, CH_WATER)
            .setContentTitle("Time for a glass of water")
            .setContentText("${Fmt.litres(drunkMl)} of ${Fmt.litres(goalMl)} L so far · ${Fmt.litres(left)} L to go")
            .setContentIntent(openApp(ctx, "today", 1))
            .addAction(0, "+250 ml", add)
            .setOnlyAlertOnce(true)
            .build()
        post(ctx, ID_WATER, n)
    }

    /** Pop-up water reminder: heads-up while using the phone, full screen when it's locked. */
    fun waterAlert(ctx: Context, drunkMl: Int, goalMl: Int, fullScreen: Boolean) {
        val add = PendingIntent.getBroadcast(
            ctx, 201,
            Intent(ctx, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION_ADD_WATER),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val later = PendingIntent.getBroadcast(
            ctx, 203,
            Intent(ctx, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION_SNOOZE_WATER),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val left = (goalMl - drunkMl).coerceAtLeast(0)
        val b = base(ctx, CH_WATER_ALERT)
            .setContentTitle("Water time · drink a glass now")
            .setContentText("${Fmt.litres(drunkMl)} of ${Fmt.litres(goalMl)} L so far · ${Fmt.litres(left)} L to go")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openApp(ctx, "today", 1))
            .addAction(0, "+250 ml", add)
            .addAction(0, "Snooze 15 min", later)
            .setTimeoutAfter(20 * 60 * 1000L)
        if (fullScreen) {
            val full = PendingIntent.getActivity(
                ctx, 204,
                Intent(ctx, WaterAlertActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            b.setFullScreenIntent(full, true)
        }
        post(ctx, ID_WATER, b.build())
    }

    fun canUseFullScreen(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < 34 || ctx.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()

    fun pauseOff(ctx: Context) {
        val n = base(ctx, CH_PAUSE)
            .setContentTitle("Your food-app pause is switched off")
            .setContentText("Swiggy and Zomato will open without your message. Tap to turn it back on.")
            .setContentIntent(openApp(ctx, "plan", 5))
            .build()
        post(ctx, ID_PAUSE_OFF, n)
    }

    fun waterDone(ctx: Context, goalMl: Int) {
        val n = base(ctx, CH_WATER)
            .setContentTitle("Water goal reached")
            .setContentText("${Fmt.litres(goalMl)} L today. Nice work.")
            .setContentIntent(openApp(ctx, "today", 1))
            .setOnlyAlertOnce(true)
            .setTimeoutAfter(60 * 60 * 1000L)
            .build()
        post(ctx, ID_WATER, n)
    }

    fun weighIn(ctx: Context, name: String, followUp: Boolean, retry: Boolean = false) {
        val replyIntent = Intent(ctx, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION_WEIGHT_REPLY)
        val flags = if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        else PendingIntent.FLAG_UPDATE_CURRENT
        val reply = PendingIntent.getBroadcast(ctx, 202, replyIntent, flags)
        val input = RemoteInput.Builder(KEY_WEIGHT).setLabel("Weight in kg, like 117.6").build()
        val action = NotificationCompat.Action.Builder(0, "Enter weight", reply)
            .addRemoteInput(input)
            .setAllowGeneratedReplies(false)
            .build()
        val title = when {
            retry -> "That didn't look like a weight"
            followUp -> "Still time to weigh in"
            else -> "Good morning, $name"
        }
        val text = when {
            retry -> "Type just the number in kg, for example 117.6"
            followUp -> "Weigh in before breakfast so the trend stays accurate."
            else -> "Step on the scale before breakfast, then type your weight here."
        }
        val n = base(ctx, CH_WEIGH)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openApp(ctx, "today", 2))
            .addAction(action)
            .build()
        post(ctx, ID_WEIGH, n)
    }

    fun weightSaved(ctx: Context, kg: Double, trend: Double?) {
        val text = if (trend != null) "7-day trend ${Fmt.kg(trend)} kg. The trend is the number that matters."
        else "Your trend line starts today."
        val n = base(ctx, CH_WEIGH)
            .setContentTitle("Saved ${Fmt.kg(kg)} kg")
            .setContentText(text)
            .setContentIntent(openApp(ctx, "weight", 2))
            .setTimeoutAfter(2 * 60 * 60 * 1000L)
            .build()
        post(ctx, ID_WEIGH, n)
    }

    fun meal(ctx: Context, meal: String) {
        val n = base(ctx, CH_MEALS)
            .setContentTitle("Log your $meal")
            .setContentText("It takes 10 seconds with your saved meals. Honest beats perfect.")
            .setContentIntent(openApp(ctx, "food", 3))
            .build()
        post(ctx, ID_MEAL, n)
    }

    fun weekly(ctx: Context) {
        val n = base(ctx, CH_WEEKLY)
            .setContentTitle("Your weekly review is ready")
            .setContentText("See how the week went and pick one focus for next week.")
            .setContentIntent(openApp(ctx, "review", 4))
            .build()
        post(ctx, ID_WEEKLY, n)
    }
}

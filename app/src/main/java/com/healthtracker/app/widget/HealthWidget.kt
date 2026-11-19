package com.healthtracker.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.widget.RemoteViews
import com.healthtracker.app.HealthApp
import com.healthtracker.app.R
import com.healthtracker.app.container
import com.healthtracker.app.domain.Fmt
import com.healthtracker.app.reminders.Notifications
import com.healthtracker.app.reminders.ReminderReceiver
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.PI
import kotlin.math.sin

/** Home-screen widget: today's water as a glass, the goal countdown, and a one-tap Drank button. */
class HealthWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        val app = context.applicationContext as HealthApp
        app.appScope.launch {
            try {
                render(app, manager, ids)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        /** Redraws every placed widget. Safe to call often; it does nothing when no widget is placed. */
        fun refresh(ctx: Context) {
            val app = ctx.applicationContext as? HealthApp ?: return
            app.appScope.launch {
                runCatching {
                    val manager = AppWidgetManager.getInstance(app)
                    val ids = manager.getAppWidgetIds(ComponentName(app, HealthWidget::class.java))
                    if (ids.isNotEmpty()) render(app, manager, ids)
                }
            }
        }

        private suspend fun render(ctx: Context, manager: AppWidgetManager, ids: IntArray) {
            val s = ctx.container.settings.current()
            val drunk = ctx.container.repo.waterToday()
            val goal = s.waterGoalMl.coerceAtLeast(1)
            val today = LocalDate.now().toEpochDay()
            val days = s.goalDay?.let { it - today }
            val countdown = when {
                !s.onboarded -> "Tap to set up"
                days == null -> "Add your birthday in the app"
                days == 0L -> if (s.goalName == "your birthday") "It's your birthday!" else "It's goal day!"
                days > 0 && s.goalName == "your birthday" -> "$days ${if (days == 1L) "day" else "days"} to your birthday"
                days > 0 -> "$days ${if (days == 1L) "day" else "days"} to ${s.goalName}"
                else -> "New goal starting"
            }
            val views = RemoteViews(ctx.packageName, R.layout.widget_health).apply {
                setImageViewBitmap(R.id.widget_glass, glassBitmap(ctx, drunk / goal.toFloat()))
                setTextViewText(R.id.widget_amount, "${Fmt.litres(drunk)} / ${Fmt.litres(goal)} L")
                setTextViewText(R.id.widget_countdown, countdown)
                setTextViewText(R.id.widget_drank, if (drunk >= goal) "Done" else "+${s.glassMl}")
                setOnClickPendingIntent(R.id.widget_root, Notifications.openApp(ctx, "today", 9))
                setOnClickPendingIntent(
                    R.id.widget_drank,
                    PendingIntent.getBroadcast(
                        ctx, 301,
                        Intent(ctx, ReminderReceiver::class.java)
                            .setAction(ReminderReceiver.ACTION_ADD_WATER)
                            .putExtra(ReminderReceiver.EXTRA_ML, s.glassMl)
                            .putExtra(ReminderReceiver.EXTRA_FROM_WIDGET, true),
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                    ),
                )
            }
            ids.forEach { manager.updateAppWidget(it, views) }
        }

        /** Draws the water glass as a bitmap, since widgets can't run Compose. */
        fun glassBitmap(ctx: Context, fraction: Float): Bitmap {
            val d = ctx.resources.displayMetrics.density
            val w = (56 * d).toInt().coerceAtLeast(1)
            val h = (84 * d).toInt().coerceAtLeast(1)
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val cv = Canvas(bmp)
            val r = 14 * d
            val inset = 3 * d
            val outer = RectF(inset / 2, inset / 2, w - inset / 2, h - inset / 2)
            val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x1FFFFFFF }
            cv.drawRoundRect(outer, r, r, fill)

            val inner = RectF(inset * 2, inset * 2, w - inset * 2, h - inset * 2)
            val clip = Path().apply { addRoundRect(inner, r - inset, r - inset, Path.Direction.CW) }
            cv.save()
            cv.clipPath(clip)
            val f = fraction.coerceIn(0f, 1f)
            if (f > 0f) {
                val level = inner.bottom - inner.height() * f
                val wave = Path().apply {
                    moveTo(inner.left - 10, inner.bottom + 10)
                    var x = inner.left - 10
                    while (x <= inner.right + 10) {
                        lineTo(x, level + 2.5f * d * sin((x / (w * 0.9f)) * 2f * PI.toFloat()))
                        x += 2 * d
                    }
                    lineTo(inner.right + 10, inner.bottom + 10)
                    close()
                }
                val water = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(
                        0f, level, 0f, inner.bottom,
                        intArrayOf(0xFF8AF4FF.toInt(), 0xFF34D3F6.toInt(), 0xFF1B5FD6.toInt()), null, Shader.TileMode.CLAMP,
                    )
                }
                cv.drawPath(wave, water)
            }
            val glare = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x33FFFFFF }
            cv.drawRoundRect(RectF(inner.left + 4 * d, inner.top + 6 * d, inner.left + 8 * d, inner.top + inner.height() * 0.7f), 3 * d, 3 * d, glare)
            cv.restore()
            val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 2 * d
                color = 0x55FFFFFF
            }
            cv.drawRoundRect(outer, r, r, stroke)
            return bmp
        }
    }
}

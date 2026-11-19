package com.healthtracker.app.reminders

import android.app.NotificationManager
import android.content.Context
import android.graphics.PixelFormat
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.healthtracker.app.HealthApp
import com.healthtracker.app.container
import com.healthtracker.app.domain.Fmt
import com.healthtracker.app.ui.components.AuroraBackground
import com.healthtracker.app.ui.components.ConfettiBurst
import com.healthtracker.app.ui.components.LiquidGlass
import com.healthtracker.app.ui.components.Meter
import com.healthtracker.app.ui.components.PrimaryButton
import com.healthtracker.app.ui.components.SecondaryButton
import com.healthtracker.app.ui.theme.App
import com.healthtracker.app.ui.theme.HealthTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun logWater(ctx: Context, ml: Int) {
    val app = ctx.applicationContext as HealthApp
    app.appScope.launch { app.container.repo.addWater(ml) }
    ctx.getSystemService(NotificationManager::class.java)?.cancel(Notifications.ID_WATER)
}

private fun snooze(ctx: Context) {
    ReminderScheduler.snoozeWater(ctx.applicationContext, 15)
    ctx.getSystemService(NotificationManager::class.java)?.cancel(Notifications.ID_WATER)
}

/** The water reminder card used both on top of other apps and on the lock screen. */
@Composable
fun WaterAlertCard(
    drunkMl: Int,
    goalMl: Int,
    glassMl: Int,
    secondsLeft: Int?,
    totalSeconds: Int,
    onAdd: (Int) -> Unit,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = App.colors
    var added by remember { mutableIntStateOf(0) }
    val now = drunkMl + added
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF14203A), Color(0xFF0B1020))))
            .border(1.dp, Brush.linearGradient(listOf(c.water.copy(alpha = 0.8f), c.data.copy(alpha = 0.3f))), RoundedCornerShape(28.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (secondsLeft != null) Meter(secondsLeft / totalSeconds.toFloat(), color = c.water, track = Color.White.copy(alpha = 0.08f), height = 3.dp)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LiquidGlass(now / goalMl.toFloat(), Modifier.width(70.dp).height(104.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.WaterDrop, null, tint = c.water, modifier = Modifier.size(18.dp))
                    Text(if (added > 0) "Nice. Logged!" else "Water time", style = MaterialTheme.typography.titleLarge, color = Color.White)
                }
                Text("${Fmt.litres(now)} of ${Fmt.litres(goalMl)} L today", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.8f))
                Text(
                    if (now >= goalMl) "Goal reached for today." else "${Fmt.litres(goalMl - now)} L to go. A glass now keeps cravings down.",
                    style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f),
                )
            }
            IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, "Dismiss", tint = Color.White.copy(alpha = 0.7f)) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton("Drank $glassMl ml", onClick = { added += glassMl; onAdd(glassMl) }, modifier = Modifier.weight(1.6f), enabled = added == 0)
            SecondaryButton("Skip", onClick = onDismiss, modifier = Modifier.weight(1f), contentColor = Color.White)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            androidx.compose.material3.TextButton(onClick = { added += glassMl * 2; onAdd(glassMl * 2) }, enabled = added == 0) {
                Text("Drank ${glassMl * 2} ml", color = c.water)
            }
            androidx.compose.material3.TextButton(onClick = onSnooze) { Text("Remind me in 15 min", color = Color.White.copy(alpha = 0.75f)) }
        }
    }
}

/** Lifecycle for a Compose view that lives outside an activity (the overlay window). */
private class OverlayOwner : LifecycleOwner, SavedStateRegistryOwner {
    private val registry = LifecycleRegistry(this)
    private val saved = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = registry
    override val savedStateRegistry: SavedStateRegistry get() = saved.savedStateRegistry

    fun start() {
        saved.performRestore(null)
        registry.currentState = Lifecycle.State.RESUMED
    }

    fun stop() {
        registry.currentState = Lifecycle.State.DESTROYED
    }
}

/** A floating water card drawn on top of other apps by the accessibility service. Hides after 12 seconds. */
class WaterOverlay(private val ctx: android.accessibilityservice.AccessibilityService) {
    private val wm = ctx.getSystemService(WindowManager::class.java)
    private val main = Handler(Looper.getMainLooper())
    private var view: ComposeView? = null
    private var owner: OverlayOwner? = null

    fun show(drunkMl: Int, goalMl: Int, glassMl: Int, seconds: Int = 12) {
        val o = OverlayOwner().also { it.start() }
        val v = ComposeView(ctx).apply {
            setViewTreeLifecycleOwner(o)
            setViewTreeSavedStateRegistryOwner(o)
            setContent {
                HealthTheme {
                    var left by remember { mutableIntStateOf(seconds) }
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        visible = true
                        while (left > 0) { delay(1000); left-- }
                        hide()
                    }
                    AnimatedVisibility(visible, enter = slideInVertically { it } + fadeIn()) {
                        WaterAlertCard(
                            drunkMl, goalMl, glassMl, left, seconds,
                            onAdd = { ml -> logWater(ctx, ml); main.postDelayed({ hide() }, 1200) },
                            onSnooze = { snooze(ctx); hide() },
                            onDismiss = { ctx.getSystemService(NotificationManager::class.java)?.cancel(Notifications.ID_WATER); hide() },
                            modifier = Modifier.padding(horizontal = 12.dp),
                        )
                    }
                }
            }
        }
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.BOTTOM
            y = (ctx.resources.displayMetrics.density * 110).toInt()
        }
        wm.addView(v, lp)
        view = v
        owner = o
    }

    fun hide() {
        main.post {
            view?.let { runCatching { wm.removeView(it) } }
            owner?.stop()
            view = null
            owner = null
        }
    }
}

/** Full-screen water reminder that wakes the screen and shows over the lock screen. */
class WaterAlertActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        lifecycleScope.launch {
            val st = container.settings.current()
            val drunk = container.repo.waterToday()
            setContent { HealthTheme { WaterAlertScreen(drunk, st.waterGoalMl, st.glassMl) } }
        }
        Handler(Looper.getMainLooper()).postDelayed({ if (!isFinishing) finish() }, 60_000)
    }

    @Composable
    private fun WaterAlertScreen(drunk: Int, goal: Int, glass: Int) {
        val c = App.colors
        var added by remember { mutableIntStateOf(0) }
        var burst by remember { mutableIntStateOf(0) }
        val pop = remember { Animatable(0.9f) }
        LaunchedEffect(Unit) { pop.animateTo(1f, tween(500, easing = LinearEasing)) }
        Box(Modifier.fillMaxSize().background(Color(0xFF050A14))) {
            AuroraBackground(listOf(c.water, c.data, Color(0xFF1B5FD6)), height = 520.dp, fadeTo = Color(0xFF050A14))
            Column(
                Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(24.dp))
                Text("Water time", style = MaterialTheme.typography.displaySmall, color = Color.White)
                Text(
                    "${Fmt.litres(drunk + added)} of ${Fmt.litres(goal)} L today",
                    style = MaterialTheme.typography.titleLarge, color = Color.White.copy(alpha = 0.8f),
                )
                LiquidGlass((drunk + added) / goal.toFloat(), Modifier.width(170.dp).height(260.dp))
                Spacer(Modifier.weight(1f))
                PrimaryButton(if (added > 0) "Logged. Nice!" else "Drank $glass ml", enabled = added == 0, onClick = {
                    added += glass; burst++; logWater(this@WaterAlertActivity, glass)
                    Handler(Looper.getMainLooper()).postDelayed({ finish() }, 1400)
                })
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton("Drank ${glass * 2} ml", enabled = added == 0, onClick = {
                        added += glass * 2; burst++; logWater(this@WaterAlertActivity, glass * 2)
                        Handler(Looper.getMainLooper()).postDelayed({ finish() }, 1400)
                    }, modifier = Modifier.weight(1f), contentColor = Color.White)
                    SecondaryButton("Skip", onClick = {
                        getSystemService(NotificationManager::class.java)?.cancel(Notifications.ID_WATER); finish()
                    }, modifier = Modifier.weight(1f), contentColor = Color.White)
                }
                SecondaryButton("Remind me in 15 min", onClick = { snooze(this@WaterAlertActivity); finish() }, contentColor = Color.White.copy(alpha = 0.75f), borderColor = Color.Transparent)
            }
            ConfettiBurst(burst)
        }
    }
}

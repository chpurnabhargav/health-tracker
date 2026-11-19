package com.healthtracker.app.pause

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CurrencyRupee
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.SouthEast
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.healthtracker.app.HealthApp
import com.healthtracker.app.container
import com.healthtracker.app.data.PauseMode
import com.healthtracker.app.domain.Calc
import com.healthtracker.app.domain.Fmt
import com.healthtracker.app.domain.PauseRules
import com.healthtracker.app.ui.components.AuroraBackground
import com.healthtracker.app.ui.components.PrimaryButton
import com.healthtracker.app.ui.components.Ring
import com.healthtracker.app.ui.components.SecondaryButton
import com.healthtracker.app.ui.theme.App
import com.healthtracker.app.ui.theme.Brushes
import com.healthtracker.app.ui.theme.HealthTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

data class PauseInfo(
    val headline: String,
    val sub: String?,
    val daysLeft: Long?,
    val goalLine: String,
    val lostLine: String?,
    val monthLine: String?,
    val waitSec: Int,
    val plannedLeft: Int,
    val mode: PauseMode = PauseMode.STRICT,
    val continuesToday: Int = 0,
    val moneySaved: Int = 0,
    val kcalLeft: Int? = null,
    val photoPath: String? = null,
)

fun motivationPhoto(ctx: Context): File = File(ctx.filesDir, "motivation.jpg")

class PauseActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PKG = "pkg"
        const val EXTRA_NAME = "name"
    }

    private val pkg: String get() = intent.getStringExtra(EXTRA_PKG).orEmpty()
    private val appName: String get() = intent.getStringExtra(EXTRA_NAME) ?: "this app"
    private val isPreview: Boolean get() = pkg.isEmpty()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this) { close() }
        render()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        render()
    }

    private fun render() {
        val name = appName
        val preview = isPreview
        val key = pkg + System.currentTimeMillis()
        setContent {
            HealthTheme {
                val ctx = this
                val info by produceState<PauseInfo?>(null, key) { value = loadPauseInfo(ctx) }
                PauseContent(
                    info = info,
                    appName = name,
                    preview = preview,
                    onClose = ::close,
                    onContinue = { order, planned -> proceed(order, planned) },
                )
            }
        }
    }

    private fun close() {
        if (!isPreview) {
            val app = application as HealthApp
            val p = pkg
            val n = appName
            app.appScope.launch { app.container.repo.logPause(p, n, "closed") }
            runCatching {
                startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
        finish()
    }

    private fun proceed(order: PauseRules.OrderSize?, planned: Boolean) {
        if (!isPreview) {
            val app = application as HealthApp
            val p = pkg
            val n = appName
            app.appScope.launch {
                app.container.repo.logPause(p, n, if (planned) "planned" else "continued")
                if (order != null) app.container.repo.logOrder(n, order.label, order.kcal, planned)
            }
            PauseGate.allowSession(pkg)
        }
        finish()
    }
}

suspend fun loadPauseInfo(ctx: Context): PauseInfo {
    val c = ctx.container
    val s = c.settings.current()
    val today = LocalDate.now()
    val day = today.toEpochDay()
    val weights = c.db.dao().allWeights()
    val trend = Calc.trendOn(weights, day) ?: s.startWeight
    val lost = s.startWeight - trend
    val all = c.repo.allPauses()
    val monthStart = today.withDayOfMonth(1).toEpochDay()
    val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toEpochDay()
    val monthPauses = all.filter { it.day >= monthStart }
    val closed = monthPauses.count { it.outcome == "closed" }
    val plannedUsed = all.count { it.outcome == "planned" && it.day >= weekStart }
    val continuesToday = all.count { it.outcome == "continued" && it.day == day }
    val eaten = c.db.dao().allFood().filter { it.day == day }.sumOf { it.kcal }
    val msg = s.pauseMessage
    val parts = msg.split(", ", limit = 2)
    val auto = s.customPauseMessage.isNullOrBlank() && parts.size == 2
    return PauseInfo(
        headline = if (auto) parts[0] + "." else msg,
        sub = if (auto) parts[1].replaceFirstChar { it.uppercase() } else null,
        daysLeft = s.goalDay?.let { it - day },
        goalLine = if (s.goalName == "your birthday") "Birthday goal ${Fmt.kg(s.goalKg)} kg" else "${s.goalName} · ${Fmt.kg(s.goalKg)} kg",
        lostLine = if (lost >= 0.1) "You've lost ${Fmt.kg(lost)} kg so far." else "Every day you skip this counts.",
        monthLine = if (monthPauses.isNotEmpty()) "This month you closed this screen $closed of ${monthPauses.size} times." else null,
        waitSec = PauseRules.waitSeconds(s.pauseMode, s.pauseWaitSec.coerceIn(3, 60), continuesToday),
        plannedLeft = (s.plannedMealsPerWeek - plannedUsed).coerceAtLeast(0),
        mode = s.pauseMode,
        continuesToday = continuesToday,
        moneySaved = PauseRules.moneySaved(all.count { it.outcome == "closed" }, s.orderValue),
        kcalLeft = Calc.calorieTarget(s, today) - eaten,
        photoPath = motivationPhoto(ctx).takeIf { it.exists() }?.path,
    )
}

private enum class Step { MAIN, PROMISE, ORDER }

private fun clockText(sec: Int): String = if (sec >= 60) "${sec / 60}:${(sec % 60).toString().padStart(2, '0')}" else "$sec"

@Composable
fun PauseContent(
    info: PauseInfo?,
    appName: String,
    preview: Boolean,
    onClose: () -> Unit,
    onContinue: (PauseRules.OrderSize?, Boolean) -> Unit,
) {
    val c = App.colors
    val haptic = LocalHapticFeedback.current
    Box(Modifier.fillMaxSize().background(Color(0xFF0A0508))) {
        AuroraBackground(listOf(c.coral, Color(0xFFB0124A), c.amber), height = 560.dp, fadeTo = Color(0xFF0A0508))
        val i = info ?: return@Box
        var left by remember(i.waitSec) { mutableIntStateOf(i.waitSec) }
        var step by remember { mutableStateOf(Step.MAIN) }
        var planned by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
        LaunchedEffect(i.waitSec) {
            while (left > 0) {
                delay(1000)
                left -= 1
            }
        }
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(if (i.mode == PauseMode.LOCKDOWN) Icons.Rounded.Lock else Icons.Rounded.PauseCircle, null, tint = c.coral, modifier = Modifier.size(20.dp))
                Text(
                    (if (preview) "Preview" else "You opened $appName") + " · ${i.mode.label} mode",
                    style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f),
                )
            }
            AnimatedContent(step, transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) }, label = "step") { st ->
                when (st) {
                    Step.MAIN -> MainStep(
                        i, appName, preview, left, onClose,
                        onPlanned = { planned = true; step = Step.ORDER },
                        onContinue = {
                            when {
                                i.mode == PauseMode.GENTLE -> onContinue(null, false)
                                PauseRules.needsPromise(i.mode) -> step = Step.PROMISE
                                else -> step = Step.ORDER
                            }
                        },
                    )
                    Step.PROMISE -> PromiseStep(onBack = onClose, onDone = { planned = false; step = Step.ORDER })
                    Step.ORDER -> OrderStep(appName, planned, i.kcalLeft, onBack = onClose) { size -> onContinue(size, planned) }
                }
            }
        }
    }
}

@Composable
private fun MainStep(
    i: PauseInfo,
    appName: String,
    preview: Boolean,
    left: Int,
    onClose: () -> Unit,
    onPlanned: () -> Unit,
    onContinue: () -> Unit,
) {
    val c = App.colors
    val pulse = rememberInfiniteTransition(label = "pulse")
    val scale by pulse.animateFloat(1f, 1.04f, infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse), label = "s")
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale; transformOrigin = TransformOrigin(0f, 0.5f) },
        ) {
            Text(i.headline, style = MaterialTheme.typography.displayMedium, color = Color.White)
            if (i.sub != null) Text(i.sub, style = MaterialTheme.typography.headlineSmall, color = Color(0xFFFFB3C1))
        }
        i.photoPath?.let { path ->
            val bmp = remember(path) {
                runCatching {
                    val o = BitmapFactory.Options().apply { inSampleSize = 2 }
                    BitmapFactory.decodeFile(path, o)?.asImageBitmap()
                }.getOrNull()
            }
            if (bmp != null) {
                Box(Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(24.dp))) {
                    Image(bmp, contentDescription = "Your motivation photo", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)))))
                    Text(
                        "Remember why you started.", style = MaterialTheme.typography.titleMedium, color = Color.White,
                        modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
                    )
                }
            }
        }
        if (i.daysLeft != null && i.daysLeft >= 0) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp)).padding(20.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("${i.daysLeft}", style = MaterialTheme.typography.displaySmall, color = Color.White)
                Column {
                    Text(if (i.daysLeft == 1L) "day to go" else "days to go", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Text(i.goalLine, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.75f))
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (i.moneySaved > 0) HeroLine(Icons.Rounded.CurrencyRupee, "₹${Fmt.int(i.moneySaved)} saved so far by closing this screen.")
            i.lostLine?.let { HeroLine(Icons.Rounded.SouthEast, it) }
            i.monthLine?.let { HeroLine(Icons.Rounded.Check, it) }
            i.kcalLeft?.let { if (it > 0) HeroLine(Icons.Rounded.Check, "You still have ${Fmt.int(it)} kcal left today for real food.") }
        }

        if (left > 0) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Ring(fraction = left / i.waitSec.toFloat(), size = 76.dp, stroke = 7.dp, color = c.coral, track = Color.White.copy(alpha = 0.12f)) {
                    Text(clockText(left), style = MaterialTheme.typography.headlineSmall, color = Color.White)
                }
                Text(
                    when {
                        i.mode == PauseMode.LOCKDOWN -> "Lockdown is on. Only a planned meal, or an emergency unlock after this wait."
                        i.continuesToday > 0 -> "You've already ordered ${i.continuesToday}× today, so the wait is longer."
                        else -> "Take a breath. Drink a glass of water while you wait."
                    },
                    style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.85f),
                )
            }
        }
        PrimaryButton(if (preview) "Close preview" else "Close $appName", onClick = onClose)
        if (i.plannedLeft > 0 && left == 0) {
            SecondaryButton("Use this week's planned meal", onClick = onPlanned, contentColor = Color.White)
        }
        SecondaryButton(
            text = when {
                left > 0 -> "Locked · ${clockText(left)}${if (left < 60) " s" else ""}"
                i.mode == PauseMode.LOCKDOWN -> "Emergency unlock"
                i.mode == PauseMode.STRICT -> "I still want to order"
                else -> "Order anyway and log it"
            },
            onClick = onContinue,
            enabled = left == 0,
            contentColor = Color.White,
            borderColor = Color.White.copy(alpha = 0.3f),
        )
        Text(
            "Closing counts as a win. Ordering is logged honestly, with no guilt.",
            style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.65f),
        )
    }
}

@Composable
private fun PromiseStep(onBack: () -> Unit, onDone: () -> Unit) {
    var typed by remember { mutableStateOf("") }
    val ok = PauseRules.promiseMatches(typed)
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text("Say it to yourself", style = MaterialTheme.typography.headlineMedium, color = Color.White)
        Text("Type this sentence exactly to continue:", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.8f))
        Text("“${PauseRules.PROMISE}”", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = Color(0xFFFFB3C1))
        OutlinedTextField(
            value = typed, onValueChange = { typed = it.take(80) }, singleLine = true,
            placeholder = { Text("Type it here") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                focusedBorderColor = Color.White.copy(alpha = 0.6f), unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                cursorColor = Color.White,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        PrimaryButton("I changed my mind", onClick = onBack)
        SecondaryButton("Continue", onClick = onDone, enabled = ok, contentColor = Color.White, borderColor = Color.White.copy(alpha = 0.3f))
    }
}

@Composable
private fun OrderStep(appName: String, planned: Boolean, kcalLeft: Int?, onBack: () -> Unit, onPick: (PauseRules.OrderSize) -> Unit) {
    val c = App.colors
    var picked by remember { mutableStateOf<PauseRules.OrderSize?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(if (planned) "Enjoy your planned meal" else "Log it first", style = MaterialTheme.typography.headlineMedium, color = Color.White)
        Text(
            "How big is this order? It goes into today's calories${kcalLeft?.let { " (${Fmt.int(it.coerceAtLeast(0))} kcal left)" } ?: ""}.",
            style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.8f),
        )
        PauseRules.orderSizes.forEach { o ->
            val sel = picked == o
            Row(
                Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(18.dp))
                    .background(if (sel) c.primary.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.06f))
                    .border(if (sel) 2.dp else 1.dp, if (sel) c.primary else Color.White.copy(alpha = 0.15f), RoundedCornerShape(18.dp))
                    .clickable(role = Role.RadioButton) { picked = o }.padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(o.label, style = MaterialTheme.typography.titleSmall, color = Color.White, modifier = Modifier.weight(1f))
                Text("~${Fmt.int(o.kcal)} kcal", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.75f))
            }
        }
        Spacer(Modifier.height(4.dp))
        PrimaryButton("Log it and open $appName", onClick = { picked?.let(onPick) }, enabled = picked != null)
        SecondaryButton("I changed my mind", onClick = onBack, contentColor = Color.White)
    }
}

@Composable
private fun HeroLine(icon: ImageVector, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, contentDescription = null, tint = Color(0xFFFFB3C1), modifier = Modifier.size(20.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, color = Color.White)
    }
}

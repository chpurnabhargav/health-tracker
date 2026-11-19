package com.healthtracker.app.pause

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.SouthEast
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.healthtracker.app.HealthApp
import com.healthtracker.app.container
import com.healthtracker.app.domain.Calc
import com.healthtracker.app.domain.Fmt
import com.healthtracker.app.ui.components.PrimaryButton
import com.healthtracker.app.ui.components.Ring
import com.healthtracker.app.ui.components.SecondaryButton
import com.healthtracker.app.ui.theme.App
import com.healthtracker.app.ui.theme.HealthTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
)

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
        setContent {
            HealthTheme {
                PauseScreen(
                    appName = name,
                    preview = preview,
                    onClose = ::close,
                    onContinue = { proceed("continued") },
                    onPlanned = { proceed("planned") },
                )
            }
        }
    }

    private fun log(outcome: String) {
        if (isPreview) return
        val app = application as HealthApp
        val p = pkg
        val n = appName
        app.appScope.launch { app.container.repo.logPause(p, n, outcome) }
    }

    private fun close() {
        log("closed")
        if (!isPreview) {
            runCatching {
                startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
        finish()
    }

    private fun proceed(outcome: String) {
        log(outcome)
        if (!isPreview) PauseGate.allow(pkg, 45)
        finish()
    }

    @Composable
    private fun PauseScreen(
        appName: String,
        preview: Boolean,
        onClose: () -> Unit,
        onContinue: () -> Unit,
        onPlanned: () -> Unit,
    ) {
        val ctx = this
        val info by produceState<PauseInfo?>(null) { value = loadPauseInfo(ctx) }
        PauseContent(info, appName, preview, onClose, onContinue, onPlanned)
    }
}

suspend fun loadPauseInfo(ctx: android.content.Context): PauseInfo {
    val s = ctx.container.settings.current()
    val today = LocalDate.now()
    val weights = ctx.container.db.dao().allWeights()
    val trend = Calc.trendOn(weights, today.toEpochDay()) ?: s.startWeight
    val lost = s.startWeight - trend
    val monthStart = today.withDayOfMonth(1).toEpochDay()
    val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toEpochDay()
    val recent = ctx.container.repo.pausesSince(minOf(monthStart, weekStart))
    val monthPauses = recent.filter { it.day >= monthStart }
    val closed = monthPauses.count { it.outcome == "closed" }
    val plannedUsed = recent.count { it.outcome == "planned" && it.day >= weekStart }
    val msg = s.pauseMessage
    val parts = msg.split(", ", limit = 2)
    val headline = if (parts.size == 2 && s.customPauseMessage.isNullOrBlank()) parts[0] + "." else msg
    val sub = if (parts.size == 2 && s.customPauseMessage.isNullOrBlank()) parts[1].replaceFirstChar { it.uppercase() } else null
    val daysLeft = s.goalDay?.let { it - today.toEpochDay() }
    val goalLine = if (s.goalName == "your birthday") "Birthday goal ${Fmt.kg(s.goalKg)} kg" else "${s.goalName} · ${Fmt.kg(s.goalKg)} kg"
    return PauseInfo(
        headline = headline,
        sub = sub,
        daysLeft = daysLeft,
        goalLine = goalLine,
        lostLine = if (lost >= 0.1) "You've lost ${Fmt.kg(lost)} kg so far." else "Every day you skip this counts.",
        monthLine = if (monthPauses.isNotEmpty()) "This month you closed this screen $closed of ${monthPauses.size} times." else null,
        waitSec = s.pauseWaitSec.coerceIn(3, 60),
        plannedLeft = (s.plannedMealsPerWeek - plannedUsed).coerceAtLeast(0),
    )
}

@Composable
fun PauseContent(
    info: PauseInfo?,
    appName: String,
    preview: Boolean,
    onClose: () -> Unit,
    onContinue: () -> Unit,
    onPlanned: () -> Unit,
) {
    val c = App.colors
    Box(Modifier.fillMaxSize().background(c.hero)) {
        val i = info ?: return@Box
        var left by remember(i.waitSec) { mutableIntStateOf(i.waitSec) }
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.PauseCircle, contentDescription = null, tint = c.heroMuted, modifier = Modifier.size(20.dp))
                Text(if (preview) "Preview of the pause screen" else "You opened $appName", style = MaterialTheme.typography.bodyMedium, color = c.heroMuted)
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(i.headline, style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold), color = c.onHero)
                if (i.sub != null) Text(i.sub, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Medium), color = c.heroMuted)
            }
            if (i.daysLeft != null && i.daysLeft >= 0) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text("${i.daysLeft}", style = MaterialTheme.typography.displaySmall, color = c.onHero)
                    Column {
                        Text(if (i.daysLeft == 1L) "day to go" else "days to go", style = MaterialTheme.typography.titleMedium, color = c.onHero)
                        Text(i.goalLine, style = MaterialTheme.typography.bodyMedium, color = c.heroMuted)
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (i.lostLine != null) HeroLine(Icons.Rounded.SouthEast, i.lostLine)
                if (i.monthLine != null) HeroLine(Icons.Rounded.Check, i.monthLine)
            }

            Spacer(Modifier.height(4.dp))
            if (left > 0) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Ring(
                        fraction = left / i.waitSec.toFloat(),
                        size = 72.dp, stroke = 6.dp, color = c.heroFill, track = c.heroTrack,
                    ) { Text("$left", style = MaterialTheme.typography.headlineSmall, color = c.onHero) }
                    Text("Take a breath. Drink a glass of water while you wait.", style = MaterialTheme.typography.bodyLarge, color = c.heroMuted)
                }
            }
            PrimaryButton(
                text = if (preview) "Close preview" else "Close $appName",
                onClick = onClose,
                container = Color.White,
                content = c.hero,
            )
            if (i.plannedLeft > 0) {
                SecondaryButton(
                    text = if (left > 0) "Planned meal out in $left s" else "Use this week's planned meal",
                    onClick = onPlanned,
                    enabled = left == 0,
                    contentColor = Color.White,
                    borderColor = Color.White.copy(alpha = 0.5f),
                )
            }
            SecondaryButton(
                text = if (left > 0) "Continue in $left s" else "Order anyway and log it",
                onClick = onContinue,
                enabled = left == 0,
                contentColor = Color.White,
                borderColor = Color.White.copy(alpha = 0.5f),
            )
            Text(
                "Ordering isn't banned. It just gets logged honestly, with no guilt.",
                style = MaterialTheme.typography.bodySmall,
                color = c.heroMuted,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun HeroLine(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    val c = App.colors
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, contentDescription = null, tint = c.heroFill, modifier = Modifier.size(20.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, color = c.onHero)
    }
}

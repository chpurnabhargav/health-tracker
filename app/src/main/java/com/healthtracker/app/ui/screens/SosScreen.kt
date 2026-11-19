package com.healthtracker.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Close
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.healthtracker.app.data.FoodCatalog
import com.healthtracker.app.domain.Fmt
import com.healthtracker.app.ui.HealthViewModel
import com.healthtracker.app.ui.components.ConfettiBurst
import com.healthtracker.app.ui.components.LiquidGlass
import com.healthtracker.app.ui.components.PrimaryButton
import com.healthtracker.app.ui.components.Ring
import com.healthtracker.app.ui.components.SecondaryButton
import com.healthtracker.app.ui.theme.App
import kotlinx.coroutines.delay

private enum class SosStep(val title: String, val body: String, val seconds: Int) {
    WATER("Drink a full glass of water", "Thirst often feels like hunger. Sip it slowly.", 45),
    BREATHE("Breathe slowly", "In for 4 seconds, out for 6. Follow the circle.", 60),
    MOVE("Move for 90 seconds", "Walk around, climb some stairs, or do 10 slow squats.", 90),
    MINT("Reset your taste", "Brush your teeth or chew mint gum. Food tastes worse after mint.", 30),
    CHECK("Did the craving pass?", "Most cravings fade within 10 to 15 minutes.", 0),
    SNACK("Then have a smart snack", "You might be really hungry. Pick one under 200 kcal and log it.", 0),
    DONE("Craving beaten", "", 0),
}

/** Snacks under 200 kcal from the built-in food list. */
private val smartSnacks = listOf(
    "Buttermilk / chaas", "Curd / dahi", "Apple", "Papaya", "Boiled egg", "Sprouts salad", "Coconut water", "Roasted peanuts",
).mapNotNull { n -> FoodCatalog.all.find { it.name == n } }

@Composable
fun SosScreen(vm: HealthViewModel, onDone: () -> Unit) {
    val st by vm.state.collectAsStateWithLifecycle()
    val c = App.colors
    val glass = st?.s?.glassMl ?: 250
    var step by remember { mutableStateOf(SosStep.WATER) }
    var left by remember(step) { mutableIntStateOf(step.seconds) }
    var drank by remember { mutableIntStateOf(0) }
    var burst by remember { mutableIntStateOf(0) }
    val order = listOf(SosStep.WATER, SosStep.BREATHE, SosStep.MOVE, SosStep.MINT, SosStep.CHECK)

    fun next() {
        step = when (step) {
            SosStep.WATER -> SosStep.BREATHE
            SosStep.BREATHE -> SosStep.MOVE
            SosStep.MOVE -> SosStep.MINT
            SosStep.MINT -> SosStep.CHECK
            else -> SosStep.DONE
        }
    }

    LaunchedEffect(step) {
        if (step.seconds > 0) {
            while (left > 0) { delay(1000); left-- }
            if (step == SosStep.BREATHE || step == SosStep.MOVE || step == SosStep.MINT) next()
        }
    }

    Box(Modifier.fillMaxSize().background(c.bg)) {
        com.healthtracker.app.ui.components.AuroraBackground(listOf(c.amber, c.coral, c.data))
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Craving SOS", style = MaterialTheme.typography.titleLarge, color = c.ink, modifier = Modifier.weight(1f))
                IconButton(onClick = onDone) { Icon(Icons.Rounded.Close, "Close", tint = c.ink2) }
            }
            if (step in order) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    order.forEachIndexed { i, s ->
                        val reached = order.indexOf(step) >= i
                        Box(Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(if (reached) c.amber else c.track))
                    }
                }
            }
            AnimatedContent(step, transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) }, label = "sos") { s ->
                Column(verticalArrangement = Arrangement.spacedBy(18.dp), horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    if (s != SosStep.DONE) {
                        Text(s.title, style = MaterialTheme.typography.headlineMedium, color = c.ink, textAlign = TextAlign.Center)
                        Text(s.body, style = MaterialTheme.typography.bodyLarge, color = c.ink2, textAlign = TextAlign.Center)
                    }
                    when (s) {
                        SosStep.WATER -> {
                            LiquidGlass(if (drank > 0) 0.9f else 0.25f, Modifier.width(150.dp).height(220.dp))
                            PrimaryButton(if (drank > 0) "Nice. Next step" else "Drank $glass ml", onClick = {
                                if (drank == 0) { drank = glass; vm.addWater(glass) }
                                next()
                            }, container = c.water)
                            SecondaryButton("Skip this step", onClick = ::next)
                        }
                        SosStep.BREATHE -> {
                            BreathingCircle()
                            Timer(left, s.seconds, c.data)
                            SecondaryButton("Next", onClick = ::next)
                        }
                        SosStep.MOVE -> {
                            StepIcon(Icons.AutoMirrored.Rounded.DirectionsWalk, c.primary)
                            Timer(left, s.seconds, c.primary)
                            SecondaryButton("Done, next", onClick = ::next)
                        }
                        SosStep.MINT -> {
                            StepIcon(Icons.Rounded.AcUnit, c.water)
                            Timer(left, s.seconds, c.water)
                            SecondaryButton("Done, next", onClick = ::next)
                        }
                        SosStep.CHECK -> {
                            Spacer(Modifier.height(8.dp))
                            PrimaryButton("Yes, it passed", onClick = { vm.cravingBeaten(); burst++; step = SosStep.DONE })
                            SecondaryButton("No, I'm still hungry", onClick = { step = SosStep.SNACK })
                        }
                        SosStep.SNACK -> {
                            smartSnacks.forEach { f ->
                                Row(
                                    Modifier.fillMaxWidth().heightIn(min = 60.dp).clip(RoundedCornerShape(18.dp))
                                        .background(c.card2).border(1.dp, c.hairline, RoundedCornerShape(18.dp))
                                        .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(f.name, style = MaterialTheme.typography.titleSmall, color = c.ink)
                                        Text("${f.serving} · ${f.kcal} kcal · ${f.protein.toInt()} g protein", style = MaterialTheme.typography.bodySmall, color = c.ink2)
                                    }
                                    androidx.compose.material3.TextButton(onClick = {
                                        vm.addCustomFood(f.name, f.serving, f.kcal, f.protein, f.carbs, f.fat, false, 1.0)
                                        vm.cravingBeaten(); burst++; step = SosStep.DONE
                                    }) { Text("Log it", color = c.primary) }
                                }
                            }
                        }
                        SosStep.DONE -> {
                            Spacer(Modifier.height(40.dp))
                            Box(
                                Modifier.size(140.dp).clip(CircleShape)
                                    .background(Brush.radialGradient(listOf(c.primary.copy(alpha = 0.5f), Color.Transparent))),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("${(st?.s?.cravingWins ?: 0).coerceAtLeast(1)}", style = MaterialTheme.typography.displayLarge, color = c.primary)
                            }
                            Text("Craving beaten", style = MaterialTheme.typography.headlineLarge, color = c.ink)
                            Text(
                                "That's ${(st?.s?.cravingWins ?: 1).coerceAtLeast(1)} so far. Every one makes the next one easier.",
                                style = MaterialTheme.typography.bodyLarge, color = c.ink2, textAlign = TextAlign.Center,
                            )
                            PrimaryButton("Back to Today", onClick = onDone)
                        }
                    }
                }
            }
        }
        ConfettiBurst(burst)
    }
}

@Composable
private fun Timer(left: Int, total: Int, color: Color) {
    Ring(left / total.toFloat(), 84.dp, 7.dp, color, App.colors.track) {
        Text(
            if (left >= 60) "${left / 60}:${(left % 60).toString().padStart(2, '0')}" else "$left",
            style = MaterialTheme.typography.headlineSmall, color = App.colors.ink,
        )
    }
}

@Composable
private fun StepIcon(icon: ImageVector, color: Color) {
    val t = rememberInfiniteTransition(label = "icon")
    val s by t.animateFloat(0.92f, 1.08f, infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), androidx.compose.animation.core.RepeatMode.Reverse), label = "s")
    Box(
        Modifier.size(150.dp).graphicsLayer { scaleX = s; scaleY = s }.clip(CircleShape)
            .background(Brush.radialGradient(listOf(color.copy(alpha = 0.35f), color.copy(alpha = 0.05f)))),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(64.dp)) }
}

/** Grows for 4 seconds (breathe in), shrinks for 6 (breathe out). */
@Composable
private fun BreathingCircle() {
    val c = App.colors
    val t = rememberInfiniteTransition(label = "breath")
    val p by t.animateFloat(
        0f, 0f,
        infiniteRepeatable(keyframes {
            durationMillis = 10_000
            0f at 0 using FastOutSlowInEasing
            1f at 4_000 using LinearEasing
            0f at 10_000
        }),
        label = "p",
    )
    val inhaling = rememberBreathPhase()
    Box(Modifier.size(220.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2 * (0.45f + 0.5f * p)
            val center = Offset(size.width / 2, size.height / 2)
            drawCircle(Brush.radialGradient(listOf(c.data.copy(alpha = 0.55f), c.water.copy(alpha = 0.15f), Color.Transparent), center, r * 1.4f), r * 1.4f, center)
            drawCircle(Brush.radialGradient(listOf(c.water, c.data), center, r), r, center)
        }
        Text(if (inhaling) "Breathe in" else "Breathe out", style = MaterialTheme.typography.titleMedium, color = Color.White)
    }
}

@Composable
private fun rememberBreathPhase(): Boolean {
    var inhaling by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        while (true) {
            inhaling = true; delay(4_000)
            inhaling = false; delay(6_000)
        }
    }
    return inhaling
}

package com.healthtracker.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.healthtracker.app.ui.HealthViewModel
import com.healthtracker.app.ui.components.AppCard
import com.healthtracker.app.ui.components.InfoNote
import com.healthtracker.app.ui.components.PrimaryButton
import com.healthtracker.app.ui.components.ScreenTitle
import com.healthtracker.app.ui.components.SecondaryButton
import com.healthtracker.app.ui.theme.App
import kotlinx.coroutines.launch

val coachSuggestions = listOf(
    "How am I doing this week?",
    "What should I eat for dinner tonight?",
    "Am I eating too many carbs?",
    "Why isn't my weight dropping faster?",
    "How can I get more protein?",
    "Plan my weekend meals",
)

@Composable
fun CoachScreen(vm: HealthViewModel) {
    val st by vm.state.collectAsStateWithLifecycle()
    val h = st ?: return
    val s = h.s
    when {
        s.coachKey.isBlank() -> CoachSetup(vm)
        !s.coachEnabled -> ScreenScroll {
            ScreenTitle("Coach", "Your AI assistant")
            InfoNote("Coach is switched off. Turn it on in Plan → Coach.", Icons.Rounded.Lock)
        }
        else -> CoachChat(vm)
    }
}

@Composable
private fun CoachSetup(vm: HealthViewModel) {
    val c = App.colors
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var key by remember { mutableStateOf("") }
    var checking by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    ScreenScroll {
        ScreenTitle("Meet Coach", "Your AI assistant, powered by Gemini")
        AppCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Rounded.AutoAwesome, null, tint = c.primary)
                Text("Coach knows your app data", style = MaterialTheme.typography.titleMedium, color = c.ink)
            }
            Text(
                "Ask anything about your food, weight, water, sleep or cravings. Coach answers using your own numbers, tells you when something's off (like too many carbs), " +
                    "estimates foods the app doesn't know, writes your weekly review, and gives you one tip a day.",
                style = MaterialTheme.typography.bodyMedium, color = c.ink2,
            )
        }
        AppCard {
            Text("Set it up in a minute", style = MaterialTheme.typography.titleMedium, color = c.ink)
            Text("1. Open Google AI Studio and sign in with your Google account.", style = MaterialTheme.typography.bodyMedium, color = c.ink)
            Text("2. Tap Get API key → Create API key, then copy it.", style = MaterialTheme.typography.bodyMedium, color = c.ink)
            Text("3. Paste it below and tap Check and save.", style = MaterialTheme.typography.bodyMedium, color = c.ink)
            SecondaryButton("Open Google AI Studio", onClick = {
                runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/apikey"))) }
            })
            OutlinedTextField(
                value = key, onValueChange = { key = it.trim(); error = null },
                label = { Text("Gemini API key") }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = error != null, supportingText = { error?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
            )
            PrimaryButton(if (checking) "Checking…" else "Check and save", enabled = key.length >= 20 && !checking, onClick = {
                scope.launch {
                    checking = true
                    val err = vm.testCoachKey(key)
                    checking = false
                    if (err == null) {
                        vm.updateSettings { it.copy(coachKey = key, coachEnabled = true) }
                        vm.showMessage("Coach is ready.")
                    } else error = err
                }
            })
        }
        InfoNote(
            "Your key is saved only on this phone. Coach only sends a short summary of your app data with each question, and only answers health, food and fitness questions. " +
                "Off-topic questions are refused on the phone without using your free quota.",
            Icons.Rounded.Lock,
        )
    }
}

@Composable
private fun CoachChat(vm: HealthViewModel) {
    val st by vm.state.collectAsStateWithLifecycle()
    val busy by vm.coachBusy.collectAsStateWithLifecycle()
    val error by vm.coachError.collectAsStateWithLifecycle()
    val h = st ?: return
    val c = App.colors
    val app = LocalContext.current.applicationContext as com.healthtracker.app.HealthApp
    val msgs = app.container.coach.history(h.s)
    val left = app.container.coach.callsLeft(h.s)
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    LaunchedEffect(msgs.size, busy) { if (msgs.isNotEmpty()) listState.animateScrollToItem(msgs.size + 1) }

    fun send(q: String) {
        if (q.isBlank() || busy) return
        vm.askCoach(q)
        input = ""
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("$left of ${h.s.coachDailyLimit} questions left today", style = MaterialTheme.typography.bodySmall, color = c.ink2)
                Text("Coach", style = MaterialTheme.typography.headlineMedium, color = c.ink)
            }
            if (msgs.isNotEmpty()) IconButton(onClick = { vm.clearCoachChat() }) { Icon(Icons.Rounded.DeleteSweep, "Clear chat", tint = c.ink2) }
        }
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            state = listState,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                AppCard(tilt = false) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(36.dp).clip(CircleShape).background(Brush.linearGradient(listOf(c.data, c.water))), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.AutoAwesome, null, tint = c.onPrimary, modifier = Modifier.size(20.dp))
                        }
                        Text("Hi ${h.s.displayName}, I can see your last 7 days. Ask me anything about your food, weight, water, sleep or cravings.",
                            style = MaterialTheme.typography.bodyMedium, color = c.ink, modifier = Modifier.weight(1f))
                    }
                }
            }
            items(msgs) { m -> Bubble(m.text, mine = m.role == "user") }
            item {
                if (busy) Thinking()
                error?.let { e ->
                    InfoNote(e, Icons.Rounded.Lock, modifier = Modifier.clickable { vm.clearCoachError() })
                }
            }
        }
        if (msgs.size < 4) {
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                coachSuggestions.forEach { q ->
                    Text(
                        q, style = MaterialTheme.typography.labelMedium, color = c.ink,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(c.card2).border(1.dp, c.hairline, RoundedCornerShape(50))
                            .clickable(enabled = !busy) { send(q) }.padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = input, onValueChange = { input = it.take(400) },
                placeholder = { Text("Ask Coach…") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { send(input) }),
            )
            FilledIconButton(
                onClick = { send(input) }, enabled = input.isNotBlank() && !busy,
                modifier = Modifier.size(52.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = c.primary, contentColor = c.onPrimary),
            ) { Icon(Icons.AutoMirrored.Rounded.Send, "Send") }
        }
    }
}

@Composable
private fun Bubble(text: String, mine: Boolean) {
    val c = App.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (mine) c.onPrimary else c.ink,
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = if (mine) 20.dp else 6.dp, bottomEnd = if (mine) 6.dp else 20.dp))
                .background(if (mine) Brush.linearGradient(listOf(c.primary, androidx.compose.ui.graphics.Color(0xFF9EF0B8))) else Brush.linearGradient(listOf(c.card2, c.card)))
                .border(1.dp, if (mine) androidx.compose.ui.graphics.Color.Transparent else c.hairline,
                    RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = if (mine) 20.dp else 6.dp, bottomEnd = if (mine) 6.dp else 20.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun Thinking() {
    val c = App.colors
    val t = rememberInfiniteTransition(label = "dots")
    val p by t.animateFloat(0f, 3f, infiniteRepeatable(tween(1200, easing = LinearEasing)), label = "p")
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
        repeat(3) { i ->
            Box(Modifier.size(8.dp).clip(CircleShape).background(if (p.toInt() == i) c.primary else c.track))
        }
        Text("Coach is thinking…", style = MaterialTheme.typography.bodySmall, color = c.ink2)
    }
}

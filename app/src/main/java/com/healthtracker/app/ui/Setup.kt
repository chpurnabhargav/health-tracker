package com.healthtracker.app.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.healthtracker.app.container
import com.healthtracker.app.pause.FoodAppWatcherService
import com.healthtracker.app.ui.components.HairlineDivider
import com.healthtracker.app.ui.theme.App
import kotlinx.coroutines.launch

data class SetupStatus(
    val notifications: Boolean = false,
    val battery: Boolean = false,
    val pause: Boolean = false,
    val healthAvailable: Boolean = false,
    val healthNeedsUpdate: Boolean = false,
    val health: Boolean = false,
) {
    val doneCount: Int get() = listOf(notifications, battery, pause, health).count { it }
}

private fun notificationsGranted(ctx: Context): Boolean =
    Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

private fun batteryUnrestricted(ctx: Context): Boolean =
    ctx.getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(ctx.packageName) == true

/** Checklist of the phone permissions the app needs, with a button for each. */
@Composable
fun SetupChecklist(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf(SetupStatus()) }
    var showPauseHelp by remember { mutableStateOf(false) }

    fun refresh() {
        scope.launch {
            val hc = ctx.container.health
            status = SetupStatus(
                notifications = notificationsGranted(ctx),
                battery = batteryUnrestricted(ctx),
                pause = FoodAppWatcherService.isEnabled(ctx),
                healthAvailable = hc.isAvailable(),
                healthNeedsUpdate = hc.needsUpdate(),
                health = hc.hasPermissions(),
            )
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh() }

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) {
            ctx.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
            )
        }
        refresh()
    }
    val healthLauncher = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) {
        refresh()
    }

    Column(modifier) {
        SetupItem(
            icon = Icons.Rounded.Notifications,
            title = "Notifications",
            sub = "For water, weigh-in and meal reminders",
            done = status.notifications,
            action = "Allow",
        ) {
            if (Build.VERSION.SDK_INT >= 33) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            else ctx.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName))
        }
        HairlineDivider()
        SetupItem(
            icon = Icons.Rounded.BatteryChargingFull,
            title = "Run in the background",
            sub = "Samsung puts apps to sleep, which stops reminders. This keeps them on time.",
            done = status.battery,
            action = "Allow",
        ) {
            runCatching {
                ctx.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${ctx.packageName}")))
            }.onFailure { ctx.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
        }
        HairlineDivider()
        SetupItem(
            icon = Icons.Rounded.PauseCircle,
            title = "Food-app pause",
            sub = "Shows your message when you open Swiggy or Zomato",
            done = status.pause,
            action = "Set up",
        ) { showPauseHelp = true }
        HairlineDivider()
        SetupItem(
            icon = Icons.Rounded.Favorite,
            title = "Steps and sleep",
            sub = when {
                status.healthNeedsUpdate -> "Health Connect needs an update first"
                !status.healthAvailable -> "Health Connect isn't available on this phone"
                else -> "Read from Health Connect. Turn on syncing in Samsung Health too."
            },
            done = status.health,
            action = if (status.healthNeedsUpdate) "Update" else "Connect",
            enabled = status.healthAvailable || status.healthNeedsUpdate,
        ) {
            if (status.healthNeedsUpdate) {
                runCatching {
                    ctx.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.apps.healthdata"))
                    )
                }
            } else {
                healthLauncher.launch(ctx.container.health.permissions)
            }
        }
    }

    if (showPauseHelp) {
        AlertDialog(
            onDismissRequest = { showPauseHelp = false },
            title = { Text("Turn on the food-app pause") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Android needs you to switch this on yourself, in two steps:", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "1. Tap App info. Tap ⋮ in the top right corner, then Allow restricted settings. If you don't see that option, skip this step.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        "2. Come back and tap Accessibility. Open Installed apps → Health Tracker food-app pause, and turn it on.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        "It only checks which app is opening. It never reads your screen.",
                        style = MaterialTheme.typography.bodySmall, color = App.colors.ink2,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showPauseHelp = false
                    ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }) { Text("Accessibility") }
            },
            dismissButton = {
                TextButton(onClick = {
                    ctx.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}"))
                    )
                }) { Text("App info") }
            },
        )
    }
}

@Composable
private fun SetupItem(
    icon: ImageVector,
    title: String,
    sub: String,
    done: Boolean,
    action: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val c = App.colors
    Row(
        Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(if (done) c.primary else c.soft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (done) Icons.Rounded.Check else icon,
                contentDescription = null,
                tint = if (done) c.onPrimary else c.primary,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = c.ink)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = c.ink2)
        }
        if (done) {
            Text("Done", style = MaterialTheme.typography.titleSmall, color = c.ink2)
        } else {
            TextButton(onClick = onClick, enabled = enabled) { Text(action, style = MaterialTheme.typography.titleSmall) }
        }
    }
}

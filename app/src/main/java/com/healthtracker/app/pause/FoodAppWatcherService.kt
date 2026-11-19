package com.healthtracker.app.pause

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import com.healthtracker.app.container
import com.healthtracker.app.data.KnownFoodApps
import com.healthtracker.app.data.UserSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Remembers which apps the person chose to continue into, so the pause doesn't loop. */
object PauseGate {
    private val allowedUntil = mutableMapOf<String, Long>()

    fun allow(pkg: String, minutes: Int) {
        allowedUntil[pkg] = System.currentTimeMillis() + minutes * 60_000L
    }

    fun isAllowed(pkg: String): Boolean = (allowedUntil[pkg] ?: 0L) > System.currentTimeMillis()
}

/**
 * Watches only which app comes to the front. When it's one of the chosen food apps, it opens
 * the pause screen. It never reads screen content (canRetrieveWindowContent is false).
 */
class FoodAppWatcherService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    @Volatile private var settings: UserSettings? = null
    private var lastPkg: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        scope.launch { container.settings.flow.collect { settings = it } }
    }

    private fun ignored(pkg: String): Boolean =
        pkg == "com.android.systemui" ||
            pkg == "android" ||
            pkg.contains("inputmethod") ||
            pkg.contains("honeyboard") ||
            pkg.contains("keyboard")

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (ignored(pkg)) return
        if (pkg == packageName) {
            lastPkg = pkg
            return
        }
        val s = settings
        val entering = lastPkg != pkg
        lastPkg = pkg
        if (s == null || !s.onboarded || !s.pauseEnabled) return
        if (pkg !in s.pauseApps || !entering || PauseGate.isAllowed(pkg)) return

        val name = appName(pkg)
        val i = Intent(this, PauseActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_ANIMATION)
            .putExtra(PauseActivity.EXTRA_PKG, pkg)
            .putExtra(PauseActivity.EXTRA_NAME, name)
        runCatching { startActivity(i) }
    }

    private fun appName(pkg: String): String =
        KnownFoodApps.find { it.pkg == pkg }?.name
            ?: runCatching {
                packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
            }.getOrDefault("this app")

    override fun onInterrupt() {}

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        fun isEnabled(ctx: Context): Boolean {
            val expected = ComponentName(ctx, FoodAppWatcherService::class.java).flattenToString()
            val enabled = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
            return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
        }
    }
}

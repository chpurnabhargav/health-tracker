package com.healthtracker.app.pause

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import com.healthtracker.app.container
import com.healthtracker.app.data.KnownFoodApps
import com.healthtracker.app.data.UserSettings
import com.healthtracker.app.reminders.WaterOverlay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Remembers the one food app you chose to continue into. The allowance only lasts while you stay in
 * that app: switching to anything else, or locking the phone, ends it, so the next open pauses again.
 */
object PauseGate {
    @Volatile private var allowedPkg: String? = null
    @Volatile private var allowedUntil: Long = 0L

    fun allowSession(pkg: String, maxMinutes: Int = 30) {
        allowedPkg = pkg
        allowedUntil = System.currentTimeMillis() + maxMinutes * 60_000L
    }

    fun revoke() {
        allowedPkg = null
    }

    fun isAllowed(pkg: String): Boolean = pkg == allowedPkg && System.currentTimeMillis() < allowedUntil

    fun allowedPackage(): String? = allowedPkg
}

/**
 * Watches only which app comes to the front. When it's one of the chosen food apps, it opens
 * the pause screen. It never reads screen content (canRetrieveWindowContent is false).
 */
class FoodAppWatcherService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val main = Handler(Looper.getMainLooper())
    @Volatile private var settings: UserSettings? = null
    private var lastPkg: String? = null
    private var pkgBeforeScreenOff: String? = null
    private var overlay: WaterOverlay? = null

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    pkgBeforeScreenOff = lastPkg
                    PauseGate.revoke()
                    lastPkg = null
                }
                Intent.ACTION_USER_PRESENT -> {
                    val pkg = pkgBeforeScreenOff ?: return
                    pkgBeforeScreenOff = null
                    if (shouldPause(pkg)) {
                        lastPkg = pkg
                        launchPause(pkg)
                    }
                }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        scope.launch { container.settings.flow.collect { settings = it } }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        ContextCompat.registerReceiver(this, screenReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    private fun ignored(pkg: String): Boolean =
        pkg == "com.android.systemui" ||
            pkg == "android" ||
            pkg.contains("inputmethod") ||
            pkg.contains("honeyboard") ||
            pkg.contains("keyboard")

    private fun shouldPause(pkg: String): Boolean {
        val s = settings ?: return false
        return s.onboarded && s.pauseEnabled && pkg in s.pauseApps && !PauseGate.isAllowed(pkg)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (ignored(pkg)) return

        if (pkg == packageName) {
            // Our own pause screen doesn't end an allowance; any other screen of this app does.
            val cls = event.className?.toString().orEmpty()
            if (!cls.contains("PauseActivity")) PauseGate.revoke()
            lastPkg = pkg
            return
        }

        if (pkg != PauseGate.allowedPackage()) PauseGate.revoke()
        val entering = lastPkg != pkg
        lastPkg = pkg
        if (entering && shouldPause(pkg)) launchPause(pkg)
    }

    private fun launchPause(pkg: String) {
        val i = Intent(this, PauseActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_ANIMATION)
            .putExtra(PauseActivity.EXTRA_PKG, pkg)
            .putExtra(PauseActivity.EXTRA_NAME, appName(pkg))
        runCatching { startActivity(i) }
    }

    private fun appName(pkg: String): String =
        KnownFoodApps.find { it.pkg == pkg }?.name
            ?: runCatching {
                packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
            }.getOrDefault("this app")

    /** Shows the water card on top of whatever app is open, for a few seconds. */
    fun showWaterOverlay(drunkMl: Int, goalMl: Int) {
        main.post {
            runCatching {
                overlay?.hide()
                overlay = WaterOverlay(this).also { it.show(drunkMl, goalMl) }
            }
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        if (instance === this) instance = null
        runCatching { unregisterReceiver(screenReceiver) }
        overlay?.hide()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        @Volatile var instance: FoodAppWatcherService? = null
            private set

        fun isEnabled(ctx: Context): Boolean {
            val expected = ComponentName(ctx, FoodAppWatcherService::class.java).flattenToString()
            val enabled = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
            return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
        }
    }
}

package com.verbum.launcher.data

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import com.verbum.launcher.model.AppInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Queries launchable activities and keeps the list fresh while packages are
 * installed, updated, or removed.
 */
class AppRepository(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refresh()
        }
    }

    init {
        refresh()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(
            context, packageReceiver, filter, ContextCompat.RECEIVER_EXPORTED
        )
    }

    fun refresh() {
        scope.launch {
            val pm = context.packageManager
            val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val resolved = if (Build.VERSION.SDK_INT >= 33) {
                pm.queryIntentActivities(
                    launcherIntent, PackageManager.ResolveInfoFlags.of(0L)
                )
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(launcherIntent, 0)
            }
            _apps.value = resolved
                .mapNotNull { info ->
                    val activityInfo = info.activityInfo ?: return@mapNotNull null
                    if (activityInfo.packageName == context.packageName) return@mapNotNull null
                    AppInfo(
                        key = "${activityInfo.packageName}/${activityInfo.name}",
                        packageName = activityInfo.packageName,
                        activityClass = activityInfo.name,
                        label = info.loadLabel(pm)?.toString() ?: activityInfo.packageName,
                    )
                }
                .distinctBy { it.key }
                .sortedBy { it.label.lowercase() }
        }
    }

    fun launch(app: AppInfo) {
        try {
            val intent = Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(ComponentName(app.packageName, app.activityClass))
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            context.startActivity(intent)
        } catch (_: Exception) {
            // App may have been uninstalled between refresh and tap; the
            // package receiver will remove it from the list shortly.
        }
    }

    /** Fires the system uninstall dialog for the app's package. */
    fun uninstall(app: AppInfo) {
        try {
            val intent = Intent(Intent.ACTION_DELETE)
                .setData(Uri.fromParts("package", app.packageName, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: Exception) {
            // No uninstaller available, or the package is already gone.
        }
    }

    fun close() {
        context.unregisterReceiver(packageReceiver)
        scope.cancel()
    }
}

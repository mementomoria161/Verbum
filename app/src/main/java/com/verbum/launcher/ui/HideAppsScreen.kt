package com.verbum.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.verbum.launcher.R
import com.verbum.launcher.model.AppInfo

/**
 * Full-screen view with two folders — Visible and Hidden. Tapping an app in
 * either folder moves it to the other. Rendered edge-to-edge inside the main
 * window (no dialog), so the system bar backgrounds stay transparent and
 * nothing is hidden behind a top bar. A single centered Done button sits at
 * the bottom; there is no header.
 */
@Composable
fun HideAppsScreen(
    visibleApps: List<AppInfo>,
    hiddenApps: List<AppInfo>,
    onDismiss: () -> Unit,
    onSetHidden: (AppInfo, Boolean) -> Unit,
) {
    BackHandler(onBack = onDismiss)

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            // Swallow taps so nothing reaches the homescreen behind.
            .pointerInput(Unit) { detectTapGestures {} }
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .safeDrawingPadding()
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppsFolder(
                title = "Visible",
                icon = R.drawable.ic_visibility,
                emptyHint = "All apps are hidden.",
                apps = visibleApps,
                onAppClick = { onSetHidden(it, true) },
            )
            AppsFolder(
                title = "Hidden",
                icon = R.drawable.ic_visibility_off,
                emptyHint = "No hidden apps. Tap an app above to hide it.",
                apps = hiddenApps,
                onAppClick = { onSetHidden(it, false) },
            )
        }

        FilledTonalButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .safeDrawingPadding()
                .padding(bottom = 20.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_check_circle),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text("Done")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppsFolder(
    title: String,
    icon: Int,
    emptyHint: String,
    apps: List<AppInfo>,
    onAppClick: (AppInfo) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "${apps.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (apps.isEmpty()) {
                Text(
                    emptyHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    apps.forEach { app ->
                        Text(
                            text = app.label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .clip(RoundedCornerShape(percent = 50))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .clickable { onAppClick(app) }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

package com.verbum.launcher

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.verbum.launcher.ui.VerbumApp
import com.verbum.launcher.ui.theme.VerbumTheme

class MainActivity : ComponentActivity() {

    private val viewModel: VerbumViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Fully transparent system bars: no scrim behind the navigation bar.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Stops the system from drawing the grey contrast strip behind
            // the gesture/navigation area.
            window.isNavigationBarContrastEnforced = false
        }
        setContent {
            VerbumTheme {
                VerbumApp(viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Pressing home while already on the launcher closes search,
        // customization, and edit mode — standard launcher behavior.
        viewModel.resetTransientState()
    }
}

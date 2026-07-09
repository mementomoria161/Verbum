package com.verbum.launcher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.verbum.launcher.ui.VerbumApp
import com.verbum.launcher.ui.theme.VerbumTheme

class MainActivity : ComponentActivity() {

    private val viewModel: VerbumViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
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

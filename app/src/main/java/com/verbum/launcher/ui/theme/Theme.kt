package com.verbum.launcher.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Verbum draws its own background color/image, so the M3 theme is used
 * mainly for surfaces (sheets, dialogs) and — importantly — to source the
 * color swatches offered in the customization sheet. On Android 12+ we use
 * the device's dynamic (Material You) palette so those swatches are the
 * user's own available Material Design 3 colors.
 */
@Composable
fun VerbumTheme(content: @Composable () -> Unit) {
    val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicDarkColorScheme(LocalContext.current)
    } else {
        darkColorScheme()
    }
    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}

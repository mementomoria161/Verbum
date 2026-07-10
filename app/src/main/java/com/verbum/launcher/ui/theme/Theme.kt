package com.verbum.launcher.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp

/**
 * The default M3 type scale, but with every style's letter spacing zeroed —
 * Verbum never tracks its text, on the homescreen or in the settings UI.
 */
private val FlatTypography: Typography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(letterSpacing = 0.sp),
        displayMedium = displayMedium.copy(letterSpacing = 0.sp),
        displaySmall = displaySmall.copy(letterSpacing = 0.sp),
        headlineLarge = headlineLarge.copy(letterSpacing = 0.sp),
        headlineMedium = headlineMedium.copy(letterSpacing = 0.sp),
        headlineSmall = headlineSmall.copy(letterSpacing = 0.sp),
        titleLarge = titleLarge.copy(letterSpacing = 0.sp),
        titleMedium = titleMedium.copy(letterSpacing = 0.sp),
        titleSmall = titleSmall.copy(letterSpacing = 0.sp),
        bodyLarge = bodyLarge.copy(letterSpacing = 0.sp),
        bodyMedium = bodyMedium.copy(letterSpacing = 0.sp),
        bodySmall = bodySmall.copy(letterSpacing = 0.sp),
        labelLarge = labelLarge.copy(letterSpacing = 0.sp),
        labelMedium = labelMedium.copy(letterSpacing = 0.sp),
        labelSmall = labelSmall.copy(letterSpacing = 0.sp),
    )
}

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
        typography = FlatTypography,
        content = content,
    )
}

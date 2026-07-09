package com.verbum.launcher.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.verbum.launcher.R
import com.verbum.launcher.model.VerbumSettings
import kotlin.math.roundToInt

/** Material 3 color roles offered as swatches, from the active (dynamic) scheme. */
private fun ColorScheme.swatchPalette(): List<Color> = listOf(
    surface, surfaceVariant, surfaceContainerHighest, background,
    primary, primaryContainer, onPrimaryContainer,
    secondary, secondaryContainer,
    tertiary, tertiaryContainer,
    error, errorContainer,
    onSurface, onSurfaceVariant, inverseSurface,
)

private fun Color.toArgbLong(): Long = toArgb().toLong() and 0xFFFFFFFFL

// Bare's settings pill metrics.
private val PILL_HEIGHT = 120.dp
private val BADGE_SIZE = 96.dp
private val PILL_SHAPE = RoundedCornerShape(percent = 50)

/**
 * The Customize overlay, styled after the Bare launcher: a dim scrim plus a
 * bottom-anchored stack of fully-rounded pill cards floating above the bottom
 * bar. No title, no section headers — just the pills. Rendered inside the grid
 * area so the bottom bar stays visible and usable beneath it.
 */
@Composable
fun CustomizeSheet(
    settings: VerbumSettings,
    onDismiss: () -> Unit,
    onOpenAppManagement: () -> Unit,
    onCustomizeHomescreen: () -> Unit,
    onSetBackgroundColor: (Long) -> Unit,
    onPickBackgroundImage: (Uri) -> Unit,
    onClearBackgroundImage: () -> Unit,
    onSetTextSize: (Float) -> Unit,
    onSetTextColor: (Long) -> Unit,
    onPickFont: (Uri) -> Unit,
    onClearFont: () -> Unit,
) {
    val palette = MaterialTheme.colorScheme.swatchPalette()

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let(onPickBackgroundImage) }

    val fontPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(onPickFont) }

    Box(Modifier.fillMaxSize()) {
        // Dim scrim — tap anywhere off the pills to close.
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .pointerInput(Unit) { detectTapGestures { onDismiss() } }
        )

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // 1. Edit layout
            ActionPill(
                icon = R.drawable.ic_edit,
                title = "Edit layout",
                subtitle = null,
                onClick = onCustomizeHomescreen,
            )

            // 2. App management
            ActionPill(
                icon = R.drawable.ic_settings,
                title = "App management",
                subtitle = "Hide, rename, and sort into folders",
                onClick = onOpenAppManagement,
            )

            // 3. Background color
            ColorPill(
                title = "Background color",
                palette = palette,
                selected = settings.bgColor,
                onSelect = onSetBackgroundColor,
            )

            // 4. Background image
            SplitPillRow(
                leftIcon = R.drawable.ic_import,
                leftTitle = "Background image",
                leftSubtitle = if (settings.bgImagePath != null) "Image set" else "Choose a photo",
                onLeft = { imagePicker.launch("image/*") },
                rightIcon = R.drawable.ic_close,
                rightEnabled = settings.bgImagePath != null,
                onRight = onClearBackgroundImage,
            )

            // 5. Text color
            ColorPill(
                title = "Text color",
                palette = palette,
                selected = settings.textColor,
                onSelect = onSetTextColor,
            )

            // 6. Text size
            var sliderValue by remember(settings.textSizeSp) {
                mutableFloatStateOf(settings.textSizeSp)
            }
            SliderPill(
                title = "Text size",
                valueLabel = "${sliderValue.roundToInt()} sp",
                value = sliderValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = { onSetTextSize(sliderValue) },
            )

            // 7. Font
            SplitPillRow(
                leftIcon = R.drawable.ic_import,
                leftTitle = "Font",
                leftSubtitle = settings.fontName
                    ?: if (settings.fontPath != null) "Custom font" else "Default",
                onLeft = {
                    fontPicker.launch(
                        arrayOf(
                            "font/ttf", "font/otf",
                            "application/x-font-ttf", "application/x-font-otf",
                            "application/octet-stream",
                        )
                    )
                },
                rightIcon = R.drawable.ic_close,
                rightEnabled = settings.fontPath != null,
                onRight = onClearFont,
            )
        }
    }
}

// ----------------------------------------------------------------------
// Bare-style building blocks
// ----------------------------------------------------------------------

/**
 * Base pill: a fully-rounded [surfaceVariant] card with a circular badge on the
 * left and arbitrary [content] filling the rest of the row.
 */
@Composable
private fun PillCard(
    modifier: Modifier,
    badgeColor: Color,
    onClick: (() -> Unit)? = null,
    badgeContent: @Composable BoxScope.() -> Unit = {},
    content: @Composable RowScope.() -> Unit,
) {
    Surface(
        shape = PILL_SHAPE,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier,
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(start = 12.dp, end = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(BADGE_SIZE)
                    .clip(CircleShape)
                    .background(badgeColor)
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), CircleShape),
                contentAlignment = Alignment.Center,
                content = badgeContent,
            )
            Spacer(Modifier.width(16.dp))
            content()
        }
    }
}

@Composable
private fun ActionPill(
    icon: Int,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
) {
    PillCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(PILL_HEIGHT),
        badgeColor = MaterialTheme.colorScheme.surfaceContainer,
        onClick = onClick,
        badgeContent = {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(34.dp),
            )
        },
    ) {
        Column(Modifier.weight(1f)) {
            PillTitle(title)
            if (subtitle != null) PillSubtitle(subtitle)
        }
    }
}

@Composable
private fun ColorPill(
    title: String,
    palette: List<Color>,
    selected: Long,
    onSelect: (Long) -> Unit,
) {
    PillCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(PILL_HEIGHT),
        // The badge previews the currently selected color.
        badgeColor = Color(selected),
    ) {
        Column(Modifier.weight(1f)) {
            PillTitle(title)
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                palette.forEach { color ->
                    val argb = color.toArgbLong()
                    val isSelected = argb == selected
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant,
                                shape = CircleShape,
                            )
                            .clickable { onSelect(argb) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SliderPill(
    title: String,
    valueLabel: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
) {
    PillCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(PILL_HEIGHT),
        badgeColor = MaterialTheme.colorScheme.surfaceContainer,
        badgeContent = {
            Text(
                "Aa",
                fontSize = 30.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PillTitle(title, modifier = Modifier.weight(1f))
                Text(
                    valueLabel,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Slider(
                value = value,
                onValueChange = onValueChange,
                onValueChangeFinished = onValueChangeFinished,
                valueRange = 12f..34f,
            )
        }
    }
}

/** A wide action pill on the left plus a circular action pill on the right. */
@Composable
private fun SplitPillRow(
    leftIcon: Int,
    leftTitle: String,
    leftSubtitle: String?,
    onLeft: () -> Unit,
    rightIcon: Int,
    rightEnabled: Boolean,
    onRight: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(PILL_HEIGHT),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PillCard(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            badgeColor = MaterialTheme.colorScheme.surfaceContainer,
            onClick = onLeft,
            badgeContent = {
                Icon(
                    painter = painterResource(leftIcon),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(34.dp),
                )
            },
        ) {
            Column(Modifier.weight(1f)) {
                PillTitle(leftTitle)
                if (leftSubtitle != null) PillSubtitle(leftSubtitle)
            }
        }

        // Circular action (clear / reset).
        val rightColor =
            if (rightEnabled) MaterialTheme.colorScheme.surfaceVariant
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        Surface(
            shape = PILL_SHAPE,
            color = rightColor,
            modifier = Modifier
                .size(PILL_HEIGHT)
                .clickable(enabled = rightEnabled, onClick = onRight),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(BADGE_SIZE)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(rightIcon),
                        contentDescription = null,
                        tint = if (rightEnabled) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun PillTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

@Composable
private fun PillSubtitle(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

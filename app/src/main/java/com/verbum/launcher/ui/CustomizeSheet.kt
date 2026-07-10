package com.verbum.launcher.ui

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.verbum.launcher.R
import com.verbum.launcher.model.VerbumSettings
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Material 3 color roles offered as presets, from the active (dynamic) scheme. */
private fun ColorScheme.swatchPalette(): List<Color> = listOf(
    surface, surfaceVariant, surfaceContainerHighest, background,
    primary, primaryContainer, onPrimaryContainer,
    secondary, secondaryContainer,
    tertiary, tertiaryContainer,
    error, errorContainer,
    onSurface, onSurfaceVariant, inverseSurface,
)

// Bare's settings pill metrics.
private val PILL_HEIGHT = 120.dp
private val BADGE_SIZE = 96.dp
private val PILL_SHAPE = RoundedCornerShape(percent = 50)

private const val MIN_TEXT_SIZE = 12f
private const val MAX_TEXT_SIZE = 34f

private enum class PickerTarget { BACKGROUND, TEXT }

/**
 * The Customize overlay, styled after the Bare launcher: a dim scrim plus a
 * bottom-anchored stack of fully-rounded pill cards floating above the bottom
 * bar. Rows: Edit layout, Hide apps, Background (image + color picker), and
 * Text (font + color picker + rotary size dial).
 */
@Composable
fun CustomizeSheet(
    settings: VerbumSettings,
    fontFamily: FontFamily?,
    onDismiss: () -> Unit,
    onOpenHideApps: () -> Unit,
    onCustomizeHomescreen: () -> Unit,
    onSetBackgroundColor: (Long) -> Unit,
    onPickBackgroundImage: (Uri) -> Unit,
    onSetTextSize: (Float) -> Unit,
    onSetTextColor: (Long) -> Unit,
    onPickFont: (Uri) -> Unit,
    onSetTextColorByUsage: (Boolean) -> Unit,
) {
    val palette = MaterialTheme.colorScheme.swatchPalette()
    var pickerTarget by remember { mutableStateOf<PickerTarget?>(null) }

    // Trigger the Bare-style staggered entrance on first composition.
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val scrimAlpha by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(250),
        label = "scrimAlpha",
    )

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let(onPickBackgroundImage) }

    val fontPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(onPickFont) }

    Box(Modifier.fillMaxSize()) {
        // Dim scrim — fades in, tap anywhere off the pills to close.
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f * scrimAlpha))
                .pointerInput(Unit) { detectTapGestures { onDismiss() } }
        )

        val pillCount = 4
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .safeDrawingPadding()
                // Keep the pill stack above the bottom bar, which stays visible.
                .padding(bottom = BottomBarHeight)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // 1. Edit layout
            AnimatedInPill(index = 0, count = pillCount, shown = shown) {
                ActionPill(
                    icon = R.drawable.ic_edit_layout,
                    title = "Edit layout",
                    subtitle = "Move and resize blocks, add folders",
                    onClick = onCustomizeHomescreen,
                )
            }

            // 2. Hide apps
            AnimatedInPill(index = 1, count = pillCount, shown = shown) {
                ActionPill(
                    icon = R.drawable.ic_visibility_off,
                    title = "Hide apps",
                    subtitle = "Tap apps to hide or unhide them",
                    onClick = onOpenHideApps,
                )
            }

            // 3. Background: thumbnail badge + image upload + color picker.
            AnimatedInPill(index = 2, count = pillCount, shown = shown) {
                BackgroundPill(
                    settings = settings,
                    onPickImage = { imagePicker.launch("image/*") },
                    onOpenColorPicker = { pickerTarget = PickerTarget.BACKGROUND },
                )
            }

            // 4. Text: font preview badge, font/color buttons, size dial.
            AnimatedInPill(index = 3, count = pillCount, shown = shown) {
                TextPill(
                    settings = settings,
                    fontFamily = fontFamily,
                    onPickFont = {
                        fontPicker.launch(
                            arrayOf(
                                "font/ttf", "font/otf",
                                "application/x-font-ttf", "application/x-font-otf",
                                "application/octet-stream",
                            )
                        )
                    },
                    onOpenColorPicker = { pickerTarget = PickerTarget.TEXT },
                    onSetTextSize = onSetTextSize,
                )
            }
        }
    }

    pickerTarget?.let { target ->
        ColorPickerDialog(
            initialColor = when (target) {
                PickerTarget.BACKGROUND -> settings.bgColor
                PickerTarget.TEXT -> settings.textColor
            },
            presets = palette,
            onDismiss = { pickerTarget = null },
            onPick = { argb ->
                when (target) {
                    PickerTarget.BACKGROUND -> onSetBackgroundColor(argb)
                    PickerTarget.TEXT -> onSetTextColor(argb)
                }
                pickerTarget = null
            },
            showUsageOption = target == PickerTarget.TEXT,
            usageEnabled = settings.textColorByUsage,
            onToggleUsage = onSetTextColorByUsage,
        )
    }
}

// Standard "overshoot" easing (Android's OvershootInterpolator, tension 2.2).
private val OvershootEasing = Easing { fraction ->
    val t = fraction - 1f
    t * t * (3.2f * t + 2.2f) + 1f
}

/**
 * Wraps a settings pill in the Bare-style entrance: it scales up from 0.3,
 * rises from below, and fades in, with a bottom-up stagger and overshoot.
 */
@Composable
private fun AnimatedInPill(
    index: Int,
    count: Int,
    shown: Boolean,
    content: @Composable () -> Unit,
) {
    val delay = 120 + (count - 1 - index) * 60
    val progress by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(durationMillis = 280, delayMillis = delay, easing = OvershootEasing),
        label = "pillProgress",
    )
    val alpha by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(durationMillis = 220, delayMillis = delay),
        label = "pillAlpha",
    )
    val offsetPx = with(LocalDensity.current) { 100.dp.toPx() }
    Box(
        Modifier.graphicsLayer {
            scaleX = 0.3f + progress * 0.7f
            scaleY = 0.3f + progress * 0.7f
            translationY = (1f - progress) * offsetPx
            this.alpha = alpha.coerceIn(0f, 1f)
        }
    ) {
        content()
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
    subtitle: String? = null,
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
                // Matches Bare: intrinsic 24dp icon centered in the 96dp badge.
                modifier = Modifier.size(24.dp),
            )
        },
    ) {
        Column(Modifier.weight(1f)) {
            PillTitle(title)
            if (subtitle != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    subtitle,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun BackgroundPill(
    settings: VerbumSettings,
    onPickImage: () -> Unit,
    onOpenColorPicker: () -> Unit,
) {
    val thumbnail = rememberThumbnail(settings.bgImagePath)

    PillCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(PILL_HEIGHT),
        badgeColor = Color(settings.bgColor),
        badgeContent = {
            if (thumbnail != null) {
                Image(
                    bitmap = thumbnail,
                    contentDescription = "Current background",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        },
    ) {
        Column(Modifier.weight(1f)) {
            PillTitle("Background")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallPillButton(R.drawable.ic_import, "Image", onPickImage)
                SmallPillButton(R.drawable.ic_color_picker, "Color", onOpenColorPicker)
            }
        }
    }
}

@Composable
private fun TextPill(
    settings: VerbumSettings,
    fontFamily: FontFamily?,
    onPickFont: () -> Unit,
    onOpenColorPicker: () -> Unit,
    onSetTextSize: (Float) -> Unit,
) {
    PillCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(PILL_HEIGHT),
        badgeColor = MaterialTheme.colorScheme.surfaceContainer,
        badgeContent = {
            // The badge itself is the rotary size dial.
            FontSizeDial(
                value = settings.textSizeSp,
                fontFamily = fontFamily,
                textColor = Color(settings.textColor),
                onCommit = onSetTextSize,
            )
        },
    ) {
        Column(Modifier.weight(1f)) {
            PillTitle("Text")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallPillButton(
                    icon = R.drawable.ic_import,
                    label = settings.fontName ?: "Font",
                    onClick = onPickFont,
                )
                SmallPillButton(R.drawable.ic_color_picker, "Color", onOpenColorPicker)
            }
        }
    }
}

/**
 * The rotary size dial that fills the Text pill's badge circle: drag around
 * its center to turn it; a full 360° sweep covers the whole size range. A
 * shaded border trail sweeps up to the current position, ending at the
 * selection knob, and the center shows the current size in the selected
 * font and color.
 *
 * The drag state is intentionally NOT keyed on [value]: re-keying would
 * recreate the state on every commit while the long-lived pointerInput
 * coroutine still wrote to the stale instance — the cause of the jerky
 * second rotation. One state instance lives for the dial's lifetime.
 */
@Composable
private fun FontSizeDial(
    value: Float,
    fontFamily: FontFamily?,
    textColor: Color,
    onCommit: (Float) -> Unit,
) {
    var current by remember { mutableFloatStateOf(value) }
    val latestOnCommit by rememberUpdatedState(onCommit)
    val fraction = (current - MIN_TEXT_SIZE) / (MAX_TEXT_SIZE - MIN_TEXT_SIZE)
    val trailColor = MaterialTheme.colorScheme.primary
    // The badge fill, used as a ring so the knob reads clearly on the trail.
    val knobRingColor = MaterialTheme.colorScheme.surfaceContainer

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = { latestOnCommit(current) },
                    onDragCancel = { latestOnCommit(current) },
                ) { change, _ ->
                    change.consume()
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val prev = change.previousPosition - center
                    val pos = change.position - center
                    var deltaDeg = Math.toDegrees(
                        (atan2(pos.y, pos.x) - atan2(prev.y, prev.x)).toDouble()
                    ).toFloat()
                    if (deltaDeg > 180f) deltaDeg -= 360f
                    if (deltaDeg < -180f) deltaDeg += 360f
                    current = (current + deltaDeg / 360f * (MAX_TEXT_SIZE - MIN_TEXT_SIZE))
                        .coerceIn(MIN_TEXT_SIZE, MAX_TEXT_SIZE)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        // Track ring, the shaded trail, and the knob — drawn with one shared
        // geometry so the knob sits exactly centered on the trail line.
        Canvas(Modifier.fillMaxSize().padding(9.dp)) {
            val strokeWidth = 4.dp.toPx()
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f
            drawArc(
                color = trailColor.copy(alpha = 0.15f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(strokeWidth, cap = StrokeCap.Round),
            )
            drawArc(
                color = trailColor.copy(alpha = 0.5f),
                startAngle = -90f,
                sweepAngle = fraction * 360f,
                useCenter = false,
                style = Stroke(strokeWidth, cap = StrokeCap.Round),
            )
            val knobAngle = Math.toRadians((-90f + fraction * 360f).toDouble())
            val knobCenter = Offset(
                center.x + radius * cos(knobAngle).toFloat(),
                center.y + radius * sin(knobAngle).toFloat(),
            )
            drawCircle(knobRingColor, radius = 8.dp.toPx(), center = knobCenter)
            drawCircle(trailColor, radius = 6.dp.toPx(), center = knobCenter)
        }
        // Current size, rendered in the selected font and color.
        Text(
            "${current.roundToInt()}",
            fontFamily = fontFamily,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
        )
    }
}

/** A small labeled icon button used inside pills, like Bare's compact actions. */
@Composable
private fun SmallPillButton(
    icon: Int,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 120.dp),
        )
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

/** Decodes a small thumbnail of the background image for the badge preview. */
@Composable
private fun rememberThumbnail(path: String?): ImageBitmap? =
    remember(path) {
        path?.let { p ->
            runCatching {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(p, bounds)
                var sample = 1
                while (bounds.outWidth / (sample * 2) > 256 ||
                    bounds.outHeight / (sample * 2) > 256
                ) {
                    sample *= 2
                }
                BitmapFactory.decodeFile(
                    p,
                    BitmapFactory.Options().apply { inSampleSize = sample },
                )?.asImageBitmap()
            }.getOrNull()
        }
    }

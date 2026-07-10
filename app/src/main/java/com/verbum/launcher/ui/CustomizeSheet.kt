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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import com.verbum.launcher.model.AppInfo
import com.verbum.launcher.model.VerbumSettings
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

enum class PickerTarget { BACKGROUND, TEXT }

/** Which panel the Customize overlay currently shows in the pills' place. */
sealed interface CustomizeScreen {
    data object Pills : CustomizeScreen
    data object HideApps : CustomizeScreen
    data class Color(val target: PickerTarget) : CustomizeScreen
}

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

/**
 * The Customize overlay: a dim scrim plus a bottom-anchored panel floating
 * above the (still visible) bottom bar. The panel shows one of three screens
 * in the same place — the settings pills, the Hide-apps folders, or a color
 * picker — with the bottom bar's button switching to Done for the sub-screens.
 */
@Composable
fun CustomizeSheet(
    settings: VerbumSettings,
    fontFamily: FontFamily?,
    screen: CustomizeScreen,
    visibleApps: List<AppInfo>,
    hiddenApps: List<AppInfo>,
    onScrimTap: () -> Unit,
    onOpenHideApps: () -> Unit,
    onOpenColorPicker: (PickerTarget) -> Unit,
    onCustomizeHomescreen: () -> Unit,
    onSetBackgroundColor: (Long) -> Unit,
    onPickBackgroundImage: (Uri) -> Unit,
    onSetTextSize: (Float) -> Unit,
    onSetTextColor: (Long) -> Unit,
    onPickFont: (Uri) -> Unit,
    onClearFont: () -> Unit,
    onSetTextColorByUsage: (Boolean) -> Unit,
    onSetHidden: (AppInfo, Boolean) -> Unit,
) {
    val palette = MaterialTheme.colorScheme.swatchPalette()

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let(onPickBackgroundImage) }

    val fontPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(onPickFont) }

    fun launchFontPicker() = fontPicker.launch(
        arrayOf(
            "font/ttf", "font/otf",
            "application/x-font-ttf", "application/x-font-otf",
            "application/octet-stream",
        )
    )

    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val scrimAlpha by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(250),
        label = "scrimAlpha",
    )
    // Keep the tap handler fresh: pointerInput(Unit) captures it only once.
    val latestScrimTap by rememberUpdatedState(onScrimTap)

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f * scrimAlpha))
                .pointerInput(Unit) { detectTapGestures { latestScrimTap() } }
        )

        // The pills and the (short) color picker float up from just above the
        // bottom bar; the Hide-apps list fills the screen. In every case the
        // safe-area and bottom-bar insets are applied *inside* the scroll, so
        // the content scrolls edge-to-edge — under the status bar and under the
        // floating Done button — with no fixed dark bands clipping it.
        val bottomAligned = screen is CustomizeScreen.Pills || screen is CustomizeScreen.Color
        Column(
            Modifier
                .then(
                    if (bottomAligned) Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    else Modifier.fillMaxSize()
                )
                .verticalScroll(rememberScrollState())
                .safeDrawingPadding()
                .padding(bottom = BottomBarHeight)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when (screen) {
                is CustomizeScreen.Pills -> PillsContent(
                    settings = settings,
                    fontFamily = fontFamily,
                    onOpenHideApps = onOpenHideApps,
                    onCustomizeHomescreen = onCustomizeHomescreen,
                    onOpenColorPicker = onOpenColorPicker,
                    onPickImage = { imagePicker.launch("image/*") },
                    onPickFont = { launchFontPicker() },
                    onClearFont = onClearFont,
                    onSetTextSize = onSetTextSize,
                )

                is CustomizeScreen.HideApps -> HideAppsContent(
                    visibleApps = visibleApps,
                    hiddenApps = hiddenApps,
                    onSetHidden = onSetHidden,
                )

                is CustomizeScreen.Color -> ColorContent(
                    target = screen.target,
                    settings = settings,
                    palette = palette,
                    onSetBackgroundColor = onSetBackgroundColor,
                    onSetTextColor = onSetTextColor,
                    onSetTextColorByUsage = onSetTextColorByUsage,
                )
            }
        }
    }
}

// ----------------------------------------------------------------------
// Screens
// ----------------------------------------------------------------------

@Composable
private fun PillsContent(
    settings: VerbumSettings,
    fontFamily: FontFamily?,
    onOpenHideApps: () -> Unit,
    onCustomizeHomescreen: () -> Unit,
    onOpenColorPicker: (PickerTarget) -> Unit,
    onPickImage: () -> Unit,
    onPickFont: () -> Unit,
    onClearFont: () -> Unit,
    onSetTextSize: (Float) -> Unit,
) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val count = 4

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AnimatedInPill(index = 0, count = count, shown = shown) {
            ActionPill(
                icon = R.drawable.ic_edit_layout,
                title = "Edit layout",
                subtitle = "Move and resize blocks, add folders",
                onClick = onCustomizeHomescreen,
            )
        }
        AnimatedInPill(index = 1, count = count, shown = shown) {
            ActionPill(
                icon = R.drawable.ic_visibility_off,
                title = "Hide apps",
                subtitle = "Move apps between Visible and Hidden",
                onClick = onOpenHideApps,
            )
        }
        AnimatedInPill(index = 2, count = count, shown = shown) {
            BackgroundPill(
                settings = settings,
                onPickImage = onPickImage,
                onOpenColorPicker = { onOpenColorPicker(PickerTarget.BACKGROUND) },
            )
        }
        AnimatedInPill(index = 3, count = count, shown = shown) {
            TextPill(
                settings = settings,
                fontFamily = fontFamily,
                onPickFont = onPickFont,
                onClearFont = onClearFont,
                onOpenColorPicker = { onOpenColorPicker(PickerTarget.TEXT) },
                onSetTextSize = onSetTextSize,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HideAppsContent(
    visibleApps: List<AppInfo>,
    hiddenApps: List<AppInfo>,
    onSetHidden: (AppInfo, Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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

@Composable
private fun ColorContent(
    target: PickerTarget,
    settings: VerbumSettings,
    palette: List<Color>,
    onSetBackgroundColor: (Long) -> Unit,
    onSetTextColor: (Long) -> Unit,
    onSetTextColorByUsage: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Just the picker — no headline; the tapped pill already names the target.
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(20.dp)) {
                ColorPickerPanel(
                    initialColor = if (target == PickerTarget.BACKGROUND) settings.bgColor
                    else settings.textColor,
                    presets = palette,
                    onColorChange = if (target == PickerTarget.BACKGROUND) onSetBackgroundColor
                    else onSetTextColor,
                )
            }
        }
        // Text has one extra option, shown as its own Bare pill (title +
        // sub-line) to match the Edit-layout / Hide-apps rows.
        if (target == PickerTarget.TEXT) {
            UsagePill(
                enabled = settings.textColorByUsage,
                onToggle = onSetTextColorByUsage,
            )
        }
    }
}

// ----------------------------------------------------------------------
// Bare-style building blocks
// ----------------------------------------------------------------------

@Composable
private fun PillCard(
    modifier: Modifier,
    badgeColor: Color,
    onClick: (() -> Unit)? = null,
    badgeContent: @Composable BoxScope.() -> Unit = {},
    content: @Composable RowScope.() -> Unit,
) {
    // The row of badge + text. Kept as one lambda so the clickable and plain
    // variants share it.
    val body: @Composable () -> Unit = {
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

    // Use Surface's own onClick so the press ripple is clipped to PILL_SHAPE
    // rather than the surrounding rectangle (a plain .clickable() modifier is
    // not bounded by the Surface's shape).
    if (onClick != null) {
        Surface(
            onClick = onClick,
            shape = PILL_SHAPE,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = modifier,
        ) { body() }
    } else {
        Surface(
            shape = PILL_SHAPE,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = modifier,
        ) { body() }
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

/** The "opacity by usage" toggle, styled as a Bare pill like the action rows. */
@Composable
private fun UsagePill(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    PillCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(PILL_HEIGHT),
        badgeColor = MaterialTheme.colorScheme.surfaceContainer,
        badgeContent = {
            Icon(
                painter = painterResource(R.drawable.ic_visibility),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        },
    ) {
        Column(Modifier.weight(1f)) {
            PillTitle("Opacity by usage")
            Spacer(Modifier.height(4.dp))
            Text(
                "Fade rarely-used apps",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = enabled, onCheckedChange = onToggle)
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
    onClearFont: () -> Unit,
    onOpenColorPicker: () -> Unit,
    onSetTextSize: (Float) -> Unit,
) {
    val hasFont = settings.fontPath != null
    PillCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(PILL_HEIGHT),
        badgeColor = MaterialTheme.colorScheme.surfaceContainer,
        badgeContent = {
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
                // Once a font is imported the button clears it back to default.
                SmallPillButton(
                    icon = if (hasFont) R.drawable.ic_close else R.drawable.ic_import,
                    label = settings.fontName ?: "Font",
                    onClick = if (hasFont) onClearFont else onPickFont,
                )
                SmallPillButton(R.drawable.ic_color_picker, "Color", onOpenColorPicker)
            }
        }
    }
}

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
        Text(
            "${current.roundToInt()}",
            fontFamily = fontFamily,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
        )
    }
}

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

// Standard "overshoot" easing (Android's OvershootInterpolator, tension 2.2).
private val OvershootEasing = Easing { fraction ->
    val t = fraction - 1f
    t * t * (3.2f * t + 2.2f) + 1f
}

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

package com.verbum.launcher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

private fun Color.toArgbLong(): Long = toArgb().toLong() and 0xFFFFFFFFL

/**
 * Inline free-color selection: a saturation/value panel, a hue bar, and two
 * static rows of deduplicated Material 3 preset swatches. The chosen color is
 * applied live through [onColorChange] (there is no Apply button — the caller's
 * Done returns to the settings menu). The currently selected preset is
 * highlighted Bare-style with a primary ring.
 */
@Composable
fun ColorPickerPanel(
    initialColor: Long,
    presets: List<Color>,
    onColorChange: (Long) -> Unit,
) {
    val initialHsv = remember {
        FloatArray(3).also { android.graphics.Color.colorToHSV(initialColor.toInt(), it) }
    }
    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var sat by remember { mutableFloatStateOf(initialHsv[1]) }
    var value by remember { mutableFloatStateOf(initialHsv[2]) }

    val current = Color.hsv(hue, sat, value)
    val currentArgb = current.toArgbLong()

    // Apply the selection live to the homescreen behind the overlay.
    val latestOnChange by rememberUpdatedState(onColorChange)
    LaunchedEffect(currentArgb) { latestOnChange(currentArgb) }

    val presetRows = remember(presets) {
        val unique = presets.distinctBy { it.toArgb() }
        val perRow = (unique.size + 1) / 2
        unique.chunked(perRow.coerceAtLeast(1))
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SaturationValuePanel(
            hue = hue,
            sat = sat,
            value = value,
            onChange = { s, v -> sat = s; value = v },
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(16.dp)),
        )

        HueBar(
            hue = hue,
            onChange = { hue = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
                .clip(RoundedCornerShape(13.dp)),
        )

        presetRows.forEach { rowColors ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowColors.forEach { preset ->
                    val selected = preset.toArgbLong() == currentArgb
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(CircleShape)
                            .background(preset)
                            .border(
                                width = if (selected) 3.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant,
                                shape = CircleShape,
                            )
                            .clickable {
                                val hsv = FloatArray(3)
                                android.graphics.Color.colorToHSV(preset.toArgb(), hsv)
                                hue = hsv[0]
                                sat = hsv[1]
                                value = hsv[2]
                            },
                    )
                }
                repeat(presetRows.first().size - rowColors.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SaturationValuePanel(
    hue: Float,
    sat: Float,
    value: Float,
    onChange: (sat: Float, value: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnChange by rememberUpdatedState(onChange)

    Canvas(
        modifier
            .pointerInput(Unit) {
                detectTapGestures { pos ->
                    currentOnChange(
                        (pos.x / size.width).coerceIn(0f, 1f),
                        1f - (pos.y / size.height).coerceIn(0f, 1f),
                    )
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    currentOnChange(
                        (change.position.x / size.width).coerceIn(0f, 1f),
                        1f - (change.position.y / size.height).coerceIn(0f, 1f),
                    )
                }
            }
    ) {
        drawRect(Brush.horizontalGradient(listOf(Color.White, Color.hsv(hue, 1f, 1f))))
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
        val pos = Offset(sat * size.width, (1f - value) * size.height)
        drawCircle(Color.Black.copy(alpha = 0.6f), 10.dp.toPx(), pos, style = Stroke(2.dp.toPx()))
        drawCircle(Color.White, 8.dp.toPx(), pos, style = Stroke(3.dp.toPx()))
    }
}

@Composable
private fun HueBar(
    hue: Float,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnChange by rememberUpdatedState(onChange)
    val rainbow = remember {
        listOf(0f, 60f, 120f, 180f, 240f, 300f, 360f).map { Color.hsv(it % 360f, 1f, 1f) }
    }

    Canvas(
        modifier
            .pointerInput(Unit) {
                detectTapGestures { pos ->
                    currentOnChange((pos.x / size.width * 360f).coerceIn(0f, 360f))
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    currentOnChange((change.position.x / size.width * 360f).coerceIn(0f, 360f))
                }
            }
    ) {
        drawRect(Brush.horizontalGradient(rainbow))
        val x = hue / 360f * size.width
        drawCircle(
            Color.White,
            size.height / 2f - 3.dp.toPx(),
            Offset(x.coerceIn(size.height / 2f, size.width - size.height / 2f), size.height / 2f),
            style = Stroke(3.dp.toPx()),
        )
    }
}

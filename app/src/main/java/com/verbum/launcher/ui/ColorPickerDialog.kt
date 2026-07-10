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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * Free color selection (saturation/value panel + hue bar) with a row of
 * preset swatches taken from the user's Material 3 dynamic color scheme.
 */
@Composable
fun ColorPickerDialog(
    initialColor: Long,
    presets: List<Color>,
    onDismiss: () -> Unit,
    onPick: (Long) -> Unit,
    showUsageOption: Boolean = false,
    usageEnabled: Boolean = false,
    onToggleUsage: (Boolean) -> Unit = {},
) {
    val initialHsv = remember {
        FloatArray(3).also { android.graphics.Color.colorToHSV(initialColor.toInt(), it) }
    }
    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var sat by remember { mutableFloatStateOf(initialHsv[1]) }
    var value by remember { mutableFloatStateOf(initialHsv[2]) }

    val current = Color.hsv(hue, sat, value)

    // Deduplicated Material 3 presets laid out in two static rows.
    val presetRows = remember(presets) {
        val unique = presets.distinctBy { it.toArgb() }
        val perRow = (unique.size + 1) / 2
        unique.chunked(perRow.coerceAtLeast(1))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Preview of the current selection.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(current)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    )
                }

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

                // Preset swatches from the user's Material You palette, two rows.
                presetRows.forEach { rowColors ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        rowColors.forEach { preset ->
                            Box(
                                Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(CircleShape)
                                    .background(preset)
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant,
                                        CircleShape,
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
                        // Pad a short second row so swatches keep their size.
                        repeat(presetRows.first().size - rowColors.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }

                if (showUsageOption) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Opacity by usage",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                "Fade rarely-used apps (last 30 days)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(checked = usageEnabled, onCheckedChange = onToggleUsage)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onPick(current.toArgb().toLong() and 0xFFFFFFFFL)
            }) { Text("Apply") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
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
        // White → pure hue, left to right.
        drawRect(
            Brush.horizontalGradient(listOf(Color.White, Color.hsv(hue, 1f, 1f)))
        )
        // Transparent → black, top to bottom.
        drawRect(
            Brush.verticalGradient(listOf(Color.Transparent, Color.Black))
        )
        // Selector ring.
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
                    currentOnChange(
                        (change.position.x / size.width * 360f).coerceIn(0f, 360f)
                    )
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

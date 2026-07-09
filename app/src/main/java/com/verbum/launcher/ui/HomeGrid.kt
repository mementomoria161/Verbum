package com.verbum.launcher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.verbum.launcher.ElementUi
import com.verbum.launcher.R
import com.verbum.launcher.model.AppInfo
import com.verbum.launcher.model.GRID_COLUMNS
import com.verbum.launcher.model.GridElement
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * The invisible 4-column grid. Each element is absolutely positioned by its
 * (col, row) cell coordinates; cells are squares of screenWidth / 4. The
 * whole canvas scrolls vertically when elements extend past the viewport.
 */
@Composable
fun HomeGrid(
    modifier: Modifier = Modifier,
    elements: List<ElementUi>,
    editMode: Boolean,
    appTextStyle: TextStyle,
    titleTextStyle: TextStyle,
    accentColor: Color,
    onAppClick: (AppInfo) -> Unit,
    onAppLongPress: (AppInfo) -> Unit,
    onEnterEditMode: () -> Unit,
    onElementMove: (id: String, col: Int, row: Int) -> Unit,
    onElementResize: (id: String, width: Int, height: Int) -> Unit,
    onElementDelete: (id: String) -> Unit,
    onElementRename: (GridElement) -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val cell: Dp = maxWidth / GRID_COLUMNS
        val cellPx = with(LocalDensity.current) { cell.toPx() }

        // The grid is bounded to the space between the top and the bottom bar,
        // with a little breathing room above and below — so it holds only as
        // many rows as actually fit on screen.
        val gridPadding = 10.dp
        val available = (maxHeight - gridPadding * 2).coerceAtLeast(cell)
        val rows = floor(available / cell).toInt().coerceAtLeast(1)

        // True when placing [id] at the given cell rect would overlap any other
        // element. Used to forbid dropping/resizing a folder onto another one.
        fun overlaps(id: String, col: Int, row: Int, w: Int, h: Int): Boolean =
            elements.any { other ->
                val o = other.element
                o.id != id &&
                    col < o.col + o.width && o.col < col + w &&
                    row < o.row + o.height && o.row < row + h
            }

        Box(
            Modifier
                .fillMaxSize()
                .padding(vertical = gridPadding)
                .pointerInput(editMode) {
                    if (!editMode) {
                        detectTapGestures(onLongPress = { onEnterEditMode() })
                    }
                }
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(cell * rows)
            ) {
                // The grid, drawn behind the elements while editing.
                if (editMode) {
                    Canvas(Modifier.matchParentSize()) {
                        val lineColor = accentColor.copy(alpha = 0.18f)
                        val stroke = 1.dp.toPx()
                        for (c in 0..GRID_COLUMNS) {
                            val x = c * cellPx
                            drawLine(lineColor, Offset(x, 0f), Offset(x, size.height), stroke)
                        }
                        val rowCount = (size.height / cellPx).roundToInt()
                        for (r in 0..rowCount) {
                            val y = r * cellPx
                            drawLine(lineColor, Offset(0f, y), Offset(size.width, y), stroke)
                        }
                    }
                }

                elements.forEach { ui ->
                    key(ui.element.id) {
                        val el = ui.element
                        ElementBlock(
                            ui = ui,
                            cellPx = cellPx,
                            maxRows = rows,
                            editMode = editMode,
                            appTextStyle = appTextStyle,
                            titleTextStyle = titleTextStyle,
                            accentColor = accentColor,
                            onAppClick = onAppClick,
                            onAppLongPress = onAppLongPress,
                            onMove = { c, r ->
                                if (!overlaps(el.id, c, r, el.width, el.height)) {
                                    onElementMove(el.id, c, r)
                                }
                            },
                            onResize = { w, h ->
                                if (!overlaps(el.id, el.col, el.row, w, h)) {
                                    onElementResize(el.id, w, h)
                                }
                            },
                            onDelete = { onElementDelete(el.id) },
                            onRename = { onElementRename(el) },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun ElementBlock(
    ui: ElementUi,
    cellPx: Float,
    maxRows: Int,
    editMode: Boolean,
    appTextStyle: TextStyle,
    titleTextStyle: TextStyle,
    accentColor: Color,
    onAppClick: (AppInfo) -> Unit,
    onAppLongPress: (AppInfo) -> Unit,
    onMove: (col: Int, row: Int) -> Unit,
    onResize: (width: Int, height: Int) -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit,
) {
    val el = ui.element
    val density = LocalDensity.current
    val shape = RoundedCornerShape(24.dp)

    // Live gesture deltas in pixels; committed to the grid on release.
    var dragDelta by remember { mutableStateOf(Offset.Zero) }
    var sizeDelta by remember { mutableStateOf(Offset.Zero) }

    val widthDp = with(density) {
        (el.width * cellPx + sizeDelta.x).coerceAtLeast(cellPx * 0.75f).toDp()
    }
    val heightDp = with(density) {
        (el.height * cellPx + sizeDelta.y).coerceAtLeast(cellPx * 0.75f).toDp()
    }

    Box(
        Modifier
            .offset {
                IntOffset(
                    (el.col * cellPx + dragDelta.x).roundToInt(),
                    (el.row * cellPx + dragDelta.y).roundToInt(),
                )
            }
            .size(widthDp, heightDp)
            .padding(4.dp)
    ) {
        // ---- Content -------------------------------------------------
        Column(
            Modifier
                .fillMaxSize()
                .clip(shape)
                .then(
                    if (editMode) {
                        Modifier
                            .background(accentColor.copy(alpha = 0.08f))
                            .border(1.dp, accentColor.copy(alpha = 0.55f), shape)
                    } else Modifier
                )
        ) {
            val title = when {
                el.name.isNotBlank() -> el.name
                editMode && el.isAllApps -> "All apps"
                else -> null
            }
            if (title != null) {
                Text(
                    text = title,
                    style = titleTextStyle,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, start = 8.dp, end = 8.dp),
                )
            }
            // Apps flow like words in a paragraph: gaps between names and a
            // line break whenever the next name would overflow the width.
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState(), enabled = !editMode)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ui.apps.forEach { app ->
                    Text(
                        text = app.label,
                        style = appTextStyle,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.combinedClickable(
                            enabled = !editMode,
                            onClick = { onAppClick(app) },
                            onLongClick = { onAppLongPress(app) },
                        ),
                    )
                }
            }
        }

        // ---- Edit-mode overlays ---------------------------------------
        if (editMode) {
            // Tap to rename, drag to move.
            Box(
                Modifier
                    .matchParentSize()
                    .pointerInput(el) {
                        detectTapGestures(onTap = { onRename() })
                    }
                    .pointerInput(el, cellPx) {
                        detectDragGestures(
                            onDragEnd = {
                                val newCol = ((el.col * cellPx + dragDelta.x) / cellPx)
                                    .roundToInt()
                                    .coerceIn(0, GRID_COLUMNS - el.width)
                                val newRow = ((el.row * cellPx + dragDelta.y) / cellPx)
                                    .roundToInt()
                                    .coerceIn(0, (maxRows - el.height).coerceAtLeast(0))
                                dragDelta = Offset.Zero
                                onMove(newCol, newRow)
                            },
                            onDragCancel = { dragDelta = Offset.Zero },
                        ) { change, amount ->
                            change.consume()
                            dragDelta += amount
                        }
                    }
            )

            // Delete (folders only — the all-apps block is permanent).
            if (!el.isAllApps) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .size(28.dp)
                        .background(accentColor.copy(alpha = 0.9f), CircleShape)
                        .clickable { onDelete() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = "Delete folder",
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            // Resize handle: drag the bottom-right corner.
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
                    .size(28.dp)
                    .background(accentColor, CircleShape)
                    .pointerInput(el, cellPx) {
                        detectDragGestures(
                            onDragEnd = {
                                val newW = ((el.width * cellPx + sizeDelta.x) / cellPx)
                                    .roundToInt()
                                    .coerceIn(1, GRID_COLUMNS - el.col)
                                val newH = ((el.height * cellPx + sizeDelta.y) / cellPx)
                                    .roundToInt()
                                    .coerceIn(1, (maxRows - el.row).coerceAtLeast(1))
                                sizeDelta = Offset.Zero
                                onResize(newW, newH)
                            },
                            onDragCancel = { sizeDelta = Offset.Zero },
                        ) { change, amount ->
                            change.consume()
                            sizeDelta += amount
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_resize),
                    contentDescription = "Resize",
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

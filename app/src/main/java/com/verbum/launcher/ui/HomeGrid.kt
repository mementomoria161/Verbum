package com.verbum.launcher.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.verbum.launcher.ElementUi
import com.verbum.launcher.R
import com.verbum.launcher.model.ALIGN_LEFT
import com.verbum.launcher.model.ALIGN_RIGHT
import com.verbum.launcher.model.ALIGN_JUSTIFIED
import com.verbum.launcher.model.AppInfo
import com.verbum.launcher.model.GRID_COLUMNS
import com.verbum.launcher.model.GridElement
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * The invisible 4-column grid, bounded to the space between the top of the
 * screen and the bottom bar. Cells are squares of screenWidth / 4.
 */
@Composable
fun HomeGrid(
    modifier: Modifier = Modifier,
    elements: List<ElementUi>,
    folders: List<GridElement>,
    editMode: Boolean,
    menuAppKey: String?,
    appTextStyle: TextStyle,
    titleTextStyle: TextStyle,
    accentColor: Color,
    onAppClick: (AppInfo) -> Unit,
    onAppMenuRequest: (AppInfo) -> Unit,
    onAppMenuDismiss: () -> Unit,
    onAppRename: (AppInfo) -> Unit,
    onAppHide: (AppInfo) -> Unit,
    onAppMove: (AppInfo, String?) -> Unit,
    onAppDelete: (AppInfo) -> Unit,
    onEnterEditMode: () -> Unit,
    onElementMove: (id: String, col: Int, row: Int) -> Unit,
    onElementResize: (id: String, width: Int, height: Int) -> Unit,
    onElementDelete: (id: String) -> Unit,
    onElementRename: (GridElement) -> Unit,
    onElementToggleSingleColumn: (id: String) -> Unit,
    onElementToggleShowName: (id: String) -> Unit,
    onElementCycleAlignment: (id: String) -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val cell: Dp = maxWidth / GRID_COLUMNS
        val cellPx = with(LocalDensity.current) { cell.toPx() }

        // Matching breathing room above and below the layout area. The bottom
        // bar adds its own 12dp on top of the grid's bottom padding, so the
        // top padding is larger to make the visual gaps equal.
        val topPadding = 22.dp
        val bottomPadding = 10.dp
        val available = (maxHeight - topPadding - bottomPadding).coerceAtLeast(cell)
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
                .padding(top = topPadding, bottom = bottomPadding)
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
                            folders = folders,
                            cellPx = cellPx,
                            maxRows = rows,
                            editMode = editMode,
                            menuAppKey = menuAppKey,
                            appTextStyle = appTextStyle,
                            titleTextStyle = titleTextStyle,
                            accentColor = accentColor,
                            onAppClick = onAppClick,
                            onAppMenuRequest = onAppMenuRequest,
                            onAppMenuDismiss = onAppMenuDismiss,
                            onAppRename = onAppRename,
                            onAppHide = onAppHide,
                            onAppMove = onAppMove,
                            onAppDelete = onAppDelete,
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
                            onToggleSingleColumn = { onElementToggleSingleColumn(el.id) },
                            onToggleShowName = { onElementToggleShowName(el.id) },
                            onCycleAlignment = { onElementCycleAlignment(el.id) },
                        )
                    }
                }
            }
        }
    }
}

private val EDIT_HANDLE_SIZE = 40.dp
private val EDIT_HANDLE_ICON = 20.dp

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun ElementBlock(
    ui: ElementUi,
    folders: List<GridElement>,
    cellPx: Float,
    maxRows: Int,
    editMode: Boolean,
    menuAppKey: String?,
    appTextStyle: TextStyle,
    titleTextStyle: TextStyle,
    accentColor: Color,
    onAppClick: (AppInfo) -> Unit,
    onAppMenuRequest: (AppInfo) -> Unit,
    onAppMenuDismiss: () -> Unit,
    onAppRename: (AppInfo) -> Unit,
    onAppHide: (AppInfo) -> Unit,
    onAppMove: (AppInfo, String?) -> Unit,
    onAppDelete: (AppInfo) -> Unit,
    onMove: (col: Int, row: Int) -> Unit,
    onResize: (width: Int, height: Int) -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit,
    onToggleSingleColumn: () -> Unit,
    onToggleShowName: () -> Unit,
    onCycleAlignment: () -> Unit,
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
            val hArrangement = when (el.alignment) {
                ALIGN_LEFT -> Arrangement.spacedBy(14.dp, Alignment.Start)
                ALIGN_RIGHT -> Arrangement.spacedBy(14.dp, Alignment.End)
                ALIGN_JUSTIFIED -> Arrangement.SpaceBetween
                else -> Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally)
            }
            val textAlign = when (el.alignment) {
                ALIGN_LEFT -> TextAlign.Start
                ALIGN_RIGHT -> TextAlign.End
                ALIGN_JUSTIFIED -> TextAlign.Justify
                else -> TextAlign.Center
            }

            // Per-folder name toggle; edit mode always shows it for orientation.
            val title = when {
                el.name.isNotBlank() && (el.showName || editMode) -> el.name
                editMode && el.isAllApps -> "All apps"
                else -> null
            }
            if (title != null) {
                Text(
                    text = title,
                    style = titleTextStyle,
                    // Dim the title in edit mode when it is toggled off.
                    color = titleTextStyle.color.copy(
                        alpha = if (editMode && !el.showName && el.name.isNotBlank()) 0.3f else 1f
                    ),
                    textAlign = textAlign,
                    maxLines = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, start = 8.dp, end = 8.dp),
                )
            }

            // Apps flow like words in a paragraph, aligned per folder and
            // centered vertically. With singleColumn each name gets its own line.
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState(), enabled = !editMode)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = hArrangement,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    maxItemsInEachRow = if (el.singleColumn) 1 else Int.MAX_VALUE,
                ) {
                    ui.apps.forEach { app ->
                        Box {
                            Text(
                                text = app.label,
                                style = appTextStyle,
                                // Per-app opacity from recent-usage weighting.
                                color = appTextStyle.color.copy(alpha = app.usageAlpha),
                                textAlign = textAlign,
                                maxLines = 1,
                                modifier = Modifier.combinedClickable(
                                    enabled = !editMode,
                                    onClick = { onAppClick(app) },
                                    onLongClick = { onAppMenuRequest(app) },
                                ),
                            )
                            if (!editMode && menuAppKey == app.key) {
                                AppActionMenu(
                                    folders = folders,
                                    onDismiss = onAppMenuDismiss,
                                    onRename = { onAppRename(app) },
                                    onHide = { onAppHide(app) },
                                    onMove = { folderId -> onAppMove(app, folderId) },
                                    onDelete = { onAppDelete(app) },
                                )
                            }
                        }
                    }
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

            // Per-folder control cluster: one-per-line, name visibility, and
            // text alignment. Laid out vertically so it does not cover the
            // (top) title — unless the block is too short to stack them, in
            // which case it falls back to a horizontal row.
            val handleCount = 2 + if (el.name.isNotBlank()) 1 else 0
            val neededHeight =
                (handleCount * EDIT_HANDLE_SIZE.value + (handleCount - 1) * 6 + 16).dp
            val stackVertically = heightDp >= neededHeight

            val handles: @Composable () -> Unit = {
                EditHandle(
                    icon = R.drawable.ic_paragraph,
                    active = el.singleColumn,
                    contentDescription = "One app per line",
                    onClick = onToggleSingleColumn,
                )
                if (el.name.isNotBlank()) {
                    EditHandle(
                        icon = if (el.showName) R.drawable.ic_match_case
                        else R.drawable.ic_match_case_off,
                        active = el.showName,
                        contentDescription = "Show folder name",
                        onClick = onToggleShowName,
                    )
                }
                EditHandle(
                    icon = when (el.alignment) {
                        ALIGN_LEFT -> R.drawable.ic_align_left
                        ALIGN_RIGHT -> R.drawable.ic_align_right
                        ALIGN_JUSTIFIED -> R.drawable.ic_align_justify
                        else -> R.drawable.ic_align_center
                    },
                    active = false,
                    contentDescription = "Text alignment",
                    onClick = onCycleAlignment,
                )
            }

            val clusterModifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
            if (stackVertically) {
                Column(
                    clusterModifier,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) { handles() }
            } else {
                Row(
                    clusterModifier,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) { handles() }
            }

            // Delete (folders only — the all-apps block is permanent).
            if (!el.isAllApps) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .size(EDIT_HANDLE_SIZE)
                        .background(accentColor.copy(alpha = 0.9f), CircleShape)
                        .clickable { onDelete() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = "Delete folder",
                        tint = Color.Black,
                        modifier = Modifier.size(EDIT_HANDLE_ICON),
                    )
                }
            }

            // Resize handle: drag the bottom-right corner.
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
                    .size(EDIT_HANDLE_SIZE)
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
                    modifier = Modifier.size(EDIT_HANDLE_ICON),
                )
            }
        }
    }
}

@Composable
private fun EditHandle(
    icon: Int,
    active: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
) {
    // Bare-style selection highlight: active = primaryContainer, else neutral.
    val bg = if (active) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (active) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        Modifier
            .size(EDIT_HANDLE_SIZE)
            .background(bg, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = fg,
            modifier = Modifier.size(EDIT_HANDLE_ICON),
        )
    }
}

// Connected split-button shapes, matching the bottom bar's button group.
private val SEGMENT_START = RoundedCornerShape(
    topStart = 28.dp, bottomStart = 28.dp, topEnd = 8.dp, bottomEnd = 8.dp
)
private val SEGMENT_MIDDLE = RoundedCornerShape(8.dp)
private val SEGMENT_END = RoundedCornerShape(
    topStart = 8.dp, bottomStart = 8.dp, topEnd = 28.dp, bottomEnd = 28.dp
)

/**
 * The floating action menu for a long-pressed app: an M3 split button in the
 * same format as the bottom bar, floating above the app name with a small
 * margin. Tapping Folder expands an animated FAB menu of folders above it,
 * while the split button itself stays visible.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun AppActionMenu(
    folders: List<GridElement>,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onHide: () -> Unit,
    onMove: (String?) -> Unit,
    onDelete: () -> Unit,
) {
    var showFolders by remember { mutableStateOf(false) }
    val marginPx = with(LocalDensity.current) { 8.dp.roundToPx() }

    // Centered above the anchor (the app name) with a small margin.
    val positionProvider = remember(marginPx) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset {
                val x = anchorBounds.left +
                    (anchorBounds.width - popupContentSize.width) / 2
                val y = anchorBounds.top - popupContentSize.height - marginPx
                return IntOffset(
                    x.coerceIn(
                        0,
                        (windowSize.width - popupContentSize.width).coerceAtLeast(0),
                    ),
                    y.coerceAtLeast(0),
                )
            }
        }
    }

    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Column(horizontalAlignment = Alignment.End) {
            // Animated FAB menu of folders, above the split button.
            AnimatedVisibility(
                visible = showFolders,
                enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom),
            ) {
                Column(
                    Modifier.padding(bottom = 10.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Bottom-most entry (nearest the button) animates in first.
                    val entries = folders.map { it.name.ifBlank { "Unnamed folder" } to it.id } +
                        ("All apps" to null)
                    entries.forEachIndexed { index, (label, folderId) ->
                        FolderFab(
                            label = label,
                            modifier = Modifier.animateEnterExit(
                                enter = fadeIn(tween(180, delayMillis = (entries.size - 1 - index) * 40)) +
                                    scaleIn(
                                        tween(180, delayMillis = (entries.size - 1 - index) * 40),
                                        initialScale = 0.6f,
                                    ),
                                exit = fadeOut(tween(100)),
                            ),
                            onClick = { onMove(folderId) },
                        )
                    }
                }
            }

            // The split button — always visible.
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                MenuSegment(R.drawable.ic_edit, "Rename", SEGMENT_START, onRename)
                MenuSegment(R.drawable.ic_visibility_off, "Hide", SEGMENT_MIDDLE, onHide)
                MenuSegment(R.drawable.ic_folder, "Folder", SEGMENT_MIDDLE) {
                    showFolders = !showFolders
                }
                MenuSegment(R.drawable.ic_delete, "Delete", SEGMENT_END, onDelete)
            }
        }
    }
}

/** A Material 3 FAB-style folder button used in the app action menu. */
@Composable
private fun FolderFab(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shadowElevation = 6.dp,
    ) {
        Row(
            Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_folder),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(label, fontSize = 14.sp, maxLines = 1)
        }
    }
}

@Composable
private fun MenuSegment(
    icon: Int?,
    label: String,
    shape: RoundedCornerShape,
    onClick: () -> Unit,
) {
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            Modifier
                .clickable(onClick = onClick)
                .height(56.dp)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(label, fontSize = 14.sp, maxLines = 1)
        }
    }
}

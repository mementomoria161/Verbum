package com.verbum.launcher.ui

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.verbum.launcher.VerbumViewModel
import com.verbum.launcher.model.AppInfo
import com.verbum.launcher.model.GridElement
import java.io.File

/** Root composable of the launcher. */
@Composable
fun VerbumApp(viewModel: VerbumViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = state.settings
    val transient = state.transient

    val textColor = Color(settings.textColor)
    val fontFamily = rememberCustomFontFamily(settings.fontPath)
    val backgroundBitmap = rememberBackgroundBitmap(settings.bgImagePath)

    val appTextStyle = TextStyle(
        fontFamily = fontFamily,
        fontSize = settings.textSizeSp.sp,
        lineHeight = (settings.textSizeSp * 1.4f).sp,
        letterSpacing = 0.sp,
        color = textColor,
    )
    val titleTextStyle = TextStyle(
        fontFamily = fontFamily,
        fontSize = (settings.textSizeSp * 0.7f).sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp,
        color = textColor.copy(alpha = 0.65f),
    )

    // Overlay & dialog state.
    var menuAppKey by remember { mutableStateOf<String?>(null) }
    var renameTarget by remember { mutableStateOf<AppInfo?>(null) }
    var elementRenameTarget by remember { mutableStateOf<GridElement?>(null) }
    var showAddFolder by remember { mutableStateOf(false) }
    // Which panel the Customize overlay shows (pills / hide apps / color).
    var customizeScreen by remember { mutableStateOf<CustomizeScreen>(CustomizeScreen.Pills) }
    // Color to restore if the user cancels a color selection.
    var colorRevert by remember { mutableLongStateOf(0L) }
    val inSubScreen = transient.customizeOpen && customizeScreen != CustomizeScreen.Pills

    BackHandler(enabled = transient.searchOpen || transient.editMode || transient.customizeOpen) {
        when {
            inSubScreen -> customizeScreen = CustomizeScreen.Pills
            transient.customizeOpen -> viewModel.closeCustomize()
            transient.searchOpen -> viewModel.closeSearch()
            transient.editMode -> viewModel.setEditMode(false)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(settings.bgColor))
    ) {
        if (backgroundBitmap != null) {
            Image(
                bitmap = backgroundBitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Layer 1: the grid, leaving room for the bottom bar below.
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                HomeGrid(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    elements = state.elements,
                    folders = state.folders,
                    editMode = transient.editMode,
                    menuAppKey = menuAppKey,
                    appTextStyle = appTextStyle,
                    titleTextStyle = titleTextStyle,
                    accentColor = textColor,
                    onAppClick = viewModel::launchApp,
                    onAppMenuRequest = { menuAppKey = it.key },
                    onAppMenuDismiss = { menuAppKey = null },
                    onAppRename = { app ->
                        menuAppKey = null
                        renameTarget = app
                    },
                    onAppHide = { app ->
                        menuAppKey = null
                        viewModel.hideApp(app)
                    },
                    onAppMove = { app, folderId ->
                        menuAppKey = null
                        viewModel.moveAppToFolder(app, folderId)
                    },
                    onAppDelete = { app ->
                        menuAppKey = null
                        viewModel.uninstallApp(app)
                    },
                    onEnterEditMode = { viewModel.setEditMode(true) },
                    onElementMove = viewModel::moveElement,
                    onElementResize = viewModel::resizeElement,
                    onElementDelete = viewModel::removeElement,
                    onElementRename = { elementRenameTarget = it },
                    onElementToggleSingleColumn = viewModel::toggleElementSingleColumn,
                    onElementToggleShowName = viewModel::toggleElementShowName,
                    onElementCycleAlignment = viewModel::cycleElementAlignment,
                )
            }
            Spacer(Modifier.height(BottomBarHeight))
        }

        // Layer 2: the Customize overlay. Its scrim covers the whole screen —
        // including behind the bottom bar, which is drawn on top of it.
        if (transient.customizeOpen) {
            CustomizeSheet(
                settings = settings,
                fontFamily = fontFamily,
                screen = customizeScreen,
                visibleApps = state.manageableApps.filter { !it.hidden }.map { it.app },
                hiddenApps = state.hiddenApps,
                onScrimTap = {
                    if (inSubScreen) customizeScreen = CustomizeScreen.Pills
                    else viewModel.closeCustomize()
                },
                onOpenHideApps = { customizeScreen = CustomizeScreen.HideApps },
                onOpenColorPicker = { target ->
                    colorRevert = when (target) {
                        PickerTarget.BACKGROUND -> settings.bgColor
                        PickerTarget.TEXT -> settings.textColor
                    }
                    customizeScreen = CustomizeScreen.Color(target)
                },
                onCustomizeHomescreen = { viewModel.setEditMode(true) },
                onSetBackgroundColor = { argb ->
                    viewModel.setBackgroundColor(argb)
                    // Picking a color replaces a previously set image.
                    if (settings.bgImagePath != null) viewModel.clearBackgroundImage()
                },
                onPickBackgroundImage = viewModel::setBackgroundImage,
                onSetTextSize = viewModel::setTextSize,
                onSetTextColor = viewModel::setTextColor,
                onPickFont = viewModel::setFont,
                onClearFont = viewModel::clearFont,
                onSetTextColorByUsage = viewModel::setTextColorByUsage,
                onSetHidden = { app, hidden ->
                    if (hidden) viewModel.hideApp(app) else viewModel.unhideApp(app)
                },
            )
        }

        // Layer 3: the bottom bar, above the scrim.
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .safeDrawingPadding()
        ) {
            BottomBar(
                searchOpen = transient.searchOpen,
                editMode = transient.editMode,
                customizeOpen = transient.customizeOpen,
                settingsSubActive = inSubScreen,
                query = transient.query,
                textColor = textColor,
                fontFamily = fontFamily,
                onQueryChange = viewModel::setQuery,
                onOpenSearch = viewModel::openSearch,
                onCloseSearch = viewModel::closeSearch,
                onSearchSubmit = {
                    // Launch the single remaining match, if there is exactly one.
                    val matches = state.elements.flatMap { it.apps }.distinctBy { it.key }
                    matches.singleOrNull()?.let(viewModel::launchApp)
                },
                onOpenCustomize = {
                    if (transient.customizeOpen) {
                        viewModel.closeCustomize()
                    } else {
                        customizeScreen = CustomizeScreen.Pills
                        viewModel.openCustomize()
                    }
                },
                onAddFolder = { showAddFolder = true },
                onSettingsDone = { customizeScreen = CustomizeScreen.Pills },
                onSettingsCancel = (customizeScreen as? CustomizeScreen.Color)?.let { colorScreen ->
                    {
                        when (colorScreen.target) {
                            PickerTarget.BACKGROUND -> viewModel.setBackgroundColor(colorRevert)
                            PickerTarget.TEXT -> viewModel.setTextColor(colorRevert)
                        }
                        customizeScreen = CustomizeScreen.Pills
                    }
                },
                onExitEditMode = { viewModel.setEditMode(false) },
            )
        }
    }

    // ------------------------------------------------------------------
    // Dialogs
    // ------------------------------------------------------------------

    renameTarget?.let { app ->
        TextInputDialog(
            title = "Rename app",
            initialValue = app.label,
            confirmLabel = "Save",
            supportingText = "Leave empty to restore the original name.",
            onDismiss = { renameTarget = null },
            onConfirm = { newName ->
                viewModel.renameApp(app, newName.ifBlank { null })
                renameTarget = null
            },
        )
    }

    elementRenameTarget?.let { element ->
        TextInputDialog(
            title = if (element.isAllApps) "Rename block" else "Rename folder",
            initialValue = element.name,
            confirmLabel = "Save",
            onDismiss = { elementRenameTarget = null },
            onConfirm = { newName ->
                viewModel.renameElement(element.id, newName)
                elementRenameTarget = null
            },
        )
    }

    if (showAddFolder) {
        TextInputDialog(
            title = "New folder",
            initialValue = "",
            confirmLabel = "Add",
            onDismiss = { showAddFolder = false },
            onConfirm = { name ->
                if (name.isNotBlank()) viewModel.addFolder(name.trim())
                showAddFolder = false
            },
        )
    }
}

/** Loads the user-imported .ttf/.otf, or null for the system default. */
@Composable
private fun rememberCustomFontFamily(path: String?): FontFamily? =
    remember(path) {
        path?.let { p ->
            runCatching { FontFamily(Font(File(p))) }.getOrNull()
        }
    }

/** Decodes (and downsamples) the user's background image. */
@Composable
private fun rememberBackgroundBitmap(path: String?): ImageBitmap? =
    remember(path) {
        path?.let { p ->
            runCatching {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(p, bounds)
                var sample = 1
                val maxDimension = 2048
                while (bounds.outWidth / (sample * 2) > maxDimension ||
                    bounds.outHeight / (sample * 2) > maxDimension
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

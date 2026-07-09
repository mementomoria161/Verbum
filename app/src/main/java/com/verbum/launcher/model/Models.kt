package com.verbum.launcher.model

import kotlinx.serialization.Serializable

const val GRID_COLUMNS = 4
const val MAX_GRID_ROWS = 16
const val ALL_APPS_ID = "all_apps"

/**
 * One block on the invisible 4-column grid. Position and spans are measured
 * in grid cells (square cells, screenWidth / 4 each).
 *
 * The element flagged [isAllApps] shows every visible app that is not
 * assigned to a folder; other elements are user-created folders whose
 * members are stored in [apps] as app keys ("package/activity").
 */
@Serializable
data class GridElement(
    val id: String,
    val name: String = "",
    val col: Int = 0,
    val row: Int = 0,
    val width: Int = GRID_COLUMNS,
    val height: Int = 6,
    val isAllApps: Boolean = false,
    val apps: List<String> = emptyList(),
)

fun defaultLayout(): List<GridElement> = listOf(
    GridElement(id = ALL_APPS_ID, isAllApps = true)
)

/** A launchable activity. [key] uniquely identifies it across renames. */
data class AppInfo(
    val key: String,
    val packageName: String,
    val activityClass: String,
    val label: String,
)

/** Everything the user can persist: layout, app management, and appearance. */
data class VerbumSettings(
    val elements: List<GridElement> = defaultLayout(),
    val renames: Map<String, String> = emptyMap(),
    val hidden: Set<String> = emptySet(),
    val textSizeSp: Float = 20f,
    val textColor: Long = 0xFFFFFFFF,
    val bgColor: Long = 0xFF000000,
    val bgImagePath: String? = null,
    val fontPath: String? = null,
    /** The typographic family name read from the imported font file. */
    val fontName: String? = null,
)

package com.verbum.launcher.data

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.verbum.launcher.model.GRID_COLUMNS
import com.verbum.launcher.model.GridElement
import com.verbum.launcher.model.MAX_GRID_ROWS
import com.verbum.launcher.model.VerbumSettings
import com.verbum.launcher.model.defaultLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

private val Context.dataStore by preferencesDataStore(name = "verbum_settings")

/**
 * DataStore-backed persistence for the grid layout, app management state
 * (renames / hidden apps / folder membership), and appearance preferences.
 * All mutations are read-modify-write inside a single edit block, so they
 * are atomic.
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val LAYOUT = stringPreferencesKey("layout_json")
        val RENAMES = stringPreferencesKey("renames_json")
        val HIDDEN = stringPreferencesKey("hidden_json")
        val TEXT_SIZE = floatPreferencesKey("text_size_sp")
        val TEXT_COLOR = longPreferencesKey("text_color")
        val BG_COLOR = longPreferencesKey("bg_color")
        val BG_IMAGE = stringPreferencesKey("bg_image_path")
        val FONT = stringPreferencesKey("font_path")
        val FONT_NAME = stringPreferencesKey("font_name")
    }

    private val json = Json { ignoreUnknownKeys = true }

    val settings: Flow<VerbumSettings> = context.dataStore.data.map { prefs ->
        VerbumSettings(
            elements = parseLayout(prefs[Keys.LAYOUT]),
            renames = decodeOrDefault(prefs[Keys.RENAMES], emptyMap()),
            hidden = decodeOrDefault(prefs[Keys.HIDDEN], emptySet()),
            textSizeSp = prefs[Keys.TEXT_SIZE] ?: 20f,
            textColor = prefs[Keys.TEXT_COLOR] ?: 0xFFFFFFFF,
            bgColor = prefs[Keys.BG_COLOR] ?: 0xFF000000,
            bgImagePath = prefs[Keys.BG_IMAGE],
            fontPath = prefs[Keys.FONT],
            fontName = prefs[Keys.FONT_NAME],
        )
    }

    private inline fun <reified T> decodeOrDefault(raw: String?, default: T): T =
        raw?.let { runCatching { json.decodeFromString<T>(it) }.getOrNull() } ?: default

    private fun parseLayout(raw: String?): List<GridElement> {
        val stored = decodeOrDefault<List<GridElement>>(raw, emptyList())
        return if (stored.any { it.isAllApps }) stored else defaultLayout() + stored
    }

    private fun writeLayout(
        prefs: MutablePreferences,
        elements: List<GridElement>,
    ) {
        prefs[Keys.LAYOUT] = json.encodeToString(elements)
    }

    // ------------------------------------------------------------------
    // Layout / grid elements
    // ------------------------------------------------------------------

    suspend fun updateElement(id: String, transform: (GridElement) -> GridElement) {
        context.dataStore.edit { prefs ->
            val layout = parseLayout(prefs[Keys.LAYOUT])
            writeLayout(prefs, layout.map { el ->
                if (el.id == id) sanitize(transform(el)) else el
            })
        }
    }

    suspend fun addFolder(name: String) {
        context.dataStore.edit { prefs ->
            val layout = parseLayout(prefs[Keys.LAYOUT])
            val w = 2
            val h = 2

            fun isFree(col: Int, row: Int): Boolean = layout.none { o ->
                col < o.col + o.width && o.col < col + w &&
                    row < o.row + o.height && o.row < row + h
            }

            // Place the new folder in the first free cell scanning from the top,
            // so it lands on-screen; fall back to below everything if the grid
            // is packed.
            var placedCol = 0
            var placedRow = layout.maxOfOrNull { it.row + it.height } ?: 0
            search@ for (r in 0 until 32) {
                for (c in 0..(GRID_COLUMNS - w)) {
                    if (isFree(c, r)) {
                        placedCol = c
                        placedRow = r
                        break@search
                    }
                }
            }

            val folder = GridElement(
                id = UUID.randomUUID().toString(),
                name = name,
                col = placedCol,
                row = placedRow,
                width = w,
                height = h,
            )
            writeLayout(prefs, layout + folder)
        }
    }

    suspend fun removeElement(id: String) {
        context.dataStore.edit { prefs ->
            val layout = parseLayout(prefs[Keys.LAYOUT])
            writeLayout(
                prefs,
                layout.filterNot { it.id == id && !it.isAllApps }
            )
        }
    }

    /** Moves an app into [folderId], or back to the all-apps block when null. */
    suspend fun moveAppToFolder(appKey: String, folderId: String?) {
        context.dataStore.edit { prefs ->
            val layout = parseLayout(prefs[Keys.LAYOUT])
            writeLayout(prefs, layout.map { el ->
                when {
                    el.id == folderId && !el.isAllApps ->
                        el.copy(apps = (el.apps - appKey) + appKey)
                    else -> el.copy(apps = el.apps - appKey)
                }
            })
        }
    }

    private fun sanitize(el: GridElement): GridElement {
        val width = el.width.coerceIn(1, GRID_COLUMNS)
        val col = el.col.coerceIn(0, GRID_COLUMNS - width)
        return el.copy(
            col = col,
            row = el.row.coerceAtLeast(0),
            width = width,
            height = el.height.coerceIn(1, MAX_GRID_ROWS),
        )
    }

    // ------------------------------------------------------------------
    // App management
    // ------------------------------------------------------------------

    suspend fun setRename(appKey: String, name: String?) {
        context.dataStore.edit { prefs ->
            val renames = decodeOrDefault<Map<String, String>>(prefs[Keys.RENAMES], emptyMap())
            val updated =
                if (name.isNullOrBlank()) renames - appKey else renames + (appKey to name.trim())
            prefs[Keys.RENAMES] = json.encodeToString(updated)
        }
    }

    suspend fun setHidden(appKey: String, hidden: Boolean) {
        context.dataStore.edit { prefs ->
            val current = decodeOrDefault<Set<String>>(prefs[Keys.HIDDEN], emptySet())
            prefs[Keys.HIDDEN] = json.encodeToString(if (hidden) current + appKey else current - appKey)
        }
    }

    // ------------------------------------------------------------------
    // Appearance
    // ------------------------------------------------------------------

    suspend fun setTextSize(sp: Float) =
        context.dataStore.edit { it[Keys.TEXT_SIZE] = sp }

    suspend fun setTextColor(argb: Long) =
        context.dataStore.edit { it[Keys.TEXT_COLOR] = argb }

    suspend fun setBackgroundColor(argb: Long) =
        context.dataStore.edit { it[Keys.BG_COLOR] = argb }

    suspend fun setBackgroundImage(uri: Uri) {
        val copied = copyToFiles(uri, "background_") ?: return
        context.dataStore.edit { prefs ->
            prefs[Keys.BG_IMAGE]?.let { File(it).delete() }
            prefs[Keys.BG_IMAGE] = copied.absolutePath
        }
    }

    suspend fun clearBackgroundImage() {
        context.dataStore.edit { prefs ->
            prefs[Keys.BG_IMAGE]?.let { File(it).delete() }
            prefs.remove(Keys.BG_IMAGE)
        }
    }

    suspend fun setFont(uri: Uri) {
        val copied = copyToFiles(uri, "font_") ?: return
        // Reject files that are not loadable fonts.
        val valid = runCatching {
            android.graphics.Typeface.createFromFile(copied)
        }.isSuccess
        if (!valid) {
            copied.delete()
            return
        }
        val name = withContext(Dispatchers.IO) { FontNameReader.read(copied) }
        context.dataStore.edit { prefs ->
            prefs[Keys.FONT]?.let { File(it).delete() }
            prefs[Keys.FONT] = copied.absolutePath
            if (name != null) prefs[Keys.FONT_NAME] = name else prefs.remove(Keys.FONT_NAME)
        }
    }

    suspend fun clearFont() {
        context.dataStore.edit { prefs ->
            prefs[Keys.FONT]?.let { File(it).delete() }
            prefs.remove(Keys.FONT)
            prefs.remove(Keys.FONT_NAME)
        }
    }

    private suspend fun copyToFiles(uri: Uri, prefix: String): File? =
        withContext(Dispatchers.IO) {
            runCatching {
                val target = File(context.filesDir, "$prefix${System.currentTimeMillis()}")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                } ?: return@runCatching null
                target
            }.getOrNull()
        }
}

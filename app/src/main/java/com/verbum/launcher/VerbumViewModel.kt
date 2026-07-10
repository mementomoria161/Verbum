package com.verbum.launcher

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.verbum.launcher.data.AppRepository
import com.verbum.launcher.data.SettingsRepository
import com.verbum.launcher.model.AppInfo
import com.verbum.launcher.model.GridElement
import com.verbum.launcher.model.VerbumSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A grid element together with the (search-filtered) apps it displays. */
data class ElementUi(
    val element: GridElement,
    val apps: List<AppInfo>,
)

/** One row in the App management screen: an app plus its current state. */
data class ManageableApp(
    val app: AppInfo,
    val hidden: Boolean,
    val folderId: String?,
    val folderName: String?,
)

/** UI-only state that is never persisted. */
data class TransientState(
    val query: String = "",
    val searchOpen: Boolean = false,
    val editMode: Boolean = false,
    val customizeOpen: Boolean = false,
)

data class UiState(
    val elements: List<ElementUi> = emptyList(),
    val folders: List<GridElement> = emptyList(),
    val hiddenApps: List<AppInfo> = emptyList(),
    val manageableApps: List<ManageableApp> = emptyList(),
    val transient: TransientState = TransientState(),
    val settings: VerbumSettings = VerbumSettings(),
)

class VerbumViewModel(application: Application) : AndroidViewModel(application) {

    private val appRepo = AppRepository(application)
    private val settingsRepo = SettingsRepository(application)
    private val transient = MutableStateFlow(TransientState())

    val uiState: StateFlow<UiState> =
        combine(appRepo.apps, settingsRepo.settings, transient) { apps, settings, t ->
            buildState(apps, settings, t)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState())

    private fun buildState(
        installed: List<AppInfo>,
        settings: VerbumSettings,
        t: TransientState,
    ): UiState {
        // Usage-based opacity: most-used app -> 1.0, unused -> 0.3, linear between.
        val maxUsage = settings.usageCounts.values.maxOrNull() ?: 0
        fun usageAlpha(key: String): Float {
            if (!settings.textColorByUsage || maxUsage <= 0) return 1f
            val count = settings.usageCounts[key] ?: 0
            return 0.3f + 0.7f * (count.toFloat() / maxUsage)
        }

        fun applyRename(app: AppInfo): AppInfo {
            val renamed = settings.renames[app.key]?.let { app.copy(label = it) } ?: app
            return renamed.copy(usageAlpha = usageAlpha(app.key))
        }

        val allApps = installed
            .map(::applyRename)
            .sortedBy { it.label.lowercase() }
        val visible = allApps.filter { it.key !in settings.hidden }
        val visibleByKey = visible.associateBy { it.key }
        val allByKey = allApps.associateBy { it.key }

        val keysInFolders = settings.elements
            .filterNot { it.isAllApps }
            .flatMap { it.apps }
            .toSet()

        val query = t.query.trim()
        // While actively searching, hidden apps are included and searched too.
        val searching = t.searchOpen && query.isNotEmpty()
        fun searchFilter(apps: List<AppInfo>) =
            if (searching) {
                apps.filter { it.label.contains(query, ignoreCase = true) }
            } else apps

        val elements = settings.elements.map { el ->
            val members = if (el.isAllApps) {
                (if (searching) allApps else visible).filter { it.key !in keysInFolders }
            } else {
                el.apps
                    .mapNotNull { (if (searching) allByKey else visibleByKey)[it] }
                    .sortedBy { it.label.lowercase() }
            }
            ElementUi(el, searchFilter(members))
        }

        val hiddenApps = installed
            .filter { it.key in settings.hidden }
            .map(::applyRename)
            .sortedBy { it.label.lowercase() }

        // Which folder (if any) each app currently belongs to.
        val folderByKey: Map<String, GridElement> = buildMap {
            settings.elements.filterNot { it.isAllApps }.forEach { folder ->
                folder.apps.forEach { key -> put(key, folder) }
            }
        }
        val manageableApps = installed
            .map(::applyRename)
            .sortedBy { it.label.lowercase() }
            .map { app ->
                val folder = folderByKey[app.key]
                ManageableApp(
                    app = app,
                    hidden = app.key in settings.hidden,
                    folderId = folder?.id,
                    folderName = folder?.name?.ifBlank { "Unnamed folder" },
                )
            }

        return UiState(
            elements = elements,
            folders = settings.elements.filterNot { it.isAllApps },
            hiddenApps = hiddenApps,
            manageableApps = manageableApps,
            transient = t,
            settings = settings,
        )
    }

    // ------------------------------------------------------------------
    // Launching & transient UI state
    // ------------------------------------------------------------------

    fun launchApp(app: AppInfo) {
        appRepo.launch(app)
        viewModelScope.launch { settingsRepo.recordLaunch(app.key) }
        closeSearch()
    }

    fun openSearch() = transient.update { it.copy(searchOpen = true) }

    fun closeSearch() = transient.update { it.copy(searchOpen = false, query = "") }

    fun setQuery(query: String) = transient.update { it.copy(query = query) }

    fun openCustomize() = transient.update { it.copy(customizeOpen = true) }

    fun closeCustomize() = transient.update { it.copy(customizeOpen = false) }

    fun setEditMode(enabled: Boolean) = transient.update {
        it.copy(editMode = enabled, customizeOpen = false, searchOpen = false, query = "")
    }

    /** Called when the home button re-launches the launcher. */
    fun resetTransientState() {
        transient.value = TransientState()
    }

    // ------------------------------------------------------------------
    // App management
    // ------------------------------------------------------------------

    fun renameApp(app: AppInfo, newName: String?) =
        viewModelScope.launch { settingsRepo.setRename(app.key, newName) }

    fun hideApp(app: AppInfo) =
        viewModelScope.launch { settingsRepo.setHidden(app.key, true) }

    fun unhideApp(app: AppInfo) =
        viewModelScope.launch { settingsRepo.setHidden(app.key, false) }

    fun moveAppToFolder(app: AppInfo, folderId: String?) =
        viewModelScope.launch { settingsRepo.moveAppToFolder(app.key, folderId) }

    // ------------------------------------------------------------------
    // Grid layout
    // ------------------------------------------------------------------

    fun addFolder(name: String) =
        viewModelScope.launch { settingsRepo.addFolder(name) }

    fun removeElement(id: String) =
        viewModelScope.launch { settingsRepo.removeElement(id) }

    fun renameElement(id: String, name: String) =
        viewModelScope.launch {
            settingsRepo.updateElement(id) { it.copy(name = name.trim()) }
        }

    fun moveElement(id: String, col: Int, row: Int) =
        viewModelScope.launch {
            settingsRepo.updateElement(id) { it.copy(col = col, row = row) }
        }

    fun resizeElement(id: String, width: Int, height: Int) =
        viewModelScope.launch {
            settingsRepo.updateElement(id) { it.copy(width = width, height = height) }
        }

    fun toggleElementSingleColumn(id: String) =
        viewModelScope.launch {
            settingsRepo.updateElement(id) { it.copy(singleColumn = !it.singleColumn) }
        }

    fun toggleElementShowName(id: String) =
        viewModelScope.launch {
            settingsRepo.updateElement(id) { it.copy(showName = !it.showName) }
        }

    fun cycleElementAlignment(id: String) =
        viewModelScope.launch {
            settingsRepo.updateElement(id) { it.copy(alignment = (it.alignment + 1) % 3) }
        }

    // ------------------------------------------------------------------
    // Appearance
    // ------------------------------------------------------------------

    fun setTextSize(sp: Float) = viewModelScope.launch { settingsRepo.setTextSize(sp) }

    fun setTextColor(argb: Long) = viewModelScope.launch { settingsRepo.setTextColor(argb) }

    fun setBackgroundColor(argb: Long) =
        viewModelScope.launch { settingsRepo.setBackgroundColor(argb) }

    fun setShowFolderNames(show: Boolean) =
        viewModelScope.launch { settingsRepo.setShowFolderNames(show) }

    fun setTextColorByUsage(enabled: Boolean) =
        viewModelScope.launch { settingsRepo.setTextColorByUsage(enabled) }

    fun setBackgroundImage(uri: Uri) =
        viewModelScope.launch { settingsRepo.setBackgroundImage(uri) }

    fun clearBackgroundImage() =
        viewModelScope.launch { settingsRepo.clearBackgroundImage() }

    fun setFont(uri: Uri) = viewModelScope.launch { settingsRepo.setFont(uri) }

    fun clearFont() = viewModelScope.launch { settingsRepo.clearFont() }

    override fun onCleared() {
        appRepo.close()
        super.onCleared()
    }
}

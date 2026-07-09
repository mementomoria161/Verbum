package com.verbum.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.verbum.launcher.ManageableApp
import com.verbum.launcher.R
import com.verbum.launcher.model.AppInfo
import com.verbum.launcher.model.GridElement

/**
 * Full-screen App management surface reached from Customize → Manage apps.
 * Each row exposes three actions: rename, hide/unhide, and a folder picker.
 */
@Composable
fun AppManagementScreen(
    apps: List<ManageableApp>,
    folders: List<GridElement>,
    onDismiss: () -> Unit,
    onRename: (AppInfo, String?) -> Unit,
    onSetHidden: (AppInfo, Boolean) -> Unit,
    onMove: (AppInfo, String?) -> Unit,
) {
    var filter by remember { mutableStateOf("") }
    var renameTarget by remember { mutableStateOf<AppInfo?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Column(
                Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .imePadding()
            ) {
                // Header
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 12.dp, top = 16.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Manage apps",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onDismiss) { Text("Done") }
                }

                OutlinedTextField(
                    value = filter,
                    onValueChange = { filter = it },
                    singleLine = true,
                    leadingIcon = {
                        Icon(painterResource(R.drawable.ic_search), contentDescription = null)
                    },
                    placeholder = { Text("Filter apps") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 4.dp),
                )

                val query = filter.trim()
                val shown = if (query.isEmpty()) apps
                else apps.filter { it.app.label.contains(query, ignoreCase = true) }

                LazyColumn(Modifier.fillMaxSize()) {
                    items(shown, key = { it.app.key }) { item ->
                        AppRow(
                            item = item,
                            folders = folders,
                            onRename = { renameTarget = item.app },
                            onToggleHidden = { onSetHidden(item.app, !item.hidden) },
                            onMove = { folderId -> onMove(item.app, folderId) },
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        )
                    }
                }
            }
        }
    }

    renameTarget?.let { app ->
        TextInputDialog(
            title = "Rename app",
            initialValue = app.label,
            confirmLabel = "Save",
            supportingText = "Leave empty to restore the original name.",
            onDismiss = { renameTarget = null },
            onConfirm = { newName ->
                onRename(app, newName.ifBlank { null })
                renameTarget = null
            },
        )
    }
}

@Composable
private fun AppRow(
    item: ManageableApp,
    folders: List<GridElement>,
    onRename: () -> Unit,
    onToggleHidden: () -> Unit,
    onMove: (String?) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = item.app.label,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (item.hidden) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            val subtitle = buildList {
                if (item.hidden) add("Hidden")
                item.folderName?.let { add("in $it") }
            }.joinToString(" · ")
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // 1) Rename
        IconButton(onClick = onRename) {
            Icon(
                painter = painterResource(R.drawable.ic_edit),
                contentDescription = "Rename ${item.app.label}",
                modifier = Modifier.size(22.dp),
            )
        }

        // 2) Hide / unhide toggle
        IconButton(onClick = onToggleHidden) {
            Icon(
                painter = painterResource(
                    if (item.hidden) R.drawable.ic_visibility_off else R.drawable.ic_visibility
                ),
                contentDescription = if (item.hidden) "Unhide ${item.app.label}" else "Hide ${item.app.label}",
                modifier = Modifier.size(22.dp),
                tint = if (item.hidden) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        }

        // 3) Folder picker (drops down all available folders)
        FolderPickerButton(
            currentFolderId = item.folderId,
            folders = folders,
            onMove = onMove,
        )
    }
}

@Composable
private fun FolderPickerButton(
    currentFolderId: String?,
    folders: List<GridElement>,
    onMove: (String?) -> Unit,
) {
    var open by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { open = true }) {
            Icon(
                painter = painterResource(R.drawable.ic_folder),
                contentDescription = "Move to folder",
                modifier = Modifier.size(22.dp),
                tint = if (currentFolderId != null) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text("All apps (no folder)") },
                trailingIcon = { if (currentFolderId == null) SelectedDot() },
                onClick = { open = false; onMove(null) },
            )
            folders.forEach { folder ->
                DropdownMenuItem(
                    text = { Text(folder.name.ifBlank { "Unnamed folder" }) },
                    trailingIcon = { if (currentFolderId == folder.id) SelectedDot() },
                    onClick = { open = false; onMove(folder.id) },
                )
            }
            if (folders.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("No folders yet") },
                    enabled = false,
                    onClick = {},
                )
            }
        }
    }
}

@Composable
private fun SelectedDot() {
    Box(
        Modifier
            .size(8.dp)
            .background(MaterialTheme.colorScheme.primary, CircleShape)
    )
}

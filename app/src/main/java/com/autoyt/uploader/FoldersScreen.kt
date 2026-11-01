package com.autoyt.uploader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun FoldersScreen(dashboard: Dashboard, settings: Settings, onChanged: () -> Unit) {
    val context = LocalContext.current
    val colors = AutoTheme.colors
    var chosen by remember { mutableStateOf(settings.albums) }
    var paused by remember { mutableStateOf(settings.pausedAlbums) }
    var moveAfter by remember { mutableStateOf(settings.moveAfterUpload) }
    var menuOpen by remember { mutableStateOf(false) }
    var removing by remember { mutableStateOf<Album?>(null) }
    val albums by produceState(emptyList<Album>(), menuOpen) {
        if (menuOpen) value = withContext(Dispatchers.IO) { VideoLibrary(context).albums() }
    }
    val updateChosen = { updated: List<Album> ->
        chosen = updated
        settings.albums = updated
        onChanged()
    }
    val updatePaused = { updated: Set<String> ->
        paused = updated
        settings.pausedAlbums = updated
        onChanged()
    }

    ScreenColumn {
        PageHeading("Your sources", "Where to look.", "Turn folders on or off. Earlier folders upload first.")

        val active = chosen.count { it.id !in paused }
        AppCard(background = colors.mint) {
            Text(
                if (active == 1) "1 folder active" else "$active folders active",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text("Every video in these folders is uploaded.", color = colors.muted, style = MaterialTheme.typography.bodySmall)
        }

        Column(Modifier.padding(vertical = 10.dp)) {
            if (chosen.isEmpty()) {
                Text("No folders added yet.", color = colors.muted, style = MaterialTheme.typography.bodyMedium)
            }
            chosen.forEachIndexed { index, album ->
                FolderRow(
                    album = album,
                    waiting = dashboard.waitingByFolder[album.id],
                    isPaused = album.id in paused,
                    canMoveUp = index > 0,
                    canMoveDown = index < chosen.lastIndex,
                    onToggle = { active -> updatePaused(if (active) paused - album.id else paused + album.id) },
                    onMove = { delta -> updateChosen(UploadRules.reordered(chosen, index, delta)) },
                    onRemove = { removing = album },
                )
            }
        }

        Box(Modifier.fillMaxWidth()) {
            SecondaryButton("Add a folder", enabled = dashboard.hasStorage, onClick = { menuOpen = true })
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                val available = albums.filter { it !in chosen }
                if (available.isEmpty()) {
                    DropdownMenuItem(text = { Text("No more folders with videos") }, enabled = false, onClick = {})
                }
                available.forEach { album ->
                    DropdownMenuItem(text = { Text(album.name) }, modifier = Modifier.focusRing(), onClick = {
                        updateChosen(chosen + album)
                        menuOpen = false
                    })
                }
            }
        }
        if (!dashboard.hasStorage) {
            Text(
                "Allow access to videos on the Overview tab first.",
                Modifier.padding(top = 6.dp),
                color = colors.muted,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Text("Upload rule", Modifier.padding(top = 20.dp, bottom = 8.dp).semantics { heading() }, style = MaterialTheme.typography.titleMedium)
        AppCard {
            SwitchRow("After upload", afterUploadDetail(moveAfter), moveAfter) {
                moveAfter = it
                settings.moveAfterUpload = it
            }
        }
    }

    removing?.let { album ->
        AlertDialog(
            onDismissRequest = { removing = null },
            title = { Text("Remove ${album.name}?") },
            text = { Text("New videos here stop uploading. Nothing is deleted from your phone or YouTube.") },
            confirmButton = {
                TextButton(modifier = Modifier.focusRing(), onClick = {
                    removing = null
                    paused = paused - album.id
                    settings.pausedAlbums = paused
                    updateChosen(chosen - album)
                }) { Text("Remove", color = colors.rustDark) }
            },
            dismissButton = {
                TextButton(modifier = Modifier.focusRing(), onClick = { removing = null }) { Text("Keep", color = colors.ink) }
            },
            containerColor = colors.cream,
        )
    }
}

fun afterUploadDetail(move: Boolean): String =
    if (move) "Move completed files to Uploaded" else "Leave completed files in their folder"

@Composable
private fun FolderRow(
    album: Album,
    waiting: Int?,
    isPaused: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onToggle: (Boolean) -> Unit,
    onMove: (Int) -> Unit,
    onRemove: () -> Unit,
) {
    val colors = AutoTheme.colors
    var menu by remember { mutableStateOf(false) }
    val state = if (isPaused) "paused" else "active"
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(painterResource(R.drawable.ic_folder), contentDescription = null, Modifier.size(24.dp), tint = colors.rust)
        Column(Modifier.weight(1f)) {
            Text(album.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Text(
                if (waiting == null) state else "$waiting waiting · $state",
                color = colors.muted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        AppSwitch(
            !isPaused,
            onToggle,
            Modifier.focusRing(RoundedCornerShape(24.dp)).semantics { contentDescription = "Upload from ${album.name}" },
        )
        Box {
            IconButton(modifier = Modifier.focusRing(CircleShape), onClick = { menu = true }) {
                Icon(painterResource(R.drawable.ic_more), contentDescription = "More for ${album.name}", tint = colors.muted)
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("Move up") }, modifier = Modifier.focusRing(), enabled = canMoveUp, onClick = { menu = false; onMove(-1) })
                DropdownMenuItem(text = { Text("Move down") }, modifier = Modifier.focusRing(), enabled = canMoveDown, onClick = { menu = false; onMove(1) })
                DropdownMenuItem(text = { Text("Remove") }, modifier = Modifier.focusRing(), onClick = { menu = false; onRemove() })
            }
        }
    }
}

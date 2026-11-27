package com.noirmusic.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.noirmusic.data.model.Track
import com.noirmusic.data.playlist.Playlist
import com.noirmusic.ui.sakuga.SakugaButton
import com.noirmusic.ui.sakuga.SakugaButtonStyle
import com.noirmusic.ui.sakuga.SakugaDialog
import com.noirmusic.ui.sakuga.SakugaPanel
import com.noirmusic.ui.sakuga.sakugaTrackTint
import com.noirmusic.ui.theme.SakugaTheme

@Composable
fun AddToPlaylistDialog(
    track: Track,
    playlists: List<Playlist>,
    onAddToPlaylist: (playlistId: String) -> Unit,
    onCreateAndAdd: (name: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val tokens = SakugaTheme.tokens
    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    if (showCreateDialog) {
        SakugaDialog(onDismissRequest = { showCreateDialog = false }, horizontalPadding = 20.dp) {
            SakugaPanel(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        shadowColor = tokens.accent,
                        cornerRadius = 16.dp,
                        contentPadding = PaddingValues(20.dp),
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Playlist Baru",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = newPlaylistName,
                                onValueChange = { newPlaylistName = it },
                                label = { Text("Nama Playlist") },
                                placeholder = { Text("Contoh: Favorit Malam") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(18.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                SakugaButton(
                                    onClick = { showCreateDialog = false },
                                    style = SakugaButtonStyle.SECONDARY,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("Batal", fontWeight = FontWeight.Bold)
                                }
                                SakugaButton(
                                    onClick = {
                                        val name = newPlaylistName.trim()
                                        if (name.isNotBlank()) {
                                            onCreateAndAdd(name)
                                            showCreateDialog = false
                                            onDismiss()
                                        }
                                    },
                                    enabled = newPlaylistName.isNotBlank(),
                                    style = SakugaButtonStyle.ACCENT,
                                    sfxText = "ドン!",
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("Buat & Tambah", fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
        }
    }

    SakugaDialog(onDismissRequest = onDismiss, horizontalPadding = 18.dp) {
        SakugaPanel(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    shadowColor = tokens.celShadow,
                    cornerRadius = 16.dp,
                    contentPadding = PaddingValues(18.dp),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(tokens.cyanFill)
                                    .border(2.dp, tokens.inkBorder, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                                    contentDescription = null,
                                    tint = tokens.onCyanFill,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Tambah ke Playlist",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = track.title,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        SakugaButton(
                            onClick = { showCreateDialog = true },
                            style = SakugaButtonStyle.CYAN,
                            sfxText = "カチッ",
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Buat Playlist Baru", fontWeight = FontWeight.Black)
                        }

                        Spacer(Modifier.height(12.dp))

                        if (playlists.isEmpty()) {
                            Text(
                                text = "Belum ada playlist dibuat.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                items(playlists, key = { it.id }) { playlist ->
                                    val alreadyIn = playlist.trackIds.contains(track.id)
                                    val (tintBg, tintFg) = sakugaTrackTint(playlist.id.hashCode().toLong())
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 54.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surfaceContainer)
                                            .border(2.dp, tokens.inkBorder, RoundedCornerShape(12.dp))
                                            .clickable {
                                                onAddToPlaylist(playlist.id)
                                                onDismiss()
                                            }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(tintBg)
                                                .border(1.5.dp, tokens.inkBorder, RoundedCornerShape(8.dp)),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                                                contentDescription = null,
                                                tint = tintFg,
                                                modifier = Modifier.size(20.dp),
                                            )
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = playlist.name,
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Text(
                                                text = "${playlist.trackIds.size} lagu",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        if (alreadyIn) {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = "Sudah ditambahkan",
                                                tint = tokens.accentText,
                                                modifier = Modifier.size(20.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                        SakugaButton(
                            onClick = onDismiss,
                            style = SakugaButtonStyle.SECONDARY,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Tutup", fontWeight = FontWeight.Bold)
                        }
                    }
                }
    }
}

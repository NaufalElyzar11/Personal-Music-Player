package com.noirmusic.ui.player

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Timer
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.noirmusic.playback.SleepTimerState
import com.noirmusic.ui.sakuga.SakugaButton
import com.noirmusic.ui.sakuga.SakugaButtonStyle
import com.noirmusic.ui.sakuga.SakugaDialog
import com.noirmusic.ui.sakuga.SakugaPanel
import com.noirmusic.ui.theme.SakugaTheme
import java.util.Locale

@Composable
fun SleepTimerDialog(
    state: SleepTimerState,
    onStartTimer: (Int) -> Unit,
    onStartEndOfTrack: () -> Unit,
    onAddMinutes: (Int) -> Unit,
    onCancelTimer: () -> Unit,
    onDismiss: () -> Unit,
) {
    val tokens = SakugaTheme.tokens
    var showCustomInput by remember { mutableStateOf(false) }
    var customMinutesText by remember { mutableStateOf("") }

    SakugaDialog(onDismissRequest = onDismiss, horizontalPadding = 18.dp) {
        SakugaPanel(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    shadowColor = if (state is SleepTimerState.Active) tokens.accent else tokens.celShadow,
                    cornerRadius = 18.dp,
                    contentPadding = PaddingValues(18.dp),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(tokens.violetFill)
                                    .border(2.dp, tokens.inkBorder, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Bedtime,
                                    contentDescription = null,
                                    tint = tokens.onVioletFill,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Text(
                                text = "Timer Tidur",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        when (state) {
                            is SleepTimerState.Active -> {
                                val statusText = if (state.isEndOfTrack) {
                                    "Musik akan berhenti setelah lagu ini selesai"
                                } else {
                                    val min = state.remainingSec / 60
                                    val sec = state.remainingSec % 60
                                    String.format(Locale.getDefault(), "%02d:%02d tersisa", min, sec)
                                }

                                SakugaPanel(
                                    modifier = Modifier.fillMaxWidth(),
                                    containerColor = tokens.cyanFill,
                                    shadowColor = tokens.accent,
                                    cornerRadius = 14.dp,
                                    shadowOffsetX = 3.dp,
                                    shadowOffsetY = 4.dp,
                                    contentPadding = PaddingValues(14.dp),
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = "TIMER AKTIF",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Black,
                                            fontStyle = FontStyle.Italic,
                                            color = tokens.onCyanFill,
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = if (state.isEndOfTrack) "Selesai Lagu Ini" else statusText,
                                            style = MaterialTheme.typography.headlineMedium,
                                            color = tokens.onCyanFill,
                                        )
                                        if (!state.isEndOfTrack) {
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                text = "Pemutaran dijeda otomatis saat waktu habis",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = tokens.onCyanFill.copy(alpha = 0.85f),
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(16.dp))

                                if (!state.isEndOfTrack) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        SakugaButton(
                                            onClick = { onAddMinutes(5) },
                                            style = SakugaButtonStyle.SECONDARY,
                                            sfxText = "カチッ",
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Text("+5 Menit", fontWeight = FontWeight.Black)
                                        }
                                        SakugaButton(
                                            onClick = { onAddMinutes(15) },
                                            style = SakugaButtonStyle.SECONDARY,
                                            sfxText = "カチッ",
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Text("+15 Menit", fontWeight = FontWeight.Black)
                                        }
                                    }
                                    Spacer(Modifier.height(12.dp))
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    SakugaButton(
                                        onClick = onDismiss,
                                        style = SakugaButtonStyle.SECONDARY,
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Text("Tutup", fontWeight = FontWeight.Bold)
                                    }
                                    SakugaButton(
                                        onClick = {
                                            onCancelTimer()
                                            onDismiss()
                                        },
                                        style = SakugaButtonStyle.DANGER,
                                        sfxText = "ズザッ",
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Text("Matikan", fontWeight = FontWeight.Black)
                                    }
                                }
                            }

                            SleepTimerState.Inactive -> {
                                if (showCustomInput) {
                                    OutlinedTextField(
                                        value = customMinutesText,
                                        onValueChange = { if (it.all { ch -> ch.isDigit() }) customMinutesText = it },
                                        label = { Text("Menit") },
                                        placeholder = { Text("Contoh: 20") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    Spacer(Modifier.height(14.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        SakugaButton(
                                            onClick = { showCustomInput = false },
                                            style = SakugaButtonStyle.SECONDARY,
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Text("Batal", fontWeight = FontWeight.Bold)
                                        }
                                        SakugaButton(
                                            onClick = {
                                                val mins = customMinutesText.toIntOrNull() ?: 0
                                                if (mins > 0) {
                                                    onStartTimer(mins)
                                                    onDismiss()
                                                }
                                            },
                                            enabled = (customMinutesText.toIntOrNull() ?: 0) > 0,
                                            style = SakugaButtonStyle.ACCENT,
                                            sfxText = "ドン!",
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Text("Mulai", fontWeight = FontWeight.Black)
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "PILIHAN CEPAT",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        listOf(15, 30, 45, 60).forEachIndexed { idx, mins ->
                                            SakugaButton(
                                                onClick = {
                                                    onStartTimer(mins)
                                                    onDismiss()
                                                },
                                                style = if (idx % 2 == 0) SakugaButtonStyle.CYAN else SakugaButtonStyle.PRIMARY,
                                                cornerRadius = 10.dp,
                                                sfxText = "ドン!",
                                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                                                modifier = Modifier.weight(1f),
                                            ) {
                                                Text(
                                                    text = "${mins}m",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.Black,
                                                )
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(14.dp))

                                    TimerOptionRow(label = "Selesai lagu ini") {
                                        onStartEndOfTrack()
                                        onDismiss()
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    TimerOptionRow(label = "Kustom menit...") {
                                        showCustomInput = true
                                    }

                                    Spacer(Modifier.height(16.dp))
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
                }
    }
}

@Composable
private fun TimerOptionRow(
    label: String,
    onClick: () -> Unit,
) {
    val tokens = SakugaTheme.tokens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(2.dp, tokens.inkBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Timer,
            contentDescription = null,
            tint = tokens.accentText,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

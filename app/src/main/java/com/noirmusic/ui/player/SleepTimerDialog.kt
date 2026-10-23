package com.noirmusic.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.noirmusic.playback.SleepTimerState
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
    var showCustomInput by remember { mutableStateOf(false) }
    var customMinutesText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Bedtime,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Timer Tidur",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
            ) {
                when (state) {
                    is SleepTimerState.Active -> {
                        val statusText = if (state.isEndOfTrack) {
                            "Musik akan berhenti setelah lagu ini selesai"
                        } else {
                            val min = state.remainingSec / 60
                            val sec = state.remainingSec % 60
                            String.format(Locale.getDefault(), "%02d:%02d tersisa", min, sec)
                        }

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = if (state.isEndOfTrack) "Selesai Lagu Ini" else statusText,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                if (!state.isEndOfTrack) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "Musik akan otomatis berhenti",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        if (!state.isEndOfTrack) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                OutlinedButton(
                                    onClick = { onAddMinutes(5) },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("+5 Menit")
                                }
                                OutlinedButton(
                                    onClick = { onAddMinutes(15) },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("+15 Menit")
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                        }

                        Button(
                            onClick = {
                                onCancelTimer()
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Matikan Timer")
                        }
                    }

                    SleepTimerState.Inactive -> {
                        if (showCustomInput) {
                            OutlinedTextField(
                                value = customMinutesText,
                                onValueChange = { if (it.all { char -> char.isDigit() }) customMinutesText = it },
                                label = { Text("Menit") },
                                placeholder = { Text("Contoh: 20") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                TextButton(onClick = { showCustomInput = false }) {
                                    Text("Batal")
                                }
                                Spacer(Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        val mins = customMinutesText.toIntOrNull() ?: 0
                                        if (mins > 0) {
                                            onStartTimer(mins)
                                            onDismiss()
                                        }
                                    },
                                    enabled = (customMinutesText.toIntOrNull() ?: 0) > 0,
                                ) {
                                    Text("Mulai")
                                }
                            }
                        } else {
                            Text(
                                text = "Pilih durasi sebelum musik otomatis berhenti:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(12.dp))

                            val options = listOf(
                                "15 Menit" to 15,
                                "30 Menit" to 30,
                                "45 Menit" to 45,
                                "60 Menit" to 60,
                            )

                            options.forEach { (label, minutes) ->
                                TimerOptionRow(label = label) {
                                    onStartTimer(minutes)
                                    onDismiss()
                                }
                            }

                            TimerOptionRow(label = "Selesai lagu ini") {
                                onStartEndOfTrack()
                                onDismiss()
                            }

                            TimerOptionRow(label = "Kustom...") {
                                showCustomInput = true
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (state is SleepTimerState.Inactive && !showCustomInput) {
                TextButton(onClick = onDismiss) {
                    Text("Tutup")
                }
            }
        },
    )
}

@Composable
private fun TimerOptionRow(
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Timer,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

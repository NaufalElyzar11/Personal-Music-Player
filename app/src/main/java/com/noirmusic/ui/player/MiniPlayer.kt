package com.noirmusic.ui.player

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import com.noirmusic.ui.sakuga.LocalSakugaFx
import com.noirmusic.ui.sakuga.SakugaIconButton
import com.noirmusic.ui.sakuga.SakugaIconVfx
import com.noirmusic.ui.sakuga.sakugaCelShadow
import com.noirmusic.ui.sakuga.trackRootCenter
import com.noirmusic.ui.theme.SakugaTheme
import kotlinx.coroutines.flow.StateFlow

@Composable
fun MiniPlayer(
    state: PlaybackUiState,
    positionMsFlow: StateFlow<Long>,
    onClick: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val track = state.nowPlaying ?: return
    val tokens = SakugaTheme.tokens
    val fx = LocalSakugaFx.current
    val positionMs by positionMsFlow.collectAsStateWithLifecycle()
    var dockCenter by remember { mutableStateOf(Offset.Zero) }

    val subtitle = state.errorMessage?.let { "Gagal memutar: $it" } ?: track.displayArtist
    val subtitleColor = if (state.errorMessage != null) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val badgeTilt = if (state.isPlaying) {
        val infinite = rememberInfiniteTransition(label = "miniTilt")
        val tilt by infinite.animateFloat(
            initialValue = -5f,
            targetValue = 5f,
            animationSpec = infiniteRepeatable(tween(520, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "tilt",
        )
        tilt
    } else {
        -3f
    }

    val progress = if (state.durationMs > 0L) {
        (positionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .trackRootCenter { dockCenter = it }
                .sakugaCelShadow(
                    color = if (state.isPlaying) tokens.accent else tokens.celShadow,
                    cornerRadius = 16.dp,
                    offsetX = 4.dp,
                    offsetY = 5.dp,
                )
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .border(2.5.dp, tokens.inkBorder, RoundedCornerShape(16.dp)),
        ) {
            // Bold cel top progress strip
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                if (progress > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(6.dp)
                            .background(tokens.accent),
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 66.dp)
                    .clickable(
                        role = Role.Button,
                        onClickLabel = "Buka Pemutar Musik",
                        onClick = {
                            if (dockCenter != Offset.Zero) {
                                fx.emitSlideSkid(dockCenter)
                            }
                            onClick()
                        },
                    )
                    .padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .graphicsLayer { rotationZ = badgeTilt }
                        .sakugaCelShadow(tokens.celShadow, cornerRadius = 10.dp, offsetX = 2.dp, offsetY = 2.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(tokens.cyanFill)
                        .border(2.dp, tokens.inkBorder, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = tokens.onCyanFill,
                        modifier = Modifier.size(22.dp),
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .semantics(mergeDescendants = true) {
                            contentDescription = "Memutar: ${track.title}, ${track.displayArtist}"
                        },
                ) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(1.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = subtitleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // Play/Pause cel trigger
                SakugaIconButton(
                    onClick = onTogglePlayPause,
                    vfx = SakugaIconVfx.PLAY_BLAST,
                    contentDescription = if (state.isPlaying) "Jeda" else "Putar",
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (state.isPlaying) tokens.accent else tokens.violetFill)
                            .border(2.dp, tokens.inkBorder, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = if (state.isPlaying) tokens.onAccent else tokens.onVioletFill,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }

                SakugaIconButton(
                    onClick = onNext,
                    enabled = state.hasNext || state.repeatMode != Player.REPEAT_MODE_OFF,
                    vfx = SakugaIconVfx.SKIP_NEXT,
                    contentDescription = "Lagu selanjutnya",
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
        }
    }
}

package com.noirmusic.ui.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.noirmusic.ui.sakuga.LocalSakugaFx
import com.noirmusic.ui.sakuga.sakugaCelShadow
import com.noirmusic.ui.sakuga.trackRootCenter
import com.noirmusic.ui.theme.SakugaTheme
import kotlinx.coroutines.launch

@Composable
fun Artwork(
    bitmap: ImageBitmap?,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
) {
    val tokens = SakugaTheme.tokens
    val fx = LocalSakugaFx.current
    val scope = rememberCoroutineScope()
    val tapScale = remember { Animatable(1f) }
    var center by remember { mutableStateOf(Offset.Zero) }

    // Lively beat groove ONLY while music is actively playing (still when paused).
    val beatScale = if (isPlaying) {
        val infinite = rememberInfiniteTransition(label = "coverBeat")
        val s by infinite.animateFloat(
            initialValue = 0.992f,
            targetValue = 1.022f,
            animationSpec = infiniteRepeatable(tween(460, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "beat",
        )
        s
    } else {
        1f
    }

    val beatTilt = if (isPlaying) {
        val infinite = rememberInfiniteTransition(label = "coverTilt")
        val deg by infinite.animateFloat(
            initialValue = -1.4f,
            targetValue = 1.4f,
            animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "tilt",
        )
        deg
    } else {
        -1.5f
    }

    Column(
        modifier = modifier.padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .weight(1f, fill = false)
                .aspectRatio(1f)
                .trackRootCenter { center = it }
                .graphicsLayer {
                    val s = beatScale * tapScale.value
                    scaleX = s
                    scaleY = s
                    rotationZ = beatTilt
                }
                .sakugaCelShadow(
                    color = tokens.accent,
                    cornerRadius = 20.dp,
                    offsetX = 7.dp,
                    offsetY = 9.dp,
                )
                .sakugaCelShadow(
                    color = tokens.celShadow,
                    cornerRadius = 20.dp,
                    offsetX = 3.5.dp,
                    offsetY = 4.5.dp,
                )
                .clip(RoundedCornerShape(20.dp))
                .background(tokens.violetFill)
                .border(3.dp, tokens.inkBorder, RoundedCornerShape(20.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        if (center != Offset.Zero) {
                            fx.spawnBurst(center, radiusDp = 64f, spikes = 14)
                            fx.spawnKira(Offset(center.x - 90f, center.y - 90f), radiusDp = 14f, useCyan = true)
                            fx.spawnKira(Offset(center.x + 95f, center.y - 70f), radiusDp = 12f)
                            fx.spawnSfx(Offset(center.x, center.y - 40f), text = "ドン!", sizeDp = 24f, angleDeg = -10f)
                        }
                        scope.launch {
                            tapScale.snapTo(0.91f)
                            tapScale.animateTo(1f, spring(dampingRatio = 0.36f, stiffness = 460f))
                        }
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = "Album artwork",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                // Stylized cel manga vinyl disc placeholder when embedded cover art is missing
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val step = 14.dp.toPx()
                        var y = step
                        while (y < size.height) {
                            var x = step
                            while (x < size.width) {
                                drawCircle(
                                    color = tokens.vfxFill.copy(alpha = 0.22f),
                                    radius = 2.4.dp.toPx(),
                                    center = Offset(x, y),
                                )
                                x += step
                            }
                            y += step
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(124.dp)
                            .sakugaCelShadow(tokens.inkBorder, cornerRadius = 62.dp, offsetX = 4.dp, offsetY = 5.dp)
                            .clip(CircleShape)
                            .background(tokens.cyanFill)
                            .border(3.dp, tokens.inkBorder, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MusicNote,
                            contentDescription = null,
                            tint = tokens.onCyanFill,
                            modifier = Modifier.size(64.dp),
                        )
                    }
                }
            }
        }
    }
}

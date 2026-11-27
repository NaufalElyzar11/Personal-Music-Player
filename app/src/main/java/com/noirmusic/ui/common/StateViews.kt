package com.noirmusic.ui.common

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.noirmusic.ui.sakuga.SakugaButton
import com.noirmusic.ui.sakuga.SakugaButtonStyle
import com.noirmusic.ui.sakuga.SakugaPanel
import com.noirmusic.ui.sakuga.sakugaCelShadow
import com.noirmusic.ui.theme.SakugaTheme
import kotlin.math.sin

@Composable
fun CenteredMessage(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val tokens = SakugaTheme.tokens
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        SakugaPanel(
            modifier = Modifier
                .size(78.dp)
                .graphicsLayer { rotationZ = -4f },
            containerColor = tokens.cyanFill,
            borderColor = tokens.inkBorder,
            shadowColor = tokens.accent,
            cornerRadius = 18.dp,
            shadowOffsetX = 5.dp,
            shadowOffsetY = 6.dp,
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tokens.onCyanFill,
                    modifier = Modifier.size(38.dp),
                )
            }
        }
        Spacer(Modifier.height(22.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 320.dp),
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(24.dp))
            SakugaButton(
                onClick = onAction,
                style = SakugaButtonStyle.ACCENT,
                sfxText = "ドン!",
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}

/** Anime storm-surge loading view inspired by `<sakuga-progress>` (riding cloud + pebbles + ゴゴゴ). */
@Composable
fun LoadingView(text: String, modifier: Modifier = Modifier) {
    val tokens = SakugaTheme.tokens
    val infinite = rememberInfiniteTransition(label = "sakugaLoad")
    val progress by infinite.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "surge",
    )
    val bob by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 6.283f,
        animationSpec = infiniteRepeatable(tween(900)),
        label = "pebble",
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "ゴゴゴ...",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic,
            color = tokens.accentText,
        )
        Spacer(Modifier.height(14.dp))

        // Cel-shaded surge progress bar with riding storm cloud and bouncing pebbles
        Canvas(
            modifier = Modifier
                .widthIn(max = 260.dp)
                .fillMaxWidth()
                .height(48.dp),
        ) {
            val barY = size.height * 0.62f
            val barH = 14.dp.toPx()
            val barW = size.width
            val r = barH / 2f

            // Cel shadow
            drawRoundRect(
                color = tokens.celShadow,
                topLeft = Offset(3.dp.toPx(), barY - r + 4.dp.toPx()),
                size = Size(barW, barH),
                cornerRadius = CornerRadius(r, r),
            )
            // Track fill
            drawRoundRect(
                color = tokens.vfxFill,
                topLeft = Offset(0f, barY - r),
                size = Size(barW, barH),
                cornerRadius = CornerRadius(r, r),
            )
            // Active surge fill
            val fillW = (barW * progress).coerceAtLeast(barH)
            drawRoundRect(
                color = tokens.vfxAccent,
                topLeft = Offset(0f, barY - r),
                size = Size(fillW, barH),
                cornerRadius = CornerRadius(r, r),
            )
            // Track ink outline
            drawRoundRect(
                color = tokens.inkBorder,
                topLeft = Offset(0f, barY - r),
                size = Size(barW, barH),
                cornerRadius = CornerRadius(r, r),
                style = Stroke(width = 2.4.dp.toPx()),
            )

            // Riding storm cloud at leading edge
            val headX = fillW.coerceIn(14.dp.toPx(), barW - 14.dp.toPx())
            val puffY = barY - 11.dp.toPx() + sin(bob) * 2.5.dp.toPx()
            val lobes = listOf(
                Triple(headX - 8.dp.toPx(), puffY + 2.dp.toPx(), 7.dp.toPx()),
                Triple(headX, puffY - 2.dp.toPx(), 9.dp.toPx()),
                Triple(headX + 8.dp.toPx(), puffY + 2.dp.toPx(), 6.5.dp.toPx()),
            )
            lobes.forEach { (lx, ly, lr) ->
                drawCircle(tokens.vfxLine, radius = lr + 2.dp.toPx(), center = Offset(lx, ly))
            }
            lobes.forEach { (lx, ly, lr) ->
                drawCircle(tokens.vfxShade, radius = lr, center = Offset(lx, ly))
                drawCircle(tokens.vfxFill, radius = lr * 0.78f, center = Offset(lx - 1.5.dp.toPx(), ly - 1.8.dp.toPx()))
            }

            // Lifting cel pebbles
            for (i in 0..2) {
                val px = (headX - (18 + i * 16).dp.toPx()).coerceAtLeast(8.dp.toPx())
                val py = barY - 12.dp.toPx() - abs(sin(bob + i * 1.4f)) * 9.dp.toPx()
                val pr = (3.2f - i * 0.5f).dp.toPx()
                drawCircle(tokens.inkBorder, radius = pr + 1.2.dp.toPx(), center = Offset(px, py))
                drawCircle(tokens.vfxCyan, radius = pr, center = Offset(px, py))
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private fun abs(v: Float): Float = if (v < 0f) -v else v

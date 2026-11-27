package com.noirmusic.ui.sakuga

import android.graphics.Paint
import android.graphics.Typeface
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotateRad
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import com.noirmusic.ui.theme.SakugaTheme
import com.noirmusic.ui.theme.SakugaTokens
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

private const val TAU = (PI * 2.0).toFloat()
private const val MAX_LIVE_PARTICLES = 96

/** Pure animation curves and spring integrator ported from sakuga-vfx for JVM testing and Compose VFX. */
object SakugaMath {
    fun clamp01(v: Float): Float = v.coerceIn(0f, 1f)
    fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t
    fun outCubic(t: Float): Float {
        val u = 1f - clamp01(t)
        return 1f - u * u * u
    }
    fun inCubic(t: Float): Float {
        val u = clamp01(t)
        return u * u * u
    }
    fun outBack(t: Float): Float {
        val u = clamp01(t) - 1f
        return 1f + 2.70158f * u * u * u + 1.70158f * u * u
    }
}

/** Damped harmonic spring matching sakuga-vfx substepping (<= 8ms steps for stability). */
class SakugaSpring(
    var x: Float = 0f,
    val stiffness: Float = 320f,
    val damping: Float = 18f,
) {
    var v: Float = 0f
    var target: Float = x

    fun step(dtSec: Float): Float {
        if (dtSec <= 0f) return x
        val steps = ceil(dtSec / 0.008f).toInt().coerceIn(1, 16)
        val h = dtSec / steps
        repeat(steps) {
            val acc = -stiffness * (x - target) - damping * v
            v += acc * h
            x += v * h
        }
        return x
    }

    fun isSettled(epsilon: Float = 0.002f): Boolean =
        abs(x - target) < epsilon && abs(v) < epsilon
}

internal data class SakugaParticle(
    var elapsed: Float = 0f,
    val life: Float,
    val draw: DrawScope.(k: Float, t: Float, tokens: SakugaTokens, density: Float) -> Unit,
)

@Stable
class SakugaFxController {
    internal val particles = ArrayList<SakugaParticle>(64)
    internal var hasActiveParticles by mutableStateOf(false)
    internal var frameTick by mutableLongStateOf(0L)
    var reducedMotion: Boolean = false
    var densityScale: Float = 3f

    private fun dp(value: Float): Float = value * densityScale

    private fun rnd(max: Float): Float = Random.nextFloat() * max
    private fun rnd(min: Float, max: Float): Float = min + Random.nextFloat() * (max - min)

    internal fun add(
        life: Float,
        draw: DrawScope.(k: Float, t: Float, tokens: SakugaTokens, density: Float) -> Unit,
    ) {
        if (reducedMotion || life <= 0f) return
        if (particles.size >= MAX_LIVE_PARTICLES) {
            particles.removeAt(0)
        }
        particles.add(SakugaParticle(elapsed = 0f, life = life, draw = draw))
        hasActiveParticles = true
    }

    internal fun advance(dtSec: Float) {
        if (particles.isEmpty()) {
            hasActiveParticles = false
            return
        }
        val dt = dtSec.coerceIn(0f, 0.034f)
        var i = particles.lastIndex
        while (i >= 0) {
            val p = particles[i]
            p.elapsed += dt
            if (p.elapsed >= p.life) {
                particles.removeAt(i)
            }
            i--
        }
        hasActiveParticles = particles.isNotEmpty()
        frameTick++
    }

    // Cel-shaded multi-lobe smoke cloud with outline, clipped shadow, offset highlight, and cumulus arcs.
    private fun DrawScope.drawCelBlob(
        lobes: List<Triple<Float, Float, Float>>,
        strokeWidth: Float,
        tokens: SakugaTokens,
        lightX: Float = -1f,
        arcs: Boolean = false,
        deep: Float = 1f,
    ) {
        val unionPath = Path()
        for ((cx, cy, r) in lobes) {
            if (r > 0.6f) {
                unionPath.addOval(Rect(cx - r, cy - r, cx + r, cy + r))
            }
        }
        if (unionPath.isEmpty) return

        drawPath(
            path = unionPath,
            color = tokens.vfxLine,
            style = Stroke(width = strokeWidth * 2f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        clipPath(unionPath) {
            drawRect(color = tokens.vfxShade)
            val baseR = lobes.firstOrNull()?.third ?: 16f
            translate(left = lightX * baseR * 0.22f * deep, top = -baseR * 0.26f * deep) {
                drawPath(path = unionPath, color = tokens.vfxFill)
            }
            if (arcs) {
                for ((cx, cy, r) in lobes) {
                    if (r > dp(4f)) {
                        drawArc(
                            color = tokens.vfxLine,
                            startAngle = -155f,
                            sweepAngle = 120f,
                            useCenter = false,
                            topLeft = Offset(cx - r, cy - r),
                            size = Size(r * 2f, r * 2f),
                            style = Stroke(width = strokeWidth * 0.72f, cap = StrokeCap.Round),
                        )
                    }
                }
            }
        }
    }

    private fun DrawScope.drawCelShape(
        path: Path,
        strokeWidth: Float,
        offsetPx: Float,
        fill: Color,
        tokens: SakugaTokens,
    ) {
        drawPath(
            path = path,
            color = tokens.vfxLine,
            style = Stroke(width = strokeWidth * 2f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        clipPath(path) {
            drawRect(color = tokens.vfxShade)
            translate(left = -offsetPx * 0.7f, top = -offsetPx) {
                drawPath(path = path, color = fill)
            }
        }
    }

    fun spawnPuff(
        center: Offset,
        lobeCount: Int = 5,
        sizeDp: Float = 12f,
        vxDp: Float = 0f,
        vyDp: Float = -24f,
        life: Float = 0.72f,
        spread: Float = 1f,
    ) {
        data class Lobe(val angle: Float, val dist: Float, val radius: Float, val phase: Float, val speed: Float)
        val sizePx = dp(sizeDp)
        val vx = dp(vxDp)
        val vy = dp(vyDp)
        val lobes = buildList {
            add(Lobe(0f, 0f, sizePx, 0f, 1f))
            for (i in 0 until lobeCount) {
                add(
                    Lobe(
                        angle = (i.toFloat() / lobeCount) * TAU + rnd(0.6f),
                        dist = rnd(0.28f, 0.92f) * sizePx * spread,
                        radius = rnd(0.55f, 1.0f) * sizePx,
                        phase = rnd(TAU),
                        speed = rnd(0.65f, 1.35f),
                    ),
                )
            }
        }
        add(life) { k, t, tokens, d ->
            val grow = SakugaMath.outBack(SakugaMath.clamp01(t / 0.2f))
            val die = 1f - SakugaMath.inCubic(SakugaMath.clamp01((k - 0.55f) / 0.45f))
            val px = center.x + vx * SakugaMath.outCubic(k) * life
            val py = center.y + vy * SakugaMath.outCubic(k) * life
            val outK = SakugaMath.outCubic(k)
            val circles = lobes.map { l ->
                val a = l.angle + t * 0.9f * l.speed
                val dist = l.dist * (0.5f + outK * 0.9f)
                val r = l.radius * grow * die * (1f + sin(t * 9f + l.phase) * 0.05f)
                Triple(px + cos(a) * dist, py + sin(a) * dist - outK * 5f * d * l.speed, r)
            }
            drawCelBlob(
                lobes = circles,
                strokeWidth = 1.4f * d,
                tokens = tokens,
                lightX = 0.5f,
                arcs = sizeDp > 7f,
                deep = 1.4f,
            )
        }
    }

    fun spawnBurst(
        center: Offset,
        radiusDp: Float = 44f,
        spikes: Int = 13,
        life: Float = 0.36f,
        useCyanNeedles: Boolean = false,
    ) {
        val radiusPx = dp(radiusDp)
        val radii = FloatArray(spikes * 2) { i -> if (i % 2 == 1) rnd(0.34f, 0.5f) else rnd(0.78f, 1.18f) }
        val rot = rnd(TAU)
        data class Pin(val angle: Float, val length: Float, val width: Float)
        val pins = List(9) { i ->
            Pin(
                angle = (i.toFloat() / 9f) * TAU + rnd(0.45f),
                length = rnd(1.25f, 1.78f),
                width = rnd(0.032f, 0.058f),
            )
        }

        fun buildStar(r: Float, turn: Float, innerMult: Float): Path {
            val p = Path()
            for (i in radii.indices) {
                val a = rot + turn + (i.toFloat() / radii.size) * TAU
                val rr = r * if (i % 2 == 1) (radii[i] * innerMult).coerceAtMost(1.05f) else radii[i]
                val x = center.x + cos(a) * rr
                val y = center.y + sin(a) * rr
                if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
            }
            p.close()
            return p
        }

        add(life) { k, _, tokens, d ->
            val rNow = radiusPx * SakugaMath.outCubic(SakugaMath.clamp01(k / 0.28f))
            val hollow = 1f + SakugaMath.inCubic(SakugaMath.clamp01((k - 0.34f) / 0.66f)) * 1.25f
            val r0 = rNow * 0.58f * hollow.coerceAtMost(1.85f)
            val needlePath = Path()
            for (pin in pins) {
                val l = rNow * pin.length
                if (l <= r0) continue
                needlePath.moveTo(center.x + cos(pin.angle - pin.width) * r0, center.y + sin(pin.angle - pin.width) * r0)
                needlePath.lineTo(center.x + cos(pin.angle) * l, center.y + sin(pin.angle) * l)
                needlePath.lineTo(center.x + cos(pin.angle + pin.width) * r0, center.y + sin(pin.angle + pin.width) * r0)
                needlePath.close()
            }
            drawPath(needlePath, color = if (useCyanNeedles) tokens.vfxCyan else tokens.vfxAccent)

            val outer = buildStar(rNow * 1.15f, 0.19f, hollow)
            drawPath(outer, color = tokens.vfxShade)
            drawPath(outer, color = tokens.vfxLine, style = Stroke(width = 1.3f * d, join = StrokeJoin.Round))

            val inner = buildStar(rNow * 0.84f, 0f, hollow)
            drawCelShape(inner, strokeWidth = 1.4f * d, offsetPx = 3f * d, fill = tokens.vfxFill, tokens = tokens)
        }
    }

    fun spawnShards(
        center: Offset,
        count: Int = 8,
        speedDp: Float = 190f,
        sizeDp: Float = 5.5f,
        life: Float = 0.72f,
        dirRad: Float = (-PI / 2.0).toFloat(),
        arcRad: Float = TAU,
        gravDp: Float = 420f,
        accentTint: Boolean = false,
    ) {
        val basePts = listOf(
            Offset(-1.4f, 0.1f),
            Offset(-0.6f, -0.7f),
            Offset(1.2f, -0.45f),
            Offset(1.5f, 0.15f),
            Offset(0.4f, 0.75f),
        )
        repeat(count) {
            val angle = dirRad + rnd(-arcRad / 2f, arcRad / 2f)
            val speed = dp(speedDp) * rnd(0.55f, 1.2f)
            val sizePx = dp(sizeDp) * rnd(0.65f, 1.35f)
            val spin = rnd(-7.5f, 7.5f)
            val grav = dp(gravDp)
            val pts = basePts.map { Offset(it.x * rnd(0.8f, 1.2f), it.y * rnd(0.8f, 1.2f)) }
            val pLife = life * rnd(0.72f, 1.05f)
            add(pLife) { k, t, tokens, d ->
                val px = center.x + cos(angle) * speed * t
                val py = center.y + sin(angle) * speed * t + grav * t * t
                val sc = sizePx * (1f - SakugaMath.inCubic(SakugaMath.clamp01((k - 0.58f) / 0.42f)))
                if (sc < 1f) return@add
                translate(px, py) {
                    rotateRad(spin * t + angle, pivot = Offset.Zero) {
                        val path = Path()
                        pts.forEachIndexed { idx, pt ->
                            if (idx == 0) path.moveTo(pt.x * sc, pt.y * sc) else path.lineTo(pt.x * sc, pt.y * sc)
                        }
                        path.close()
                        val fill = if (accentTint) tokens.vfxAccent else tokens.vfxFill
                        drawCelShape(path, strokeWidth = 1.05f * d, offsetPx = sc * 0.45f, fill = fill, tokens = tokens)
                        if (sc > 2.5f * d) {
                            drawLine(
                                color = tokens.vfxLine,
                                start = Offset(pts[1].x * sc, pts[1].y * sc),
                                end = Offset(pts[4].x * sc, pts[4].y * sc),
                                strokeWidth = 0.9f * d,
                                cap = StrokeCap.Round,
                            )
                        }
                    }
                }
            }
        }
    }

    fun spawnRing(
        center: Offset,
        rxDp: Float = 42f,
        ryDp: Float = 16f,
        life: Float = 0.44f,
        widthDp: Float = 4.5f,
    ) {
        val rxPx = dp(rxDp)
        val ryPx = dp(ryDp)
        val wPx = dp(widthDp)
        add(life) { k, _, tokens, d ->
            val s = SakugaMath.outCubic(k)
            val lw = wPx * (1f - k)
            if (lw < 0.4f) return@add
            val rx = rxPx * (0.28f + s)
            val ry = ryPx * (0.28f + s)
            val rect = Rect(center.x - rx, center.y - ry, center.x + rx, center.y + ry)
            drawOval(
                color = tokens.vfxLine,
                topLeft = rect.topLeft,
                size = rect.size,
                style = Stroke(width = lw + 2.4f * d),
            )
            drawOval(
                color = tokens.vfxFill,
                topLeft = rect.topLeft,
                size = rect.size,
                style = Stroke(width = lw),
            )
        }
    }

    fun spawnSplash(
        center: Offset,
        widthDp: Float = 38f,
        heightDp: Float = 26f,
        life: Float = 0.65f,
    ) {
        val wPx = dp(widthDp)
        val hPx = dp(heightDp)
        val tongueCount = 7
        data class Tongue(val u: Float, val h: Float, val lean: Float, val delay: Float)
        val tongues = List(tongueCount) { i ->
            val u = i.toFloat() / (tongueCount - 1) - 0.5f
            Tongue(
                u = u,
                h = hPx * rnd(0.55f, 1.12f) * (1f - abs(u) * 0.7f),
                lean = rnd(-0.4f, 0.4f),
                delay = rnd(0f, 0.14f),
            )
        }
        add(life) { k, _, tokens, d ->
            val path = Path()
            val tips = ArrayList<Triple<Float, Float, Float>>(tongueCount)
            path.moveTo(center.x - wPx * 0.62f, center.y)
            for (s in tongues) {
                val kk = SakugaMath.clamp01((k - s.delay) / 0.82f)
                val up = sin(kk * PI.toFloat()) * s.h * (1f - kk * 0.25f)
                val bx = center.x + s.u * wPx
                val half = wPx / tongueCount * 0.62f
                val tx = bx + (s.u * 1.6f + s.lean) * up * 0.5f
                tips.add(Triple(tx, center.y - up, up))
                path.quadraticTo(bx - half, center.y - up * 0.25f, tx, center.y - up)
                path.quadraticTo(bx + half * 0.5f, center.y - up * 0.3f, bx + half, center.y)
            }
            path.lineTo(center.x + wPx * 0.62f, center.y)
            path.close()
            drawPath(path, color = tokens.vfxLine, style = Stroke(width = 2.6f * d, join = StrokeJoin.Round))
            drawPath(path, color = tokens.vfxShade)
            clipPath(path) {
                for ((tx, ty, up) in tips) {
                    if (up > 4f * d) {
                        val rx = up * 0.2f + 1.4f * d
                        val ry = up * 0.34f
                        drawOval(
                            color = tokens.vfxFill,
                            topLeft = Offset(tx - rx, ty + up * 0.2f - ry),
                            size = Size(rx * 2f, ry * 2f),
                        )
                    }
                }
            }
        }
        repeat(6) {
            val angle = (-PI / 2.0).toFloat() + rnd(-1.1f, 1.1f)
            val speed = dp(rnd(110f, 210f))
            val r0 = dp(rnd(2.2f, 4.2f))
            val grav = dp(520f)
            add(life * rnd(0.8f, 1.1f)) { k, t, tokens, d ->
                val px = center.x + cos(angle) * speed * t
                val py = center.y - 6f * d + sin(angle) * speed * t + grav * t * t
                val r = r0 * (1f - SakugaMath.inCubic(SakugaMath.clamp01((k - 0.6f) / 0.4f)))
                if (r < 0.8f) return@add
                val ang = atan2(sin(angle) * speed + 2f * grav * t, cos(angle) * speed)
                translate(px, py) {
                    rotateRad(ang, Offset.Zero) {
                        val drop = Path().apply {
                            moveTo(-r * 2.5f, 0f)
                            quadraticTo(-r * 0.5f, -r * 1.05f, r * 0.6f, -r * 0.85f)
                            quadraticTo(r * 1.4f, 0f, r * 0.6f, r * 0.85f)
                            quadraticTo(-r * 0.5f, r * 1.05f, -r * 2.5f, 0f)
                            close()
                        }
                        drawCelShape(drop, strokeWidth = 1f * d, offsetPx = r * 0.5f, fill = tokens.vfxFill, tokens = tokens)
                    }
                }
            }
        }
    }

    fun spawnSlash(
        center: Offset,
        lenDp: Float = 104f,
        angleRad: Float = -0.45f,
        life: Float = 0.4f,
        thickDp: Float = 14f,
        useCyan: Boolean = false,
    ) {
        val lenPx = dp(lenDp)
        val thickPx = dp(thickDp)
        val radius = lenPx * 0.9f
        val cx = center.x - cos(angleRad - PI.toFloat() / 2f) * radius * 0.86f
        val cy = center.y - sin(angleRad - PI.toFloat() / 2f) * radius * 0.86f
        val a0 = angleRad - PI.toFloat() / 2f - 0.62f
        val sweep = 1.24f

        add(life) { k, _, tokens, d ->
            val head = SakugaMath.outCubic(SakugaMath.clamp01(k / 0.42f))
            val tail = SakugaMath.inCubic(SakugaMath.clamp01((k - 0.22f) / 0.78f))
            if (head - tail < 0.015f) return@add

            fun crescent(thickness: Float): Path {
                val p = Path()
                val steps = 22
                for (i in 0..steps) {
                    val u = SakugaMath.lerp(tail, head, i.toFloat() / steps)
                    val a = a0 + sweep * u
                    val x = cx + cos(a) * radius
                    val y = cy + sin(a) * radius
                    if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
                }
                for (i in steps downTo 0) {
                    val q = i.toFloat() / steps
                    val u = SakugaMath.lerp(tail, head, q)
                    val a = a0 + sweep * u
                    val rr = radius - thickness * sin(q * PI.toFloat()).coerceAtLeast(0f).pow(0.7f)
                    p.lineTo(cx + cos(a) * rr, cy + sin(a) * rr)
                }
                p.close()
                return p
            }

            val outer = crescent(thickPx)
            val inner = crescent(thickPx * 0.42f)
            drawPath(outer, color = if (useCyan) tokens.vfxCyan else tokens.vfxAccent)
            drawPath(inner, color = tokens.vfxFill)
            drawPath(outer, color = tokens.vfxLine, style = Stroke(width = 1.3f * d, join = StrokeJoin.Round))
        }
    }

    fun spawnSpeedLines(
        center: Offset,
        count: Int = 8,
        dirRad: Float = PI.toFloat(),
        lenDp: Float = 52f,
        spreadDp: Float = 28f,
        life: Float = 0.3f,
    ) {
        val lenPx = dp(lenDp)
        val spreadPx = dp(spreadDp)
        repeat(count) {
            val offset = rnd(-spreadPx, spreadPx)
            val l = lenPx * rnd(0.55f, 1.3f)
            val delay = rnd(0f, 0.24f)
            val strokeDp = rnd(1.2f, 2.8f)
            add(life) { k, _, tokens, d ->
                val kk = SakugaMath.clamp01((k - delay) / (1f - delay))
                if (kk <= 0f) return@add
                val a = SakugaMath.outCubic(kk)
                val b = SakugaMath.inCubic(kk)
                val px = center.x - sin(dirRad) * offset
                val py = center.y + cos(dirRad) * offset
                val w = strokeDp * d * (1f - b)
                if (w < 0.4f) return@add
                drawLine(
                    color = tokens.vfxLine,
                    start = Offset(px + cos(dirRad) * l * b * 1.4f, py + sin(dirRad) * l * b * 1.4f),
                    end = Offset(px + cos(dirRad) * l * (0.2f + a * 1.2f), py + sin(dirRad) * l * (0.2f + a * 1.2f)),
                    strokeWidth = w,
                    cap = StrokeCap.Round,
                )
            }
        }
    }

    fun spawnTicks(
        center: Offset,
        count: Int = 4,
        radiusDp: Float = 18f,
        lenDp: Float = 11f,
        dirRad: Float = -0.9f,
        life: Float = 0.38f,
        fanRad: Float = 0.46f,
        useCyan: Boolean = false,
    ) {
        val rPx = dp(radiusDp)
        val lenPx = dp(lenDp)
        add(life) { k, _, tokens, d ->
            val a = SakugaMath.outCubic(SakugaMath.clamp01(k / 0.5f))
            val b = SakugaMath.inCubic(SakugaMath.clamp01((k - 0.4f) / 0.6f))
            val path = Path()
            for (i in 0 until count) {
                val an = dirRad + (i - (count - 1) / 2f) * fanRad
                val r0 = rPx + lenPx * b * 1.25f
                val r1 = rPx + lenPx * a * 1.25f
                if (r1 - r0 < 1f) continue
                val hw = 0.075f + (2.4f * d) / (r1 + 8f * d)
                path.moveTo(center.x + cos(an) * r0, center.y + sin(an) * r0)
                path.lineTo(center.x + cos(an - hw) * r1, center.y + sin(an - hw) * r1)
                path.lineTo(center.x + cos(an + hw) * r1, center.y + sin(an + hw) * r1)
                path.close()
            }
            drawPath(path, color = if (useCyan) tokens.vfxCyan else tokens.vfxAccent)
        }
    }

    fun spawnDonut(
        center: Offset,
        radiusDp: Float = 20f,
        vxDp: Float = 24f,
        vyDp: Float = -48f,
        life: Float = 0.85f,
        tiltRad: Float = -0.25f,
    ) {
        val rPx = dp(radiusDp)
        val vx = dp(vxDp)
        val vy = dp(vyDp)
        val phase = rnd(TAU)
        add(life) { k, t, tokens, d ->
            val s = SakugaMath.outBack(SakugaMath.clamp01(t / 0.22f)) *
                (1f - SakugaMath.inCubic(SakugaMath.clamp01((k - 0.58f) / 0.42f)))
            if (s < 0.04f) return@add
            val px = center.x + vx * SakugaMath.outCubic(k) * life
            val py = center.y + vy * SakugaMath.outCubic(k) * life
            val hole = 0.44f + 0.22f * k
            val hoop = Path().apply {
                fillType = PathFillType.EvenOdd
                for ((mult, bump) in listOf(1f to 0.09f, hole to 0.05f)) {
                    val steps = 32
                    for (i in 0..steps) {
                        val a = (i.toFloat() / steps) * TAU
                        val rr = rPx * s * mult * (1f + bump * sin(a * 9f + phase + t * 2f))
                        val ex = cos(a) * rr
                        val ey = sin(a) * rr * 0.52f
                        val x = px + ex * cos(tiltRad) - ey * sin(tiltRad)
                        val y = py + ex * sin(tiltRad) + ey * cos(tiltRad)
                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                    }
                    close()
                }
            }
            drawPath(hoop, color = tokens.vfxLine, style = Stroke(width = 2.6f * d, join = StrokeJoin.Round))
            clipPath(hoop) {
                drawRect(color = tokens.vfxShade)
                translate(left = rPx * 0.06f, top = -rPx * 0.2f) {
                    drawPath(hoop, color = tokens.vfxFill)
                }
            }
        }
    }

    fun spawnWob(
        center: Offset,
        dirSign: Float = 1f,
        radiusDp: Float = 16f,
        life: Float = 0.42f,
    ) {
        val rPx = dp(radiusDp)
        add(life) { k, _, tokens, d ->
            val lw = 2.2f * d * (1f - k)
            if (lw < 0.4f) return@add
            val startAngle = if (dirSign > 0f) -26f else 154f
            for (extra in listOf(0f, 7f * d)) {
                val rr = rPx + extra + SakugaMath.outCubic(k) * 8f * d
                val cx = center.x - dirSign * rPx
                drawArc(
                    color = tokens.vfxLine,
                    startAngle = startAngle,
                    sweepAngle = 52f,
                    useCenter = false,
                    topLeft = Offset(cx - rr, center.y - rr),
                    size = Size(rr * 2f, rr * 2f),
                    style = Stroke(width = lw, cap = StrokeCap.Round),
                )
            }
        }
    }

    fun spawnKira(
        center: Offset,
        radiusDp: Float = 11f,
        life: Float = 0.58f,
        useCyan: Boolean = false,
    ) {
        val rPx = dp(radiusDp)
        add(life) { k, t, tokens, d ->
            val s = rPx * sin(k * PI.toFloat()).coerceAtLeast(0f).pow(0.6f)
            if (s < 1f) return@add
            val q = s * 0.16f
            translate(center.x, center.y) {
                rotateRad(sin(t * 3.5f) * 0.24f, Offset.Zero) {
                    val p = Path().apply {
                        moveTo(0f, -s)
                        quadraticTo(q, -q, s * 0.72f, 0f)
                        quadraticTo(q, q, 0f, s)
                        quadraticTo(-q, q, -s * 0.72f, 0f)
                        quadraticTo(-q, -q, 0f, -s)
                        close()
                    }
                    drawPath(p, color = tokens.vfxFill)
                    drawPath(
                        p,
                        color = if (useCyan) tokens.vfxCyan else tokens.vfxAccent,
                        style = Stroke(width = 1.9f * d, join = StrokeJoin.Round),
                    )
                }
            }
        }
    }

    fun spawnImpactFlash(center: Offset, radiusDp: Float = 56f) {
        val rPx = dp(radiusDp)
        add(0.09f) { k, _, tokens, d ->
            val alpha = (1f - k).coerceIn(0f, 1f)
            drawCircle(
                color = tokens.vfxFill.copy(alpha = alpha * 0.85f),
                radius = rPx * (0.4f + k * 0.9f),
                center = center,
            )
            drawCircle(
                color = tokens.vfxAccent.copy(alpha = alpha),
                radius = rPx * (0.55f + k * 1.15f),
                center = center,
                style = Stroke(width = (4f * d * (1f - k)).coerceAtLeast(1f)),
            )
        }
    }

    /** Bold skewed manga sound-effect lettering (ドン!, カチッ, ズザッ, シュッ!, キュン!). */
    fun spawnSfx(
        center: Offset,
        text: String,
        sizeDp: Float = 22f,
        angleDeg: Float = -11f,
        life: Float = 0.68f,
        useCyan: Boolean = false,
        riseDp: Float = 26f,
    ) {
        val sizePx = dp(sizeDp)
        val risePx = dp(riseDp)
        val jitterSeed = rnd(100f)
        add(life) { k, t, tokens, d ->
            val pop = SakugaMath.outBack(SakugaMath.clamp01(t / 0.16f))
            val fade = 1f - SakugaMath.inCubic(SakugaMath.clamp01((k - 0.62f) / 0.38f))
            if (fade <= 0.02f) return@add
            val shake = if (k < 0.28f) sin(t * 90f + jitterSeed) * 2.2f * d * (1f - k / 0.28f) else 0f
            val marginX = 48f * d
            val marginTop = 58f * d
            val marginBottom = 26f * d
            val px = (center.x + shake).coerceIn(marginX, (size.width - marginX).coerceAtLeast(marginX))
            val py = (center.y - SakugaMath.outCubic(k) * risePx)
                .coerceIn(marginTop, (size.height - marginBottom).coerceAtLeast(marginTop))
            val alphaInt = (fade * 255f).toInt().coerceIn(0, 255)

            val canvas = drawContext.canvas.nativeCanvas
            canvas.save()
            canvas.translate(px, py)
            canvas.rotate(angleDeg)
            canvas.scale(pop, pop)

            val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = sizePx
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD_ITALIC)
                textSkewX = -0.22f
            }

            // Hard cel offset shadow behind lettering
            val shadowPaint = Paint(basePaint).apply {
                style = Paint.Style.FILL_AND_STROKE
                strokeWidth = sizePx * 0.22f
                strokeJoin = Paint.Join.ROUND
                color = tokens.celShadow.toArgb()
                alpha = alphaInt
            }
            canvas.drawText(text, 3.2f * d, 3.8f * d, shadowPaint)

            // Thick ink outline
            val outlinePaint = Paint(basePaint).apply {
                style = Paint.Style.STROKE
                strokeWidth = sizePx * 0.22f
                strokeJoin = Paint.Join.ROUND
                color = tokens.sfxStroke.toArgb()
                alpha = alphaInt
            }
            canvas.drawText(text, 0f, 0f, outlinePaint)

            // Crisp bright manga fill
            val fillColor = when {
                useCyan -> tokens.vfxCyan
                else -> tokens.sfxFill
            }
            val fillPaint = Paint(basePaint).apply {
                style = Paint.Style.FILL
                color = fillColor.toArgb()
                alpha = alphaInt
            }
            canvas.drawText(text, 0f, 0f, fillPaint)
            canvas.restore()
        }
    }

    // Semantic sakuga combos mapped to music player interactions.

    fun emitPrimaryConfirm(center: Offset, halfWDp: Float = 44f, halfHDp: Float = 20f, sfx: String = "ドン!") {
        spawnImpactFlash(center, radiusDp = 50f)
        spawnBurst(center, radiusDp = 44f, spikes = 13)
        spawnRing(center, rxDp = halfWDp * 0.9f, ryDp = halfHDp * 0.7f)
        val hw = dp(halfWDp)
        val hh = dp(halfHDp)
        spawnPuff(Offset(center.x - hw * 0.75f, center.y + hh * 0.35f), lobeCount = 4, sizeDp = 10f, vxDp = -28f, vyDp = -16f)
        spawnPuff(Offset(center.x + hw * 0.75f, center.y - hh * 0.35f), lobeCount = 4, sizeDp = 10f, vxDp = 28f, vyDp = -20f)
        spawnTicks(center, count = 5, radiusDp = 26f, lenDp = 12f, dirRad = -0.7f, fanRad = 0.55f)
        spawnSfx(Offset(center.x + dp(10f), center.y - dp(24f)), text = sfx, sizeDp = 22f, angleDeg = -10f)
    }

    fun emitSecondarySnap(center: Offset, sfx: String = "カチッ") {
        spawnTicks(center, count = 3, radiusDp = 18f, lenDp = 10f, dirRad = -0.85f, fanRad = 0.45f)
        spawnPuff(Offset(center.x - dp(10f), center.y + dp(6f)), lobeCount = 3, sizeDp = 7.5f, vxDp = -14f, vyDp = -12f, life = 0.46f)
        spawnWob(Offset(center.x + dp(18f), center.y), dirSign = 1f, radiusDp = 14f)
        spawnSfx(Offset(center.x + dp(8f), center.y - dp(18f)), text = sfx, sizeDp = 16f, angleDeg = -8f, useCyan = true, life = 0.52f)
    }

    fun emitPlayBlast(center: Offset, isNowPlaying: Boolean) {
        spawnImpactFlash(center, radiusDp = 64f)
        spawnBurst(center, radiusDp = if (isNowPlaying) 54f else 36f, spikes = 15, useCyanNeedles = !isNowPlaying)
        spawnRing(center, rxDp = 52f, ryDp = 22f, life = 0.48f)
        spawnShards(center, count = if (isNowPlaying) 10 else 5, speedDp = 200f, accentTint = isNowPlaying)
        spawnDonut(Offset(center.x, center.y - dp(8f)), radiusDp = 22f, vxDp = rnd(-16f, 16f), vyDp = -46f)
        spawnPuff(Offset(center.x - dp(28f), center.y + dp(14f)), lobeCount = 4, sizeDp = 11f, vxDp = -32f, vyDp = -14f)
        spawnPuff(Offset(center.x + dp(28f), center.y + dp(14f)), lobeCount = 4, sizeDp = 11f, vxDp = 32f, vyDp = -14f)
        spawnSfx(
            center = Offset(center.x, center.y - dp(38f)),
            text = if (isNowPlaying) "ドン!" else "カチッ",
            sizeDp = if (isNowPlaying) 25f else 18f,
            angleDeg = if (isNowPlaying) -12f else 8f,
            useCyan = !isNowPlaying,
        )
    }

    fun emitSkipSlash(center: Offset, forward: Boolean) {
        val dir = if (forward) 0f else PI.toFloat()
        val slashAngle = if (forward) -0.35f else (PI.toFloat() + 0.35f)
        spawnSlash(center, lenDp = 96f, angleRad = slashAngle, thickDp = 14f, useCyan = !forward)
        spawnSpeedLines(center, count = 7, dirRad = dir, lenDp = 56f, spreadDp = 24f)
        spawnTicks(center, count = 4, radiusDp = 22f, lenDp = 11f, dirRad = dir, useCyan = !forward)
        spawnPuff(center, lobeCount = 4, sizeDp = 9f, vxDp = if (forward) -26f else 26f, vyDp = -12f, life = 0.5f)
        spawnSfx(
            center = Offset(center.x, center.y - dp(24f)),
            text = "シュッ!",
            sizeDp = 19f,
            angleDeg = if (forward) -10f else 10f,
            useCyan = !forward,
        )
    }

    fun emitFavoriteBurst(center: Offset, nowFavorite: Boolean) {
        if (nowFavorite) {
            spawnBurst(center, radiusDp = 38f, spikes = 11)
            spawnShards(center, count = 9, speedDp = 175f, sizeDp = 5f, accentTint = true)
            for (i in 0 until 4) {
                val a = (i / 4f) * TAU + 0.4f
                val dist = dp(rnd(20f, 34f))
                spawnKira(Offset(center.x + cos(a) * dist, center.y + sin(a) * dist), radiusDp = rnd(9f, 14f))
            }
            spawnSfx(Offset(center.x, center.y - dp(24f)), text = "キュン!", sizeDp = 19f, angleDeg = -9f)
        } else {
            spawnPuff(center, lobeCount = 4, sizeDp = 9f, vxDp = 0f, vyDp = -20f, life = 0.5f)
            spawnTicks(center, count = 3, radiusDp = 16f, lenDp = 9f, dirRad = -1.2f, useCyan = true)
            spawnSfx(Offset(center.x, center.y - dp(20f)), text = "ポン!", sizeDp = 16f, angleDeg = 6f, useCyan = true, life = 0.48f)
        }
    }

    fun emitTabLand(center: Offset, dirSign: Float) {
        spawnTicks(center, count = 4, radiusDp = 18f, lenDp = 10f, dirRad = if (dirSign >= 0f) -0.5f else -2.6f, useCyan = true)
        spawnWob(Offset(center.x + dirSign * dp(24f), center.y), dirSign = dirSign, radiusDp = 15f)
        spawnPuff(Offset(center.x - dirSign * dp(18f), center.y + dp(8f)), lobeCount = 4, sizeDp = 8.5f, vxDp = -dirSign * 26f, vyDp = -12f, life = 0.48f)
        spawnSfx(Offset(center.x, center.y - dp(22f)), text = "カチッ", sizeDp = 15f, angleDeg = -7f, useCyan = true, life = 0.45f)
    }

    fun emitSlideSkid(center: Offset) {
        spawnSpeedLines(center, count = 7, dirRad = (-PI / 2.0).toFloat(), lenDp = 48f, spreadDp = 46f)
        spawnPuff(Offset(center.x - dp(30f), center.y), lobeCount = 4, sizeDp = 11f, vxDp = -28f, vyDp = -14f)
        spawnPuff(Offset(center.x + dp(30f), center.y), lobeCount = 4, sizeDp = 11f, vxDp = 28f, vyDp = -14f)
        spawnSfx(Offset(center.x, center.y - dp(18f)), text = "ズザッ", sizeDp = 18f, angleDeg = -8f, life = 0.56f)
    }

    fun emitScrubSmoke(pos: Offset, velocityX: Float) {
        val dir = if (velocityX >= 0f) -1f else 1f
        spawnPuff(
            center = Offset(pos.x + dir * dp(8f), pos.y + dp(2f)),
            lobeCount = 4,
            sizeDp = (7f + (abs(velocityX) / 220f).coerceAtMost(6f)),
            vxDp = dir * 24f,
            vyDp = -18f,
            life = 0.52f,
            spread = 0.85f,
        )
    }

    fun emitScrubRelease(pos: Offset, dirSign: Float) {
        val slashAngle = if (dirSign >= 0f) -0.3f else (PI.toFloat() + 0.3f)
        spawnSlash(pos, lenDp = 76f, angleRad = slashAngle, thickDp = 11f, life = 0.34f)
        spawnTicks(pos, count = 3, radiusDp = 16f, lenDp = 10f, dirRad = if (dirSign >= 0f) 0f else PI.toFloat())
        spawnSfx(Offset(pos.x, pos.y - dp(24f)), text = "シュッ!", sizeDp = 17f, angleDeg = -8f, life = 0.46f)
    }

    fun emitKeystrokeDust(pos: Offset) {
        spawnPuff(pos, lobeCount = 3, sizeDp = 5.5f, vxDp = rnd(-14f, 14f), vyDp = -16f, life = 0.36f, spread = 0.7f)
        if (Random.nextFloat() < 0.45f) {
            spawnKira(Offset(pos.x + dp(rnd(-8f, 8f)), pos.y - dp(6f)), radiusDp = 7f, life = 0.38f, useCyan = true)
        }
    }

    fun emitToggleSplash(center: Offset, turnedOn: Boolean) {
        spawnSplash(center, widthDp = 36f, heightDp = 22f, life = 0.56f)
        spawnWob(Offset(center.x + if (turnedOn) dp(16f) else -dp(16f), center.y), dirSign = if (turnedOn) 1f else -1f, radiusDp = 14f)
        spawnTicks(center, count = 3, radiusDp = 18f, lenDp = 9f, dirRad = -1.1f, useCyan = !turnedOn)
        spawnSfx(Offset(center.x, center.y - dp(24f)), text = "カチッ", sizeDp = 16f, angleDeg = -6f, useCyan = !turnedOn, life = 0.48f)
    }

    /** Lightweight anime sparkle and wedge pop along the left edge when synced lyrics advance. */
    fun emitLyricBeat(leftEdge: Offset) {
        spawnTicks(leftEdge, count = 3, radiusDp = 10f, lenDp = 9f, dirRad = -0.35f, life = 0.34f, fanRad = 0.52f)
        spawnKira(Offset(leftEdge.x + dp(6f), leftEdge.y - dp(14f)), radiusDp = 8.5f, life = 0.48f, useCyan = true)
        spawnKira(Offset(leftEdge.x + dp(14f), leftEdge.y + dp(12f)), radiusDp = 7f, life = 0.44f, useCyan = false)
        spawnPuff(Offset(leftEdge.x - dp(4f), leftEdge.y), lobeCount = 3, sizeDp = 5.5f, vxDp = -14f, vyDp = -8f, life = 0.36f, spread = 0.7f)
    }

    fun emitThemeShift(center: Offset) {
        spawnImpactFlash(center, radiusDp = 88f)
        spawnBurst(center, radiusDp = 64f, spikes = 16)
        spawnRing(center, rxDp = 70f, ryDp = 32f, life = 0.52f)
        spawnShards(center, count = 12, speedDp = 240f, sizeDp = 6f, accentTint = true)
        for (i in 0 until 5) {
            val a = (i / 5f) * TAU
            spawnKira(Offset(center.x + cos(a) * dp(44f), center.y + sin(a) * dp(44f)), radiusDp = 12f, useCyan = i % 2 == 0)
        }
        spawnSfx(Offset(center.x, center.y - dp(30f)), text = "ドン!", sizeDp = 26f, angleDeg = -12f)
    }
}

val LocalSakugaFx = compositionLocalOf<SakugaFxController> {
    error("LocalSakugaFx not provided. Wrap root composable in SakugaFxHost.")
}

/** Root container that provides [LocalSakugaFx] and renders all anime VFX on one shared overlay canvas. */
@Composable
fun SakugaFxHost(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val tokens = SakugaTheme.tokens
    val controller = remember { SakugaFxController() }
    controller.densityScale = density

    val reducedMotion = remember(context) {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
    }
    controller.reducedMotion = reducedMotion

    LaunchedEffect(controller.hasActiveParticles) {
        if (!controller.hasActiveParticles) return@LaunchedEffect
        var lastNanos = 0L
        while (controller.hasActiveParticles) {
            withFrameNanos { frameNanos ->
                val dt = if (lastNanos == 0L) 0.016f else (frameNanos - lastNanos) / 1_000_000_000f
                lastNanos = frameNanos
                controller.advance(dt)
            }
        }
    }

    CompositionLocalProvider(LocalSakugaFx provides controller) {
        Box(modifier = modifier.fillMaxSize()) {
            content()

            if (controller.hasActiveParticles) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Read frameTick so Compose redraws every active animation frame.
                    val tick = controller.frameTick
                    if (tick >= 0L) {
                        for (i in 0 until controller.particles.size) {
                            val p = controller.particles[i]
                            val k = (p.elapsed / p.life).coerceIn(0f, 1f)
                            p.draw(this, k, p.elapsed, tokens, density)
                        }
                    }
                }
            }
        }
    }
}

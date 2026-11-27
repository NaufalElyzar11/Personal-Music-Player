package com.noirmusic.ui.sakuga

import androidx.compose.ui.geometry.Offset
import com.noirmusic.data.preferences.DataStoreThemePreferencesStore
import com.noirmusic.ui.theme.SakugaThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SakugaMathTest {

    @Test
    fun easingCurves_stayWithinExpectedBoundsAndOvershootOnBack() {
        assertEquals(0f, SakugaMath.outCubic(0f), 1e-4f)
        assertEquals(1f, SakugaMath.outCubic(1f), 1e-4f)
        assertEquals(0f, SakugaMath.inCubic(0f), 1e-4f)
        assertEquals(1f, SakugaMath.inCubic(1f), 1e-4f)
        assertEquals(0f, SakugaMath.outBack(0f), 1e-4f)
        assertEquals(1f, SakugaMath.outBack(1f), 1e-4f)
        assertTrue("outBack should overshoot > 1 mid-curve", SakugaMath.outBack(0.6f) > 1.0f)
    }

    @Test
    fun spring_overshootsAndSettlesAtTarget() {
        val spring = SakugaSpring(x = 0f, stiffness = 360f, damping = 16f)
        spring.target = 1f
        var peak = 0f
        repeat(120) {
            val pos = spring.step(0.016f)
            if (pos > peak) peak = pos
        }
        assertTrue("underdamped spring should overshoot target", peak > 1.05f)
        assertTrue("spring should settle near target", spring.isSettled(epsilon = 0.01f))
        assertEquals(1f, spring.x, 0.01f)
    }

    @Test
    fun fxController_respectsReducedMotionAndExpiresParticles() {
        val fx = SakugaFxController()
        fx.reducedMotion = true
        fx.emitPrimaryConfirm(Offset(100f, 100f))
        assertFalse(fx.hasActiveParticles)
        assertEquals(0, fx.particles.size)

        fx.reducedMotion = false
        fx.emitPlayBlast(Offset(100f, 100f), isNowPlaying = true)
        assertTrue(fx.hasActiveParticles)
        assertTrue(fx.particles.isNotEmpty())

        // Advance past longest particle lifetime (donut = 0.85s)
        repeat(60) { fx.advance(0.03f) }
        assertFalse(fx.hasActiveParticles)
        assertEquals(0, fx.particles.size)
    }

    @Test
    fun fxController_lyricBeatAndToggleSplashEmitExpectedParticles() {
        val fx = SakugaFxController()
        fx.emitLyricBeat(Offset(24f, 300f))
        assertTrue(fx.hasActiveParticles)
        assertEquals(4, fx.particles.size)

        repeat(40) { fx.advance(0.03f) }
        assertFalse(fx.hasActiveParticles)

        fx.emitToggleSplash(Offset(200f, 500f), turnedOn = true)
        assertTrue(fx.hasActiveParticles)
        // Splash emits 1 crown + 6 drops + 1 wob + 1 ticks + 1 single sfx (no duplicate sfx)
        assertEquals(10, fx.particles.size)
    }

    @Test
    fun themeModeDecoder_defaultsToDaylightAndRecognizesMidnight() {
        assertEquals(SakugaThemeMode.DAYLIGHT, DataStoreThemePreferencesStore.decodeMode(null))
        assertEquals(SakugaThemeMode.DAYLIGHT, DataStoreThemePreferencesStore.decodeMode("UNKNOWN"))
        assertEquals(SakugaThemeMode.DAYLIGHT, DataStoreThemePreferencesStore.decodeMode("DAYLIGHT"))
        assertEquals(SakugaThemeMode.MIDNIGHT, DataStoreThemePreferencesStore.decodeMode("MIDNIGHT"))
    }
}

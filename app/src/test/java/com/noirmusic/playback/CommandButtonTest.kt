package com.noirmusic.playback

import android.os.Bundle
import androidx.media3.session.CommandButton
import androidx.media3.session.SessionCommand
import com.noirmusic.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class CommandButtonTest {

    @Test
    fun commandButtons_buildSuccessfullyWithValidIconEnumAndResId() {
        val shuffleCommand = SessionCommand(ACTION_TOGGLE_SHUFFLE, Bundle())
        val shuffleButton = CommandButton.Builder(CommandButton.ICON_SHUFFLE_ON)
            .setDisplayName("Shuffle on")
            .setSessionCommand(shuffleCommand)
            .setCustomIconResId(R.drawable.ic_shuffle_on)
            .build()

        assertNotNull(shuffleButton)
        assertEquals(CommandButton.ICON_SHUFFLE_ON, shuffleButton.icon)
        assertEquals(R.drawable.ic_shuffle_on, shuffleButton.iconResId)
        assertEquals("Shuffle on", shuffleButton.displayName)
        assertEquals(ACTION_TOGGLE_SHUFFLE, shuffleButton.sessionCommand?.customAction)

        val repeatCommand = SessionCommand(ACTION_CYCLE_REPEAT, Bundle())
        val repeatButton = CommandButton.Builder(CommandButton.ICON_REPEAT_ONE)
            .setDisplayName("Repeat one")
            .setSessionCommand(repeatCommand)
            .setCustomIconResId(R.drawable.ic_repeat_one)
            .build()

        assertNotNull(repeatButton)
        assertEquals(CommandButton.ICON_REPEAT_ONE, repeatButton.icon)
        assertEquals(R.drawable.ic_repeat_one, repeatButton.iconResId)
        assertEquals("Repeat one", repeatButton.displayName)
        assertEquals(ACTION_CYCLE_REPEAT, repeatButton.sessionCommand?.customAction)
    }
}

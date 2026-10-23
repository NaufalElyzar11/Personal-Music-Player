package com.noirmusic.ui.permission

import org.junit.Assert.assertEquals
import org.junit.Test

class AudioPermissionTest {

    @Test
    fun selectsPermissionForSdkAndMapsResults() {
        assertEquals(AudioPermission.READ_MEDIA_AUDIO, AudioPermission.audioPermissionFor(33))
        assertEquals(AudioPermission.READ_MEDIA_AUDIO, AudioPermission.audioPermissionFor(36))
        assertEquals(AudioPermission.READ_EXTERNAL_STORAGE, AudioPermission.audioPermissionFor(26))
        assertEquals(AudioPermission.READ_EXTERNAL_STORAGE, AudioPermission.audioPermissionFor(32))

        assertEquals(PermissionStatus.GRANTED, AudioPermission.resolveInitial(true))
        assertEquals(PermissionStatus.CAN_REQUEST, AudioPermission.resolveInitial(false))

        assertEquals(PermissionStatus.GRANTED, AudioPermission.resolveAfterResult(granted = true, shouldShowRationale = false))
        assertEquals(PermissionStatus.CAN_REQUEST, AudioPermission.resolveAfterResult(granted = false, shouldShowRationale = true))
        assertEquals(
            PermissionStatus.PERMANENTLY_DENIED,
            AudioPermission.resolveAfterResult(granted = false, shouldShowRationale = false),
        )
    }
}

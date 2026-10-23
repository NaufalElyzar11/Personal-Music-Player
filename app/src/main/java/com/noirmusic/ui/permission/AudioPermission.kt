package com.noirmusic.ui.permission

/** Pure permission decisions. Permission names are string literals so this stays JVM-testable. */
object AudioPermission {

    const val READ_MEDIA_AUDIO = "android.permission.READ_MEDIA_AUDIO"
    const val READ_EXTERNAL_STORAGE = "android.permission.READ_EXTERNAL_STORAGE"
    private const val TIRAMISU = 33

    fun audioPermissionFor(sdkInt: Int): String =
        if (sdkInt >= TIRAMISU) READ_MEDIA_AUDIO else READ_EXTERNAL_STORAGE

    fun resolveInitial(granted: Boolean): PermissionStatus =
        if (granted) PermissionStatus.GRANTED else PermissionStatus.CAN_REQUEST

    fun resolveAfterResult(granted: Boolean, shouldShowRationale: Boolean): PermissionStatus = when {
        granted -> PermissionStatus.GRANTED
        shouldShowRationale -> PermissionStatus.CAN_REQUEST
        else -> PermissionStatus.PERMANENTLY_DENIED
    }
}

enum class PermissionStatus { GRANTED, CAN_REQUEST, PERMANENTLY_DENIED }

package com.noirmusic.ui.permission

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.noirmusic.ui.common.CenteredMessage

@Composable
fun PermissionGate(onGranted: @Composable () -> Unit) {
    val context = LocalContext.current
    val permission = AudioPermission.audioPermissionFor(Build.VERSION.SDK_INT)

    var status by rememberSaveable {
        mutableStateOf(
            AudioPermission.resolveInitial(
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED,
            ),
        )
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            status = PermissionStatus.GRANTED
        }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val rationale = context.findActivity()?.let {
            ActivityCompat.shouldShowRequestPermissionRationale(it, permission)
        } ?: false
        status = AudioPermission.resolveAfterResult(granted, rationale)
    }

    if (status == PermissionStatus.GRANTED) {
        onGranted()
    } else {
        val permanent = status == PermissionStatus.PERMANENTLY_DENIED
        CenteredMessage(
            icon = Icons.Filled.LibraryMusic,
            title = "Let Noir Music see your music",
            body = "Noir Music needs access to audio files on this phone to play them. " +
                "Only song title, artist and duration are sent to LRCLIB to find lyrics.",
            actionLabel = if (permanent) "Open settings" else "Allow access",
            onAction = {
                if (permanent) {
                    context.openAppSettings()
                } else {
                    launcher.launch(permission)
                }
            },
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .systemBarsPadding(),
        )
    }
}

private fun Context.findActivity(): Activity? {
    var cur = this
    while (cur is ContextWrapper) {
        if (cur is Activity) return cur
        cur = cur.baseContext
    }
    return null
}

private fun Context.openAppSettings() {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { startActivity(intent) }
}

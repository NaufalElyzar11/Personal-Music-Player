package com.noirmusic.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.ExecutionException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Builds and releases the app's single MediaController for [PlaybackService]. Main-thread only. */
class PlaybackConnection(context: Context) {

    private val appContext = context.applicationContext
    private var future: ListenableFuture<MediaController>? = null

    suspend fun await(): MediaController {
        val f = future ?: MediaController.Builder(
            appContext,
            SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java)),
        ).buildAsync().also { future = it }

        if (f.isDone) return f.getUnwrapped()
        return suspendCancellableCoroutine { cont ->
            f.addListener(
                {
                    try {
                        cont.resume(f.getUnwrapped())
                    } catch (t: Throwable) {
                        cont.resumeWithException(t)
                    }
                },
                ContextCompat.getMainExecutor(appContext),
            )
        }
    }

    fun release() {
        future?.let { MediaController.releaseFuture(it) }
        future = null
    }

    private fun ListenableFuture<MediaController>.getUnwrapped(): MediaController =
        try {
            get()
        } catch (e: ExecutionException) {
            throw e.cause ?: e
        }
}

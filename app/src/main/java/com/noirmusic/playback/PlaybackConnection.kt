package com.noirmusic.playback

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.ExecutionException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "PlaybackConnection"

/** Builds and releases the app's single MediaController for [PlaybackService]. Main-thread only. */
class PlaybackConnection(context: Context) {

    private val appContext = context.applicationContext
    private var future: ListenableFuture<MediaController>? = null

    suspend fun await(): MediaController {
        val f = future ?: run {
            Log.d(TAG, "Creating new MediaController for PlaybackService...")
            MediaController.Builder(
                appContext,
                SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java)),
            ).buildAsync().also { future = it }
        }

        if (f.isDone) {
            val existing = try {
                f.getUnwrapped()
            } catch (t: Throwable) {
                release()
                throw t
            }
            if (existing.isConnected) {
                Log.d(TAG, "MediaController already connected")
                return existing
            }
            Log.w(TAG, "Cached MediaController disconnected; reconnecting")
            release()
            return await()
        }

        return suspendCancellableCoroutine { cont ->
            f.addListener(
                {
                    try {
                        val controller = f.getUnwrapped()
                        Log.d(TAG, "MediaController successfully connected!")
                        cont.resume(controller)
                    } catch (t: Throwable) {
                        Log.e(TAG, "MediaController failed to connect", t)
                        release() // clear failed future so retry will work!
                        cont.resumeWithException(t)
                    }
                },
                ContextCompat.getMainExecutor(appContext),
            )
        }
    }

    fun release() {
        Log.d(TAG, "Releasing MediaController future")
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

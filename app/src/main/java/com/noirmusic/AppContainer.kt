package com.noirmusic

import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.noirmusic.data.lyrics.HttpErrorMapping
import com.noirmusic.data.lyrics.LrclibApi
import com.noirmusic.data.lyrics.LrclibJson
import com.noirmusic.data.lyrics.LyricsRepository
import com.noirmusic.data.media.MediaStoreTrackRepository
import com.noirmusic.data.media.TrackRepository
import com.noirmusic.playback.PlaybackConnection
import com.noirmusic.ui.library.LibraryViewModel
import com.noirmusic.ui.player.PlayerViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** Manual dependency container, created once by [NoirApplication]. */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    /** Long-lived scope used only for shared in-flight lyrics requests. */
    val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val userAgent: String = HttpErrorMapping.buildUserAgent(BuildConfig.VERSION_NAME, BuildConfig.LRCLIB_CLIENT_CONTACT)

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("User-Agent", userAgent)
                        .header("Accept", "application/json")
                        .build(),
                )
            }
            .build()
    }

    val lyricsRepository: LyricsRepository by lazy {
        LyricsRepository(
            api = LrclibApi(okHttpClient, LrclibJson, "https://lrclib.net/".toHttpUrl()),
            scope = appScope,
            clock = SystemClock::elapsedRealtime,
        )
    }

    val trackRepository: TrackRepository by lazy { MediaStoreTrackRepository(appContext.contentResolver) }

    val libraryViewModelFactory: ViewModelProvider.Factory = viewModelFactory {
        initializer { LibraryViewModel(trackRepository) }
    }

    val playerViewModelFactory: ViewModelProvider.Factory = viewModelFactory {
        initializer { PlayerViewModel(PlaybackConnection(appContext), lyricsRepository) }
    }
}

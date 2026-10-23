package com.noirmusic.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.noirmusic.ui.library.LibraryScreen
import com.noirmusic.ui.library.LibraryViewModel
import com.noirmusic.ui.permission.PermissionGate
import com.noirmusic.ui.player.NowPlayingScreen
import com.noirmusic.ui.player.PlayerViewModel
import kotlinx.coroutines.flow.StateFlow

@Composable
fun NoirApp(
    libraryViewModel: LibraryViewModel,
    playerViewModel: PlayerViewModel,
    openPlayerRequest: StateFlow<Boolean>,
    onConsumeOpenPlayerRequest: () -> Unit,
) {
    PermissionGate {
        LaunchedEffect(Unit) {
            libraryViewModel.scan(force = false)
        }

        var nowPlayingOpen by rememberSaveable { mutableStateOf(false) }
        val shouldOpenPlayer by openPlayerRequest.collectAsStateWithLifecycle()
        val libraryState by libraryViewModel.uiState.collectAsStateWithLifecycle()
        val query by libraryViewModel.query.collectAsStateWithLifecycle()
        val sort by libraryViewModel.sort.collectAsStateWithLifecycle()
        val playback by playerViewModel.playbackState.collectAsStateWithLifecycle()
        val pane by playerViewModel.nowPlayingPane.collectAsStateWithLifecycle()

        LaunchedEffect(shouldOpenPlayer) {
            if (shouldOpenPlayer) {
                nowPlayingOpen = true
                onConsumeOpenPlayerRequest()
            }
        }

        LaunchedEffect(playback.nowPlaying) {
            if (playback.nowPlaying == null) nowPlayingOpen = false
        }

        BackHandler(enabled = nowPlayingOpen) {
            nowPlayingOpen = false
        }

        Box(modifier = Modifier.fillMaxSize()) {
            LibraryScreen(
                state = libraryState,
                query = query,
                sort = sort,
                playback = playback,
                onQueryChange = libraryViewModel::setQuery,
                onSortChange = libraryViewModel::setSort,
                onRefresh = { libraryViewModel.scan(force = true) },
                onTrackClick = playerViewModel::playQueue,
                onTogglePlayPause = playerViewModel::togglePlayPause,
                onNext = playerViewModel::next,
                onOpenNowPlaying = { nowPlayingOpen = true },
            )

            AnimatedVisibility(
                visible = nowPlayingOpen && playback.nowPlaying != null,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
            ) {
                NowPlayingScreen(
                    playback = playback,
                    positionMsFlow = playerViewModel.positionMs,
                    lyricsStateFlow = playerViewModel.lyricsState,
                    activeLineIndexFlow = playerViewModel.activeLineIndex,
                    artworkFlow = playerViewModel.artwork,
                    pane = pane,
                    onPaneChange = playerViewModel::setNowPlayingPane,
                    onClose = { nowPlayingOpen = false },
                    onTogglePlayPause = playerViewModel::togglePlayPause,
                    onPrevious = playerViewModel::previous,
                    onNext = playerViewModel::next,
                    onSeekTo = playerViewModel::seekTo,
                    onToggleShuffle = playerViewModel::toggleShuffle,
                    onCycleRepeat = playerViewModel::cycleRepeat,
                    onRetryLyrics = playerViewModel::retryLyrics,
                )
            }
        }
    }
}

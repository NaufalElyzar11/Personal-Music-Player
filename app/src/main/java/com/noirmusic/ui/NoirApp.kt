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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.noirmusic.data.model.Track
import com.noirmusic.ui.library.AddToPlaylistDialog
import com.noirmusic.ui.library.LibraryScreen
import com.noirmusic.ui.library.LibraryUiState
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
        var trackToAddFromNowPlaying by remember { mutableStateOf<Track?>(null) }

        val shouldOpenPlayer by openPlayerRequest.collectAsStateWithLifecycle()
        val libraryState by libraryViewModel.uiState.collectAsStateWithLifecycle()
        val query by libraryViewModel.query.collectAsStateWithLifecycle()
        val sort by libraryViewModel.sort.collectAsStateWithLifecycle()
        val playlists by libraryViewModel.playlists.collectAsStateWithLifecycle()
        val selectedPlaylistWithTracks by libraryViewModel.selectedPlaylistWithTracks.collectAsStateWithLifecycle()

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

        BackHandler(enabled = selectedPlaylistWithTracks != null && !nowPlayingOpen) {
            libraryViewModel.selectPlaylist(null)
        }

        trackToAddFromNowPlaying?.let { track ->
            AddToPlaylistDialog(
                track = track,
                playlists = playlists,
                onAddToPlaylist = { playlistId ->
                    libraryViewModel.addTrackToPlaylist(playlistId, track.id)
                    trackToAddFromNowPlaying = null
                },
                onCreateAndAdd = { name ->
                    libraryViewModel.createPlaylist(name, track.id)
                    trackToAddFromNowPlaying = null
                },
                onDismiss = { trackToAddFromNowPlaying = null },
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            LibraryScreen(
                state = libraryState,
                query = query,
                sort = sort,
                playback = playback,
                playlists = playlists,
                selectedPlaylistWithTracks = selectedPlaylistWithTracks,
                onQueryChange = libraryViewModel::setQuery,
                onSortChange = libraryViewModel::setSort,
                onRefresh = { libraryViewModel.scan(force = true) },
                onTrackClick = { tracks, index ->
                    playerViewModel.playQueue(tracks, index)
                    nowPlayingOpen = true
                },
                onTogglePlayPause = playerViewModel::togglePlayPause,
                onNext = playerViewModel::next,
                onOpenNowPlaying = { nowPlayingOpen = true },
                onSelectPlaylist = libraryViewModel::selectPlaylist,
                onCreatePlaylist = { name, initialTrackId ->
                    libraryViewModel.createPlaylist(name, initialTrackId)
                },
                onDeletePlaylist = libraryViewModel::deletePlaylist,
                onRenamePlaylist = libraryViewModel::renamePlaylist,
                onAddToPlaylist = libraryViewModel::addTrackToPlaylist,
                onRemoveFromPlaylist = libraryViewModel::removeTrackFromPlaylist,
            )

            AnimatedVisibility(
                visible = nowPlayingOpen && playback.nowPlaying != null,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
            ) {
                val currentTrack = (libraryState as? LibraryUiState.Content)?.all?.find {
                    it.id.toString() == playback.nowPlaying?.mediaId
                }

                NowPlayingScreen(
                    playback = playback,
                    positionMsFlow = playerViewModel.positionMs,
                    lyricsStateFlow = playerViewModel.lyricsState,
                    activeLineIndexFlow = playerViewModel.activeLineIndex,
                    artworkFlow = playerViewModel.artwork,
                    sleepTimerStateFlow = playerViewModel.sleepTimerState,
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
                    onStartSleepTimer = playerViewModel::startSleepTimer,
                    onStartSleepTimerEndOfTrack = playerViewModel::startSleepTimerEndOfTrack,
                    onAddSleepTimerMinutes = playerViewModel::addSleepTimerMinutes,
                    onCancelSleepTimer = playerViewModel::cancelSleepTimer,
                    onSkipToQueueItem = playerViewModel::skipToQueueItem,
                    onAddToPlaylist = currentTrack?.let { track ->
                        { trackToAddFromNowPlaying = track }
                    },
                )
            }
        }
    }
}

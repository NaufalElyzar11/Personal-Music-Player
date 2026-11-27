package com.noirmusic.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import com.noirmusic.ui.sakuga.SakugaFxHost
import com.noirmusic.ui.theme.SakugaThemeMode
import kotlinx.coroutines.flow.StateFlow

@Composable
fun NoirApp(
    libraryViewModel: LibraryViewModel,
    playerViewModel: PlayerViewModel,
    openPlayerRequest: StateFlow<Boolean>,
    onConsumeOpenPlayerRequest: () -> Unit,
    themeMode: SakugaThemeMode = SakugaThemeMode.DAYLIGHT,
    onToggleTheme: () -> Unit = {},
) {
    SakugaFxHost {
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
            val favoriteIds by libraryViewModel.favoriteIds.collectAsStateWithLifecycle()
            val favoriteTracks by libraryViewModel.favoriteTracks.collectAsStateWithLifecycle()
            val recentTracks by libraryViewModel.recentTracks.collectAsStateWithLifecycle()
            val mostPlayedTracks by libraryViewModel.mostPlayedTracks.collectAsStateWithLifecycle()
            val selectedPlaylistWithTracks by libraryViewModel.selectedPlaylistWithTracks.collectAsStateWithLifecycle()

            val playback by playerViewModel.playbackState.collectAsStateWithLifecycle()
            val pane by playerViewModel.nowPlayingPane.collectAsStateWithLifecycle()
            val isCurrentFavorite by playerViewModel.isCurrentFavorite.collectAsStateWithLifecycle()

            LaunchedEffect(libraryState) {
                val content = libraryState as? LibraryUiState.Content ?: return@LaunchedEffect
                playerViewModel.restoreLastSessionIfEmpty(content.all)
            }

            LaunchedEffect(shouldOpenPlayer) {
                if (shouldOpenPlayer) {
                    nowPlayingOpen = true
                    onConsumeOpenPlayerRequest()
                }
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
                    positionMsFlow = playerViewModel.positionMs,
                    playlists = playlists,
                    favoriteIds = favoriteIds,
                    favoriteTracks = favoriteTracks,
                    recentTracks = recentTracks,
                    mostPlayedTracks = mostPlayedTracks,
                    selectedPlaylistWithTracks = selectedPlaylistWithTracks,
                    themeMode = themeMode,
                    onToggleTheme = onToggleTheme,
                    onQueryChange = libraryViewModel::setQuery,
                    onSortChange = libraryViewModel::setSort,
                    onRefresh = { libraryViewModel.scan(force = true) },
                    onTrackClick = { tracks, index ->
                        playerViewModel.playQueue(tracks, index)
                        nowPlayingOpen = true
                    },
                    onPlayNext = playerViewModel::playNext,
                    onAddToQueue = playerViewModel::addToQueue,
                    onToggleFavorite = libraryViewModel::toggleFavorite,
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
                    onReorderPlaylistTracks = libraryViewModel::reorderPlaylistTracks,
                )

                AnimatedVisibility(
                    visible = nowPlayingOpen && playback.nowPlaying != null,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = spring(dampingRatio = 0.74f, stiffness = 380f),
                    ) + fadeIn(),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = spring(dampingRatio = 0.85f, stiffness = 420f),
                    ) + fadeOut(),
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
                        isFavorite = isCurrentFavorite,
                        onToggleFavorite = playerViewModel::toggleCurrentFavorite,
                        onSkipToQueueItem = playerViewModel::skipToQueueItem,
                        onMoveQueueItem = playerViewModel::moveQueueItem,
                        onRemoveQueueItem = playerViewModel::removeFromQueue,
                        onClearUpcomingQueue = playerViewModel::clearUpcomingQueue,
                        onAddToPlaylist = currentTrack?.let { track ->
                            { trackToAddFromNowPlaying = track }
                        },
                    )
                }
            }
        }
    }
}

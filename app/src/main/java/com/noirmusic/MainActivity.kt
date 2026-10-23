package com.noirmusic

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.noirmusic.ui.NoirApp
import com.noirmusic.ui.library.LibraryViewModel
import com.noirmusic.ui.player.PlayerViewModel
import com.noirmusic.ui.theme.NoirTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainActivity : ComponentActivity() {

    private val container: AppContainer
        get() = (application as NoirApplication).container

    private val libraryViewModel: LibraryViewModel by viewModels { container.libraryViewModelFactory }
    private val playerViewModel: PlayerViewModel by viewModels { container.playerViewModelFactory }

    private val _openPlayerRequest = MutableStateFlow(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        handleOpenPlayerIntent(intent)

        setContent {
            NoirTheme {
                NoirApp(
                    libraryViewModel = libraryViewModel,
                    playerViewModel = playerViewModel,
                    openPlayerRequest = _openPlayerRequest.asStateFlow(),
                    onConsumeOpenPlayerRequest = { _openPlayerRequest.value = false },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOpenPlayerIntent(intent)
    }

    private fun handleOpenPlayerIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_PLAYER, false) == true) {
            _openPlayerRequest.value = true
            intent.removeExtra(EXTRA_OPEN_PLAYER)
        }
    }

    companion object {
        const val EXTRA_OPEN_PLAYER = "com.noirmusic.EXTRA_OPEN_PLAYER"
    }
}

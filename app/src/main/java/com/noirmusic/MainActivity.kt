package com.noirmusic

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.noirmusic.ui.NoirApp
import com.noirmusic.ui.library.LibraryViewModel
import com.noirmusic.ui.player.PlayerViewModel
import com.noirmusic.ui.theme.NoirTheme
import com.noirmusic.ui.theme.SakugaThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val container: AppContainer
        get() = (application as NoirApplication).container

    private val libraryViewModel: LibraryViewModel by viewModels { container.libraryViewModelFactory }
    private val playerViewModel: PlayerViewModel by viewModels { container.playerViewModelFactory }

    private val _openPlayerRequest = MutableStateFlow(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        handleOpenPlayerIntent(intent)

        setContent {
            val themeMode by container.themePreferencesStore.themeMode
                .collectAsStateWithLifecycle(initialValue = SakugaThemeMode.DAYLIGHT)
            val coroutineScope = rememberCoroutineScope()

            LaunchedEffect(themeMode) {
                val style = if (themeMode == SakugaThemeMode.MIDNIGHT) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }

            NoirTheme(themeMode = themeMode) {
                NoirApp(
                    libraryViewModel = libraryViewModel,
                    playerViewModel = playerViewModel,
                    openPlayerRequest = _openPlayerRequest.asStateFlow(),
                    onConsumeOpenPlayerRequest = { _openPlayerRequest.value = false },
                    themeMode = themeMode,
                    onToggleTheme = {
                        val next = if (themeMode == SakugaThemeMode.DAYLIGHT) {
                            SakugaThemeMode.MIDNIGHT
                        } else {
                            SakugaThemeMode.DAYLIGHT
                        }
                        coroutineScope.launch {
                            container.themePreferencesStore.setThemeMode(next)
                        }
                    },
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

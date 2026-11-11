package com.captain.kplayer

import android.app.PictureInPictureParams
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.captain.kplayer.ui.navigation.NavGraph
import com.captain.kplayer.ui.theme.KPlayerTheme
import com.captain.kplayer.viewmodel.LibraryViewModel
import com.captain.kplayer.viewmodel.PlayerViewModel
import com.captain.kplayer.viewmodel.SettingsStore
import com.captain.kplayer.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {

    private lateinit var playerViewModel: PlayerViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = SettingsStore(applicationContext)
        setContent {
            val settingsVm: SettingsViewModel =
                viewModel(factory = factory { SettingsViewModel(application, store) })
            val libraryVm: LibraryViewModel =
                viewModel(factory = factory { LibraryViewModel() })
            val playerVm: PlayerViewModel =
                viewModel(factory = factory { PlayerViewModel(application, store) })
            playerViewModel = playerVm

            val themeName by settingsVm.themeName.collectAsState()
            val blur by settingsVm.blurIntensity.collectAsState()

            KPlayerTheme(themeName = themeName, blurIntensity = blur) {
                val navController = rememberNavController()
                NavGraph(
                    navController = navController,
                    settingsVm = settingsVm,
                    libraryVm = libraryVm,
                    playerVm = playerVm
                )
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (::playerViewModel.isInitialized) {
            playerViewModel.onAppBackgrounded()
        }
    }

    /**
     * Auto Picture-in-Picture when the user leaves the app (e.g. Home)
     * while a video is playing and PiP is enabled — same 16:9 params as
     * VideoPlayerScreen's enterPip(). Audio-only playback does not enter
     * PiP (a black PiP window would be useless).
     */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (::playerViewModel.isInitialized &&
            playerViewModel.pipEnabled.value &&
            playerViewModel.currentEntry.value?.isVideo == true &&
            playerViewModel.isPlaying.value &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
        ) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            enterPictureInPictureMode(params)
        }
    }
}

/** Small factory helper so ViewModels with constructor arguments can be created. */
inline fun <reified T : ViewModel> factory(crossinline create: () -> T): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = create() as T
    }

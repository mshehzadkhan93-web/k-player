package com.captain.kplayer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.captain.kplayer.ui.screens.HomeScreen
import com.captain.kplayer.ui.screens.MusicPlayerScreen
import com.captain.kplayer.ui.screens.PlaylistScreen
import com.captain.kplayer.ui.screens.SettingsScreen
import com.captain.kplayer.ui.screens.SplashScreen
import com.captain.kplayer.ui.screens.VideoPlayerScreen
import com.captain.kplayer.viewmodel.LibraryViewModel
import com.captain.kplayer.viewmodel.PlayerViewModel
import com.captain.kplayer.viewmodel.SettingsViewModel

object Routes {
    const val SPLASH = "splash"
    const val HOME = "home"
    const val PLAYER = "player"
    const val MUSIC = "music"
    const val PLAYLIST = "playlist"
    const val SETTINGS = "settings"
}

@Composable
fun NavGraph(
    navController: NavHostController,
    settingsVm: SettingsViewModel,
    libraryVm: LibraryViewModel,
    playerVm: PlayerViewModel
) {
    NavHost(navController = navController, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onFinished = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.HOME) {
            HomeScreen(
                navController = navController,
                libraryVm = libraryVm,
                playerVm = playerVm,
                settingsVm = settingsVm
            )
        }
        composable(Routes.PLAYER) {
            VideoPlayerScreen(
                navController = navController,
                libraryVm = libraryVm,
                playerVm = playerVm,
                settingsVm = settingsVm,
                onOpenPlaylist = { navController.navigate(Routes.PLAYLIST) }
            )
        }
        composable(Routes.MUSIC) {
            MusicPlayerScreen(
                navController = navController,
                playerVm = playerVm,
                onOpenPlaylist = { navController.navigate(Routes.PLAYLIST) }
            )
        }
        composable(Routes.PLAYLIST) {
            PlaylistScreen(navController = navController, playerVm = playerVm)
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                navController = navController,
                settingsVm = settingsVm,
                playerVm = playerVm,
                libraryVm = libraryVm
            )
        }
    }
}

package com.captain.kplayer.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.captain.kplayer.ui.screens.FolderDetailScreen
import com.captain.kplayer.ui.screens.HomeScreen
import com.captain.kplayer.ui.screens.MusicPlayerScreen
import com.captain.kplayer.ui.screens.NetworkScreen
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
    const val NETWORK = "network"
    const val SETTINGS = "settings"
    const val FOLDER = "folder/{encodedPath}"

    /** Folder-detail route: the path is URL-encoded (it contains slashes). */
    fun folderRoute(path: String): String = "folder/${Uri.encode(path)}"
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
        composable(Routes.NETWORK) {
            NetworkScreen(
                navController = navController,
                libraryVm = libraryVm,
                playerVm = playerVm
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                navController = navController,
                settingsVm = settingsVm,
                playerVm = playerVm,
                libraryVm = libraryVm
            )
        }
        composable(Routes.FOLDER) { backStackEntry ->
            val encoded = backStackEntry.arguments?.getString("encodedPath").orEmpty()
            val path = runCatching { Uri.decode(encoded) }.getOrDefault(encoded)
            FolderDetailScreen(
                folderPath = path,
                navController = navController,
                libraryVm = libraryVm,
                playerVm = playerVm
            )
        }
    }
}

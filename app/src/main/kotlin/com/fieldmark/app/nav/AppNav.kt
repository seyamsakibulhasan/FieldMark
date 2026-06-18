package com.fieldmark.app.nav

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fieldmark.app.ui.screens.AnaglyphScreen
import com.fieldmark.app.ui.screens.CameraScreen
import com.fieldmark.app.ui.screens.CompassScreen
import com.fieldmark.app.ui.screens.EditorScreen
import com.fieldmark.app.ui.screens.HomeScreen

object Routes {
    const val HOME = "home"
    const val CAMERA = "camera"
    const val EDITOR = "editor/{path}"
    const val COMPASS = "compass"
    const val ANAGLYPH = "anaglyph/{path}"
    fun editor(path: String) = "editor/${android.net.Uri.encode(path)}"
    fun anaglyph(path: String) = "anaglyph/${android.net.Uri.encode(path)}"
}

@Composable
fun AppNav() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) { HomeScreen(nav) }
        composable(Routes.CAMERA) { CameraScreen(nav) }
        composable(Routes.EDITOR) { entry ->
            val path = android.net.Uri.decode(entry.arguments?.getString("path").orEmpty())
            EditorScreen(nav, path)
        }
        composable(Routes.COMPASS) { CompassScreen(nav) }
        composable(Routes.ANAGLYPH) { entry ->
            val path = android.net.Uri.decode(entry.arguments?.getString("path").orEmpty())
            AnaglyphScreen(nav, path)
        }
    }
}

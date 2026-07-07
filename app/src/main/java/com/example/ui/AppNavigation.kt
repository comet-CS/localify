package com.example.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MusicViewModel
import com.example.ui.screens.NowPlayingScreen

@Composable
fun AppNavigation(viewModel: MusicViewModel, navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(viewModel, onNavigateToNowPlaying = {
                navController.navigate("now_playing")
            })
        }
        composable(
            route = "now_playing",
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, animationSpec = tween(300)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, animationSpec = tween(300)) }
        ) {
            NowPlayingScreen(viewModel, onBack = { navController.popBackStack() })
        }
    }
}

package com.gamevault.app.ui

import androidx.compose.animation.*
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.gamevault.app.ui.detail.DetailScreen
import com.gamevault.app.ui.onboarding.OnboardingScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val DETAIL = "detail/{packageName}"

    fun detail(packageName: String) = "detail/$packageName"
}

@Composable
fun GameShelfNavHost(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { slideInHorizontally { it } + fadeIn() },
        exitTransition = { slideOutHorizontally { -it / 3 } + fadeOut() },
        popEnterTransition = { slideInHorizontally { -it / 3 } + fadeIn() },
        popExitTransition = { slideOutHorizontally { it } + fadeOut() }
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            MainScreen(
                onGameDetail = { navController.navigate(Routes.detail(it)) }
            )
        }

        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("packageName") { type = NavType.StringType })
        ) {
            DetailScreen(onBack = { navController.popBackStack() })
        }
    }
}

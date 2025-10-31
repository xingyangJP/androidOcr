package com.example.logoocr.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.logoocr.ui.screen.confirm.ConfirmRecognitionScreen
import com.example.logoocr.ui.screen.history.HistoryScreen
import com.example.logoocr.ui.screen.main.MainScreen
import com.example.logoocr.ui.screen.register.RegisterLogoScreen

@Composable
fun LogoOcrNavHost(
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = LogoOcrDestination.Main.name,
        modifier = modifier
    ) {
        composable(route = LogoOcrDestination.Main.name) {
            MainScreen(
                onNavigateToRegister = {
                    navController.navigate(LogoOcrDestination.RegisterLogo.name)
                },
                onNavigateToHistory = {
                    navController.navigate(LogoOcrDestination.History.name)
                },
                onNavigateToConfirm = { resultId ->
                    navController.navigate(
                        LogoOcrDestination.ConfirmRecognition.name + "/$resultId"
                    )
                }
            )
        }

        composable(route = LogoOcrDestination.RegisterLogo.name) {
            RegisterLogoScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(route = LogoOcrDestination.History.name) {
            HistoryScreen(
                onBack = { navController.popBackStack() },
                onOpenResult = { resultId ->
                    navController.navigate(
                        LogoOcrDestination.ConfirmRecognition.name + "/$resultId"
                    )
                }
            )
        }

        composable(
            route = LogoOcrDestination.ConfirmRecognition.name + "/{resultId}",
            arguments = listOf(
                navArgument("resultId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) {
            ConfirmRecognitionScreen(
                onBack = { navController.popBackStack() },
                onRegisterNewLogo = {
                    navController.navigate(LogoOcrDestination.RegisterLogo.name)
                }
            )
        }
    }
}

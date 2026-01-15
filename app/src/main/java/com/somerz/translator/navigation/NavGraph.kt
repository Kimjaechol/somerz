package com.somerz.translator.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.somerz.translator.ui.screens.SettingsRoute
import com.somerz.translator.ui.screens.SettingsScreen
import com.somerz.translator.ui.screens.TranslatorRoute
import com.somerz.translator.ui.screens.TranslatorScreen
import com.somerz.translator.viewmodel.TranslatorViewModel

@Composable
fun TranslatorNavGraph() {
    val navController = rememberNavController()

    // Share ViewModel across screens
    val translatorViewModel: TranslatorViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = TranslatorRoute
    ) {
        composable<TranslatorRoute> {
            TranslatorScreen(
                onNavigateToSettings = {
                    navController.navigate(SettingsRoute)
                },
                viewModel = translatorViewModel
            )
        }

        composable<SettingsRoute> {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                viewModel = translatorViewModel
            )
        }
    }
}

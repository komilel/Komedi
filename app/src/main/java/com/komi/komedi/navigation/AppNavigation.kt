package com.komi.komedi.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.komi.komedi.KomediApplication
import com.komi.komedi.ui.screens.home.HomeScreen
import com.komi.komedi.ui.screens.add_edit_medication.AddEditMedicationScreen
import com.komi.komedi.ui.screens.medication_details.MedicationDetailsScreen
import com.komi.komedi.ui.screens.settings.SettingsScreen
import com.komi.komedi.ui.viewmodels.AddEditMedicationViewModel
import com.komi.komedi.ui.viewmodels.HomeViewModel
import com.komi.komedi.ui.viewmodels.MedicationDetailsViewModel
import com.komi.komedi.ui.viewmodels.SettingsViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val appContainer = (context.applicationContext as KomediApplication).appContainer

    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            val viewModel: HomeViewModel = viewModel(
                factory = HomeViewModel.Factory(
                    appContainer.medicationRepository,
                    appContainer.medicationLogRepository,
                    context
                )
            )
            HomeScreen(navController = navController, viewModel = viewModel)
        }

        composable(Screen.AddMedication.route) {
            val viewModel: AddEditMedicationViewModel = viewModel(
                factory = AddEditMedicationViewModel.Factory(
                    appContainer.medicationRepository,
                    appContainer.drugApiService,
                    context.applicationContext
                )
            )
            AddEditMedicationScreen(
                navController = navController,
                viewModel = viewModel,
                medicationId = null
            )
        }

        composable(
            route = Screen.EditMedication.route,
            arguments = listOf(navArgument("medicationId") { type = NavType.IntType })
        ) { backStackEntry ->
            val medicationId = backStackEntry.arguments?.getInt("medicationId") ?: return@composable
            val viewModel: AddEditMedicationViewModel = viewModel(
                factory = AddEditMedicationViewModel.Factory(
                    appContainer.medicationRepository,
                    appContainer.drugApiService,
                    context.applicationContext
                )
            )
            AddEditMedicationScreen(
                navController = navController,
                viewModel = viewModel,
                medicationId = medicationId
            )
        }

        composable(
            route = Screen.MedicationDetails.route,
            arguments = listOf(navArgument("medicationId") { type = NavType.IntType })
        ) { backStackEntry ->
            val medicationId = backStackEntry.arguments?.getInt("medicationId") ?: return@composable
            val viewModel: MedicationDetailsViewModel = viewModel(
                factory = MedicationDetailsViewModel.Factory(
                    appContainer.medicationRepository,
                    appContainer.medicationLogRepository,
                    context.applicationContext
                )
            )
            MedicationDetailsScreen(
                navController = navController,
                viewModel = viewModel,
                medicationId = medicationId
            )
        }

        composable(Screen.Settings.route) {
            val viewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.Factory(context)
            )
            SettingsScreen(navController = navController, viewModel = viewModel)
        }
    }
}

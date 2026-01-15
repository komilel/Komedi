package com.komi.komedi.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")

    object AddMedication : Screen("add_medication")

    object EditMedication : Screen("edit_medication/{medicationId}") {
        fun createRoute(medicationId: Int) = "edit_medication/$medicationId"
    }

    object MedicationDetails : Screen("medication_details/{medicationId}") {
        fun createRoute(medicationId: Int) = "medication_details/$medicationId"
    }

    object Settings : Screen("settings")
}

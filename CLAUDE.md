# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build the project
./gradlew build

# Build debug APK
./gradlew assembleDebug

# Install on connected device/emulator
./gradlew installDebug

# Run unit tests
./gradlew test

# Run a single unit test class
./gradlew test --tests "com.komi.komedi.ExampleUnitTest"

# Run instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest

# Clean build
./gradlew clean
```

## Architecture Overview

Komedi is an Android medication tracking app built with Kotlin and Jetpack Compose.

**Tech Stack:**
- Kotlin with Jetpack Compose for UI
- Room for local database persistence
- Ktor for HTTP client (OpenFDA drug info API)
- Navigation Compose for screen navigation
- ViewModel with StateFlow for state management
- WorkManager for background notification scheduling
- Manual dependency injection via AppContainer

**Project Structure:**

```
app/src/main/java/com/komi/komedi/
├── KomediApplication.kt    # Application class, initializes AppContainer & notifications
├── MainActivity.kt         # Single activity, hosts Compose navigation
├── di/
│   └── AppContainer.kt     # Manual DI container (repositories, API service)
├── data/
│   ├── local/
│   │   ├── Medication.kt       # Room entity for medications
│   │   ├── MedicationLog.kt    # Room entity for tracking taken medications
│   │   ├── MedicationDao.kt    # DAO interfaces for both entities
│   │   └── AppDatabase.kt      # Room database singleton
│   ├── remote/
│   │   └── DrugApiService.kt   # Ktor client for OpenFDA API
│   └── repository/
│       └── MedicationRepository.kt  # Repository classes
├── navigation/
│   ├── Screen.kt           # Sealed class defining routes with arguments
│   └── AppNavigation.kt    # NavHost setup with ViewModel creation
├── notification/
│   └── MedicationNotificationWorker.kt  # WorkManager for reminders
└── ui/
    ├── viewmodels/         # ViewModels for each screen
    │   ├── HomeViewModel.kt
    │   ├── AddEditMedicationViewModel.kt
    │   ├── MedicationDetailsViewModel.kt
    │   └── SettingsViewModel.kt
    ├── screens/            # Composable screens
    │   ├── home/
    │   ├── add_edit_medication/
    │   ├── medication_details/
    │   └── settings/
    └── theme/              # Material3 theming
```

**Key Patterns:**

- **MVVM Architecture**: Each screen has a corresponding ViewModel that exposes UI state via `StateFlow`
- **Repository pattern**: `MedicationRepository` and `MedicationLogRepository` wrap DAOs
- **Dependency injection**: `AppContainer` in `KomediApplication` provides dependencies
- **Navigation with arguments**: Use `Screen.MedicationDetails.createRoute(id)` for navigation with parameters
- **Room entities use `Flow`** for reactive data
- **API integration**: `DrugApiService` calls OpenFDA API for drug info lookup when adding medications

**Access dependencies from composables:**
```kotlin
val appContainer = (LocalContext.current.applicationContext as KomediApplication).appContainer
```

**ViewModel creation in navigation:**
```kotlin
val viewModel: HomeViewModel = viewModel(
    factory = HomeViewModel.Factory(
        appContainer.medicationRepository,
        appContainer.medicationLogRepository
    )
)
```

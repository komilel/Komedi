package com.komi.komedi.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit

object ThemeState {
    private const val PREFS_NAME = "komedi_settings"
    private const val KEY_DARK_MODE = "dark_mode"

    fun isDarkMode(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_DARK_MODE, false)
    }

    fun setDarkMode(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putBoolean(KEY_DARK_MODE, enabled) }
    }
}

@Composable
fun rememberDarkModeState(): State<Boolean> {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("komedi_settings", Context.MODE_PRIVATE)
    }

    val darkModeState = remember {
        mutableStateOf(prefs.getBoolean("dark_mode", false))
    }

    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "dark_mode") {
                darkModeState.value = prefs.getBoolean("dark_mode", false)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)

        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    return darkModeState
}

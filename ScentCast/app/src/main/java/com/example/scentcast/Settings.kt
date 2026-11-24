package com.example.scentcast

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Small user settings, saved in SharedPreferences. The values are Compose state, so
 * changing the theme on the settings screen updates every open screen straight away.
 */
object Settings {
    const val THEME_SYSTEM = 0
    const val THEME_LIGHT = 1
    const val THEME_DARK = 2

    private const val FILE = "scentcast_settings"
    private const val KEY_THEME = "theme"
    private const val KEY_SPRAYS = "sprays"
    private const val KEY_REMINDERS = "reminders"
    private const val KEY_DEMO = "demo_reminders"

    var theme by mutableIntStateOf(THEME_SYSTEM)
        private set
    var spraysPerWear by mutableIntStateOf(4)
        private set
    var remindersOn by mutableStateOf(true)
        private set
    var demoReminders by mutableStateOf(false) // minutes instead of hours
        private set

    /** Reads the saved values. Safe to call from every activity. */
    fun load(context: Context) {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        theme = prefs.getInt(KEY_THEME, THEME_SYSTEM)
        spraysPerWear = prefs.getInt(KEY_SPRAYS, 4)
        remindersOn = prefs.getBoolean(KEY_REMINDERS, true)
        demoReminders = prefs.getBoolean(KEY_DEMO, false)
    }

    fun saveRemindersOn(context: Context, value: Boolean) {
        remindersOn = value
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putBoolean(KEY_REMINDERS, value).apply()
    }

    fun saveDemoReminders(context: Context, value: Boolean) {
        demoReminders = value
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putBoolean(KEY_DEMO, value).apply()
    }

    // apply() writes to disk in the background, so the screen never waits for it.
    fun saveTheme(context: Context, value: Int) {
        theme = value
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putInt(KEY_THEME, value).apply()
    }

    fun saveSprays(context: Context, value: Int) {
        spraysPerWear = value
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putInt(KEY_SPRAYS, value).apply()
    }

    @Composable
    fun useDarkTheme(): Boolean = when (theme) {
        THEME_LIGHT -> false
        THEME_DARK -> true
        else -> isSystemInDarkTheme()
    }
}

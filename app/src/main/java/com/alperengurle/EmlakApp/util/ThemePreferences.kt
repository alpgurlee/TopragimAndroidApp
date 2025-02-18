package com.alperengurle.EmlakApp.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatActivity

object ThemePreferences {
    private const val PREF_NAME = "theme_preferences"
    private const val KEY_DARK_MODE = "dark_mode"

    fun setDarkMode(context: Context, isDarkMode: Boolean) {
        // SharedPreferences'e kaydet
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DARK_MODE, isDarkMode)
            .apply()

        // Temayı hemen uygula
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )

        // Activity'yi yenile
        (context as? AppCompatActivity)?.recreate()
    }

    fun isDarkMode(context: Context): Boolean {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_DARK_MODE, false)
    }
}
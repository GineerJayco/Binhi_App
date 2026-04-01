package com.example.binhi.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

object ThemeManager {
    private val isDarkMode: MutableState<Boolean> = mutableStateOf(false)

    fun setDarkMode(isDark: Boolean) {
        isDarkMode.value = isDark
    }

    fun toggleTheme() {
        isDarkMode.value = !isDarkMode.value
    }

    fun getCurrentTheme(): Boolean = isDarkMode.value

    fun getThemeIcon() = if (isDarkMode.value) Icons.Default.Brightness7 else Icons.Default.Brightness4

    fun getThemeLabel() = if (isDarkMode.value) "Light Mode" else "Dark Mode"
}


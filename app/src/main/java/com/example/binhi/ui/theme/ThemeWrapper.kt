package com.example.binhi.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.material3.MaterialTheme

@Composable
fun ThemedScreen(
    isDarkModeState: MutableState<Boolean>,
    content: @Composable () -> Unit
) {
    // The theme is already applied at the NavHost level in MainUI
    // This is a utility wrapper that can be used if needed for local theme adjustments
    content()
}

// Helper function to get theme-aware colors
@Composable
fun getThemedBackgroundColor(isDarkMode: Boolean): androidx.compose.ui.graphics.Color {
    return if (isDarkMode) {
        androidx.compose.ui.graphics.Color(0xFF121212) // Dark background
    } else {
        androidx.compose.ui.graphics.Color.White // Light background
    }
}

@Composable
fun getThemedTextColor(isDarkMode: Boolean): androidx.compose.ui.graphics.Color {
    return if (isDarkMode) {
        androidx.compose.ui.graphics.Color.White
    } else {
        androidx.compose.ui.graphics.Color.Black
    }
}

@Composable
fun getThemedSurfaceColor(isDarkMode: Boolean): androidx.compose.ui.graphics.Color {
    return if (isDarkMode) {
        androidx.compose.ui.graphics.Color(0xFF1E1E1E)
    } else {
        androidx.compose.ui.graphics.Color(0xFFF5F5F5)
    }
}


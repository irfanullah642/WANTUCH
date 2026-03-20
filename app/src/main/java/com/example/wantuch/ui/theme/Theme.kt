package com.example.wantuch.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val WantuchDarkScheme = darkColorScheme(
    primary          = Color(0xFF6366F1),
    onPrimary        = Color.White,
    secondary        = Color(0xFF4F46E5),
    onSecondary      = Color.White,
    tertiary         = Color(0xFF00F3FF),
    background       = Color(0xFF0F0C29),
    onBackground     = Color.White,
    surface          = Color(0xFF1E293B),
    onSurface        = Color.White,
    error            = Color(0xFFE74C3C),
    onError          = Color.White
)

@Composable
fun WANTUCHTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = WantuchDarkScheme,
        typography  = Typography,
        content     = content
    )
}
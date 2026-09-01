package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryGreen,
    secondary = OnSurfaceMuted,
    tertiary = LiquidOrange,
    background = TrueBlack,
    surface = DarkGrayBg,
    onPrimary = TrueBlack,
    onSecondary = TrueBlack,
    onTertiary = TrueBlack,
    onBackground = OnSurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceGray,
    onSurfaceVariant = OnSurfaceMuted
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // We enforce our signature True Black / Precision Dark Liquid Glass theme
    // regardless of system state, to preserve 100% fidelity to the source screens.
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}

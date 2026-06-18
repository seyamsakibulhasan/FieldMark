package com.fieldmark.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightScheme = lightColorScheme(
    primary = FieldBlue40,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = FieldBlue80,
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF001A41),
    secondary = FieldTeal40,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    tertiary = FieldAmber40,
    onTertiary = androidx.compose.ui.graphics.Color.White,
    background = SurfaceLight,
    onBackground = OnSurfaceLight,
    surface = androidx.compose.ui.graphics.Color.White,
    onSurface = OnSurfaceLight,
    error = ErrorRed
)

private val DarkScheme = darkColorScheme(
    primary = FieldBlue80,
    onPrimary = androidx.compose.ui.graphics.Color(0xFF002F66),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFF0A448F),
    onPrimaryContainer = FieldBlue80,
    secondary = FieldTeal80,
    onSecondary = androidx.compose.ui.graphics.Color(0xFF00382E),
    tertiary = FieldAmber80,
    onTertiary = androidx.compose.ui.graphics.Color(0xFF3D2E00),
    background = SurfaceDark,
    onBackground = OnSurfaceDark,
    surface = androidx.compose.ui.graphics.Color(0xFF161A20),
    onSurface = OnSurfaceDark,
    error = ErrorRed
)

@Composable
fun FieldMarkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> DarkScheme
        else -> LightScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = FieldMarkTypography,
        content = content
    )
}

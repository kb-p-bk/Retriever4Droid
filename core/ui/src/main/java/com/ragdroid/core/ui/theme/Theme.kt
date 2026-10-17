package com.ragdroid.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = RagPrimary,
    onPrimary = Color.White,
    primaryContainer = RagPrimaryDark,
    onPrimaryContainer = Color.White,
    secondary = RagSecondary,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF2B3A4A),
    onSecondaryContainer = Color.White,
    background = RagBackground,
    onBackground = Color.White,
    surface = RagSurface,
    onSurface = Color.White,
    surfaceVariant = RagSurfaceVariant,
    onSurfaceVariant = Color(0xFFE0E0E0),
    error = RagError,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    onPrimary = Color.White,
    primaryContainer = Purple80,
    secondary = PurpleGrey40,
    onSecondary = Color.White,
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    error = RagError
)

@Composable
fun RetrieverTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = RetrieverTypography,
        content = content
    )
}

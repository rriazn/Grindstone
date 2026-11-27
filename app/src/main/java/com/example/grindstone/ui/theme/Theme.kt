package com.example.grindstone.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = Ember40,
    primaryContainer = Ember90,
    onPrimaryContainer = Ember30,
    secondary = Stone40,
    secondaryContainer = Stone90,
    onSecondaryContainer = Stone30,
    tertiary = Slate40,
)

private val DarkColorScheme = darkColorScheme(
    primary = Ember80,
    onPrimary = Ember20,
    primaryContainer = Ember30,
    onPrimaryContainer = Ember90,
    secondary = Stone80,
    secondaryContainer = Stone30,
    onSecondaryContainer = Stone90,
    tertiary = Slate80,
)

@Composable
fun GrindstoneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
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
        typography = Typography,
        content = content,
    )
}

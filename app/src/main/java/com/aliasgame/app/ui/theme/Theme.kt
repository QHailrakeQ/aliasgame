package com.aliasgame.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.aliasgame.app.domain.model.AppStyle

/** The palette of the active [AppStyle]; provided by [AliasGameTheme]. */
val LocalGamePalette = staticCompositionLocalOf { AppStyle.DEFAULT.palette() }

object GameTheme {
    /** The palette of the style chosen in the settings. */
    val palette: GamePalette
        @Composable @ReadOnlyComposable
        get() = LocalGamePalette.current
}

/**
 * The app's theme. The game always uses its own dark, colorful look, so the system light/dark
 * setting and dynamic colors are deliberately not followed; [style] is chosen in the settings.
 */
@Composable
fun AliasGameTheme(
    style: AppStyle = AppStyle.DEFAULT,
    content: @Composable () -> Unit
) {
    val palette = style.palette()
    val colorScheme = remember(palette) {
        darkColorScheme(
            primary = palette.accent,
            onPrimary = palette.onAccent,
            secondary = palette.secondary,
            onSecondary = palette.onSecondary,
            tertiary = palette.accent,
            background = palette.backgroundTop,
            onBackground = Color.White,
            surface = palette.backgroundBottom,
            onSurface = Color.White
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = palette.backgroundTop.toArgb()
            window.navigationBarColor = palette.backgroundBottom.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = false
            insetsController.isAppearanceLightNavigationBars = false
        }
    }

    CompositionLocalProvider(LocalGamePalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

package com.ultraguru.liquidglass.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.ultraguru.liquidglass.data.ThemeMode

private val DarkScheme = darkColorScheme(
    primary = BrandRed,
    onPrimary = PaperWhite,
    primaryContainer = BrandRedDeep,
    onPrimaryContainer = PaperWhite,
    secondary = TextSecondaryDark,
    onSecondary = InkBlack,
    surface = DarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = DarkSurfaceHigh,
    onSurfaceVariant = TextSecondaryDark,
    background = DarkBg,
    onBackground = TextPrimaryDark,
    outline = DarkOutline,
    error = Color_AccentError
)

private val LightScheme = lightColorScheme(
    primary = BrandRedDeep,
    onPrimary = PaperWhite,
    primaryContainer = Color(0xFFFFDAD5),
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    background = LightBg,
    onBackground = TextPrimaryLight,
    surfaceVariant = LightSurfaceHigh,
    onSurfaceVariant = TextSecondaryLight,
    outline = LightOutline
)

private val Color_AccentError = androidx.compose.ui.graphics.Color(0xFFFFB4A9)

@Composable
fun GlassLabTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    val context = LocalContext.current
    val scheme = when {
        dynamicColor && android.os.Build.VERSION.SDK_INT >= 33 ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> DarkScheme
        else -> LightScheme
    }
    MaterialTheme(
        colorScheme = scheme,
        content = content
    )
}

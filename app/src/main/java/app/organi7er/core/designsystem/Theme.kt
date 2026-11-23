package app.organi7er.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import app.organi7er.core.model.ThemeMode

val LocalOrganiColors = staticCompositionLocalOf { LightOrganiColors }

object OrganiTheme {
    val colors: OrganiColors
        @Composable @ReadOnlyComposable
        get() = LocalOrganiColors.current
    val type: OrganiType get() = OrganiType
}

@Composable
fun ThemeMode.isDark(): Boolean = when (this) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

/**
 * Eigenes Theme. Das Material-Farbschema wird vollständig aus unseren Tokens gefüllt,
 * damit Picker, Sheets und Snackbars nie die Material-Standardfarben (Lila) zeigen.
 */
@Composable
fun OrganiTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val c = if (darkTheme) DarkOrganiColors else LightOrganiColors
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = c.primary, onPrimary = c.onPrimary,
            primaryContainer = Primitives.Navy700, onPrimaryContainer = c.text,
            secondary = c.brandBlue, onSecondary = Primitives.Navy900,
            secondaryContainer = Primitives.Navy700, onSecondaryContainer = c.text,
            tertiary = c.accent, onTertiary = c.onAccent,
            tertiaryContainer = Primitives.Yellow800, onTertiaryContainer = c.text,
            background = c.bg, onBackground = c.text,
            surface = c.surface, onSurface = c.text,
            surfaceVariant = c.surface2, onSurfaceVariant = c.textMuted,
            surfaceContainerLowest = c.bg, surfaceContainerLow = c.surface,
            surfaceContainer = c.surface, surfaceContainerHigh = c.surface,
            surfaceContainerHighest = c.surface2, surfaceBright = c.surface2, surfaceDim = c.bg,
            inverseSurface = Primitives.Paper, inverseOnSurface = Primitives.Navy900, inversePrimary = Primitives.Blue600,
            outline = c.border, outlineVariant = c.border,
            error = c.danger, onError = Color.White, scrim = Color(0xFF010814),
        )
    } else {
        lightColorScheme(
            primary = c.primary, onPrimary = c.onPrimary,
            primaryContainer = Primitives.Blue100, onPrimaryContainer = Primitives.Navy900,
            secondary = c.brandBlue, onSecondary = Color.White,
            secondaryContainer = Primitives.Blue50, onSecondaryContainer = Primitives.Navy900,
            tertiary = c.accent, onTertiary = c.onAccent,
            tertiaryContainer = Primitives.Yellow100, onTertiaryContainer = Primitives.Navy900,
            background = c.bg, onBackground = c.text,
            surface = c.surface, onSurface = c.text,
            surfaceVariant = c.surface2, onSurfaceVariant = c.textMuted,
            surfaceContainerLowest = Color.White, surfaceContainerLow = Color.White,
            surfaceContainer = Color.White, surfaceContainerHigh = Color.White,
            surfaceContainerHighest = c.surface2, surfaceBright = Color.White, surfaceDim = c.surface2,
            inverseSurface = Primitives.Navy900, inverseOnSurface = Color.White, inversePrimary = Primitives.Blue300,
            outline = c.border, outlineVariant = c.border,
            error = c.danger, onError = Color.White, scrim = Primitives.Navy900,
        )
    }
    CompositionLocalProvider(LocalOrganiColors provides c) {
        MaterialTheme(colorScheme = scheme, typography = OrganiMaterialTypography, content = content)
    }
}

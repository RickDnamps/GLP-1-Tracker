package com.rickdnamps.glp1tracker.widget

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.glance.material3.ColorProviders

/**
 * The app palette (ui/theme/Theme.kt, whose schemes are private) for Glance.
 * Each colour has a day and a night value; the launcher picks one.
 */
internal object WidgetTheme {
    private val light = lightColorScheme(
        primary = Color(0xFF00695C),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFA6F2E0),
        onPrimaryContainer = Color(0xFF00201B),
        secondary = Color(0xFF4A635D),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFCCE8E0),
        onSecondaryContainer = Color(0xFF06201B),
        tertiary = Color(0xFF7A5900),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFDEA6),
        onTertiaryContainer = Color(0xFF261900),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        background = Color(0xFFF4FBF8),
        onBackground = Color(0xFF161D1B),
        surface = Color(0xFFF4FBF8),
        onSurface = Color(0xFF161D1B),
        surfaceVariant = Color(0xFFDAE5E1),
        onSurfaceVariant = Color(0xFF3F4946),
        outline = Color(0xFF6F7976),
    )

    private val dark = darkColorScheme(
        primary = Color(0xFF8AD6C4),
        onPrimary = Color(0xFF00382F),
        primaryContainer = Color(0xFF005045),
        onPrimaryContainer = Color(0xFFA6F2E0),
        secondary = Color(0xFFB1CCC4),
        onSecondary = Color(0xFF1C352F),
        secondaryContainer = Color(0xFF334B45),
        onSecondaryContainer = Color(0xFFCCE8E0),
        tertiary = Color(0xFFF5BF48),
        onTertiary = Color(0xFF402D00),
        tertiaryContainer = Color(0xFF5C4200),
        onTertiaryContainer = Color(0xFFFFDEA6),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        background = Color(0xFF0E1513),
        onBackground = Color(0xFFDDE4E1),
        surface = Color(0xFF0E1513),
        onSurface = Color(0xFFDDE4E1),
        surfaceVariant = Color(0xFF3F4946),
        onSurfaceVariant = Color(0xFFBEC9C5),
        outline = Color(0xFF899390),
    )

    val colors = ColorProviders(light = light, dark = dark)
}

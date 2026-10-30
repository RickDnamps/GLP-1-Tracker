package com.rickdnamps.glp1tracker.ui.theme

import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.rickdnamps.glp1tracker.R
import java.time.LocalTime
import kotlinx.coroutines.delay
import com.rickdnamps.glp1tracker.domain.ThemeSchedule
import androidx.compose.runtime.produceState
import androidx.compose.runtime.getValue

/** Accent palettes the user can pick; neutrals stay shared so every accent looks polished. */
enum class Accent(
    @StringRes val label: Int,
    val light: Triple<Color, Color, Color>, // primary, primaryContainer, tertiary
    val dark: Triple<Color, Color, Color>,
    /** "Bright" theme: livelier primary, paler container, end colour of the hero gradient. */
    val bright: Triple<Color, Color, Color>,
) {
    TEAL(
        R.string.accent_teal,
        Triple(Color(0xFF00695C), Color(0xFFA6F2E0), Color(0xFF7A5900)), Triple(Color(0xFF6FE0C5), Color(0xFF005045), Color(0xFFF5BF48)),
        Triple(Color(0xFF00897B), Color(0xFFD5F6EE), Color(0xFF0097A7)),
    ),
    OCEAN(
        R.string.accent_ocean,
        Triple(Color(0xFF0B57D0), Color(0xFFD3E3FD), Color(0xFF00677D)), Triple(Color(0xFFA8C7FA), Color(0xFF0842A0), Color(0xFF5DD5F5)),
        Triple(Color(0xFF1A73E8), Color(0xFFE1ECFE), Color(0xFF0398DC)),
    ),
    VIOLET(
        R.string.accent_violet,
        Triple(Color(0xFF6750A4), Color(0xFFEADDFF), Color(0xFF7D5260)), Triple(Color(0xFFCFBCFF), Color(0xFF4F378B), Color(0xFFEFB8C8)),
        Triple(Color(0xFF7B5CD6), Color(0xFFF0E8FF), Color(0xFFAB47BC)),
    ),
    ROSE(
        R.string.accent_rose,
        Triple(Color(0xFFB4235F), Color(0xFFFFD9E2), Color(0xFF7C5800)), Triple(Color(0xFFFFB1C8), Color(0xFF8E0049), Color(0xFFF7BD48)),
        Triple(Color(0xFFD81B60), Color(0xFFFFE6EE), Color(0xFFF4511E)),
    ),
    SUNSET(
        R.string.accent_sunset,
        Triple(Color(0xFFB3261E), Color(0xFFFFDAD4), Color(0xFF8B5000)), Triple(Color(0xFFFFB4A9), Color(0xFF8C1D18), Color(0xFFFFB870)),
        Triple(Color(0xFFE53935), Color(0xFFFFE6E1), Color(0xFFEF6C00)),
    );

    companion object {
        fun fromName(name: String?) = entries.firstOrNull { it.name == name } ?: TEAL
    }
}

private fun onColorFor(c: Color): Color = if (c.luminance() > 0.5f) Color(0xFF111614) else Color.White

private fun Color.luminance(): Float = 0.2126f * red + 0.7152f * green + 0.0722f * blue

private fun lightScheme(a: Accent): ColorScheme {
    val (p, pc, t) = a.light
    return lightColorScheme(
        primary = p, onPrimary = Color.White,
        primaryContainer = pc, onPrimaryContainer = Color(0xFF0B1F1A),
        secondary = Color(0xFF4A635D), onSecondary = Color.White,
        secondaryContainer = Color(0xFFDCE8E3), onSecondaryContainer = Color(0xFF06201B),
        tertiary = t, onTertiary = Color.White,
        tertiaryContainer = Color(0xFFFFE3B0), onTertiaryContainer = Color(0xFF2A1C00),
        error = Color(0xFFBA1A1A), errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
        background = Color(0xFFF6F8F7), onBackground = Color(0xFF151A18),
        surface = Color(0xFFF6F8F7), onSurface = Color(0xFF151A18),
        surfaceVariant = Color(0xFFDDE5E1), onSurfaceVariant = Color(0xFF414946),
        outline = Color(0xFF717976), outlineVariant = Color(0xFFC1C9C5),
        surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFFFFFFF),
        surfaceContainer = Color(0xFFEFF3F1), surfaceContainerHigh = Color(0xFFE8EDEA),
        surfaceContainerHighest = Color(0xFFE1E7E4),
    )
}

/** White background, livelier accent, very light containers: for people who find the light theme too dark. */
private fun brightScheme(a: Accent): ColorScheme {
    val (p, pc, _) = a.bright
    return lightColorScheme(
        primary = p, onPrimary = Color.White,
        primaryContainer = pc, onPrimaryContainer = Color(0xFF0B1F1A),
        secondary = Color(0xFF52665F), onSecondary = Color.White,
        secondaryContainer = Color(0xFFEAF4F0), onSecondaryContainer = Color(0xFF0F2520),
        tertiary = a.light.third, onTertiary = Color.White,
        tertiaryContainer = Color(0xFFFFF2D8), onTertiaryContainer = Color(0xFF2A1C00),
        error = Color(0xFFBA1A1A), errorContainer = Color(0xFFFFE4E0), onErrorContainer = Color(0xFF410002),
        background = Color.White, onBackground = Color(0xFF1A1F1D),
        surface = Color.White, onSurface = Color(0xFF1A1F1D),
        surfaceVariant = Color(0xFFEBF0EE), onSurfaceVariant = Color(0xFF4A5350),
        outline = Color(0xFF7D8683), outlineVariant = Color(0xFFD7DEDB),
        surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFFCFDFD),
        surfaceContainer = Color(0xFFF6F9F8), surfaceContainerHigh = Color(0xFFF1F5F3),
        surfaceContainerHighest = Color(0xFFEAEFED),
    )
}

private fun darkScheme(a: Accent): ColorScheme {
    val (p, pc, t) = a.dark
    return darkColorScheme(
        primary = p, onPrimary = onColorFor(p),
        primaryContainer = pc, onPrimaryContainer = Color(0xFFE0F7F1),
        secondary = Color(0xFFB1CCC4), onSecondary = Color(0xFF1C352F),
        secondaryContainer = Color(0xFF2C3D39), onSecondaryContainer = Color(0xFFCCE8E0),
        tertiary = t, onTertiary = onColorFor(t),
        tertiaryContainer = Color(0xFF4A3A10), onTertiaryContainer = Color(0xFFFFE3B0),
        error = Color(0xFFFFB4AB), errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
        background = Color(0xFF0B0F0E), onBackground = Color(0xFFE1E6E3),
        surface = Color(0xFF0B0F0E), onSurface = Color(0xFFE1E6E3),
        surfaceVariant = Color(0xFF3F4946), onSurfaceVariant = Color(0xFFBEC9C5),
        outline = Color(0xFF899390), outlineVariant = Color(0xFF3F4946),
        surfaceContainerLowest = Color(0xFF070A09), surfaceContainerLow = Color(0xFF141917),
        surfaceContainer = Color(0xFF181D1B), surfaceContainerHigh = Color(0xFF222826),
        surfaceContainerHighest = Color(0xFF2D3330),
    )
}

@OptIn(ExperimentalTextApi::class)
private fun variable(res: Int, weight: Int) =
    Font(res, FontWeight(weight), variationSettings = FontVariation.Settings(FontVariation.weight(weight)))

/** Plus Jakarta Sans for text, Outfit for big numbers and titles (both SIL OFL). */
val BodyFont = FontFamily(
    variable(R.font.jakarta, 400), variable(R.font.jakarta, 500), variable(R.font.jakarta, 600), variable(R.font.jakarta, 700),
)
val DisplayFont = FontFamily(
    variable(R.font.outfit, 400), variable(R.font.outfit, 500), variable(R.font.outfit, 600), variable(R.font.outfit, 700), variable(R.font.outfit, 800),
)

private val AppTypography: Typography = Typography().let { t ->
    fun TextStyle.body() = copy(fontFamily = BodyFont)
    fun TextStyle.display(w: FontWeight = FontWeight.SemiBold) = copy(fontFamily = DisplayFont, fontWeight = w)
    t.copy(
        displayLarge = t.displayLarge.display(FontWeight.Bold),
        displayMedium = t.displayMedium.display(FontWeight.Bold),
        displaySmall = t.displaySmall.display(FontWeight.Bold),
        headlineLarge = t.headlineLarge.display(),
        headlineMedium = t.headlineMedium.display(),
        headlineSmall = t.headlineSmall.display(),
        titleLarge = t.titleLarge.display(),
        titleMedium = t.titleMedium.body().copy(fontWeight = FontWeight.SemiBold),
        titleSmall = t.titleSmall.body(),
        bodyLarge = t.bodyLarge.body(),
        bodyMedium = t.bodyMedium.body(),
        bodySmall = t.bodySmall.body(),
        labelLarge = t.labelLarge.body().copy(fontWeight = FontWeight.SemiBold),
        labelMedium = t.labelMedium.body(),
        labelSmall = t.labelSmall.body().copy(fontSize = 11.sp),
    )
}

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/** Extra brand colours not covered by Material: hero gradients, strength of the background glow. */
@Immutable
data class Brand(val hero: Brush, val heroAlt: Brush, val isDark: Boolean, val glow: Float = 1f)

val LocalBrand = staticCompositionLocalOf { Brand(Brush.linearGradient(listOf(Color.Gray, Color.Gray)), Brush.linearGradient(listOf(Color.Gray, Color.Gray)), false) }

/** Theme modes stored in settings: 0 system, 1 light, 2 dark, 3 bright (extra-light). */
const val THEME_BRIGHT = 3

@Composable
fun isDarkTheme(themeMode: Int): Boolean = when (themeMode) {
    1, THEME_BRIGHT -> false
    2 -> true
    else -> isSystemInDarkTheme()
}

/**
 * True during the night window when "dark in the evening" is on; re-checked every minute so the
 * theme flips by itself while the app is open.
 */
@Composable
fun rememberNightMode(enabled: Boolean, startMinutes: Int, endMinutes: Int): Boolean {
    val night by produceState(
        initialValue = enabled && ThemeSchedule.isNight(LocalTime.now(), startMinutes, endMinutes),
        enabled, startMinutes, endMinutes,
    ) {
        while (true) {
            value = enabled && ThemeSchedule.isNight(LocalTime.now(), startMinutes, endMinutes)
            if (!enabled) break
            delay(30_000)
        }
    }
    return night
}

@Composable
fun Glp1Theme(
    themeMode: Int = 0,
    accent: Accent = Accent.TEAL,
    dynamicColor: Boolean = false,
    /** Dark regardless of [themeMode] (night window of "dark in the evening"). */
    forceDark: Boolean = false,
    content: @Composable () -> Unit,
) {
    val dark = forceDark || isDarkTheme(themeMode)
    val bright = themeMode == THEME_BRIGHT && !dark
    val context = LocalContext.current
    val scheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> darkScheme(accent)
        bright -> brightScheme(accent)
        else -> lightScheme(accent)
    }
    val brand = Brand(
        hero = if (bright && !dynamicColor) Brush.linearGradient(listOf(accent.bright.first, accent.bright.third))
        else Brush.linearGradient(listOf(scheme.primary, scheme.tertiary.copy(alpha = 0.85f).compositeOver(scheme.primary))),
        heroAlt = Brush.linearGradient(listOf(scheme.primaryContainer, scheme.surfaceContainerHigh)),
        isDark = dark,
        glow = if (bright) 0.4f else 1f,
    )
    androidx.compose.runtime.CompositionLocalProvider(LocalBrand provides brand) {
        MaterialTheme(colorScheme = scheme, typography = AppTypography, shapes = AppShapes, content = content)
    }
}

private fun Color.compositeOver(background: Color): Color {
    val a = alpha
    return Color(
        red = red * a + background.red * (1 - a),
        green = green * a + background.green * (1 - a),
        blue = blue * a + background.blue * (1 - a),
        alpha = 1f,
    )
}

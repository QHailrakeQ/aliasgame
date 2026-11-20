package com.aliasgame.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.aliasgame.app.R
import com.aliasgame.app.domain.model.AppStyle

/**
 * Every color (and the two display fonts) a screen needs, for one [AppStyle].
 *
 * Screens read the active palette through [GameTheme.palette] instead of global constants,
 * so switching the style in the settings restyles the whole app at once.
 */
@Immutable
data class GamePalette(
    val backgroundTop: Color,
    val backgroundBottom: Color,
    /** Soft colored light in the upper corner of the background; transparent for none. */
    val backgroundGlowPrimary: Color,
    /** Soft colored light in the lower corner of the background; transparent for none. */
    val backgroundGlowSecondary: Color,
    /** Fill of cards, panels and secondary buttons. */
    val surface: Color,
    val surfaceBorder: Color,
    /** The main action of a screen ("Play", "Start game", "Next round"). */
    val accent: Color,
    val onAccent: Color,
    /** Information and selection: scores, selected chips, the running timer. */
    val secondary: Color,
    val onSecondary: Color,
    /** Border of secondary buttons such as the home menu entries. */
    val outline: Color,
    /** Border of large panels: the game HUD and dialogs. */
    val panelOutline: Color,
    /** The "got it" button. */
    val correct: Color,
    val onCorrect: Color,
    /** Tint of the word card while it is dragged towards "got it". */
    val correctTint: Color,
    /** Border of the "skip" button. */
    val skipOutline: Color,
    /** Neon styles draw a glow around outlines and text; the classic style stays flat. */
    val isNeon: Boolean,
    /** Font of big words: the word to explain, team names, main buttons. */
    val displayFont: FontFamily,
    /** Font of the app name on the home screen. */
    val titleFont: FontFamily
) {
    /** The glow to draw around something colored [color], or transparent when the style is flat. */
    fun glow(color: Color, strength: Float = 0.6f): Color =
        if (isNeon) color.copy(alpha = strength) else Color.Transparent
}

// Classic palette (the original look)
private val ClassicBgTop = Color(0xFF12005E)    // Deep Indigo
private val ClassicBgBottom = Color(0xFF311B92) // Deep Purple
private val ClassicAccent = Color(0xFFFF0266)   // Vibrant Magenta for CTAs
private val ClassicSecondary = Color(0xFF03DAC5)
private val GlassWhite = Color(0x1AFFFFFF)       // 10% White for glass effect
private val GlassWhiteBorder = Color(0x33FFFFFF) // 20% White for borders

// Neon palettes
private val NeonNight = Color(0xFF0A0518)
private val NeonNightBottom = Color(0xFF140733)
private val NeonPanel = Color(0xC70E0622)
private val NeonInk = Color(0xFF06200F)
private val NeonPink = Color(0xFFFF2E97)
private val NeonCyan = Color(0xFF22E8FF)
private val NeonPurple = Color(0xFFA855F7)
private val NeonMint = Color(0xFF4DFF9A)
private val NeonViolet = Color(0xFFA23BFF)
private val NeonLime = Color(0xFFC6FF3D)
private val NeonIndigo = Color(0xFF7C5CFF)
private val NeonAqua = Color(0xFF3DFFC8)

private val NeonDisplayFont = FontFamily(Font(R.font.russo_one_regular))
private val NeonTitleFont = FontFamily(Font(R.font.monoton_regular))

val ClassicPalette = GamePalette(
    backgroundTop = ClassicBgTop,
    backgroundBottom = ClassicBgBottom,
    backgroundGlowPrimary = Color.Transparent,
    backgroundGlowSecondary = Color.Transparent,
    surface = GlassWhite,
    surfaceBorder = GlassWhiteBorder,
    accent = ClassicAccent,
    onAccent = Color.White,
    secondary = ClassicSecondary,
    onSecondary = ClassicBgTop,
    outline = GlassWhiteBorder,
    panelOutline = GlassWhiteBorder,
    correct = ClassicAccent,
    onCorrect = Color.White,
    correctTint = Color(0xFF4CAF50),
    skipOutline = GlassWhiteBorder,
    isNeon = false,
    displayFont = FontFamily.Default,
    titleFont = FontFamily.Default
)

val NeonPinkPalette = GamePalette(
    backgroundTop = NeonNight,
    backgroundBottom = NeonNightBottom,
    backgroundGlowPrimary = NeonPink.copy(alpha = 0.30f),
    backgroundGlowSecondary = NeonCyan.copy(alpha = 0.16f),
    surface = NeonPanel,
    surfaceBorder = NeonPurple.copy(alpha = 0.55f),
    accent = NeonPink,
    onAccent = Color.White,
    secondary = NeonCyan,
    onSecondary = NeonNight,
    outline = NeonCyan,
    panelOutline = NeonPurple,
    correct = NeonMint,
    onCorrect = NeonInk,
    correctTint = NeonMint,
    skipOutline = NeonPink,
    isNeon = true,
    displayFont = NeonDisplayFont,
    titleFont = NeonTitleFont
)

val NeonVioletPalette = NeonPinkPalette.copy(
    backgroundGlowPrimary = NeonViolet.copy(alpha = 0.32f),
    backgroundGlowSecondary = NeonLime.copy(alpha = 0.12f),
    surfaceBorder = NeonIndigo.copy(alpha = 0.6f),
    accent = NeonViolet,
    secondary = NeonLime,
    outline = NeonLime,
    panelOutline = NeonIndigo,
    correct = NeonAqua,
    correctTint = NeonAqua,
    skipOutline = NeonViolet
)

fun AppStyle.palette(): GamePalette = when (this) {
    AppStyle.CLASSIC -> ClassicPalette
    AppStyle.NEON_PINK -> NeonPinkPalette
    AppStyle.NEON_VIOLET -> NeonVioletPalette
}

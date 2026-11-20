package com.aliasgame.app.presentation.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aliasgame.app.R
import com.aliasgame.app.ui.components.PartyBackground
import com.aliasgame.app.ui.components.PartyScene
import com.aliasgame.app.ui.components.neonGlow
import com.aliasgame.app.ui.components.withGlow
import com.aliasgame.app.ui.theme.*

@Composable
fun HomeScreen(
    onPlayClick: () -> Unit,
    onRulesClick: () -> Unit,
    onStatisticsClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val palette = GameTheme.palette
    BoxWithConstraints {
        val availableHeight = this.maxHeight
        val compact = availableHeight < 640.dp
        PartyBackground {
        if (palette.isNeon) {
            PartyScene(
                accent = palette.accent,
                beam = palette.secondary,
                rimLight = palette.accent.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxSize()
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = availableHeight)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = GameSpacing.screenHorizontal, vertical = GameSpacing.screenVertical)
                // Lifts the menu above the crowd at the bottom of the neon styles.
                .padding(bottom = if (palette.isNeon && !compact) 140.dp else 0.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AppTitle(compact = compact)

            Spacer(modifier = Modifier.height(if (compact) 40.dp else 56.dp))

            Button(
                onClick = onPlayClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 72.dp)
                    .neonGlow(palette.glow(palette.accent), GameShapeSize.card, glowRadius = 22.dp),
                shape = RoundedCornerShape(GameShapeSize.card),
                border = BorderStroke(1.dp, Color.White.copy(alpha = if (palette.isNeon) 0.35f else 0.18f)),
                colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = palette.onAccent),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = if (palette.isNeon) 0.dp else 4.dp)
            ) {
                Text(
                    text = stringResource(R.string.menu_play),
                    style = Typography.headlineMedium.copy(fontSize = 22.sp, fontFamily = palette.displayFont),
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(GameSpacing.cardGap))

            MenuButton(
                text = stringResource(R.string.menu_rules),
                onClick = onRulesClick
            )

            Spacer(modifier = Modifier.height(GameSpacing.controlGap))

            MenuButton(
                text = stringResource(R.string.menu_statistics),
                onClick = onStatisticsClick
            )

            Spacer(modifier = Modifier.height(GameSpacing.controlGap))

            MenuButton(
                text = stringResource(R.string.menu_settings),
                onClick = onSettingsClick
            )
        }
        }
    }
}

/**
 * The game's name, always the English "ALIAS". The neon styles light it like a neon sign: a
 * wide colored halo drawn by a second copy behind the text, plus a tight glow on the letters.
 */
@Composable
private fun AppTitle(compact: Boolean) {
    val palette = GameTheme.palette
    val title = stringResource(R.string.app_name).uppercase()
    val style = if (palette.isNeon) {
        TextStyle(
            fontFamily = palette.titleFont,
            fontSize = if (compact) 48.sp else 60.sp,
            letterSpacing = 2.sp,
            textAlign = TextAlign.Center
        )
    } else {
        Typography.displaySmall.copy(
            fontSize = if (compact) 32.sp else 40.sp,
            letterSpacing = 2.sp,
            textAlign = TextAlign.Center
        )
    }
    Box(contentAlignment = Alignment.Center) {
        if (palette.isNeon) {
            Text(
                text = title,
                modifier = Modifier.clearAndSetSemantics {},
                style = style.withGlow(palette.accent, blurRadius = 60f),
                color = palette.accent.copy(alpha = 0.6f),
                maxLines = 1
            )
        }
        Text(
            text = title,
            style = style.withGlow(palette.glow(palette.accent, strength = 1f), blurRadius = 16f),
            color = Color.White,
            maxLines = 2
        )
    }
}

@Composable
private fun MenuButton(text: String, onClick: () -> Unit) {
    val palette = GameTheme.palette
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .neonGlow(palette.glow(palette.outline, strength = 0.5f), GameShapeSize.control, glowRadius = 10.dp),
        shape = RoundedCornerShape(GameShapeSize.control),
        color = palette.surface,
        border = BorderStroke(if (palette.isNeon) 1.5.dp else 1.dp, palette.outline)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = Typography.titleMedium.copy(letterSpacing = if (palette.isNeon) 1.5.sp else 0.1.sp),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

package com.aliasgame.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aliasgame.app.R
import com.aliasgame.app.ui.theme.GameShapeSize
import com.aliasgame.app.ui.theme.GameSpacing
import com.aliasgame.app.ui.theme.GameTheme
import com.aliasgame.app.ui.theme.Typography

@Composable
fun PartyBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val palette = GameTheme.palette
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(palette.backgroundTop, palette.backgroundBottom)))
            .drawBehind {
                // Soft colored light in two corners; both are transparent in the classic style.
                if (palette.backgroundGlowPrimary.alpha > 0f) {
                    val center = Offset(0f, size.height * 0.08f)
                    val radius = size.width * 0.95f
                    drawCircle(
                        Brush.radialGradient(listOf(palette.backgroundGlowPrimary, Color.Transparent), center, radius),
                        radius,
                        center
                    )
                }
                if (palette.backgroundGlowSecondary.alpha > 0f) {
                    val center = Offset(size.width, size.height * 0.9f)
                    val radius = size.width * 1.05f
                    drawCircle(
                        Brush.radialGradient(listOf(palette.backgroundGlowSecondary, Color.Transparent), center, radius),
                        radius,
                        center
                    )
                }
            },
        content = content
    )
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val palette = GameTheme.palette
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .neonGlow(palette.glow(palette.surfaceBorder, strength = 0.35f), GameShapeSize.card, glowRadius = 10.dp),
        shape = RoundedCornerShape(GameShapeSize.card),
        color = palette.surface,
        border = BorderStroke(1.dp, palette.surfaceBorder)
    ) {
        content()
    }
}

@Composable
fun PartyScreenHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = GameTheme.palette
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(GameShapeSize.control),
            color = palette.surface,
            border = BorderStroke(1.dp, if (palette.isNeon) palette.outline else palette.surfaceBorder)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.content_desc_back),
                    tint = Color.White
                )
            }
        }
        Spacer(modifier = Modifier.width(GameSpacing.controlGap))
        Text(
            text = title,
            style = Typography.titleLarge.copy(fontFamily = palette.displayFont)
                .withGlow(palette.glow(palette.accent), blurRadius = 18f),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            maxLines = 2
        )
    }
}

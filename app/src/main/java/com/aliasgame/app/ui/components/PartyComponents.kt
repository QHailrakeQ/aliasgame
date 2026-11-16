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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aliasgame.app.R
import com.aliasgame.app.ui.theme.BgEnd
import com.aliasgame.app.ui.theme.BgStart
import com.aliasgame.app.ui.theme.GameShapeSize
import com.aliasgame.app.ui.theme.GameSpacing
import com.aliasgame.app.ui.theme.GlassWhite
import com.aliasgame.app.ui.theme.GlassWhiteBorder
import com.aliasgame.app.ui.theme.GameSecondary
import com.aliasgame.app.ui.theme.Typography

@Composable
fun PartyBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgStart, BgEnd))),
        content = content
    )
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(GameShapeSize.card),
        color = GlassWhite,
        border = BorderStroke(1.dp, GlassWhiteBorder)
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
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(GameShapeSize.control),
            color = GlassWhite,
            border = BorderStroke(1.dp, GlassWhiteBorder)
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
            style = Typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            maxLines = 2
        )
    }
}

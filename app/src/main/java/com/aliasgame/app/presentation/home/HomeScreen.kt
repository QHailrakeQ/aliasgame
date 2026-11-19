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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aliasgame.app.R
import com.aliasgame.app.ui.components.PartyBackground
import com.aliasgame.app.ui.theme.*

@Composable
fun HomeScreen(
    onPlayClick: () -> Unit,
    onRulesClick: () -> Unit,
    onStatisticsClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    BoxWithConstraints {
        val availableHeight = this.maxHeight
        val compact = availableHeight < 640.dp
        PartyBackground {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = availableHeight)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = GameSpacing.screenHorizontal, vertical = GameSpacing.screenVertical),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.app_name).uppercase(),
                style = Typography.displaySmall.copy(
                    fontSize = if (compact) 32.sp else 40.sp,
                    letterSpacing = 2.sp
                ),
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(if (compact) 40.dp else 64.dp))

            Button(
                onClick = onPlayClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 72.dp),
                shape = RoundedCornerShape(GameShapeSize.card),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                colors = ButtonDefaults.buttonColors(containerColor = GameAccent),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text(
                    text = stringResource(R.string.menu_play),
                    style = Typography.headlineMedium.copy(fontSize = 22.sp),
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

@Composable
private fun MenuButton(text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp),
        shape = RoundedCornerShape(GameShapeSize.control),
        color = GlassWhite,
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassWhiteBorder)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = Typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

package com.aliasgame.app.presentation.rules

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Score
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aliasgame.app.R
import com.aliasgame.app.ui.components.GlassCard
import com.aliasgame.app.ui.components.PartyBackground
import com.aliasgame.app.ui.components.PartyScreenHeader
import com.aliasgame.app.ui.theme.*

/**
 * Rules Screen with consistent branding and staggered entrance animations.
 */
@Composable
fun RulesScreen(
    onBack: () -> Unit
) {
    PartyBackground {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = GameSpacing.screenHorizontal),
            verticalArrangement = Arrangement.spacedBy(GameSpacing.cardGap),
            contentPadding = PaddingValues(
                top = GameSpacing.screenVertical,
                bottom = GameSpacing.section
            )
        ) {
            item {
                PartyScreenHeader(
                    title = stringResource(R.string.rules_title),
                    onBack = onBack
                )
            }

            item {
                RuleItem(
                    icon = Icons.Default.Flag,
                    title = stringResource(R.string.rules_section_goal),
                    description = stringResource(R.string.rules_desc_goal)
                )
            }

            item {
                RuleItem(
                    icon = Icons.Default.Score,
                    title = stringResource(R.string.rules_section_scoring),
                    description = stringResource(R.string.rules_desc_scoring)
                )
            }

            item {
                RuleItem(
                    icon = Icons.Default.Timer,
                    title = stringResource(R.string.rules_section_final_word),
                    description = stringResource(R.string.rules_desc_final_word)
                )
            }

            item {
                RuleItem(
                    icon = Icons.Default.EmojiEvents,
                    title = stringResource(R.string.rules_section_winning),
                    description = stringResource(R.string.rules_desc_winning)
                )
            }
        }
    }
}

@Composable
private fun RuleItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    GlassCard {
        Column(modifier = Modifier.padding(GameSpacing.cardPadding)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = GameTheme.palette.secondary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    text = title.uppercase(),
                    style = Typography.labelLarge,
                    color = Color.White.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = description,
                style = Typography.bodyLarge,
                color = Color.White,
                lineHeight = 22.sp
            )
        }
    }
}

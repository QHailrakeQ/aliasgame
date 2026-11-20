package com.aliasgame.app.presentation.game

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aliasgame.app.R
import com.aliasgame.app.domain.model.Team
import com.aliasgame.app.ui.theme.*
import com.aliasgame.app.ui.components.neonGlow
import com.aliasgame.app.ui.components.withGlow

@Composable
internal fun GameHUD(
    currentTeamId: String?,
    teamName: String,
    teams: List<Team>,
    score: Int,
    timeRemaining: Long,
    roundTime: Long,
    onExitClick: () -> Unit
) {
    val isUrgent = timeRemaining < URGENT_SECONDS
    val palette = GameTheme.palette
    // In the neon styles the whole panel turns to the accent color while time is running out.
    val panelOutline by animateColorAsState(
        targetValue = when {
            !palette.isNeon -> palette.surfaceBorder
            isUrgent -> palette.accent
            else -> palette.panelOutline
        },
        label = "PanelOutline"
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .neonGlow(palette.glow(panelOutline, strength = 0.45f), GameShapeSize.card),
        shape = RoundedCornerShape(GameShapeSize.card),
        color = palette.surface,
        border = BorderStroke(if (palette.isNeon) 1.5.dp else 1.dp, panelOutline)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(GameSpacing.controlGap)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onExitClick,
                    modifier = Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Close,
                        contentDescription = stringResource(R.string.content_desc_exit),
                        tint = Color.White
                    )
                }

                Column(
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = teamName.uppercase(),
                        style = Typography.titleMedium
                            .copy(letterSpacing = 1.sp, fontFamily = palette.displayFont)
                            .withGlow(palette.glow(palette.accent), blurRadius = 20f),
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.turn_score_label, score),
                        style = Typography.labelLarge,
                        color = GameTheme.palette.secondary
                    )
                }

                val timerBorderColor by animateColorAsState(
                    targetValue = when {
                        isUrgent -> palette.accent
                        palette.isNeon -> palette.secondary
                        else -> Color.White.copy(alpha = 0.2f)
                    },
                    label = "TimerBorder"
                )

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isUrgent) GameTheme.palette.accent.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f),
                    modifier = Modifier
                        .widthIn(min = 64.dp)
                        .neonGlow(palette.glow(timerBorderColor), 16.dp, glowRadius = if (isUrgent) 18.dp else 10.dp),
                    border = BorderStroke(if (isUrgent || palette.isNeon) 2.dp else 1.dp, timerBorderColor)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (isUrgent) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp).padding(end = 4.dp)
                            )
                        }
                        Text(
                            text = timeRemaining.toString(),
                            style = Typography.titleMedium
                                .copy(fontFamily = palette.displayFont)
                                .withGlow(palette.glow(timerBorderColor), blurRadius = 16f),
                            color = Color.White,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                teams.forEach { team ->
                    val teamScoreDescription = stringResource(
                        R.string.team_score_description,
                        team.name,
                        team.score
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (team.id == currentTeamId) {
                            GameTheme.palette.secondary.copy(alpha = 0.22f)
                        } else {
                            Color.White.copy(alpha = 0.07f)
                        },
                        modifier = Modifier.semantics(mergeDescendants = true) {
                            contentDescription = teamScoreDescription
                        },
                        border = BorderStroke(
                            1.dp,
                            if (team.id == currentTeamId) GameTheme.palette.secondary.copy(alpha = 0.7f)
                            else Color.White.copy(alpha = 0.12f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = team.name,
                                style = Typography.labelSmall,
                                color = Color.White.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = team.score.toString(),
                                style = Typography.labelMedium,
                                color = GameTheme.palette.secondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            TimerProgressBar(
                timeRemaining = timeRemaining,
                roundTime = roundTime,
                isUrgent = isUrgent
            )
        }
    }
}

/**
 * A thin bar showing the share of the turn that is left, readable from the corner of the eye
 * while explaining a word. It glides down over each second instead of jumping once per tick,
 * and refills instantly when a new turn starts.
 */
@Composable
private fun TimerProgressBar(timeRemaining: Long, roundTime: Long, isUrgent: Boolean) {
    val target = if (roundTime > 0) (timeRemaining.toFloat() / roundTime).coerceIn(0f, 1f) else 0f
    val progress = remember { Animatable(target) }
    LaunchedEffect(target) {
        if (target >= progress.value) {
            progress.snapTo(target)
        } else {
            progress.animateTo(target, tween(durationMillis = 1000, easing = LinearEasing))
        }
    }
    val color by animateColorAsState(
        targetValue = if (isUrgent) GameTheme.palette.accent else GameTheme.palette.secondary,
        label = "TimerBarColor"
    )
    LinearProgressIndicator(
        progress = { progress.value },
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            // The remaining seconds are already announced by the timer text.
            .clearAndSetSemantics {},
        color = color,
        trackColor = Color.White.copy(alpha = 0.12f),
        strokeCap = StrokeCap.Round,
        drawStopIndicator = {}
    )
}

/** Below this many seconds the timer is highlighted as running out. */
private const val URGENT_SECONDS = 10

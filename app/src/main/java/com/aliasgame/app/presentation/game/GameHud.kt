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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aliasgame.app.R
import com.aliasgame.app.domain.model.Team
import com.aliasgame.app.ui.theme.*

@Composable
internal fun GameHUD(
    currentTeamId: String?,
    teamName: String,
    teams: List<Team>,
    score: Int,
    timeRemaining: Long,
    onExitClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        shape = RoundedCornerShape(GameShapeSize.card),
        color = GlassWhite,
        border = BorderStroke(1.dp, GlassWhiteBorder)
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
                        style = Typography.titleMedium.copy(letterSpacing = 1.sp),
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.turn_score_label, score),
                        style = Typography.labelLarge,
                        color = GameSecondary
                    )
                }

                val isUrgent = timeRemaining < 10
                val timerBorderColor by animateColorAsState(
                    targetValue = if (isUrgent) GameAccent else Color.White.copy(alpha = 0.2f),
                    label = "TimerBorder"
                )

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isUrgent) GameAccent.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f),
                    modifier = Modifier.widthIn(min = 64.dp),
                    border = BorderStroke(if (isUrgent) 2.dp else 1.dp, timerBorderColor)
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
                            style = Typography.titleMedium,
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
                            GameSecondary.copy(alpha = 0.22f)
                        } else {
                            Color.White.copy(alpha = 0.07f)
                        },
                        modifier = Modifier.semantics(mergeDescendants = true) {
                            contentDescription = teamScoreDescription
                        },
                        border = BorderStroke(
                            1.dp,
                            if (team.id == currentTeamId) GameSecondary.copy(alpha = 0.7f)
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
                                color = GameSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

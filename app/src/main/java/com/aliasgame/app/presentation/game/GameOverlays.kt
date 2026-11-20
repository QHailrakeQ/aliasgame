package com.aliasgame.app.presentation.game

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.aliasgame.app.R
import com.aliasgame.app.domain.model.RoundResult
import com.aliasgame.app.domain.model.Team
import com.aliasgame.app.ui.theme.*
import com.aliasgame.app.ui.components.neonGlow
import com.aliasgame.app.ui.components.withGlow

/**
 * Makes the overlay a touch target so that controls drawn underneath cannot be tapped
 * through it. A Box with only a background does not take part in hit testing in Compose;
 * with a pointer input handler it does, and Compose then delivers the touch to the overlay
 * and its children only, never to the siblings below it.
 *
 * The events are deliberately observed, not consumed: a button cancels its click when a
 * parent consumes the small moves a real finger makes during a tap, which made the overlay's
 * own buttons ignore taps on devices.
 */
internal fun Modifier.blockUnderlyingTouches(): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinalWordDialog(
    word: String,
    teams: List<Team>,
    onTeamSelected: (String?) -> Unit,
    onRequestExit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onRequestExit,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false
        ),
        modifier = Modifier.padding(GameSpacing.screenHorizontal).clip(
            RoundedCornerShape(GameShapeSize.card)
        ),
        content = {
            Surface(
                color = GameTheme.palette.backgroundBottom,
                border = BorderStroke(1.dp, GameTheme.palette.panelOutline),
                shape = RoundedCornerShape(GameShapeSize.card)
            ) {
                Column(
                    modifier = Modifier
                        .heightIn(max = 640.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(GameSpacing.cardPadding),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.last_word_label, word).uppercase(),
                        style = Typography.titleLarge,
                        textAlign = TextAlign.Center,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = stringResource(R.string.who_got_point),
                        style = Typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    teams.forEach { team ->
                        Button(
                            onClick = { onTeamSelected(team.id) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(vertical = 4.dp),
                            shape = RoundedCornerShape(GameShapeSize.control),
                            colors = ButtonDefaults.buttonColors(containerColor = GameTheme.palette.surface, contentColor = Color.White),
                            border = if (GameTheme.palette.isNeon) BorderStroke(1.5.dp, GameTheme.palette.outline) else null
                        ) { Text(team.name, fontWeight = FontWeight.Bold) }
                    }
                    
                    Button(
                        onClick = { onTeamSelected(null) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(vertical = 4.dp),
                        shape = RoundedCornerShape(GameShapeSize.control),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                    ) { 
                        Text(
                            text = stringResource(R.string.no_one_correct),
                            color = Color.White.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    )
}

@Composable
internal fun PauseOverlay(onContinue: () -> Unit) {
    Box(
        Modifier.fillMaxSize().blockUnderlyingTouches().background(Color.Black.copy(alpha = 0.72f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.padding(GameSpacing.cardPadding),
            shape = RoundedCornerShape(GameShapeSize.card),
            color = GameTheme.palette.backgroundTop,
            border = BorderStroke(1.dp, GameTheme.palette.panelOutline)
        ) {
            Column(
                Modifier.padding(GameSpacing.cardPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.pause_title).uppercase(),
                    style = Typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = onContinue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .neonGlow(GameTheme.palette.glow(GameTheme.palette.accent), 20.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GameTheme.palette.accent, contentColor = GameTheme.palette.onAccent)
                ) {
                    Text(stringResource(R.string.resume_button).uppercase(), fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
internal fun RoundResultsOverlay(results: List<RoundResult>, teams: List<Team>, onToggleResult: (Int) -> Unit, onNextRound: () -> Unit) {
    Box(
        Modifier.fillMaxSize().blockUnderlyingTouches().background(GameTheme.palette.backgroundTop.copy(alpha = 0.95f)).padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(GameShapeSize.card),
            color = GameTheme.palette.surface,
            border = BorderStroke(1.dp, GameTheme.palette.surfaceBorder)
        ) {
            Column(
                Modifier.fillMaxSize().padding(GameSpacing.cardPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.round_over).uppercase(),
                    style = Typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(GameSpacing.cardGap))
                
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(GameSpacing.cardGap)
                ) {
                    teams.forEach { team ->
                        Column(
                            modifier = Modifier.widthIn(min = 72.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                team.name,
                                style = Typography.labelLarge,
                                color = Color.White.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                team.score.toString(),
                                style = Typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = GameTheme.palette.secondary
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(GameSpacing.cardGap))
                Text(stringResource(R.string.tap_to_toggle), style = Typography.bodySmall, color = Color.White.copy(alpha = 0.5f))
                
                val correctDesc = stringResource(R.string.content_desc_correct_icon)
                val wrongDesc = stringResource(R.string.content_desc_wrong_icon)

                LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                    itemsIndexed(results) { index, result ->
                        val isLastWord = index == results.size - 1
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                .then(
                                    if (!isLastWord) {
                                        Modifier.clickable { onToggleResult(index) }
                                            .semantics { role = Role.Button }
                                    } else {
                                        Modifier
                                    }
                                )
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = result.word.text + if (isLastWord) " (${stringResource(R.string.got_it_button)})" else "",
                                modifier = Modifier.weight(1f).padding(end = GameSpacing.controlGap),
                                style = Typography.bodyLarge,
                                color = if (isLastWord) Color.White.copy(alpha = 0.7f) else Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(
                                imageVector = if (result.isCorrect) Icons.Default.Check else Icons.Default.Close,
                                contentDescription = if (result.isCorrect) correctDesc else wrongDesc,
                                tint = if (result.isCorrect) GameTheme.palette.secondary else GameTheme.palette.accent
                            )
                        }
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onNextRound,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .neonGlow(GameTheme.palette.glow(GameTheme.palette.accent), GameShapeSize.control),
                    shape = RoundedCornerShape(GameShapeSize.control),
                    colors = ButtonDefaults.buttonColors(containerColor = GameTheme.palette.accent, contentColor = GameTheme.palette.onAccent)
                ) {
                    Text(stringResource(R.string.next_round).uppercase(), style = Typography.titleMedium, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
internal fun ReadyOverlay(teamName: String, onStart: () -> Unit) {
    Box(
        Modifier.fillMaxSize().blockUnderlyingTouches().background(GameTheme.palette.backgroundTop.copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .padding(GameSpacing.cardPadding)
                .neonGlow(GameTheme.palette.glow(GameTheme.palette.secondary, strength = 0.5f), GameShapeSize.card, glowRadius = 18.dp),
            shape = RoundedCornerShape(GameShapeSize.card),
            color = GameTheme.palette.surface,
            border = BorderStroke(2.dp, GameTheme.palette.secondary)
        ) {
            Column(
                Modifier.padding(GameSpacing.cardPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(R.string.ready_title).uppercase(), style = Typography.labelLarge, color = Color.White.copy(alpha = 0.7f))
                Text(teamName.uppercase(), style = Typography.displaySmall.copy(fontFamily = GameTheme.palette.displayFont).withGlow(GameTheme.palette.glow(GameTheme.palette.accent), blurRadius = 28f), fontWeight = FontWeight.Black, color = Color.White, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(GameSpacing.section))
                Button(
                    onClick = onStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp)
                        .neonGlow(GameTheme.palette.glow(GameTheme.palette.secondary), GameShapeSize.control),
                    shape = RoundedCornerShape(GameShapeSize.control),
                    colors = ButtonDefaults.buttonColors(containerColor = GameTheme.palette.secondary)
                ) {
                    Text(stringResource(R.string.ready_button).uppercase(), style = Typography.titleLarge, color = GameTheme.palette.onSecondary, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

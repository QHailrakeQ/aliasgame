package com.aliasgame.app.presentation.game

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aliasgame.app.R
import com.aliasgame.app.domain.model.Team
import com.aliasgame.app.ui.theme.*
import kotlin.math.sin
import kotlin.random.Random

@Composable
internal fun VictoryOverlay(
    winners: List<Team>,
    onNewGame: () -> Unit,
    onExit: () -> Unit
) {
    val isDraw = winners.size > 1
    Box(
        modifier = Modifier
            .fillMaxSize()
            .blockUnderlyingTouches()
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to Color(0xFF35105F),
                        0.45f to Color(0xFF1F0B4D),
                        1f to Color(0xFF10052E)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(GameAccent.copy(alpha = 0.32f), Color.Transparent),
                    center = Offset(size.width * 0.5f, size.height * 0.22f),
                    radius = size.width * 0.82f
                ),
                radius = size.width * 0.82f,
                center = Offset(size.width * 0.5f, size.height * 0.22f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(GameSecondary.copy(alpha = 0.18f), Color.Transparent),
                    center = Offset(size.width * 0.5f, size.height * 0.82f),
                    radius = size.width * 0.72f
                ),
                radius = size.width * 0.72f,
                center = Offset(size.width * 0.5f, size.height * 0.82f)
            )
        }
        ConfettiEffect()
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Surface(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(144.dp),
                shape = CircleShape,
                color = Color(0xFF29145A).copy(alpha = 0.88f),
                border = BorderStroke(2.dp, Color(0xFFFFD166).copy(alpha = 0.82f)),
                shadowElevation = 28.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Surface(
                        modifier = Modifier.size(116.dp),
                        shape = CircleShape,
                        color = GameAccent.copy(alpha = 0.10f),
                        border = BorderStroke(1.dp, GameSecondary.copy(alpha = 0.30f))
                    ) {}
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = stringResource(R.string.winner_title),
                        tint = Color(0xFFFFD166),
                        modifier = Modifier.size(82.dp)
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = if (isDraw) {
                    stringResource(R.string.draw_title)
                } else {
                    stringResource(R.string.winner_title)
                },
                style = Typography.headlineMedium.copy(fontSize = 25.sp, lineHeight = 32.sp),
                fontWeight = FontWeight.Black,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(24.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                color = Color(0xFF211044).copy(alpha = 0.90f),
                border = BorderStroke(1.dp, GameSecondary.copy(alpha = 0.30f)),
                shadowElevation = 24.dp
            ) {
                Box(
                    modifier = Modifier.background(
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.08f),
                                Color.White.copy(alpha = 0.015f)
                            )
                        )
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp, vertical = 26.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (isDraw) {
                            Text(
                                text = stringResource(R.string.winners_share),
                                style = Typography.labelLarge,
                                color = GameSecondary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                        winners.forEachIndexed { index, winner ->
                            if (index > 0) Spacer(Modifier.height(10.dp))
                            Text(
                                text = winner.name.uppercase(),
                                style = Typography.displaySmall.copy(fontSize = 36.sp, lineHeight = 42.sp),
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.height(22.dp))
                        Box(
                            modifier = Modifier
                                .width(64.dp)
                                .height(3.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(GameAccent, GameSecondary)
                                    )
                                )
                        )
                        Spacer(Modifier.height(18.dp))
                        Text(
                            text = stringResource(
                                R.string.points_count,
                                winners.firstOrNull()?.score ?: 0
                            ),
                            style = Typography.titleLarge.copy(fontSize = 24.sp),
                            color = Color(0xFFFFD166),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Surface(
                onClick = onNewGame,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp),
                shape = RoundedCornerShape(GameShapeSize.control),
                color = GameAccent,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                shadowElevation = 14.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFFFF3478), Color(0xFF9C32FF))
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.new_game).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        style = Typography.titleMedium
                    )
                }
            }
            TextButton(
                onClick = onExit,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Text(
                    text = stringResource(R.string.back_to_menu),
                    color = Color.White.copy(alpha = 0.78f),
                    style = Typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun ConfettiEffect() {
    val density = LocalDensity.current.density
    val particles = remember(density) {
        val random = Random(System.nanoTime())
        List(52) {
            ConfettiParticle(
                x = random.nextFloat(),
                startDelay = random.nextFloat() * 0.22f,
                fallDuration = 0.58f + random.nextFloat() * 0.24f,
                sway = (8f + random.nextFloat() * 20f) * density,
                rotation = random.nextFloat() * 360f,
                spin = -240f + random.nextFloat() * 480f,
                size = (5f + random.nextFloat() * 7f) * density,
                shape = random.nextInt(3),
                color = when (random.nextInt(4)) {
                    0 -> GameSecondary
                    1 -> GameAccent
                    2 -> Color(0xFFFFD166)
                    else -> Color.White
                }
            )
        }
    }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(progress) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 5200, easing = LinearEasing)
        )
    }

    Canvas(Modifier.fillMaxSize()) {
        particles.forEach { particle ->
            val particleProgress = (
                (progress.value - particle.startDelay) /
                    (1f - particle.startDelay)
                ).coerceIn(0f, 1f)
            val fall = (particleProgress / particle.fallDuration).coerceIn(0f, 1f)
            val y = fall * (size.height + 64f * density) - 32f * density
            val x = (
                particle.x * size.width +
                    sin((fall * 1.8f + particle.x) * (2f * Math.PI).toFloat()) * particle.sway
                ).coerceIn(0f, size.width)
            val fadeOut = (1f - ((fall - 0.82f) / 0.18f).coerceIn(0f, 1f))
            val color = particle.color.copy(alpha = fadeOut)
            val center = Offset(x, y)
            rotate(
                degrees = particle.rotation + fall * particle.spin,
                pivot = center
            ) {
                when (particle.shape) {
                    0 -> drawRoundRect(
                        color = color,
                        topLeft = Offset(x - particle.size, y - particle.size / 2f),
                        size = androidx.compose.ui.geometry.Size(
                            particle.size * 2f,
                            particle.size
                        ),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                            particle.size / 3f
                        )
                    )
                    1 -> drawCircle(
                        color = color,
                        radius = particle.size * 0.65f,
                        center = center
                    )
                    else -> drawOval(
                        color = color,
                        topLeft = Offset(x - particle.size, y - particle.size / 2f),
                        size = androidx.compose.ui.geometry.Size(
                            particle.size * 2f,
                            particle.size
                        )
                    )
                }
            }
        }
    }
}

private data class ConfettiParticle(
    val x: Float,
    val startDelay: Float,
    val fallDuration: Float,
    val sway: Float,
    val rotation: Float,
    val spin: Float,
    val size: Float,
    val shape: Int,
    val color: Color
)

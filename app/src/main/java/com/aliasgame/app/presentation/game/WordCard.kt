package com.aliasgame.app.presentation.game

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aliasgame.app.domain.model.Word
import com.aliasgame.app.ui.theme.*
import com.aliasgame.app.ui.components.neonGlow
import com.aliasgame.app.ui.components.withGlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun WordCard(
    word: Word,
    vibrationEnabled: Boolean,
    onSwipeUp: () -> Unit,
    onSwipeDown: () -> Unit
) {
    val offsetY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val palette = GameTheme.palette

    val targetColor = when {
        offsetY.value < -100f -> palette.correctTint.copy(alpha = 0.3f) // towards "got it"
        offsetY.value > 100f -> palette.accent.copy(alpha = 0.3f)       // towards "skip"
        else -> palette.surface
    }
    // The neon outline follows the swipe direction too.
    val targetOutline = when {
        !palette.isNeon -> palette.surfaceBorder
        offsetY.value < -100f -> palette.correctTint
        offsetY.value > 100f -> palette.accent
        else -> palette.outline
    }

    val backgroundColor by animateColorAsState(targetValue = targetColor, label = "cardColor")
    val outlineColor by animateColorAsState(targetValue = targetOutline, label = "cardOutline")

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 12.dp)
    ) {
        val density = LocalDensity.current
        val swipeThreshold = with(density) { maxHeight.toPx() * 0.3f }

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, offsetY.value.roundToInt()) }
                .neonGlow(palette.glow(outlineColor, strength = 0.55f), GameShapeSize.card, glowRadius = 18.dp)
                .pointerInput(word) {
                    detectDragGestures(
                        onDragEnd = {
                            scope.launch {
                                if (offsetY.value < -swipeThreshold) {
                                    if (vibrationEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSwipeUp()
                                } else if (offsetY.value > swipeThreshold) {
                                    if (vibrationEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSwipeDown()
                                }
                                offsetY.animateTo(0f, spring(stiffness = Spring.StiffnessLow))
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch { offsetY.snapTo(offsetY.value + dragAmount.y) }
                        }
                    )
                },
            shape = RoundedCornerShape(GameShapeSize.card),
            color = backgroundColor,
            border = BorderStroke(2.dp, outlineColor)
        ) {
            Box(
                modifier = Modifier.fillMaxSize().padding(GameSpacing.cardPadding),
                contentAlignment = Alignment.Center
            ) {
                // Adaptive text sizing for long words
                var textSize by remember(word) { mutableStateOf(42.sp) }

                Text(
                    text = word.text,
                    style = Typography.displaySmall
                        .copy(fontSize = textSize, fontFamily = palette.displayFont)
                        .withGlow(palette.glow(outlineColor, strength = 0.9f), blurRadius = 28f),
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    color = Color.White,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { textLayoutResult ->
                        if (textLayoutResult.hasVisualOverflow && textSize > 24.sp) {
                            textSize *= 0.9f
                        }
                    }
                )
            }
        }
    }
}

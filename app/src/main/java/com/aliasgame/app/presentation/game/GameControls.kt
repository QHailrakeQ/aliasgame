package com.aliasgame.app.presentation.game

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aliasgame.app.R
import com.aliasgame.app.ui.theme.*

@Composable
internal fun GameControls(
    isVibrationEnabled: Boolean,
    onSkip: () -> Unit,
    onPause: () -> Unit,
    onCorrect: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val triggerHaptic = { if (isVibrationEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Skip Button
        Surface(
            onClick = { triggerHaptic(); onSkip() },
            modifier = Modifier.weight(1f).height(64.dp),
            shape = RoundedCornerShape(GameShapeSize.control),
            color = GlassWhite,
            border = BorderStroke(1.dp, GlassWhiteBorder)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.skip_button),
                    style = Typography.titleMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        
        // Pause Button
        val pauseDesc = stringResource(R.string.content_desc_pause)
        Surface(
            onClick = onPause,
            modifier = Modifier
                .size(56.dp)
                .semantics { contentDescription = pauseDesc },
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.1f),
            border = BorderStroke(1.dp, GlassWhiteBorder)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Pause, contentDescription = null, tint = Color.White)
            }
        }

        // Correct Button
        Button(
            onClick = { triggerHaptic(); onCorrect() },
            modifier = Modifier.weight(1f).height(64.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GameAccent),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.got_it_button),
                    style = Typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

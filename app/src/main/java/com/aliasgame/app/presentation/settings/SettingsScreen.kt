package com.aliasgame.app.presentation.settings

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aliasgame.app.R
import com.aliasgame.app.domain.model.AppStyle
import com.aliasgame.app.ui.components.GlassCard
import com.aliasgame.app.ui.components.PartyBackground
import com.aliasgame.app.ui.components.PartyScreenHeader
import com.aliasgame.app.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    PartyBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = GameSpacing.screenHorizontal)
                .verticalScroll(rememberScrollState())
        ) {
            PartyScreenHeader(
                title = stringResource(R.string.menu_settings),
                onBack = onBack,
                modifier = Modifier.padding(top = GameSpacing.screenVertical)
            )

            Spacer(modifier = Modifier.height(GameSpacing.section))

            // Language Selection
            SettingsCard {
                Column {
                    SectionHeader(stringResource(R.string.language), Icons.Default.Translate)
                    
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        maxItemsInEachRow = 3
                    ) {
                        state.availableLanguages.forEach { lang ->
                            val isSelected = state.selectedLanguage == lang
                            LanguageChip(
                                text = lang,
                                isSelected = isSelected,
                                onClick = {
                                    val changed = viewModel.onLanguageSelected(lang)
                                    // Android 13+ delivers the new language as a configuration
                                    // change the activity handles in place. On older versions
                                    // AppCompat does not refresh the strings that way, so the
                                    // screen is recreated as before.
                                    if (changed && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                                        activity?.recreate()
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(GameSpacing.section))

            // Visual style
            SettingsCard {
                Column {
                    SectionHeader(stringResource(R.string.style_header), Icons.Default.Palette)
                    AppStyle.entries.forEach { style ->
                        StyleOption(
                            name = stringResource(style.labelRes()),
                            style = style,
                            isSelected = state.appStyle == style,
                            onClick = { viewModel.onStyleSelected(style) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(GameSpacing.section))

            // Sound & Vibration
            SettingsCard {
                Column {
                    SectionHeader(stringResource(R.string.preferences_header), Icons.Default.Settings)
                    
                    PreferenceRow(
                        label = stringResource(R.string.sound_effects),
                        checked = state.isSoundEnabled,
                        onCheckedChange = viewModel::onSoundToggled
                    )
                    
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = Color.White.copy(alpha = 0.1f)
                    )
                    
                    PreferenceRow(
                        label = stringResource(R.string.haptic_feedback),
                        checked = state.isVibrationEnabled,
                        onCheckedChange = viewModel::onVibrationToggled
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(GameSpacing.section))
        }
    }
}

@Composable
private fun LanguageChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .padding(vertical = 4.dp)
            .widthIn(min = 68.dp, max = 88.dp)
            .heightIn(min = 48.dp)
            .semantics {
                role = Role.RadioButton
                selected = isSelected
            },
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) GameTheme.palette.secondary else GameTheme.palette.surface,
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, GameTheme.palette.surfaceBorder)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = Typography.titleSmall,
                color = if (isSelected) GameTheme.palette.onSecondary else Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun AppStyle.labelRes(): Int = when (this) {
    AppStyle.CLASSIC -> R.string.style_classic
    AppStyle.NEON_PINK -> R.string.style_neon_pink
    AppStyle.NEON_VIOLET -> R.string.style_neon_violet
}

/** One selectable style with a small preview of its colors. */
@Composable
private fun StyleOption(
    name: String,
    style: AppStyle,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val current = GameTheme.palette
    val preview = style.palette()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .heightIn(min = 56.dp)
            .selectable(selected = isSelected, role = Role.RadioButton, onClick = onClick)
            .background(
                if (isSelected) current.secondary.copy(alpha = 0.12f) else Color.Transparent,
                RoundedCornerShape(GameShapeSize.pill)
            )
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Preview: the style's background with its two main colors on it.
        Box(
            modifier = Modifier
                .size(width = 56.dp, height = 36.dp)
                .background(
                    Brush.verticalGradient(listOf(preview.backgroundTop, preview.backgroundBottom)),
                    RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(14.dp).background(preview.accent, CircleShape))
                Box(Modifier.size(14.dp).background(preview.secondary, CircleShape))
            }
        }
        Spacer(Modifier.width(GameSpacing.controlGap))
        Text(
            text = name,
            modifier = Modifier.weight(1f),
            style = Typography.bodyLarge,
            color = Color.White,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
        RadioButton(
            selected = isSelected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = current.secondary,
                unselectedColor = Color.White.copy(alpha = 0.6f)
            )
        )
    }
}

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 16.dp)
    ) {
        Icon(icon, null, tint = GameTheme.palette.secondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = title.uppercase(),
            style = Typography.labelLarge,
            color = Color.White.copy(alpha = 0.7f),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun PreferenceRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f).padding(end = GameSpacing.controlGap),
            style = Typography.bodyLarge,
            color = Color.White
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = GameTheme.palette.secondary,
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = GameTheme.palette.surface
            )
        )
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    GlassCard {
        Box(modifier = Modifier.padding(GameSpacing.cardPadding)) {
            content()
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

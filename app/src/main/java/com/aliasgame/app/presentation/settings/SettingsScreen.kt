package com.aliasgame.app.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
                                onClick = { viewModel.onLanguageSelected(lang) }
                            )
                        }
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
        color = if (isSelected) GameSecondary else GlassWhite,
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, GlassWhiteBorder)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = Typography.titleSmall,
                color = if (isSelected) BgStart else Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 16.dp)
    ) {
        Icon(icon, null, tint = GameSecondary, modifier = Modifier.size(20.dp))
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
                checkedTrackColor = GameSecondary,
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = GlassWhite
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

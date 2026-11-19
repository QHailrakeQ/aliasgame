package com.aliasgame.app.presentation.statistics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aliasgame.app.R
import com.aliasgame.app.domain.model.WordStat
import com.aliasgame.app.ui.components.PartyBackground
import com.aliasgame.app.ui.components.PartyScreenHeader
import com.aliasgame.app.ui.theme.*

/** The words players skipped most often, per language, with an option to reset the counters. */
@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showResetConfirmation by remember { mutableStateOf(false) }

    PartyBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = GameSpacing.screenHorizontal)
        ) {
            PartyScreenHeader(
                title = stringResource(R.string.stats_title),
                onBack = onBack,
                modifier = Modifier.padding(top = GameSpacing.screenVertical)
            )

            Spacer(modifier = Modifier.height(GameSpacing.cardGap))

            Text(
                text = stringResource(R.string.stats_hardest_words),
                style = Typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.stats_hardest_words_hint),
                style = Typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f)
            )

            if (state.languages.size > 1) {
                Spacer(modifier = Modifier.height(GameSpacing.controlGap))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.languages.forEach { language ->
                        LanguageFilterChip(
                            language = language,
                            isSelected = language == state.selectedLanguage,
                            onClick = { viewModel.onLanguageSelected(language) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(GameSpacing.cardGap))

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when {
                    !state.isLoaded -> Unit
                    state.hardestWords.isEmpty() -> EmptyStatistics()
                    else -> HardestWordsList(state.hardestWords)
                }
            }

            if (state.hardestWords.isNotEmpty()) {
                TextButton(
                    onClick = { showResetConfirmation = true },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(vertical = GameSpacing.controlGap)
                ) {
                    Text(
                        text = stringResource(R.string.stats_reset),
                        color = GameAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (showResetConfirmation) {
            AlertDialog(
                onDismissRequest = { showResetConfirmation = false },
                title = { Text(stringResource(R.string.stats_reset_title)) },
                text = { Text(stringResource(R.string.stats_reset_message)) },
                confirmButton = {
                    Button(
                        onClick = {
                            showResetConfirmation = false
                            viewModel.onResetConfirmed()
                        }
                    ) {
                        Text(stringResource(R.string.stats_reset_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetConfirmation = false }) {
                        Text(stringResource(R.string.exit_confirmation_cancel))
                    }
                }
            )
        }
    }
}

@Composable
private fun HardestWordsList(words: List<WordStat>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = GameSpacing.cardGap)
    ) {
        itemsIndexed(words, key = { _, stat -> stat.text }) { index, stat ->
            HardestWordRow(rank = index + 1, stat = stat)
        }
    }
}

@Composable
private fun HardestWordRow(rank: Int, stat: WordStat) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(GameShapeSize.control),
        color = GlassWhite,
        border = BorderStroke(1.dp, GlassWhiteBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = CircleShape,
                color = if (rank <= 3) GameAccent else Color.White.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = rank.toString(),
                        style = Typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            Spacer(modifier = Modifier.width(GameSpacing.controlGap))
            Text(
                text = stat.text,
                modifier = Modifier.weight(1f),
                style = Typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(GameSpacing.controlGap))
            Text(
                text = stringResource(R.string.stats_skipped_of_shown, stat.skipped, stat.shown),
                style = Typography.labelMedium,
                color = GameSecondary,
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
private fun EmptyStatistics() {
    Text(
        text = stringResource(R.string.stats_empty),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = GameSpacing.section),
        style = Typography.bodyLarge,
        color = Color.White.copy(alpha = 0.7f),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun LanguageFilterChip(language: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .heightIn(min = 48.dp)
            .widthIn(min = 56.dp)
            .semantics {
                role = Role.RadioButton
                selected = isSelected
            },
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) GameSecondary else GlassWhite,
        border = if (isSelected) null else BorderStroke(1.dp, GlassWhiteBorder)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp)) {
            Text(
                text = language,
                style = Typography.titleSmall,
                color = if (isSelected) BgStart else Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

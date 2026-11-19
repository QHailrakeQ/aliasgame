package com.aliasgame.app.presentation.setup

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aliasgame.app.R
import com.aliasgame.app.domain.model.WordPack
import com.aliasgame.app.ui.components.PartyBackground
import com.aliasgame.app.ui.components.PartyScreenHeader
import com.aliasgame.app.ui.theme.*

/**
 * Screen for choosing a word category with consistent brand styling.
 * Visuals unified in Phase 1: spacing 24dp, radius 28dp.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackSelectionScreen(
    viewModel: SetupViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    PartyBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = GameSpacing.screenHorizontal)
        ) {
            PartyScreenHeader(
                title = stringResource(R.string.select_category_title),
                onBack = onBack,
                modifier = Modifier.padding(vertical = GameSpacing.screenVertical)
            )

            when {
                state.isLoadingPacks -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = GameSecondary)
                    }
                }

                state.hasPacksLoadError || state.packs.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(
                                if (state.hasPacksLoadError) {
                                    R.string.categories_load_error
                                } else {
                                    R.string.packs_empty
                                }
                            ),
                            style = Typography.bodyLarge,
                            color = Color.White,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        if (state.hasPacksLoadError) {
                            Spacer(Modifier.height(16.dp))
                            TextButton(onClick = viewModel::retryLoadPacks) {
                                Text(
                                    stringResource(R.string.retry),
                                    color = GameSecondary
                                )
                            }
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(GameSpacing.cardGap),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        val (themes, levels) = state.packs.partition { it.isTheme }
                        val onPackClick = { pack: WordPack ->
                            viewModel.onPackSelected(pack.id)
                            onBack()
                        }
                        items(levels, key = { it.id }) { pack ->
                            PackItem(pack, pack.id == state.selectedPack) { onPackClick(pack) }
                        }
                        if (themes.isNotEmpty()) {
                            item(key = "themes_header") {
                                Text(
                                    text = stringResource(R.string.pack_section_themes).uppercase(),
                                    modifier = Modifier.padding(top = GameSpacing.cardGap),
                                    style = Typography.labelLarge,
                                    color = GameSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            items(themes, key = { it.id }) { pack ->
                                PackItem(pack, pack.id == state.selectedPack) { onPackClick(pack) }
                            }
                        }
                    }
                }
                }
            }
    }
}

@Composable
private fun PackItem(
    pack: WordPack,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val displayName = packDisplayName(pack.id)
    val description = packDescription(pack.id)

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(GameShapeSize.card),
        color = if (isSelected) GameSecondary.copy(alpha = 0.12f) else GlassWhite,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) GameSecondary.copy(alpha = 0.8f) else GlassWhiteBorder
        )
    ) {
        Row(
            modifier = Modifier
                .padding(GameSpacing.cardPadding)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(12.dp),
                shape = RoundedCornerShape(GameShapeSize.pill),
                color = GameSecondary
            ) {}
            
            Spacer(modifier = Modifier.width(GameSpacing.controlGap))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    style = Typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = description,
                    style = Typography.bodyLarge.copy(fontSize = 14.sp),
                    color = Color.White.copy(alpha = 0.76f),
                    lineHeight = 20.sp
                )
                Text(
                    text = packWordCount(pack.wordCount),
                    style = Typography.labelMedium,
                    color = GameSecondary,
                    fontWeight = FontWeight.Bold
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = stringResource(R.string.content_desc_selected),
                    tint = GameSecondary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

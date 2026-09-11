package com.aliasgame.app.presentation.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

/**
 * Screen for choosing a difficulty category (word pack).
 * Features a modern card-based layout with difficulty indicators and descriptions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackSelectionScreen(
    viewModel: SetupViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val packs by viewModel.packs.collectAsState()
    val selectedPack by viewModel.selectedPack.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF6200EE), // Primary Brand Purple
                        Color(0xFF03DAC5)  // Secondary Accent Teal
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header with navigation back
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.background(Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Select Category",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            // Scrollable list of categories
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(packs) { pack ->
                    PackItem(
                        name = pack,
                        isSelected = pack == selectedPack,
                        onClick = {
                            viewModel.onPackSelected(pack)
                            onBack() // Navigate back after making a choice
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PackItem(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // Difficulty-based visual feedback
    val packColor = when (name.lowercase()) {
        "easy" -> Color(0xFF4CAF50)
        "medium" -> Color(0xFFFFC107)
        "hard" -> Color(0xFFFF9800)
        "insane" -> Color(0xFFF44336)
        else -> Color(0xFF6200EE)
    }

    val description = when (name.lowercase()) {
        "easy" -> "Simple everyday words. Great for kids and beginners."
        "medium" -> "General knowledge words for balanced gameplay."
        "hard" -> "Complex terms and abstract concepts for experts."
        "insane" -> "Scientific terms, rare words, and brain-teasers."
        else -> "Dive into the $name world collection!"
    }

    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isSelected) Color.White else Color.White.copy(alpha = 0.85f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Visual indicator of difficulty level
            Surface(
                modifier = Modifier.size(14.dp),
                shape = CircleShape,
                color = packColor
            ) {}
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = Color(0xFF6200EE),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

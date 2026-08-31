package com.sunjk.sunjktool.feature.habit.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

// M3 predefined color palette for habits
private val HABIT_COLORS = listOf(
    0xFF6750A4.toInt(), // Primary (purple)
    0xFFB3261E.toInt(), // Red
    0xFFEA7B2D.toInt(), // Orange
    0xFF2D7D46.toInt(), // Green
    0xFF1B6AB0.toInt(), // Blue
    0xFF009688.toInt(), // Teal
    0xFF9C27B0.toInt(), // Deep purple
    0xFF795548.toInt(), // Brown
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitEditScreen(
    viewModel: HabitEditViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.saveComplete) {
        if (uiState.saveComplete) onNavigateBack()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditMode) "编辑习惯" else "新建习惯") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Name
            OutlinedTextField(
                value = uiState.name,
                onValueChange = { viewModel.updateName(it) },
                label = { Text("习惯名称") },
                isError = uiState.nameError != null,
                supportingText = uiState.nameError?.let { { Text(it) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            // Description
            OutlinedTextField(
                value = uiState.description,
                onValueChange = { viewModel.updateDescription(it) },
                label = { Text("描述（可选）") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            Spacer(Modifier.height(20.dp))

            // Color picker
            Text("颜色", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                HABIT_COLORS.forEach { argb ->
                    val color = Color(argb)
                    val isSelected = argb == uiState.colorArgb
                    val onColor = if (color.luminance() > 0.5f) Color.Black else Color.White
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(color)
                            .then(
                                if (isSelected)
                                    Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                else Modifier
                            )
                            .clickable { viewModel.updateColor(argb) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(onColor)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Save button
            Button(
                onClick = { viewModel.save() },
                enabled = !uiState.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (uiState.isSaving) "保存中…" else "保存")
            }
        }
    }
}

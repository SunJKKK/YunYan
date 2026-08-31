package com.sunjk.sunjktool.feature.home.edit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sunjk.sunjktool.ui.components.LoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeEditScreen(
    viewModel: HomeEditViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.saveComplete) {
        if (uiState.saveComplete) onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0.dp),
                title = { Text("编辑首页") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.save() }) {
                        Text("保存")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> LoadingIndicator()
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    modifier = modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    itemsIndexed(uiState.modules, key = { _, m -> m.key }) { idx, module ->
                        val isFirst = idx == 0
                        val isLast = idx == uiState.modules.lastIndex

                        // Module row: checkbox + title + sort buttons
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggleModule(module.key) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = module.enabled,
                                onCheckedChange = { viewModel.toggleModule(module.key) }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = module.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = module.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                            IconButton(
                                onClick = { viewModel.moveUp(module.key) },
                                enabled = !isFirst
                            ) {
                                Icon(
                                    Icons.Default.KeyboardArrowUp,
                                    contentDescription = "上移",
                                    tint = if (isFirst)
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { viewModel.moveDown(module.key) },
                                enabled = !isLast
                            ) {
                                Icon(
                                    Icons.Default.KeyboardArrowDown,
                                    contentDescription = "下移",
                                    tint = if (isLast)
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            // 单按钮切换卡片大小：显示当前大小，点击切换
                            TextButton(
                                onClick = { viewModel.toggleSize(module.key) },
                                modifier = Modifier.padding(start = 4.dp)
                            ) {
                                Icon(
                                    Icons.Default.AspectRatio,
                                    contentDescription = null,
                                    modifier = Modifier.height(14.dp).width(14.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    if (module.size == "large") "大" else "小",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

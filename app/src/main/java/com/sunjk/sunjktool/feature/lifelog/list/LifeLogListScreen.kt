package com.sunjk.sunjktool.feature.lifelog.list

import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sunjk.sunjktool.domain.model.LifeLogEntry
import com.sunjk.sunjktool.domain.model.LifeLogTimelineDay
import com.sunjk.sunjktool.feature.lifelog.MoodConfig
import com.sunjk.sunjktool.util.formatDate
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LifeLogListScreen(
    viewModel: LifeLogListViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToCreate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            if (uiState.isSearchActive) {
                SearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = viewModel::updateSearchQuery,
                    onSearch = {},
                    active = false,
                    onActiveChange = {},
                    leadingIcon = {
                        IconButton(onClick = { viewModel.toggleSearch() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                        }
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(Icons.Default.Clear, "清除")
                            }
                        }
                    },
                    placeholder = { Text("搜索生活记录…") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = SearchBarDefaults.colors(containerColor = MaterialTheme.colorScheme.background)
                ) {}
            } else {
                TopAppBar(
                windowInsets = WindowInsets(0.dp),
                    title = { Text("生活记录") },
                    navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
                    actions = { IconButton(onClick = { viewModel.toggleSearch() }) { Icon(Icons.Default.Search, "搜索") } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToCreate) { Icon(Icons.Default.Add, contentDescription = "新建") }
        },
        modifier = modifier
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        if (uiState.days.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text(
                    if (uiState.searchQuery.isNotEmpty()) "没有匹配的记录" else "还没有生活记录\n点击右下角 + 开始记录",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Scaffold
        }
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            items(uiState.days, key = { it.date }) { day ->
                LifeLogTimelineDayItem(day, onEntryClick = onNavigateToDetail)
            }
        }
    }
}

@Composable
private fun LifeLogTimelineDayItem(day: LifeLogTimelineDay, onEntryClick: (Long) -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(5.dp)))
            Spacer(Modifier.width(8.dp))
            Text(formatDate(day.date), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.width(8.dp))
            Text("共 ${day.entries.size} 条记录", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(8.dp))
        Column {
            for (i in day.entries.indices) {
                val entry = day.entries[i]
                Card(
                    onClick = { onEntryClick(entry.id) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        if (entry.moods.isNotEmpty()) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(entry.moods.size) { idx ->
                                    val moodName = MoodConfig.moodMap[entry.moods[idx]] ?: entry.moods[idx]
                                    Text(moodName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                        }
                        Text(entry.content, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        if (entry.imagePaths.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                entry.imagePaths.take(6).forEach { path ->
                                    val file = remember(path) { File(path) }
                                    if (file.exists()) {
                                        coil.compose.AsyncImage(model = file, contentDescription = null, modifier = Modifier.size(48.dp).clip(MaterialTheme.shapes.small), contentScale = ContentScale.Crop)
                                    }
                                }
                                if (entry.imagePaths.size > 6) {
                                    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                                        Text("+${entry.imagePaths.size - 6}", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
                if (i < day.entries.size - 1) Spacer(Modifier.height(8.dp))
            }
        }
    }
}


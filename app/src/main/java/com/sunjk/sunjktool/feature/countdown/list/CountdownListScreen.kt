package com.sunjk.sunjktool.feature.countdown.list

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sunjk.sunjktool.domain.model.Countdown
import com.sunjk.sunjktool.ui.components.EmptyState
import com.sunjk.sunjktool.ui.components.LoadingIndicator
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountdownListScreen(
    viewModel: CountdownListViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0.dp),
                title = { Text("倒数日") },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToEdit(null) },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加倒数日")
            }
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> LoadingIndicator()
            uiState.items.isEmpty() -> EmptyState(
                title = "还没有倒数日",
                subtitle = "点击右下角 + 按钮，记录重要日子吧",
                modifier = Modifier.padding(innerPadding)
            )
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    items(uiState.items, key = { it.id }) { countdown ->
                        CountdownCard(
                            countdown = countdown,
                            onClick = { onNavigateToEdit(countdown.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CountdownCard(
    countdown: Countdown,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val today = remember { LocalDate.now() }
    val daysRemaining = ChronoUnit.DAYS.between(today, countdown.targetDate)
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy年M月d日") }

    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = countdown.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = countdown.targetDate.format(dateFormatter),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (countdown.note.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = countdown.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            CountdownDayBadge(daysRemaining = daysRemaining)
        }
    }
}

@Composable
private fun CountdownDayBadge(daysRemaining: Long) {
    val (label, countColor) = when {
        daysRemaining > 0 -> "还有" to MaterialTheme.colorScheme.primary
        daysRemaining == 0L -> "" to MaterialTheme.colorScheme.primary
        else -> "已过" to MaterialTheme.colorScheme.error
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (label.isNotEmpty()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = countColor.copy(alpha = 0.7f)
            )
        }
        Text(
            text = if (daysRemaining == 0L) "今天" else "${kotlin.math.abs(daysRemaining)}",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = countColor
        )
        Text(
            text = "天",
            style = MaterialTheme.typography.labelSmall,
            color = countColor.copy(alpha = 0.7f)
        )
    }
}

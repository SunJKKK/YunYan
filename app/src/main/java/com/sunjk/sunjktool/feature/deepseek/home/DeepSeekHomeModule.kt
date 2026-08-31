package com.sunjk.sunjktool.feature.deepseek.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sunjk.sunjktool.domain.model.BalanceHistoryPoint
import com.sunjk.sunjktool.domain.model.DeepSeekBalance
import com.sunjk.sunjktool.ui.components.VicoAreaLineChart

@Composable
fun DeepSeekHomeModule(
    balance: DeepSeekBalance,
    history: List<BalanceHistoryPoint>,
    onRefresh: () -> Unit,
    onNavigateToDetail: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().clickable { onNavigateToDetail() }.padding(16.dp)
    ) {
        // Balance display
        Row(verticalAlignment = Alignment.Bottom) {
            Text(String.format("%.2f", balance.totalBalance), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(balance.currency, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
            Spacer(Modifier.weight(1f))
            if (!balance.isAvailable) {
                Text("额度不足", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 4.dp))
            }
        }

        // Mini line chart (last 7 days)
        if (history.size >= 2) {
            Spacer(Modifier.height(8.dp))
            val values = history.map { it.totalBalance.toFloat() }
            val minVal = values.min()
            val maxVal = values.max()
            val range = (maxVal - minVal).coerceAtLeast(1f)
            val normalized = values.map { (it - minVal) / range }
            VicoAreaLineChart(
                values = normalized,
                lineColor = MaterialTheme.colorScheme.primary,
                lineWidth = 2.dp,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            )
        }

        Spacer(Modifier.height(4.dp))
        Text("DeepSeek API 额度", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
    }
}

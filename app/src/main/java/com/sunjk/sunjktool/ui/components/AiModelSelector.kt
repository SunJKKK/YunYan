package com.sunjk.sunjktool.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.sunjk.sunjktool.data.local.AiModelOption

/**
 * 统一 AI 模型选择器：所有 AI 功能在执行前展示（DeepSeek V4 Flash / V4 Pro / Qwen 3.5 Flash）。
 * 选择结果持久化到对应功能的 ApiPreferences，由各 ViewModel 负责存储。
 */
@Composable
fun AiModelSelector(
    selected: AiModelOption,
    onSelect: (AiModelOption) -> Unit,
    modifier: Modifier = Modifier
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        AiModelOption.entries.forEachIndexed { index, opt ->
            SegmentedButton(
                selected = selected == opt,
                onClick = { onSelect(opt) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = AiModelOption.entries.size)
            ) {
                Text(
                    opt.label,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

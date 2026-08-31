package com.sunjk.sunjktool.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * 首页统一模块容器：粗体标题（左上角）+ 圆角矩形卡片。
 * 所有首页模块通过此组件保持风格统一。可通过 [trailing] 在标题右侧放按钮。
 */
@Composable
fun HomeSection(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {},
    content: @Composable () -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            trailing()
        }

        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            content()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeSectionPreview() {
    com.sunjk.sunjktool.ui.theme.SunJKToolTheme {
        HomeSection(title = "示例模块") {
            Text(
                text = "这是模块内容",
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

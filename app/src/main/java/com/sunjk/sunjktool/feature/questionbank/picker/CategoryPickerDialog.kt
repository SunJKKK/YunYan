package com.sunjk.sunjktool.feature.questionbank.picker

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sunjk.sunjktool.domain.model.QuestionBankCategory

private data class PickerLevel(val parentId: Long?, val title: String)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CategoryPickerDialog(
    allCategories: List<QuestionBankCategory>,
    selectedCategoryId: Long?,
    excludeIds: Set<Long> = emptySet(),
    onSelected: (Long?, String) -> Unit,
    onDismiss: () -> Unit
) {
    var navStack by remember { mutableStateOf(listOf(PickerLevel(null, "选择题集"))) }
    var selectedId by remember { mutableStateOf(selectedCategoryId) }
    var selectedName by remember {
        mutableStateOf(
            if (selectedCategoryId != null) allCategories.find { it.id == selectedCategoryId }?.name ?: ""
            else "无（根目录）"
        )
    }
    var transitionKey by remember { mutableIntStateOf(0) }

    val currentLevel = navStack.last()
    val currentCategories = allCategories.filter { it.parentId == currentLevel.parentId && it.id !in excludeIds }

    fun navigateTo(level: PickerLevel) {
        navStack = navStack + level
        transitionKey++
    }

    fun navigateBack() {
        if (navStack.size > 1) {
            navStack = navStack.dropLast(1)
            transitionKey++
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = true)
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (navStack.size > 1) {
                        IconButton(onClick = ::navigateBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回",
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(46.dp))
                    }

                    Text(
                        text = currentLevel.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "关闭",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                AnimatedContent(
                    targetState = transitionKey,
                    transitionSpec = {
                        val direction = if (targetState > initialState) 1 else -1
                        (slideInHorizontally(
                            animationSpec = tween(250, easing = FastOutSlowInEasing)
                        ) { it * direction } + fadeIn(tween(200)))
                            .togetherWith(
                                slideOutHorizontally(
                                    animationSpec = tween(250, easing = FastOutSlowInEasing)
                                ) { -it * direction } + fadeOut(tween(150))
                            )
                            .using(SizeTransform(clip = false))
                    },
                    label = "picker_level"
                ) {
                    Column {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .heightIn(max = 340.dp)
                        ) {
                            if (currentLevel.parentId == null) {
                                item(key = "none") {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedId = null
                                                selectedName = "无（根目录）"
                                            }
                                            .padding(horizontal = 12.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = selectedId == null,
                                            onClick = {
                                                selectedId = null
                                                selectedName = "无（根目录）"
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "无（根目录）",
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                }
                            }

                            if (currentCategories.isEmpty()) {
                                item(key = "empty") {
                                    Text(
                                        text = "暂无题集",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(24.dp)
                                    )
                                }
                            }

                            items(currentCategories, key = { "cat_${it.id}" }) { category ->
                                val hasChildren = allCategories.any { c ->
                                    c.parentId == category.id && c.id !in excludeIds
                                }
                                val isSelected = selectedId == category.id

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (hasChildren) {
                                                navigateTo(PickerLevel(category.id, category.name))
                                            } else {
                                                selectedId = category.id
                                                selectedName = category.name
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = {
                                            selectedId = category.id
                                            selectedName = category.name
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        Icons.Default.Quiz,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = if (isSelected)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = category.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (hasChildren) {
                                        Icon(
                                            Icons.Default.ChevronRight,
                                            contentDescription = "进入",
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(onClick = {
                                onSelected(selectedId, selectedName)
                                onDismiss()
                            }) {
                                Text("确定")
                            }
                        }
                    }
                }
            }
        }
    }
}

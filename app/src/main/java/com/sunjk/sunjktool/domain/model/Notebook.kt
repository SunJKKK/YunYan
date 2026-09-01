package com.sunjk.sunjktool.domain.model

import androidx.compose.runtime.Stable
import java.time.LocalDateTime

@Stable
data class Notebook(
    val id: Long = 0,
    val name: String,
    val parentId: Long? = null,
    val sortOrder: Int = 0,
    val icon: String = "folder",
    val pinned: Boolean = false,
    val subNotebookCount: Int = 0,
    val entryCount: Int = 0,
    // 含所有后代笔记本的递归汇总数
    val totalSubNotebookCount: Int = 0,
    val totalEntryCount: Int = 0,
    val createdDate: LocalDateTime,
    val updatedDate: LocalDateTime
)

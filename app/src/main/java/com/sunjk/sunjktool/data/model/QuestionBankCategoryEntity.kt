package com.sunjk.sunjktool.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "question_bank_categories")
data class QuestionBankCategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val parentId: Long? = null,
    val sortOrder: Int = 0,
    val createdDate: Long,
    val updatedDate: Long
)

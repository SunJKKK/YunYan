package com.sunjk.sunjktool.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "greeting_quotes")
data class GreetingQuoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val createdAt: Long
)

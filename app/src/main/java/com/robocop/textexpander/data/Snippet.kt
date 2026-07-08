package com.robocop.textexpander.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class SnippetType {
    PLAIN_TEXT,
    AI_PROMPT,
    IMAGE,
    SHELL_SCRIPT,
    PYTHON_SCRIPT,
    FORM
}

@Entity(tableName = "snippets")
data class Snippet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trigger: String,
    val name: String,
    val type: SnippetType = SnippetType.PLAIN_TEXT,
    val content: String,
    val group: String = "General",
    val caseSensitive: Boolean = false,
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

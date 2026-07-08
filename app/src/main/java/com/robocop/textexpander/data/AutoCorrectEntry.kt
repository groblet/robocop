package com.robocop.textexpander.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "autocorrect_entries")
data class AutoCorrectEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val typo: String,
    val correction: String,
    val isBuiltIn: Boolean = false,
    val enabled: Boolean = true
)

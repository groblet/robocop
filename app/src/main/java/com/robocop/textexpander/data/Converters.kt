package com.robocop.textexpander.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromSnippetType(type: SnippetType): String = type.name

    @TypeConverter
    fun toSnippetType(value: String): SnippetType =
        runCatching { SnippetType.valueOf(value) }.getOrDefault(SnippetType.PLAIN_TEXT)
}

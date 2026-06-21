package com.robocop.textexpander.expansion

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

enum class FormFieldType { TEXT, DROPDOWN }

data class FormFieldDef(
    val key: String,
    val label: String,
    val type: FormFieldType = FormFieldType.TEXT,
    val options: List<String> = emptyList(),
    val default: String = ""
)

/** Stored as JSON in [com.robocop.textexpander.data.Snippet.content] when type == FORM. */
data class FormSchema(
    val fields: List<FormFieldDef> = emptyList(),
    @SerializedName("template") val template: String = ""
) {
    fun toJson(): String = gson.toJson(this)

    companion object {
        private val gson = Gson()
        fun fromJson(json: String): FormSchema =
            runCatching { gson.fromJson(json, FormSchema::class.java) }.getOrDefault(FormSchema())
    }
}

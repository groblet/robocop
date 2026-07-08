package com.robocop.textexpander.expansion

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RenderedTemplate(val text: String, val cursorOffset: Int)

/**
 * Expands `${date}`, `${time}`, `${clipboard}` and `${field:key}` placeholders, and
 * reports where `${cursor}` landed so the caller can reposition the text cursor after insertion.
 */
object TemplateRenderer {
    // Private-use Unicode code point: effectively never appears in real user content,
    // and being a single char keeps offset math simple when it's stripped back out.
    private const val CURSOR_MARKER = ""

    fun render(template: String, clipboardText: String, fieldValues: Map<String, String> = emptyMap()): RenderedTemplate {
        var working = template.replace("\${cursor}", CURSOR_MARKER)

        working = working.replace(Regex("\\$\\{date\\}")) {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        }
        working = working.replace(Regex("\\$\\{time\\}")) {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        }
        working = working.replace("\${clipboard}", clipboardText)

        working = working.replace(Regex("\\$\\{field:([a-zA-Z0-9_]+)\\}")) { match ->
            val key = match.groupValues[1]
            fieldValues[key].orEmpty()
        }

        val cursorIndex = working.indexOf(CURSOR_MARKER)
        val finalText = working.replace(CURSOR_MARKER, "")
        val offset = if (cursorIndex >= 0) cursorIndex else finalText.length
        return RenderedTemplate(finalText, offset)
    }

    fun extractFieldKeys(template: String): List<String> =
        Regex("\\$\\{field:([a-zA-Z0-9_]+)\\}").findAll(template)
            .map { it.groupValues[1] }
            .distinct()
            .toList()
}

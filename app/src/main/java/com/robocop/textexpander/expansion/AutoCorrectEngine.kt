package com.robocop.textexpander.expansion

import com.robocop.textexpander.data.AutoCorrectEntry

/**
 * Unlike [ExpansionEngine], auto-correct fires on its own at every word boundary
 * (space/punctuation/newline) - the user never types an explicit trigger.
 */
class AutoCorrectEngine {
    @Volatile private var byTypo: Map<String, String> = emptyMap()

    fun updateEntries(entries: List<AutoCorrectEntry>) {
        byTypo = entries.associate { it.typo.lowercase() to it.correction }
    }

    private val wordBoundary = setOf(' ', '\n', '\t', '.', ',', '!', '?', ';', ':')

    /**
     * [buffer] is the text immediately preceding the cursor. If the last character just typed
     * is a word boundary and the word before it is a known typo, returns the replacement word
     * (boundary character preserved by the caller) plus how many characters to remove.
     */
    fun findCorrection(buffer: String): AutoCorrectResult? {
        if (buffer.isEmpty()) return null
        val lastChar = buffer.last()
        if (lastChar !in wordBoundary) return null

        val withoutBoundary = buffer.dropLast(1)
        val wordStart = withoutBoundary.indexOfLast { it in wordBoundary }.let { if (it == -1) 0 else it + 1 }
        val word = withoutBoundary.substring(wordStart)
        if (word.isEmpty()) return null

        val correction = byTypo[word.lowercase()] ?: return null
        if (correction.equals(word, ignoreCase = true)) return null

        val matchedCase = matchCase(word, correction)
        return AutoCorrectResult(replacement = matchedCase, charsToRemove = word.length)
    }

    private fun matchCase(original: String, correction: String): String = when {
        original == original.uppercase() && original.length > 1 -> correction.uppercase()
        original.firstOrNull()?.isUpperCase() == true -> correction.replaceFirstChar { it.uppercase() }
        else -> correction
    }
}

data class AutoCorrectResult(val replacement: String, val charsToRemove: Int)

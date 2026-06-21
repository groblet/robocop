package com.robocop.textexpander.expansion

import com.robocop.textexpander.data.Snippet

/**
 * Holds the current set of enabled snippets in memory (refreshed from Room via a Flow collector
 * elsewhere) and finds the longest trigger that a given buffer ends with. Accessibility events
 * fire on every keystroke, so this lookup needs to be cheap.
 */
class ExpansionEngine {
    @Volatile private var byTrigger: Map<String, Snippet> = emptyMap()
    @Volatile private var maxTriggerLength: Int = 0

    fun updateSnippets(snippets: List<Snippet>) {
        byTrigger = snippets.associateBy { it.trigger }
        maxTriggerLength = snippets.maxOfOrNull { it.trigger.length } ?: 0
    }

    /** Returns the matching snippet if [buffer] ends with one of its triggers, else null. */
    fun findMatch(buffer: String): Snippet? {
        if (buffer.isEmpty() || maxTriggerLength == 0) return null
        val window = if (buffer.length > maxTriggerLength) buffer.takeLast(maxTriggerLength) else buffer

        var best: Snippet? = null
        for (snippet in byTrigger.values) {
            val trigger = snippet.trigger
            if (trigger.isEmpty() || trigger.length > window.length) continue
            val matches = if (snippet.caseSensitive) {
                window.endsWith(trigger)
            } else {
                window.endsWith(trigger, ignoreCase = true)
            }
            if (matches && (best == null || trigger.length > best!!.trigger.length)) {
                best = snippet
            }
        }
        return best
    }
}

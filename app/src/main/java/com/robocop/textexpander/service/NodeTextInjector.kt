package com.robocop.textexpander.service

import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Edits the currently focused input field through the AccessibilityNodeInfo API.
 *
 * Two strategies, in order of preference:
 *  1. ACTION_SET_TEXT - directly rewrites the field's text and then ACTION_SET_SELECTION moves
 *     the cursor. Works on most modern EditTexts/Compose text fields.
 *  2. Clipboard + ACTION_PASTE fallback - select the trigger text, copy the replacement to the
 *     clipboard, paste over the selection. Needed for fields that don't support ACTION_SET_TEXT.
 */
object NodeTextInjector {

    fun replaceTrailingText(
        context: Context,
        node: AccessibilityNodeInfo,
        charsToRemove: Int,
        replacement: String,
        cursorOffsetFromReplacementStart: Int? = null,
        selectionEndOverride: Int? = null
    ): Boolean {
        val refreshed = node
        val currentText = refreshed.text?.toString() ?: ""
        val selectionEnd = selectionEndOverride ?: (refreshed.textSelectionEnd.takeIf { it >= 0 } ?: currentText.length)
        val selectionStart = (selectionEnd - charsToRemove).coerceAtLeast(0)

        return setTextDirectly(refreshed, currentText, selectionStart, selectionEnd, replacement, cursorOffsetFromReplacementStart)
            || pasteOverSelection(context, refreshed, selectionStart, selectionEnd, replacement, cursorOffsetFromReplacementStart)
    }

    private fun setTextDirectly(
        node: AccessibilityNodeInfo,
        currentText: String,
        selectionStart: Int,
        selectionEnd: Int,
        replacement: String,
        cursorOffsetFromReplacementStart: Int?
    ): Boolean {
        if (!node.actionList.any { it.id == AccessibilityNodeInfo.ACTION_SET_TEXT }) return false

        val newText = currentText.substring(0, selectionStart) + replacement + currentText.substring(selectionEnd.coerceAtMost(currentText.length))
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, newText)
        }
        val setOk = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        if (!setOk) return false

        val cursorPos = selectionStart + (cursorOffsetFromReplacementStart ?: replacement.length)
        moveCursor(node, cursorPos)
        return true
    }

    private fun pasteOverSelection(
        context: Context,
        node: AccessibilityNodeInfo,
        selectionStart: Int,
        selectionEnd: Int,
        replacement: String,
        cursorOffsetFromReplacementStart: Int?
    ): Boolean {
        val selectArgs = Bundle().apply {
            putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, selectionStart)
            putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, selectionEnd)
        }
        node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, selectArgs)

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val previousClip = clipboard.primaryClip
        clipboard.setPrimaryClip(ClipData.newPlainText("robocop_expansion", replacement))

        val pasted = node.performAction(AccessibilityNodeInfo.ACTION_PASTE)
        if (pasted && cursorOffsetFromReplacementStart != null) {
            moveCursor(node, selectionStart + cursorOffsetFromReplacementStart)
        }
        if (previousClip != null) {
            clipboard.setPrimaryClip(previousClip)
        }
        return pasted
    }

    private fun moveCursor(node: AccessibilityNodeInfo, position: Int) {
        val args = Bundle().apply {
            putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, position)
            putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, position)
        }
        node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, args)
    }

    fun findFocusedEditableNode(service: AccessibilityService): AccessibilityNodeInfo? {
        val focused = service.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        return focused?.takeIf { it.isEditable }
    }
}

package com.robocop.textexpander.service

import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.PixelFormat
import android.net.Uri
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.robocop.textexpander.data.Snippet
import com.robocop.textexpander.data.SnippetRepository
import com.robocop.textexpander.data.SnippetType
import com.robocop.textexpander.expansion.AutoCorrectEngine
import com.robocop.textexpander.expansion.ExpansionEngine
import com.robocop.textexpander.expansion.FormSchema
import com.robocop.textexpander.expansion.TemplateRenderer
import com.robocop.textexpander.ui.form.FormFillOverlay
import com.robocop.textexpander.ui.theme.RobocopTheme
import com.robocop.textexpander.util.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TextExpanderAccessibilityService : AccessibilityService(), LifecycleOwner, SavedStateRegistryOwner {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val expansionEngine = ExpansionEngine()
    private val autoCorrectEngine = AutoCorrectEngine()
    private var autoCorrectEnabled = true
    private var suppressNextEvents = 0

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private val windowManager by lazy { getSystemService(Context.WINDOW_SERVICE) as WindowManager }
    private var formOverlayView: ComposeView? = null
    private var pendingFormNode: AccessibilityNodeInfo? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        autoCorrectEnabled = AppPreferences.isAutoCorrectEnabled(applicationContext)
        val repo = SnippetRepository.get(applicationContext)
        scope.launch {
            repo.observeEnabledSnippets().collect { expansionEngine.updateSnippets(it) }
        }
        scope.launch {
            repo.observeEnabledAutoCorrectEntries().collect { autoCorrectEngine.updateEntries(it) }
        }
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        dismissFormOverlay()
        if (instance === this) instance = null
        scope.coroutineContext[Job]?.cancel()
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) return
        if (suppressNextEvents > 0) {
            suppressNextEvents--
            return
        }

        val node = NodeTextInjector.findFocusedEditableNode(this) ?: return
        val text = node.text?.toString() ?: ""
        val cursor = node.textSelectionEnd.takeIf { it >= 0 } ?: text.length
        if (cursor <= 0) return

        val windowStart = (cursor - 200).coerceAtLeast(0)
        val buffer = text.substring(windowStart, cursor)

        if (autoCorrectEnabled) {
            val correction = autoCorrectEngine.findCorrection(buffer)
            if (correction != null) {
                applyAutoCorrect(node, cursor, correction.charsToRemove, correction.replacement)
                return
            }
        }

        val snippet = expansionEngine.findMatch(buffer) ?: return
        handleSnippetMatch(node, cursor, snippet)
    }

    private fun applyAutoCorrect(node: AccessibilityNodeInfo, cursor: Int, charsToRemove: Int, replacement: String) {
        // The boundary char (space/punctuation) the user just typed sits at [cursor-1, cursor)
        // and must be preserved after the corrected word, so we stop the removed range there
        // and let the cursor land one char past the replacement to account for it.
        suppressNextEvents = 2
        NodeTextInjector.replaceTrailingText(
            context = this,
            node = node,
            charsToRemove = charsToRemove,
            replacement = replacement,
            cursorOffsetFromReplacementStart = replacement.length + 1,
            selectionEndOverride = cursor - 1
        )
    }

    private fun handleSnippetMatch(node: AccessibilityNodeInfo, cursor: Int, snippet: Snippet) {
        when (snippet.type) {
            SnippetType.PLAIN_TEXT, SnippetType.AI_PROMPT -> {
                val clipboardText = currentClipboardText()
                val rendered = TemplateRenderer.render(snippet.content, clipboardText)
                injectText(node, snippet.trigger.length, rendered.text, rendered.cursorOffset)
            }
            SnippetType.SHELL_SCRIPT -> {
                injectText(node, snippet.trigger.length, "…", null)
                scope.launch {
                    val output = ScriptExecutor.runShell(snippet.content)
                    replacePlaceholderResult(output)
                }
            }
            SnippetType.PYTHON_SCRIPT -> {
                val status = ScriptExecutor.runPythonViaTermux(applicationContext, snippet.content)
                injectText(node, snippet.trigger.length, status, null)
            }
            SnippetType.IMAGE -> {
                injectText(node, snippet.trigger.length, "", null)
                pasteImage(node, snippet.content)
            }
            SnippetType.FORM -> {
                injectText(node, snippet.trigger.length, "", null)
                pendingFormNode = node
                showFormOverlay(snippet.name, FormSchema.fromJson(snippet.content))
            }
        }
    }

    private fun injectText(node: AccessibilityNodeInfo, charsToRemove: Int, replacement: String, cursorOffset: Int?) {
        suppressNextEvents = 2
        NodeTextInjector.replaceTrailingText(this, node, charsToRemove, replacement, cursorOffset)
    }

    /** Replaces the "…" placeholder left by a running shell script with its real output. */
    private fun replacePlaceholderResult(output: String) {
        val node = NodeTextInjector.findFocusedEditableNode(this) ?: return
        val text = node.text?.toString() ?: return
        val cursor = node.textSelectionEnd.takeIf { it >= 0 } ?: text.length
        if (cursor < 1 || text.getOrNull(cursor - 1) != '…') return
        suppressNextEvents = 2
        NodeTextInjector.replaceTrailingText(this, node, 1, output, null)
    }

    private fun pasteImage(node: AccessibilityNodeInfo, imageUriString: String) {
        val uri = runCatching { Uri.parse(imageUriString) }.getOrNull() ?: return
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newUri(contentResolver, "robocop_image", uri))
        suppressNextEvents = 2
        node.performAction(AccessibilityNodeInfo.ACTION_PASTE)
    }

    private fun currentClipboardText(): String {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val item = clipboard.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)
        return item?.coerceToText(this)?.toString().orEmpty()
    }

    override fun onInterrupt() {}

    /** Used by [com.robocop.textexpander.service.QuickSearchBubbleService] to insert a result
     *  into whatever field is currently focused. */
    fun injectIntoFocusedField(text: String, cursorOffset: Int? = null) {
        val node = NodeTextInjector.findFocusedEditableNode(this) ?: return
        injectText(node, 0, text, cursorOffset)
    }

    fun setAutoCorrectEnabled(enabled: Boolean) {
        autoCorrectEnabled = enabled
    }

    /**
     * Shown as a TYPE_ACCESSIBILITY_OVERLAY (no SYSTEM_ALERT_WINDOW permission needed, and the
     * underlying app's window is left alone) so the field we're about to write into stays valid.
     */
    private fun showFormOverlay(snippetName: String, schema: FormSchema) {
        dismissFormOverlay()
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
        }

        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@TextExpanderAccessibilityService)
            setViewTreeSavedStateRegistryOwner(this@TextExpanderAccessibilityService)
            setContent {
                RobocopTheme {
                    FormFillOverlay(
                        snippetName = snippetName,
                        schema = schema,
                        onSubmit = { fieldValues ->
                            val rendered = TemplateRenderer.render(schema.template, currentClipboardText(), fieldValues)
                            pendingFormNode?.let { node ->
                                injectText(node, 0, rendered.text, rendered.cursorOffset)
                            }
                            pendingFormNode = null
                            dismissFormOverlay()
                        },
                        onCancel = {
                            pendingFormNode = null
                            dismissFormOverlay()
                        }
                    )
                }
            }
        }
        windowManager.addView(view, params)
        formOverlayView = view
    }

    private fun dismissFormOverlay() {
        formOverlayView?.let { runCatching { windowManager.removeView(it) } }
        formOverlayView = null
    }

    companion object {
        @Volatile var instance: TextExpanderAccessibilityService? = null
            private set

        fun isRunning(): Boolean = instance != null
    }
}

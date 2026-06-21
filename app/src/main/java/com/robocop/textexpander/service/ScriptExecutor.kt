package com.robocop.textexpander.service

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Runs shell snippets directly. Android has no AppleScript equivalent (that's macOS-only) and no
 * built-in Python runtime, so Python snippets are best-effort: if Termux + Termux:API are
 * installed, we hand the script to Termux's RUN_COMMAND intent; otherwise we say so.
 */
object ScriptExecutor {
    private const val TIMEOUT_SECONDS = 15L
    private const val TERMUX_PACKAGE = "com.termux"

    suspend fun runShell(script: String): String = withContext(Dispatchers.IO) {
        try {
            val process = ProcessBuilder("sh", "-c", script)
                .redirectErrorStream(true)
                .start()
            val finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                return@withContext "[Robocop] Script timed out after ${TIMEOUT_SECONDS}s"
            }
            process.inputStream.bufferedReader().readText().trim()
        } catch (e: Exception) {
            "[Robocop] Shell error: ${e.message}"
        }
    }

    fun isTermuxAvailable(context: Context): Boolean = try {
        context.packageManager.getPackageInfo(TERMUX_PACKAGE, PackageManager.GET_ACTIVITIES)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    /**
     * Best-effort Python execution via Termux:API's RUN_COMMAND intent. Requires the user to have
     * Termux and Termux:API installed and to have granted `allow-external-apps=true` in
     * ~/.termux/termux.properties. Returns a status string immediately; output is not captured
     * since Termux:API runs the command asynchronously in its own session.
     */
    fun runPythonViaTermux(context: Context, script: String): String {
        if (!isTermuxAvailable(context)) {
            return "[Robocop] Python snippets need Termux + Termux:API installed (Android has no built-in Python runtime)."
        }
        return try {
            val intent = android.content.Intent("com.termux.RUN_COMMAND").apply {
                setClassName(TERMUX_PACKAGE, "com.termux.app.RunCommandService")
                putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/python")
                putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf("-c", script))
                putExtra("com.termux.RUN_COMMAND_BACKGROUND", true)
            }
            context.startForegroundService(intent)
            "[Robocop] Python script sent to Termux."
        } catch (e: Exception) {
            "[Robocop] Failed to reach Termux: ${e.message}"
        }
    }
}

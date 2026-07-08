package com.robocop.textexpander.util

import android.content.Context

object AppPreferences {
    private const val PREFS_NAME = "robocop_prefs"
    private const val KEY_AUTOCORRECT_ENABLED = "autocorrect_enabled"

    fun isAutoCorrectEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_AUTOCORRECT_ENABLED, true)

    fun setAutoCorrectEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_AUTOCORRECT_ENABLED, enabled).apply()
    }
}

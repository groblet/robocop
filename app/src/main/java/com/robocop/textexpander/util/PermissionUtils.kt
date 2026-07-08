package com.robocop.textexpander.util

import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import com.robocop.textexpander.service.TextExpanderAccessibilityService

object PermissionUtils {
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expectedComponent = "${context.packageName}/${TextExpanderAccessibilityService::class.java.name}"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabledServices)
        while (splitter.hasNext()) {
            if (splitter.next().equals(expectedComponent, ignoreCase = true)) return true
        }
        return false
    }

    fun canDrawOverlays(context: Context): Boolean = Settings.canDrawOverlays(context)
}

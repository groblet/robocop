package com.robocop.textexpander.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.robocop.textexpander.service.QuickSearchBubbleService
import com.robocop.textexpander.service.TextExpanderAccessibilityService
import com.robocop.textexpander.util.AppPreferences
import com.robocop.textexpander.util.PermissionUtils

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    var accessibilityEnabled by remember { mutableStateOf(PermissionUtils.isAccessibilityServiceEnabled(context)) }
    var overlayEnabled by remember { mutableStateOf(PermissionUtils.canDrawOverlays(context)) }
    var bubbleActive by remember { mutableStateOf(false) }
    var autoCorrectEnabled by remember { mutableStateOf(AppPreferences.isAutoCorrectEnabled(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                accessibilityEnabled = PermissionUtils.isAccessibilityServiceEnabled(context)
                overlayEnabled = PermissionUtils.canDrawOverlays(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        SettingsCard(
            title = "Enable text expansion",
            description = "Robocop needs Accessibility access to detect what you type and expand snippets in any app.",
            actionLabel = if (accessibilityEnabled) "Enabled" else "Open settings",
            actionEnabled = !accessibilityEnabled,
            onAction = {
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        )

        SettingsCard(
            title = "Enable quick-search bubble",
            description = "Allow Robocop to draw a floating search bubble over other apps to insert snippets on demand.",
            actionLabel = if (overlayEnabled) "Enabled" else "Open settings",
            actionEnabled = !overlayEnabled,
            onAction = {
                context.startActivity(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                )
            }
        )

        Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Quick-search bubble", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Show floating bubble")
                    Switch(
                        checked = bubbleActive,
                        enabled = overlayEnabled,
                        onCheckedChange = { checked ->
                            bubbleActive = checked
                            if (checked) QuickSearchBubbleService.start(context) else QuickSearchBubbleService.stop(context)
                        }
                    )
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Auto-correct typos", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Automatically fix common typos as you type, without needing a trigger.",
                    style = MaterialTheme.typography.bodySmall
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Enabled")
                    Switch(
                        checked = autoCorrectEnabled,
                        onCheckedChange = { checked ->
                            autoCorrectEnabled = checked
                            AppPreferences.setAutoCorrectEnabled(context, checked)
                            TextExpanderAccessibilityService.instance?.setAutoCorrectEnabled(checked)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    description: String,
    actionLabel: String,
    actionEnabled: Boolean,
    onAction: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
            Button(onClick = onAction, enabled = actionEnabled, modifier = Modifier.padding(top = 8.dp)) {
                Text(actionLabel)
            }
        }
    }
}

package com.robocop.textexpander.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.robocop.textexpander.ui.autocorrect.AutoCorrectScreen
import com.robocop.textexpander.ui.settings.SettingsScreen
import com.robocop.textexpander.ui.snippets.SnippetEditScreen
import com.robocop.textexpander.ui.snippets.SnippetListScreen

private sealed class Destination(val route: String, val label: String) {
    data object Snippets : Destination("snippets", "Snippets")
    data object AutoCorrect : Destination("autocorrect", "Auto-correct")
    data object Settings : Destination("settings", "Settings")
}

private const val SNIPPET_EDIT_ROUTE = "snippet_edit"

@Composable
fun RobocopNavHost() {
    val navController = rememberNavController()
    val tabs = listOf(Destination.Snippets, Destination.AutoCorrect, Destination.Settings)

    Scaffold(
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination
            NavigationBar {
                tabs.forEach { destination ->
                    val selected = currentRoute?.hierarchy?.any { it.route == destination.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            val icon = when (destination) {
                                Destination.Snippets -> Icons.Filled.List
                                Destination.AutoCorrect -> Icons.Filled.Edit
                                Destination.Settings -> Icons.Filled.Settings
                            }
                            Icon(icon, contentDescription = destination.label)
                        },
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Snippets.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Destination.Snippets.route) {
                SnippetListScreen(
                    onAddSnippet = { navController.navigate("$SNIPPET_EDIT_ROUTE/-1") },
                    onEditSnippet = { id -> navController.navigate("$SNIPPET_EDIT_ROUTE/$id") }
                )
            }
            composable(Destination.AutoCorrect.route) { AutoCorrectScreen() }
            composable(Destination.Settings.route) { SettingsScreen() }
            composable(
                route = "$SNIPPET_EDIT_ROUTE/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: -1L
                SnippetEditScreen(
                    snippetId = if (id < 0) null else id,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

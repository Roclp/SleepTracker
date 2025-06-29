// BottomNavItem.kt
package com.example.SleepTracker

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Data class representing an item in the bottom navigation bar.
 *
 * @property route The route associated with the navigation item.
 * @property icon The icon displayed for the navigation item.
 * @property label The label displayed for the navigation item.
 */
data class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
)

/**
 * A list of all bottom navigation items used in the app.
 */
val BottomNavItems = listOf(
    BottomNavItem("home", Icons.Default.Home, "首页"),
    BottomNavItem("analysis", Icons.Default.Search, "分析"),
    BottomNavItem("settings", Icons.Default.Settings, "设置"),
    BottomNavItem("account", Icons.Default.AccountCircle, "账户")
)

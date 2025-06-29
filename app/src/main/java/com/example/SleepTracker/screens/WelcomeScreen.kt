package com.example.SleepTracker.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.SleepTracker.utils.PermissionsHelper

/**
 * Displays the Welcome screen with an introduction to the app's features and a "Get Started" button.
 *
 * @param navController The NavController for navigating between screens.
 * @param permissionsHelper The PermissionsHelper instance to handle permissions.
 */
@Composable
fun WelcomeScreen(
    navController: NavController,
    permissionsHelper: PermissionsHelper
) {
    var showRuntimePermissionDialog by remember { mutableStateOf(false) }
    var showExactAlarmDialog by remember { mutableStateOf(false) }

    // Pager state to manage the current page in the horizontal pager
    val pagerState = rememberPagerState(pageCount = { 3 }) // Number of pages in the pager

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Horizontal pager for swiping through feature pages
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f) // Take up remaining space
        ) { page ->
            when (page) {
                0 -> FeaturePage("欢迎使用睡眠跟踪APP", "记录您的每一次睡眠")
                1 -> FeaturePage("睡眠分析", "了解您的睡眠模式。")
                2 -> FeaturePage("智能闹钟", "让您轻松醒来，焕发活力！")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                when {
                    permissionsHelper.getMissingRuntimePermissions().isNotEmpty() -> {
                        showRuntimePermissionDialog = true
                    }
                    permissionsHelper.needsExactAlarmPermission() -> {
                        showExactAlarmDialog = true
                    }
                    else -> {
                        navController.navigate("home")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "快速开始")
        }

        // Runtime Permissions Dialog
        if (showRuntimePermissionDialog) {
            val missingPermissions = permissionsHelper.getMissingRuntimePermissions()
            AlertDialog(
                onDismissRequest = { showRuntimePermissionDialog = false },
                title = { Text("请求权限") },
                text = {
                    Column {
                        Text("SleepTrack APP 需要以下权限:")
                        missingPermissions.forEach { permission ->
                            Text("• ${permissionsHelper.getPermissionExplanation(permission)}")
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        permissionsHelper.requestRuntimePermissions { granted ->
                            showRuntimePermissionDialog = false
                            if (granted) {
                                if (permissionsHelper.needsExactAlarmPermission()) {
                                    showExactAlarmDialog = true
                                } else {
                                    navController.navigate("home")
                                }
                            }
                        }
                    }) {
                        Text("同意")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRuntimePermissionDialog = false }) {
                        Text("稍后")
                    }
                }
            )
        }

        // Exact Alarm Permission Dialog
        if (showExactAlarmDialog) {
            AlertDialog(
                onDismissRequest = { showExactAlarmDialog = false },
                title = { Text("需要额外的权限") },
                text = {
                    Column {
                        Text("SleepTracker APP 需要额外的一个权限，以确保闹钟正常工作:")
                        Text("• ${permissionsHelper.getPermissionExplanation("android.permission.SCHEDULE_EXACT_ALARM")}")
                        Text("\\n请您访问系统设置并授权。")
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        showExactAlarmDialog = false
                        permissionsHelper.openExactAlarmSettings()
                    }) {
                        Text("打开设置")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showExactAlarmDialog = false
                        navController.navigate("home")
                    }) {
                        Text("跳过")
                    }
                }
            )
        }
    }
}

/**
 * A composable function to display individual feature pages in the welcome screen.
 *
 * @param title The title of the feature.
 * @param description The description of the feature.
 */
@Composable
fun FeaturePage(title: String, description: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = description, style = MaterialTheme.typography.bodyMedium)
    }
}

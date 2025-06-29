package com.example.SleepTracker.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.SleepTracker.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(settingsViewModel: SettingsViewModel = viewModel()) {
    val motionSensitivity by settingsViewModel.motionSensitivity.observeAsState(5)
    val audioSensitivity by settingsViewModel.audioSensitivity.observeAsState(5)
    val updateInterval by settingsViewModel.updateInterval.observeAsState(5)
    val isDarkTheme by settingsViewModel.isDarkTheme.observeAsState(false)
    val useSmartAlarm by settingsViewModel.useSmartAlarm.observeAsState(true)
    val smartAlarmWindow by settingsViewModel.smartAlarmWindow.observeAsState(30)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Tracking Settings
        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "睡 眠 跟 踪 设 置",
                    style = MaterialTheme.typography.titleLarge
                )

                // Motion Sensitivity
                Column {
                    Text(
                        text = "体动灵敏度",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Slider(
                        value = motionSensitivity.toFloat(),
                        onValueChange = { settingsViewModel.setMotionSensitivity(it.toInt()) },
                        valueRange = 1f..10f,
                        steps = 8
                    )
                    Text(
                        text = when(motionSensitivity) {
                            1, 2 -> "低 - 仅检测较大幅度的运动"
                            3, 4 -> "中低"
                            5, 6 -> "中 - 兼顾灵敏度与稳定性"
                            7, 8 -> "中高"
                            else -> "高 - 可检测微小运动"
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Audio Sensitivity
                Column {
                    Text(
                        text = "音频灵敏度",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Slider(
                        value = audioSensitivity.toFloat(),
                        onValueChange = { settingsViewModel.setAudioSensitivity(it.toInt()) },
                        valueRange = 1f..10f,
                        steps = 8
                    )
                    Text(
                        text = when(audioSensitivity) {
                            1, 2 -> "低 - 仅检测较大声响"
                            3, 4 -> "中低"
                            5, 6 -> "中 - 兼顾灵敏度与稳定性"
                            7, 8 -> "中高"
                            else -> "高 - 可检测微弱声音"
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Update Interval
                Column {
                    Text(
                        text = "睡眠监测周期",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Slider(
                        value = updateInterval.toFloat(),
                        onValueChange = { settingsViewModel.setUpdateInterval(it.toInt()) },
                        valueRange = 1f..30f,
                        steps = 28
                    )
                    Text(
                        text = "$updateInterval 分钟",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // Smart Alarm Settings
        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "智能闹钟",
                    style = MaterialTheme.typography.titleLarge
                )

                // Smart Alarm Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "使用智能闹钟",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Switch(
                        checked = useSmartAlarm,
                        onCheckedChange = { settingsViewModel.setUseSmartAlarm(it) }
                    )
                }

                if (useSmartAlarm) {
                    // Smart Alarm Window
                    Column {
                        Text(
                            text = "舒适唤醒区",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Slider(
                            value = smartAlarmWindow.toFloat(),
                            onValueChange = { settingsViewModel.setSmartAlarmWindow(it.toInt()) },
                            valueRange = 5f..45f,
                            steps = 7
                        )
                        Text(
                            text = "$smartAlarmWindow 分钟内",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        // App Settings
        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "应用设置",
                    style = MaterialTheme.typography.titleLarge
                )

                // Theme Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "夜间主题",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = { settingsViewModel.setDarkTheme(it) }
                    )
                }

                // Data Management
                Button(
                    onClick = { settingsViewModel.clearAllData() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("清除所有数据")
                }
            }
        }
    }
}

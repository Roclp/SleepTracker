package com.example.SleepTracker.screens

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Brightness3
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.PhoneIphone
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.SleepTracker.data.SleepQualityMetrics
import com.example.SleepTracker.utils.SleepTracker
import com.example.SleepTracker.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun HomeScreen(homeViewModel: HomeViewModel = viewModel()) {
    val context = LocalContext.current
    val sleepTime by homeViewModel.sleepTime.observeAsState()
    val isTracking by homeViewModel.isTracking.observeAsState(false)
    val alarmTime by homeViewModel.alarmTime.observeAsState()
    val permissionRequired by homeViewModel.permissionRequired.observeAsState(false)
    val currentPhase by homeViewModel.currentSleepPhase.observeAsState(SleepTracker.SleepPhase.AWAKE)
    val sleepQuality by homeViewModel.lastSleepQuality.observeAsState()
    val sleepDuration by homeViewModel.currentSleepDuration.observeAsState(0L)

    var showAlarmTimePicker by remember { mutableStateOf(false) }
    var showQuickAlarmPicker by remember { mutableStateOf(false) }
    var showSleepTimePicker by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Card
            SleepStatusCard(
                isTracking = isTracking,
                currentPhase = currentPhase,
                sleepDuration = sleepDuration,
                alarmTime = alarmTime
            )

            // Quick Actions
            QuickActionsRow(
                isTracking = isTracking,
                onStartTracking = {
                    if (!isTracking) {
                        // Use regular bedtime/alarm if set
                        if (sleepTime != null || alarmTime != null) {
                            homeViewModel.startTracking(context)
                        } else {
                            showQuickAlarmPicker = true
                        }
                    }
                },
                onStopTracking = { homeViewModel.stopTracking(context) }
            )

            // Time Settings
            TimeSettingsCard(
                sleepTime = sleepTime,
                alarmTime = alarmTime,
                onSetSleepTime = { showSleepTimePicker = true },
                onSetAlarmTime = { showAlarmTimePicker = true }
            )

            // Last Sleep Insights
            sleepQuality?.let { quality ->
                SleepInsightsCard(quality)
            }

            // Sleep Tips
            SleepTipsCard()
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = {
                if (isTracking) {
                    homeViewModel.stopTracking(context)
                } else {
                    // Use regular bedtime/alarm if set
                    if (sleepTime != null || alarmTime != null) {
                        homeViewModel.startTracking(context)
                    } else {
                        showQuickAlarmPicker = true
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = if (isTracking)
                MaterialTheme.colorScheme.error
            else
                MaterialTheme.colorScheme.primary
        ) {
            Icon(
                imageVector = if (isTracking) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                contentDescription = if (isTracking) "Stop Tracking" else "Start Tracking"
            )
        }
    }

    // Regular Time Picker
    if (showAlarmTimePicker) {
        TimePickerDialog(
            initialHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
            initialMinute = Calendar.getInstance().get(Calendar.MINUTE),
            onTimeSelected = { hour, minute ->
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    // If selected time is before current time, add one day
                    if (timeInMillis <= System.currentTimeMillis()) {
                        add(Calendar.DAY_OF_YEAR, 1)
                    }
                }
                homeViewModel.setAlarm(calendar.timeInMillis)
                showAlarmTimePicker = false
            },
            onDismiss = { showAlarmTimePicker = false }
        )
    }

    // Quick Test Picker
    if (showQuickAlarmPicker) {
        QuickTimePickerDialog(
            onTimeSelected = { timestamp ->
                if (permissionRequired) {
                    homeViewModel.getExactAlarmPermissionIntent()?.let { intent ->
                        context.startActivity(intent)
                    }
                } else {
                    homeViewModel.startQuickTest(context, ((timestamp - System.currentTimeMillis()) / 60000).toInt())
                }
                showQuickAlarmPicker = false
            },
            onDismiss = { showQuickAlarmPicker = false }
        )
    }

    // Sleep Time Picker
    if (showSleepTimePicker) {
        TimePickerDialog(
            initialHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
            initialMinute = Calendar.getInstance().get(Calendar.MINUTE),
            onTimeSelected = { hour, minute ->
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                }
                homeViewModel.setSleepTime(calendar)
                showSleepTimePicker = false
            },
            onDismiss = { showSleepTimePicker = false }
        )
    }
}

@Composable
fun SleepStatusCard(
    isTracking: Boolean,
    currentPhase: SleepTracker.SleepPhase,
    sleepDuration: Long,
    alarmTime: Long?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sleep Phase Indicator
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = when (currentPhase) {
                            SleepTracker.SleepPhase.DEEP_SLEEP -> Icons.Outlined.DarkMode
                            SleepTracker.SleepPhase.LIGHT_SLEEP -> Icons.Outlined.Brightness3
                            SleepTracker.SleepPhase.REM -> Icons.Outlined.Visibility
                            else -> Icons.Outlined.LightMode
                        },
                        contentDescription = "Sleep Phase",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = when (currentPhase) {
                            SleepTracker.SleepPhase.DEEP_SLEEP -> "深睡眠"
                            SleepTracker.SleepPhase.LIGHT_SLEEP -> "浅睡眠"
                            SleepTracker.SleepPhase.REM -> "REM"
                            else -> "清醒"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.width(32.dp))

            // Duration and Time Left
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                if (isTracking) {
                    Text(
                        text = "正在运行 ${formatDuration(sleepDuration)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    alarmTime?.let {
                        val timeLeft = it - System.currentTimeMillis()
                        if (timeLeft > 0) {
                            Text(
                                text = "闹钟将在 ${formatDuration(timeLeft)} 后响起",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Text(
                        text = "睡眠跟踪未启动",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
@Composable
fun QuickActionsRow(
    isTracking: Boolean,
    onStartTracking: () -> Unit,
    onStopTracking: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        QuickActionButton(
            icon = Icons.Outlined.Brightness3,
            label = "开始",
            enabled = !isTracking,
            onClick = onStartTracking
        )
        QuickActionButton(
            icon = Icons.Filled.Stop,
            label = "结束",
            enabled = isTracking,
            onClick = onStopTracking
        )
        QuickActionButton(
            icon = Icons.AutoMirrored.Outlined.ShowChart,
            label = "分析",
            enabled = true,
            onClick = { /* Navigate to analysis */ }
        )
    }
}

@Composable
fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {
        FilledTonalIconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(icon, contentDescription = label)
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (enabled)
                MaterialTheme.colorScheme.onSurface
            else
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun TimeSettingsCard(
    sleepTime: Calendar?,
    alarmTime: Long?,
    onSetSleepTime: () -> Unit,
    onSetAlarmTime: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "睡 眠 计 划",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TimeSettingItem(
                    icon = Icons.Outlined.Brightness3,
                    label = "睡觉时间",
                    time = sleepTime?.let { formatTime(it.time) } ?: "未设置",
                    onClick = onSetSleepTime
                )

                TimeSettingItem(
                    icon = Icons.Outlined.LightMode,
                    label = "起床时间",
                    time = alarmTime?.let { formatTime(Date(it)) } ?: "未设置",
                    onClick = onSetAlarmTime
                )
            }
        }
    }
}

@Composable
fun TimeSettingItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    time: String,
    onClick: () -> Unit
) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.padding(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = label)
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = time,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
fun SleepInsightsCard(quality: SleepQualityMetrics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "最 近 一 次 睡 眠",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InsightItem(
                    value = quality.calculateQualityScore().toString(),
                    label = "质量分数",
                    suffix = "分"
                )
                InsightItem(
                    value = quality.avgMotion.format(1),
                    label = "体动情况"
                )
                InsightItem(
                    value = quality.avgAudio.format(1),
                    label = "噪音情况"
                )
            }
        }
    }
}

@Composable
fun InsightItem(value: String, label: String, suffix: String = "") {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$value$suffix",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SleepTipsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "使 用 说 明",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            ListItem(
                headlineContent = { Text(
                    text = "请授权应用权限 所有数据仅存于本地",
                    style = MaterialTheme.typography.labelSmall
                )},
                leadingContent = {
                    Icon(Icons.Outlined.Schedule, contentDescription = null)
                }
            )

            ListItem(
                headlineContent = { Text(
                    "请将手机置于床头 距离头部约30厘米",
                    style = MaterialTheme.typography.labelSmall
                )},
                leadingContent = {
                    Icon(Icons.Outlined.PhoneIphone, contentDescription = null)
                }
            )

            ListItem(
                headlineContent = { Text(
                    "睡眠监测仅供参考 请以医疗建议为准",
                    style = MaterialTheme.typography.labelSmall
                )},
                leadingContent = {
                    Icon(Icons.Outlined.AcUnit, contentDescription = null)
                }
            )
        }
    }
}

private fun formatTime(date: Date): String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
}

@SuppressLint("DefaultLocale")
private fun formatDuration(millis: Long): String {
    val hours = millis / (1000 * 60 * 60)
    val minutes = (millis % (1000 * 60 * 60)) / (1000 * 60)
    return String.format("%dh %02dm", hours, minutes)
}

private fun Float.format(decimals: Int): String = "%.${decimals}f".format(this)

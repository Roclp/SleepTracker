// AnalysisScreen.kt
package com.example.SleepTracker.screens

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.SleepTracker.data.SleepData
import com.example.SleepTracker.viewmodel.AnalysisViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@Composable
fun AnalysisScreen(analysisViewModel: AnalysisViewModel = viewModel()) {
    val sleepData by analysisViewModel.sleepData.observeAsState(emptyList())
    val availableSessions by analysisViewModel.availableSessions.observeAsState(emptyList())

    // Auto-refresh effect
    LaunchedEffect(Unit) {
        analysisViewModel.startAutoRefresh()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        if (sleepData.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "暂无睡眠数据\n快开始你的第一次睡眠吧",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            // Current Session Info
            CurrentSessionInfo(sleepData.first(), sleepData.last())
            Spacer(modifier = Modifier.height(16.dp))

//            LLMSuggesion(sleepData)
//            Spacer(modifier = Modifier.height(16.dp))

            // Session History
            SessionHistoryView(
                availableSessions = availableSessions,
                onSessionSelected = { analysisViewModel.loadSleepSession(it) }
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Sleep Patterns Section
            SleepPatternsView(sleepData)
            Spacer(modifier = Modifier.height(16.dp))

            // snore show
            // 展示出当前app中所有snore开头的mav文件
            SnoreView()
//            Spacer(modifier = Modifier.height(16.dp))

            // Latest Readings
//            LatestReadingsView(sleepData.lastOrNull())
//            Spacer(modifier = Modifier.height(16.dp))


        }
    }
}

fun formatDurationWithHoursAndMinutes(durationMillis: Long): String {
    val hours = durationMillis / (1000 * 60 * 60)
    val minutes = (durationMillis % (1000 * 60 * 60)) / (1000 * 60)

    return when {
        hours > 0 -> "${hours}小时${minutes}分钟后响起"
        minutes > 0 -> "${minutes}分钟后响起"
        else -> "暂无闹钟"
    }
}


@Composable
fun CurrentSessionInfo(firstReading: SleepData, lastReading: SleepData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "睡 眠 信 息",
                style = MaterialTheme.typography.titleLarge
            )

            val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

            val startTime = Date(firstReading.sleepStart)
            val currentDuration = lastReading.timestamp - firstReading.sleepStart

            Text("开始时间： ${timeFormat.format(startTime)}")
//            Text("持续时长： ${formatDurationWithHoursAndMinutes(currentDuration)}")

            if (lastReading.alarmTime > 0) {
                val remainingTime = lastReading.alarmTime - lastReading.timestamp
                if (remainingTime >= 0) {
                    Text("闹钟计划: ${formatDurationWithHoursAndMinutes(remainingTime)}")
                }
            }

        }
    }
}

@Composable
fun SleepPatternsView(sleepData: List<SleepData>) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("睡眠阶段", fontWeight = FontWeight.Bold)
            fun sleepPhaseToFactor(phase: String): Float {
                return when (phase.lowercase()) {
                    "awake" -> 0.1f
                    "light_sleep" -> 0.5f
                    "deep_sleep" -> 1f
                    else -> 0.66f
                }
            }


            val sleepPhaseCounts = sleepData.groupingBy { it.sleepPhase.lowercase() }.eachCount()
            val total = sleepPhaseCounts.values.sum()

            Row( // 👈 用 Row 包住就可以横向排列
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()), // 内容过多时可横向滑动
                horizontalArrangement = Arrangement.spacedBy(12.dp) // 每项之间加点间距
            ) {
                sleepPhaseCounts.forEach { (phase, count) ->
                    val phaseText = when (phase) {
                        "awake" -> "清醒"
                        "light_sleep" -> "浅睡眠"
                        "deep_sleep" -> "深睡眠"
                        else -> "REM"
                    }
                    val percent = count * 100f / total
                    Text(
                        text = "$phaseText：${"%.0f".format(percent)}%",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }



            val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                sleepData.forEach { data ->
                    val targetHeightFactor = sleepPhaseToFactor(data.sleepPhase)
                    val animatedHeightFactor by animateFloatAsState(targetValue = targetHeightFactor)
                    val formattedTime = timeFormat.format(Date(data.timestamp))
                    val sleepPhaseText = when (data.sleepPhase.lowercase()) {
                        "awake" -> "清醒"
                        "light" -> "浅睡眠"
                        "deep" -> "深睡眠"
                        else -> "REM"
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(animatedHeightFactor)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                            .clickable {
                                Toast.makeText(
                                    context,
                                    "时间：$formattedTime\n睡眠阶段：$sleepPhaseText",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    )
                }
            }



            // Motion Pattern
            Text("体动情况", fontWeight = FontWeight.Bold)
            // 1. 计算公共数据
            val maxMotion = sleepData.maxOfOrNull { it.motion } ?: 1f
            val lazyListState2 = rememberLazyListState()

            LazyRow(
                state = lazyListState2,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                horizontalArrangement = Arrangement.spacedBy(1.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                items(sleepData) { data ->
                    // 2. 计算当前数据的高度比例
                    val targetHeightFactor = (data.motion / maxMotion).coerceIn(0f, 1f)

                    // 可选：为高度变化添加动画效果
                    val animatedHeightFactor by animateFloatAsState(targetValue = targetHeightFactor)

                    Box(
                        modifier = Modifier
                            .width(10.dp)
                            .fillMaxHeight(animatedHeightFactor)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                    )
                }
            }



//            Text(
//                "最大体动量: ${sleepData.maxOfOrNull { it.motion }?.format(2) ?: "0.00"}",
//                style = MaterialTheme.typography.bodySmall,
//                modifier = Modifier.fillMaxWidth(),
//                textAlign = TextAlign.End
//            )

//            Spacer(modifier = Modifier.height(8.dp))

            // Audio Pattern
            Text("噪音情况", fontWeight = FontWeight.Bold)
            val maxAudio = sleepData.maxOfOrNull { it.audioLevel } ?: 1f
            val lazyListStateAudio = rememberLazyListState()

            LazyRow(
                state = lazyListStateAudio,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                items(sleepData) { data ->
                    // 计算当前数据的高度比例
                    val targetHeightFactor = (data.audioLevel / maxAudio).coerceIn(0f, 1f)
                    // 添加动画效果，使柱状图高度变化更平滑
                    val animatedHeightFactor by animateFloatAsState(targetValue = targetHeightFactor)

                    Box(
                        modifier = Modifier
                            .width(10.dp)
                            .fillMaxHeight(animatedHeightFactor)
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f))
                    )
                }
            }

//            Text(
//                "最大噪音量: ${sleepData.maxOfOrNull { it.audioLevel }?.format(2) ?: "0.00"}",
//                style = MaterialTheme.typography.bodySmall,
//                modifier = Modifier.fillMaxWidth(),
//                textAlign = TextAlign.End
//            )
        }
    }
}

@Composable
fun LLMSuggesion(latestData: List<SleepData>) {
    val context = LocalContext.current
    val files = remember { getSnoreFiles(context) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "大模型给出的睡眠建议",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "suggestion",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun LatestReadingsView(latestData: SleepData?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "最 近 一 次 睡 眠",
                style = MaterialTheme.typography.titleLarge
            )

            if (latestData != null) {
                val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                Text("时间: ${timeFormat.format(Date(latestData.timestamp))}")
                Text("体动: ${latestData.motion.format(2)}")
                Text("噪音: ${latestData.audioLevel.format(2)}")
                Text("睡眠阶段: ${when (latestData.sleepPhase) {
                    "DEEP_SLEEP" -> "深睡眠"
                    "LIGHT_SLEEP" -> "浅睡眠"
                    "REM" -> "REM"
                    else -> "清醒"
                }}")

            } else {
                Text("暂无上一次睡眠数据记录")
            }
        }
    }
}

@Composable
fun SessionHistoryView(
    availableSessions: List<Long>,
    onSessionSelected: (Long) -> Unit
) {
    if (availableSessions.isNotEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "历 史 记 录",
                    style = MaterialTheme.typography.titleLarge
                )

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableSessions.forEach { sessionStart ->
                        val sessionDate = Date(sessionStart)
                        val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        FilterChip(
                            selected = false,
                            onClick = { onSessionSelected(sessionStart) },
                            label = { Text(timeFormat.format(sessionDate)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SnoreView() {
    val context = LocalContext.current
    val files = remember { getSnoreFiles(context) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "打 呼 噜 瞬 间",
                style = MaterialTheme.typography.titleLarge
            )

            if (files.isEmpty()) {
                Text("暂无打呼噜片段")
            } else {
                LazyColumn (modifier = Modifier.heightIn(max = 100.dp)){
                    items(files) { file ->
                        Text(

                            text = SimpleDateFormat("yyyy年M月d日H时m分", Locale.getDefault()).format(
                                SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).parse(
                                    file.name.removePrefix("snore_audio_").removeSuffix(".wav")
                                )!!
                            ) + "   可能在打鼾哟" ,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    Log.d("SnoreView", "Clicked file: ${file.absolutePath}, size: ${file.length()} bytes")
                                    try {
                                        val mediaPlayer = MediaPlayer.create(context, Uri.fromFile(file))
                                        if (mediaPlayer == null) {
                                            Log.e("SnoreView", "MediaPlayer.create returned null for file: ${file.absolutePath}")
                                            return@clickable
                                        }
                                        mediaPlayer.apply {
                                            start()
                                            Log.d("SnoreView", "Playback started for file: ${file.name}")
                                            setOnCompletionListener { mp ->
                                                Log.d("SnoreView", "Playback completed for file: ${file.name}")
                                                mp.release()
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.e("SnoreView", "Error playing audio", e)
                                    }
                                },
                        )
                    }
                }
            }
        }
    }
}


private fun getSnoreFiles(context: Context): List<File> {
    // 获取外部 Music 目录
    val musicDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
    return musicDir?.listFiles { file ->
        file.name.startsWith("snore") && file.name.endsWith(".wav")
    }?.toList()
        ?.sortedByDescending { it.lastModified() } // 按最后修改时间倒序排序
        ?: emptyList()
}



@Preview(showBackground = true)
@Composable
fun PreviewSnoreView() {
    SnoreView()
}

private fun Float.format(decimals: Int) = "%.${decimals}f".format(this)

@SuppressLint("DefaultLocale")
private fun formatDurationWithSeconds(millis: Long): String {
    val hours = millis / (1000 * 60 * 60)
    val minutes = (millis % (1000 * 60 * 60)) / (1000 * 60)
    val seconds = (millis % (1000 * 60)) / 1000
    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}

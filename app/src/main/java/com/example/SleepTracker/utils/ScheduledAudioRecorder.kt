package com.example.SleepTracker.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Environment
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.jlibrosa.audio.JLibrosa
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException

class ScheduledAudioRecorder(private val context: Context) {
    private var scheduledAudioRecorder: ScheduledAudioRecorder? = null
    private var recordingThread: Thread? = null
    private var running = false
    private var sensitivity = 0.5f // Default sensitivity (range 0.1-1.0)
    private var audioRecord: AudioRecord? = null
    private var model = TFLiteLoader(context)

    private val jLibrosa = JLibrosa()

//    private val augMelSTFT = AugmentMelSTFT(
//        nMels = 128,
//        sampleRate = 32000,
//        winLength = 800,
//        hopSize = 320,
//        nFFT = 1024,
//        freqm = 48,
//        timem = 192,
//        fMin = 0.0,
//        fMax = 16000.0, // Nyquist 频率
//        fMinAugRange = 10,
//        fMaxAugRange = 2000
//    )

    companion object {
        private const val TAG = "ScheduledAudioRecorder"
        private const val SAMPLE_RATE = 44100
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private val BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        private const val RECORD_DURATION_MS = 10000L    // 每次录制 10 秒
        private const val INTERVAL_MS = 60000L          // 每隔 1 分钟启动录制
        private const val WAV_HEADER_SIZE = 44
        private const val VOLUME_THRESHOLD_DB = 20
    }



    /**
     * 开始定时录制
     */
    fun startRecording(updateInterval: Int): Boolean {
        if (running) {
            return true
        }

        if (!checkPermission()) {
            Log.e(TAG, "Missing RECORD_AUDIO permission")
            return false
        }

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            BUFFER_SIZE
        )

        running = true

        val scope = CoroutineScope(Dispatchers.IO)

        recordingThread = Thread({
            scope.launch {
                while (running) {
                    recordSegment(audioRecord)
                    val sleepTime = updateInterval * 60 * 1000L - RECORD_DURATION_MS
                    if (sleepTime > 0 && running) {
                        try {
                            delay(sleepTime) // 这里可以安全地使用 delay
                        } catch (e: CancellationException) {
                            Log.e(TAG, "协程被取消", e)
                            if (!running) break // 只有在 running 变为 false 时才退出
                            delay(1000) // 短暂等待后重试
                        }
                    }
                }
            }
        }, "ScheduledRecordingThread").apply {
            priority = Thread.MAX_PRIORITY
            start()
        }

//        recordingThread = Thread({
//            while (running) {
//                recordSegment(audioRecord)
//                // 录制 5 秒后，等待剩余时间（60秒 - 5秒 = 55秒）
//                val sleepTime = updateInterval * 60 * 10000L - RECORD_DURATION_MS
//                if (sleepTime > 0 && running) {
//                    try {
//                        Thread.sleep(sleepTime)
//                    } catch (e: InterruptedException) {
//                        Log.e(TAG, "等待间隔被中断", e)
//                        if (!running) break // Only break if we're supposed to stop
//                        Thread.sleep(1000) // Brief pause before retrying
//                    }
//                }
//            }
//        }, "ScheduledRecordingThread").apply {
//            priority = Thread.MAX_PRIORITY
//            start()
//        }
        // Log.d(TAG, "定时录音启动")

        Log.d(TAG, "Audio ScheduledAudioRecorder started successfully")
        return true
    }

    /**
     * 录制一段  秒钟的音频，并保存为 WAV 文件
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun recordSegment(audioRecord: AudioRecord?) {
        // 初始化 AudioRecord 对象（这里使用 MIC，获取原始数据）

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord 初始化失败")
            return
        }

        // 准备存储录音数据的文件（文件名使用当前时间戳）
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "audio_$timestamp.wav")
        val outputStream: FileOutputStream
        try {
            outputStream = FileOutputStream(file)
        } catch (e: Exception) {
            Log.e(TAG, "创建输出文件失败", e)
            audioRecord.release()
            return
        }

        // 预先写入空白的 WAV 头部，后续录音完毕后更新
        val emptyHeader = ByteArray(WAV_HEADER_SIZE)
        outputStream.write(emptyHeader)

        audioRecord.startRecording()
        val startTime = System.currentTimeMillis()
        var totalAudioLen = 0
        val buffer = ByteArray(BUFFER_SIZE)

        // 循环读取录音数据直到达到 5 秒或外部停止请求
        while (System.currentTimeMillis() - startTime < RECORD_DURATION_MS && running) {
            val read = audioRecord.read(buffer, 0, buffer.size)
            if (read > 0) {
                // 判断音量是否超过阈值
                outputStream.write(buffer, 0, read)
                totalAudioLen += read
            }
        }

        try {
            outputStream.flush()
        } catch (e: Exception) {
            Log.e(TAG, "刷新输出流异常", e)
        }
        // 更新 WAV 文件头
        updateWavHeader(file, totalAudioLen.toLong())
        try {
            outputStream.close()
        } catch (e: Exception) {
            Log.e(TAG, "关闭输出流异常", e)
        }
//        Log.d(TAG, "录音片段已保存到：${file.absolutePath}")
    }

    /**
     * 更新 WAV 文件头，写入正确的音频数据长度信息
     */
    private fun updateWavHeader(file: File, totalAudioLen: Long) {
        val totalDataLen = totalAudioLen + 36
        val channels = 1
        val byteRate = SAMPLE_RATE * 16 * channels / 8
        val header = ByteArray(WAV_HEADER_SIZE)
        val bb = ByteBuffer.wrap(header)
        bb.order(ByteOrder.LITTLE_ENDIAN)
        bb.put("RIFF".toByteArray(Charsets.US_ASCII))
        bb.putInt(totalDataLen.toInt())
        bb.put("WAVE".toByteArray(Charsets.US_ASCII))
        bb.put("fmt ".toByteArray(Charsets.US_ASCII))
        bb.putInt(16) // PCM 格式 chunk 大小
        bb.putShort(1.toShort()) // PCM 格式
        bb.putShort(channels.toShort())
        bb.putInt(SAMPLE_RATE)
        bb.putInt(byteRate)
        bb.putShort((channels * 16 / 8).toShort())
        bb.putShort(16.toShort()) // 每个采样的位数
        bb.put("data".toByteArray(Charsets.US_ASCII))
        bb.putInt(totalAudioLen.toInt())

        // 计算录音时长
        val durationInSeconds = totalAudioLen.toDouble() / (SAMPLE_RATE * channels * 16 / 8)
        Log.d(TAG,"录音片段时长: $durationInSeconds 秒")

        // 如果时长小于 9 秒，则删除文件并返回
        if (durationInSeconds < 9) {
            Log.d(TAG,"录音时长不足 9 秒，删除文件 ${file.absolutePath}")
            file.delete()
            return
        }

        try {
            // 使用 RandomAccessFile 定位文件开头更新 header
            val raf = file.run { java.io.RandomAccessFile(this, "rw") }
            raf.seek(0)
            raf.write(header)
            raf.close()
            Log.d(TAG, "录音片段已保存到：${file.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "更新 WAV 头失败", e)
        }

        try {
//            val model = TFLiteLoader(context)
            // 假设 runInference 返回 Boolean，表示是否检测到打鼾
            val isSnore = model.runInference(file.absolutePath,jLibrosa)
            if (isSnore) {
                // 重命名文件为 "snore_原有文件名"
                val newFile = File(file.parent, "snore_${file.name}")
                if (file.renameTo(newFile)) {
                    Log.d(TAG, "文件已重命名为：${newFile.absolutePath}")
                } else {
                    Log.e(TAG, "文件重命名失败")
                }
            } else {
                // 删除文件
                if (file.delete()) {
                    Log.d(TAG, "文件已删除")
                } else {
                    Log.e(TAG, "文件删除失败")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "运行推理时出现错误", e)
        }

    }

    /**
     * 停止定时录制
     */
    @Synchronized
    fun stopRecording() {
        running = false
        try {
            recordingThread?.join()
            audioRecord?.stop()
            audioRecord?.release()
            model.close()
            audioRecord = null
            recordingThread = null
            Log.d(TAG, "Audio scheduledAudioRecorder stopped")
        } catch (e: InterruptedException) {
            Log.e(TAG, "停止录音线程异常", e)
        }
        recordingThread = null
        Log.d(TAG, "定时录音停止")
    }

    private fun checkPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
}

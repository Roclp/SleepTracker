package com.example.SleepTracker.utils

import android.content.Context
import android.util.Log
import com.example.SleepTracker.ml.Model
import com.jlibrosa.audio.JLibrosa
import convertToTensorBuffer
import kotlin.math.exp


// 假设已存在 Resnet18 TFLite 模型类
// import com.example.myapp.tflite.Resnet18
// 假设已存在 AugmentMelSTFT 类，用于生成 Mel 频谱
// import com.example.myapp.audio.AugmentMelSTFT

class TFLiteLoader(context: Context) {
    val totalStartTime = System.nanoTime()
    val loadStartTime = System.nanoTime()
    private val model = Model.newInstance(context)
    val loadEndTime = System.nanoTime()


    // Sigmoid 扩展函数用于 FloatArray
    private fun FloatArray.sigmoid(): FloatArray {
        return this.map { 1.0f / (1.0f + exp(-it)) }.toFloatArray()
    }

    /**
     * 运行推理流程
     *
     * @param audiopath 音频文件路径
     */
//    fun runInference(audiopath: String, augMelSTFT: AugmentMelSTFT): Boolean {
//        val targetSampleRate = 32000
//
//        // 加载音频，loadAudio 返回 Pair(DoubleArray, Int) 或 null
//        val (waveformSingle, sr) = loadAudio(audiopath, targetSampleRate)
//            ?: Pair(DoubleArray(0), 0)
//        println("加载的波形长度: ${waveformSingle.size}, 采样率: $sr")
//
//        // 将一维波形包装成二维数组，形状为 [1, numSamples]
//        val waveformBatch: Array<DoubleArray> = arrayOf(waveformSingle)
//
//        // 生成 Mel 频谱，melspec 的形状为 [1, 128, 1000]（假设 forward 已经确保了最后一维大小为 1000）
//        val melspec = augMelSTFT.forward(waveformBatch)
//        val inputFeature0 = melspec
//
//        // 运行模型推理
//        val outputs = model.process(inputFeature0)
//        val outputFeature0 = outputs.outputFeature0AsTensorBuffer
//        val outputFeature1 = outputs.outputFeature1AsTensorBuffer
//
//        Log.d("TFLiteLoader", "OutputFeature0: ${outputFeature0.floatArray.joinToString()}")
//        Log.d("TFLiteLoader", "OutputFeature1: ${outputFeature1.floatArray.joinToString()}")
//
//        // 对输出结果应用 Sigmoid 函数
//        val preds = outputFeature1.floatArray.sigmoid()
//        println("Preds size: ${preds.size}")
//
//        // 获取最大概率的索引
//        val maxIndex = preds.indices.maxByOrNull { index -> preds[index] } ?: -1
//
//        // 对预测结果进行排序（降序排列索引）
//        val sortedIndexes = preds.indices.sortedByDescending { index -> preds[index] }
//
//        // 打印预测结果
//        println("************* Acoustic Event Detected: *****************")
//        for (k in 0 until 10) {
//            if (k < sortedIndexes.size) {
//                println("${sortedIndexes[k]}: ${"%.3f".format(preds[sortedIndexes[k]])}")
//            }
//        }
//        println("********************************************************")
//
//        return true
//    }

    fun runInference(audiopath: String, jLibrosa: JLibrosa): Boolean {
        try {
            val inferenceStartTime = System.nanoTime()

//            val audiopath="/storage/emulated/0/Android/data/com.example.SleepTracker/files/Music/snore_audio_20250420_012940.wav"

            val loadTimeMs = (loadEndTime - loadStartTime) / 1_000_000.0
            println("模型加载时间: %.2f ms".format(loadTimeMs))
            // 读取音频并返回一维特征数组
            val sampleRate = 32000
            val readDurationInSeconds = 8
            var audioFeatureValues: FloatArray = jLibrosa.loadAndRead(
                audiopath, sampleRate, readDurationInSeconds
            )

            println(audioFeatureValues.size)
            fun zScoreNormalize(data: FloatArray): FloatArray {
                val mean = data.average().toFloat()
                val stdDev = kotlin.math.sqrt(data.map { (it - mean) * (it - mean) }.average()).toFloat()
                return if (stdDev == 0f) data else data.map { (it - mean) / stdDev }.toFloatArray()
            }

            // 使用 zScoreNormalize 对 audioFeatureValues 进行标准化
            audioFeatureValues = zScoreNormalize(audioFeatureValues)


            // 生成 Mel 频谱图：参数：输入数据、采样率、FFT窗口大小、Mel通道数、Hop size
            val melSpectrogram: Array<FloatArray> =
                jLibrosa.generateMelSpectroGram(audioFeatureValues, sampleRate, 1024, 128, 320)

            // 对 Mel 频谱图进行 fast normalization
            fun logMelSpectrogram(melSpectrogram: Array<FloatArray>, logOffset: Float = 0.00001f): Array<FloatArray> {
                return melSpectrogram.map { row ->
                    row.map { value ->
                        kotlin.math.ln(value + logOffset)
                    }.toFloatArray()
                }.toTypedArray()
            }

            fun fastNormalizeMelSpectrogram(melSpectrogram: Array<FloatArray>): Array<FloatArray> {
                return melSpectrogram.map { row ->
                    row.map { value ->
                        (value + 4.5f) / 5.0f
                    }.toFloatArray()
                }.toTypedArray()
            }

            val logMelSpec = logMelSpectrogram(melSpectrogram)
            val normalizedMelSpectrogram = fastNormalizeMelSpectrogram(logMelSpec)
            println(logMelSpec.contentToString())

            val melspec =convertToTensorBuffer(arrayOf(normalizedMelSpectrogram))
            val inputFeature0 = melspec

            // 运行模型推理
            val outputs = model.process(inputFeature0)
            val outputFeature0 = outputs.outputFeature0AsTensorBuffer
            val outputFeature1 = outputs.outputFeature1AsTensorBuffer

            Log.d("TFLiteLoader", "OutputFeature0: ${outputFeature0.floatArray.joinToString()}")
            Log.d("TFLiteLoader", "OutputFeature1: ${outputFeature1.floatArray.joinToString()}")

            // 对输出结果应用 Sigmoid 函数
            val preds = outputFeature1.floatArray.sigmoid()
            println("Preds size: ${preds.size}")

            val inferenceEndTime = System.nanoTime()
            val inferenceTimeMs = (inferenceEndTime - inferenceStartTime) / 1_000_000.0
            println("推理时间: %.2f ms".format(inferenceTimeMs))

            return preds[0] > preds[1]


            // 获取最大概率的索引
            val maxIndex = preds.indices.maxByOrNull { index -> preds[index] } ?: -1

            // 对预测结果进行排序（降序排列索引）
            val sortedIndexes = preds.indices.sortedByDescending { index -> preds[index] }

            // 打印预测结果
            println("************* Acoustic Event Detected: *****************")
            for (k in 0 until 2) {
                if (k < sortedIndexes.size) {
                    println("${sortedIndexes[k]}: ${"%.3f".format(preds[sortedIndexes[k]])}")
                }
            }
//            println(preds[sortedIndexes[43]])
            println("********************************************************")


            return true
        }
        finally {
            println("Failed to execute the transaction: tId: ClientTransaction")
        }
    }


    /**
     * 释放模型资源
     */
    fun close() {
        model.close()
    }
}

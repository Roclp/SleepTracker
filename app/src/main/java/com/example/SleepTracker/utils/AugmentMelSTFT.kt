import android.media.MediaExtractor
import android.media.MediaFormat
import com.jlibrosa.audio.JLibrosa
import org.jtransforms.fft.DoubleFFT_1D
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

class AugmentMelSTFT(
    private val nMels: Int = 128,
    private val sampleRate: Int = 32000,
    private val winLength: Int = 800,
    private val hopSize: Int = 320,
    private val nFFT: Int = 1024,
    private val freqm: Int = 48,   // 本例中未使用频率遮罩（可扩展）
    private val timem: Int = 192,  // 本例中未使用时间遮罩（可扩展）
    private val fMin: Double = 0.0,
    private var fMax: Double? = null,
    private val fMinAugRange: Int = 10,
    private val fMaxAugRange: Int = 2000
) {
    private val preemphasisKernel = doubleArrayOf(-0.97, 1.0)
    private val window: DoubleArray = DoubleArray(winLength) { i ->
        0.5 * (1 - cos(2 * Math.PI * i / (winLength - 1)))
    }

    init {
        if (fMax == null) {
            fMax = sampleRate / 2.0 - fMaxAugRange / 2.0
            println("Warning: FMAX is None setting to $fMax")
        }
        require(fMinAugRange >= 1) { "fmin_aug_range=$fMinAugRange should be >=1; 1 means no augmentation" }
        require(fMaxAugRange >= 1) { "fmax_aug_range=$fMaxAugRange should be >=1; 1 means no augmentation" }
    }

    private fun applyPreemphasis(waveform: DoubleArray): DoubleArray {
        val out = DoubleArray(waveform.size - 1)
        for (i in 0 until waveform.size - 1) {
            out[i] = preemphasisKernel[0] * waveform[i] + preemphasisKernel[1] * waveform[i + 1]
        }
        return out
    }

    private fun computeSTFT(waveform: DoubleArray): Array<DoubleArray> {
        val preemphasized = applyPreemphasis(waveform)
        val numFrames = ((preemphasized.size - winLength) / hopSize) + 1
        val stftResult = Array(numFrames) { DoubleArray(nFFT) }
        val fft = DoubleFFT_1D(nFFT.toLong())

        for (i in 0 until numFrames) {
            val start = i * hopSize
            val frame = DoubleArray(nFFT) { 0.0 }
            for (j in 0 until winLength) {
                if (start + j < preemphasized.size) {
                    frame[j] = preemphasized[start + j] * window[j]
                }
            }
            fft.realForward(frame)
            stftResult[i] = frame
        }
        return stftResult
    }

    private fun computePowerSpectrum(stft: Array<DoubleArray>): Array<DoubleArray> {
        val numFrames = stft.size
        val nBins = nFFT / 2 + 1
        val powerSpec = Array(numFrames) { DoubleArray(nBins) }
        for (i in 0 until numFrames) {
            val frame = stft[i]
            powerSpec[i][0] = frame[0].pow(2)
            for (k in 1 until nFFT / 2) {
                val real = frame[2 * k - 1]
                val imag = frame[2 * k]
                powerSpec[i][k] = real * real + imag * imag
            }
            powerSpec[i][nBins - 1] = frame[nFFT - 1].pow(2)
        }
        return powerSpec
    }

    private fun getMelFilterBank(nMels: Int, nFFT: Int, sampleRate: Int, fMin: Double, fMax: Double): Array<DoubleArray> {
        fun hzToMel(hz: Double): Double = 2595 * ln(1 + hz / 700.0)
        fun melToHz(mel: Double): Double = 700 * (exp(mel / 2595) - 1)

        val nBins = nFFT / 2 + 1
        val melMin = hzToMel(fMin)
        val melMax = hzToMel(fMax)
        val melPoints = DoubleArray(nMels + 2) { i ->
            melToHz(melMin + i * (melMax - melMin) / (nMels + 1))
        }
        val bin = melPoints.map { (nFFT + 1) * it / sampleRate }
            .map { it.roundToInt() }
        val filterBank = Array(nMels) { DoubleArray(nBins) { 0.0 } }
        for (m in 0 until nMels) {
            val binStart = bin[m]
            val binCenter = bin[m + 1]
            val binEnd = bin[m + 2]
            for (k in binStart until binCenter.coerceAtMost(nBins)) {
                filterBank[m][k] = (k - binStart).toDouble() / (binCenter - binStart)
            }
            for (k in binCenter until binEnd.coerceAtMost(nBins)) {
                filterBank[m][k] = (binEnd - k).toDouble() / (binEnd - binCenter)
            }
        }
        return filterBank
    }

    private fun matMul(melBasis: Array<DoubleArray>, powerSpec: Array<DoubleArray>): Array<DoubleArray> {
        return Array(melBasis.size) { m ->
            DoubleArray(powerSpec.size) { t ->
                melBasis[m].zip(powerSpec[t]).sumOf { (a, b) -> a * b }
            }
        }
    }

    private fun normalize(melSpectrogram: Array<DoubleArray>): Array<DoubleArray> {
        return Array(melSpectrogram.size) { m ->
            DoubleArray(melSpectrogram[0].size) { t ->
                (ln(melSpectrogram[m][t] + 1e-5) + 4.5) / 5.0
            }
        }
    }

    fun forward(x: Array<DoubleArray>): TensorBuffer {
        // require(x.size == 1 && x[0].size == 32000) { "Input must have shape [1, 32000]" }
        val waveform = x[0]
        val stftResult = computeSTFT(waveform)
        val powerSpec = computePowerSpectrum(stftResult)
        val melBasis = getMelFilterBank(nMels, nFFT, sampleRate, fMin, fMax!!)
        val melspec = matMul(melBasis, powerSpec)
//        return normalize(melspec)
        val normalized = normalize(melspec)
        println("melspec shape: ${normalized.size} x ${if (normalized.isNotEmpty()) normalized[0].size else 0}")

        val mel = arrayOf(normalized)
        return convertToTensorBuffer(mel)
    }

    fun convertToTensorBuffer(specwave: Array<Array<DoubleArray>>): TensorBuffer {
        val tensorBuffer = TensorBuffer.createFixedSize(intArrayOf(1, 1, 128, 1000), DataType.FLOAT32)

        // 手动展平三维数组
        val flatArray = specwave.flatMap { it.flatMap { it.toList() } }.map { it.toFloat() }.toFloatArray()

        tensorBuffer.loadArray(flatArray)
        return tensorBuffer
    }

}

/**
 * 加载音频文件，并将其转换为单声道 DoubleArray 波形数据。
 * @param audioPath 文件路径
 * @param targetSampleRate 目标采样率
 * @return Pair(waveform, sampleRate) 波形数据及采样率
 */
fun loadAudio(audioPath: String, targetSampleRate: Int): Pair<DoubleArray, Int>? {
    val extractor = MediaExtractor()

    try {
        extractor.setDataSource(audioPath)

        // 查找音频轨道
        var trackIndex = -1
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("audio/")) {
                trackIndex = i
                extractor.selectTrack(i)
                break
            }
        }

        if (trackIndex == -1) return null  // 没有音频轨道

        val format = extractor.getTrackFormat(trackIndex)
        val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)  // 获取原始采样率
        val bufferSize = 44100
        val buffer = ByteBuffer.allocate(bufferSize)
        val samples = mutableListOf<Byte>()

        // 读取音频数据
        while (true) {
            val bytesRead = extractor.readSampleData(buffer, 0)
            if (bytesRead < 0) break  // 读取完成

            val chunk = ByteArray(bytesRead)
            buffer.get(chunk)
            samples.addAll(chunk.toList())

            extractor.advance()
            buffer.clear()
        }

        extractor.release()

        // 将字节转换为 DoubleArray
        val waveform = samples.map { it.toDouble() / 128.0 }.toDoubleArray()

        // 如果需要重采样
        val resampledWaveform = if (sampleRate != targetSampleRate) {
            resample(waveform, sampleRate, targetSampleRate)
        } else {
            waveform
        }

        return Pair(resampledWaveform, targetSampleRate)

    } catch (e: Exception) {
        e.printStackTrace()
        return null
    } finally {
        extractor.release()
    }
}

/**
 * 重采样波形数据，使用最简单的线性插值法进行采样。
 * @param waveform 原始波形数据
 * @param originalSampleRate 原始采样率
 * @param targetSampleRate 目标采样率
 * @return 重采样后的波形数据
 */
fun resample(waveform: DoubleArray, originalSampleRate: Int, targetSampleRate: Int): DoubleArray {
    if (originalSampleRate == targetSampleRate) {
        return waveform
    }

    var resampledLength = (waveform.size * targetSampleRate.toDouble() / originalSampleRate).toInt()
    resampledLength=320800
    val resampledWaveform = DoubleArray(resampledLength)

    println(resampledLength)
    for (i in 0 until resampledLength) {
        val srcIndex = (i * originalSampleRate.toDouble() / targetSampleRate).toInt()
        resampledWaveform[i] = waveform[min(srcIndex, waveform.size - 1)]
    }

    return resampledWaveform
}


fun load() {
    // 假设你已正确获取音频文件的绝对路径
    val audioFilePath = "D:\\desktop\\w.wav"
    // 默认采样率和音频时长（例如：22050 Hz和30秒）
    val defaultSampleRate = 32000
    var defaultAudioDuration = 8
    val sampleRate = defaultSampleRate

    val jLibrosa = JLibrosa()
    try {
        // 读取音频并返回一维特征数组
        val audioFeatureValues: FloatArray = jLibrosa.loadAndRead(
            audioFilePath, sampleRate, defaultAudioDuration
        )
        val r=arrayOf(audioFeatureValues)
        println(audioFeatureValues.size)
        fun zScoreNormalize(data: FloatArray): FloatArray {
            val mean = data.average().toFloat()
            val stdDev = kotlin.math.sqrt(data.map { (it - mean) * (it - mean) }.average()).toFloat()
            return if (stdDev == 0f) data else data.map { (it - mean) / stdDev }.toFloatArray()
        }

// 使用 zScoreNormalize 对 audioFeatureValues 进行标准化
        val normalizedValues = zScoreNormalize(audioFeatureValues)


// 输出
        println(normalizedValues.contentToString())




//        println(audioFeatureValues.joinToString(", "))
//        println(audioFeatureValues.contentToString())
//

        println(r.size)
        println(r[0].size)



        // 生成 Mel 频谱图：参数：输入数据、采样率、FFT窗口大小、Mel通道数、Hop size
        val melSpectrogram: Array<FloatArray> =
            jLibrosa.generateMelSpectroGram(normalizedValues, sampleRate, 1024, 128, 320)

//        println(melSpectrogram.joinToString("\n") { it.joinToString(", ") })

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

        val rawMelSpectrogram: Array<FloatArray> = jLibrosa.generateMelSpectroGram(normalizedValues, sampleRate, 1024, 128, 320)
        val logMelSpec = logMelSpectrogram(rawMelSpectrogram)
        val normalizedMelSpectrogram = fastNormalizeMelSpectrogram(logMelSpec)
        println(normalizedMelSpectrogram[1].contentToString())



//        melSpectrogram = zScoreNormalize(audioFeatureValues)
// 输出部分数据查看
//        println(melSpectrogram[0].contentToString())



        // 生成 MFCC 特征：参数：输入数据、采样率、MFCC通道数
        val mfccValues: Array<FloatArray> =
            jLibrosa.generateMFCCFeatures(audioFeatureValues, sampleRate, 40)

        // 输出特征矩阵的尺寸
        println("Mel Spectrogram size: ${melSpectrogram.size} x ${if (melSpectrogram.isNotEmpty()) melSpectrogram[0].size else 0}")
        println("MFCC size: ${mfccValues.size} x ${if (mfccValues.isNotEmpty()) mfccValues[0].size else 0}")
    } catch (e: Exception) {
        e.printStackTrace()
        println("加载或特征提取失败: ${e.message}")
    }
}

fun convertToTensorBuffer(specwave: Array<Array<FloatArray>>): TensorBuffer {
    val tensorBuffer = TensorBuffer.createFixedSize(intArrayOf(1, 1, 128, 1103), DataType.FLOAT32)

    // 手动展平三维数组
    val flatArray = specwave.flatMap { it.flatMap { it.toList() } }.map { it.toFloat() }.toFloatArray()

    tensorBuffer.loadArray(flatArray)
    return tensorBuffer
}
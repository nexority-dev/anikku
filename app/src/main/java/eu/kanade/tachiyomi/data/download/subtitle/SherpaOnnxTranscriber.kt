package eu.kanade.tachiyomi.data.download.subtitle

import com.hippo.unifile.UniFile
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizerResult
import com.k2fsa.sherpa.onnx.OfflineWhisperModelConfig
import com.k2fsa.sherpa.onnx.WaveReader

class SherpaOnnxTranscriber(
    private val modelManager: TranscriptionModelManager,
) : SubtitleTranscriber {

    override suspend fun transcribe(audioFile: UniFile): Transcript {
        modelManager.ensureModelReady()

        val filePath = audioFile.filePath
            ?: throw IllegalStateException("Audio file is not accessible via a filesystem path")

        val waveData = WaveReader.readWave(filePath)

        val config = OfflineRecognizerConfig(
            featConfig = FeatureConfig(sampleRate = SAMPLE_RATE, featureDim = FEATURE_DIM),
            modelConfig = OfflineModelConfig(
                whisper = OfflineWhisperModelConfig(
                    encoder = modelManager.encoderFile.absolutePath,
                    decoder = modelManager.decoderFile.absolutePath,
                    language = "",
                    task = "transcribe",
                    enableTokenTimestamps = true,
                ),
                tokens = modelManager.tokensFile.absolutePath,
                modelType = "whisper",
                numThreads = NUM_THREADS,
            ),
        )

        val recognizer = OfflineRecognizer(config = config)
        try {
            val stream = recognizer.createStream()
            try {
                stream.acceptWaveform(waveData.samples, waveData.sampleRate)
                recognizer.decode(stream)
                val result = recognizer.getResult(stream)
                return buildTranscript(result, waveData.samples.size.toLong(), waveData.sampleRate)
            } finally {
                stream.release()
            }
        } finally {
            recognizer.release()
        }
    }

    private fun buildTranscript(
        result: OfflineRecognizerResult,
        sampleCount: Long,
        sampleRate: Int,
    ): Transcript {
        if (result.text.isBlank()) {
            return Transcript(emptyList())
        }

        if (result.timestamps.isEmpty()) {
            val durationMs = sampleCount * 1000L / sampleRate
            return Transcript(
                listOf(
                    TranscriptSegment(
                        startMs = 0L,
                        endMs = durationMs,
                        text = result.text.trim(),
                    ),
                ),
            )
        }

        return Transcript(segments = buildSegments(result.tokens, result.timestamps, result.durations))
    }

    private fun buildSegments(
        tokens: Array<String>,
        timestamps: FloatArray,
        durations: FloatArray,
    ): List<TranscriptSegment> {
        if (tokens.isEmpty()) return emptyList()

        val segments = mutableListOf<TranscriptSegment>()
        val accumulated = StringBuilder()
        var segmentStartMs = secondsToMs(timestamps[0])
        var segmentEndMs = secondsToMs(timestamps[0] + durations[0])

        for (i in tokens.indices) {
            val tokenStartMs = secondsToMs(timestamps[i])
            val tokenEndMs = secondsToMs(timestamps[i] + durations[i])

            if (accumulated.isNotEmpty() && (tokenStartMs - segmentStartMs) > MAX_SEGMENT_DURATION_MS) {
                val text = accumulated.toString().trim()
                if (text.isNotBlank()) {
                    segments.add(TranscriptSegment(segmentStartMs, segmentEndMs, text))
                }
                accumulated.clear()
                segmentStartMs = tokenStartMs
                segmentEndMs = tokenEndMs
            }

            accumulated.append(tokens[i])
            if (tokenEndMs > segmentEndMs) {
                segmentEndMs = tokenEndMs
            }
        }

        val remaining = accumulated.toString().trim()
        if (remaining.isNotBlank()) {
            segments.add(TranscriptSegment(segmentStartMs, segmentEndMs, remaining))
        }

        return segments
    }

    private fun secondsToMs(seconds: Float): Long = (seconds * 1000f).toLong()

    companion object {
        private const val SAMPLE_RATE = 16000
        private const val FEATURE_DIM = 80
        private const val NUM_THREADS = 2
        private const val MAX_SEGMENT_DURATION_MS = 5_000L
    }
}

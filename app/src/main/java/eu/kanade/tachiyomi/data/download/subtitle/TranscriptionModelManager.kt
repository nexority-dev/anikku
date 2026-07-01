package eu.kanade.tachiyomi.data.download.subtitle

import android.content.Context
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.ProgressListener
import tachiyomi.core.common.util.lang.withIOContext
import uy.kohesive.injekt.injectLazy
import java.io.File
import java.io.IOException

class TranscriptionModelManager(context: Context) {

    private val network: NetworkHelper by injectLazy()

    private val modelDir = File(context.filesDir, MODEL_DIR_NAME).apply { mkdirs() }

    val encoderFile: File get() = File(modelDir, ENCODER_FILE_NAME)
    val decoderFile: File get() = File(modelDir, DECODER_FILE_NAME)
    val tokensFile: File get() = File(modelDir, TOKENS_FILE_NAME)

    fun isModelReady(): Boolean {
        return encoderFile.isFile &&
            encoderFile.length() > 0 &&
            decoderFile.isFile &&
            decoderFile.length() > 0 &&
            tokensFile.isFile &&
            tokensFile.length() > 0
    }

    suspend fun ensureModelReady(progressListener: ProgressListener = NO_OP_PROGRESS_LISTENER) {
        if (isModelReady()) return

        withIOContext {
            downloadModelFile(ENCODER_URL, encoderFile, progressListener)
            downloadModelFile(DECODER_URL, decoderFile, progressListener)
            downloadModelFile(TOKENS_URL, tokensFile, progressListener)
        }
    }

    private fun downloadModelFile(url: String, finalFile: File, progressListener: ProgressListener) {
        if (finalFile.isFile && finalFile.length() > 0) return

        val tempFile = File(modelDir, "${finalFile.name}.download")
        network.downloadFileWithResume(url, tempFile, progressListener)
        if (!tempFile.renameTo(finalFile)) {
            throw IOException("Unable to finalize downloaded model file: ${finalFile.name}")
        }
    }

    fun deleteModel() {
        modelDir.listFiles()?.forEach { it.delete() }
    }

    companion object {
        private const val MODEL_DIR_NAME = "asr_models"

        const val ENCODER_FILE_NAME = "small-encoder.int8.onnx"
        const val DECODER_FILE_NAME = "small-decoder.int8.onnx"
        const val TOKENS_FILE_NAME = "small-tokens.txt"

        private const val MODEL_BASE_URL =
            "https://huggingface.co/csukuangfj/sherpa-onnx-whisper-small/resolve/main"
        private const val ENCODER_URL = "$MODEL_BASE_URL/$ENCODER_FILE_NAME"
        private const val DECODER_URL = "$MODEL_BASE_URL/$DECODER_FILE_NAME"
        private const val TOKENS_URL = "$MODEL_BASE_URL/$TOKENS_FILE_NAME"

        private val NO_OP_PROGRESS_LISTENER = object : ProgressListener {
            override fun update(bytesRead: Long, contentLength: Long, done: Boolean) = Unit
        }
    }
}

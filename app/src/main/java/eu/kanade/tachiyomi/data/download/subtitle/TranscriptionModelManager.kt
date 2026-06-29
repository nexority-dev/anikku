package eu.kanade.tachiyomi.data.download.subtitle

import android.content.Context
import java.io.File

class TranscriptionModelManager(context: Context) {

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

    fun deleteModel() {
        modelDir.listFiles()?.forEach { it.delete() }
    }

    companion object {
        private const val MODEL_DIR_NAME = "asr_models"

        const val ENCODER_FILE_NAME = "small-encoder.int8.onnx"
        const val DECODER_FILE_NAME = "small-decoder.int8.onnx"
        const val TOKENS_FILE_NAME = "small-tokens.txt"
    }
}

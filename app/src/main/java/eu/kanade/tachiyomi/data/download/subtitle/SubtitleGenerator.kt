package eu.kanade.tachiyomi.data.download.subtitle

import android.content.Context
import com.hippo.unifile.UniFile
import logcat.LogPriority
import tachiyomi.core.common.storage.nameWithoutExtension
import tachiyomi.core.common.util.system.logcat

class SubtitleGenerator(
    private val context: Context,
    private val transcriber: SubtitleTranscriber,
    private val audioExtractor: AudioExtractor = FfmpegAudioExtractor(context),
    private val srtWriter: SrtWriter = SrtWriter(),
) {

    suspend fun generate(videoFile: UniFile, episodeDir: UniFile) {
        val workDir = UniFile.fromFile(context.cacheDir)?.createDirectory(WORK_DIR_NAME)
            ?: throw Exception("Unable to create subtitle working directory")

        try {
            val audioFile = audioExtractor.extract(videoFile, workDir)
            if (audioFile == null) {
                logcat(LogPriority.DEBUG) {
                    "No audio stream found for ${videoFile.name}, skipping subtitle generation"
                }
                return
            }

            val transcript = transcriber.transcribe(audioFile)

            val baseFileName = videoFile.nameWithoutExtension
                ?: throw Exception("Unable to determine subtitle file name")
            srtWriter.write(transcript, episodeDir, baseFileName)
        } finally {
            workDir.listFiles().orEmpty().forEach { it.delete() }
        }
    }

    companion object {
        private const val WORK_DIR_NAME = "subtitle_tmp"
    }
}

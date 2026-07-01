package eu.kanade.tachiyomi.data.download.subtitle

import android.content.Context
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.FFmpegSession
import com.arthenica.ffmpegkit.FFprobeSession
import com.arthenica.ffmpegkit.Level
import com.arthenica.ffmpegkit.LogCallback
import com.arthenica.ffmpegkit.ReturnCode
import com.arthenica.ffmpegkit.StatisticsCallback
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.util.storage.toFFmpegString
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat

class FfmpegAudioExtractor(
    private val context: Context,
) : AudioExtractor {

    override suspend fun extract(videoFile: UniFile, workDir: UniFile): UniFile? {
        val inputPath = videoFile.toFFmpegString(context)

        if (!hasAudioStream(inputPath)) {
            return null
        }

        val outputFile = workDir.createFile(AUDIO_FILE_NAME)
            ?: throw Exception("Unable to create temporary audio file")
        val outputPath = outputFile.toFFmpegString(context)

        val command = FFmpegKitConfig.parseArguments(
            "-i \"$inputPath\" -map 0:a:0 -vn -ac 1 -ar 16000 -c:a pcm_s16le \"$outputPath\" -y",
        )

        val logCallback = LogCallback { log ->
            if (log.level <= Level.AV_LOG_WARNING) {
                log.message?.let { logcat(LogPriority.ERROR) { it } }
            }
        }
        val session = FFmpegSession.create(command, {}, logCallback, StatisticsCallback {})
        FFmpegKitConfig.ffmpegExecute(session)

        if (!ReturnCode.isSuccess(session.returnCode)) {
            outputFile.delete()
            session.failStackTrace?.let { trace -> logcat(LogPriority.ERROR) { trace } }
            throw Exception("Audio extraction failed")
        }

        return outputFile
    }

    private fun hasAudioStream(inputPath: String): Boolean {
        val command = FFmpegKitConfig.parseArguments(
            "-v error -select_streams a -show_entries stream=index -of csv=p=0 \"$inputPath\"",
        )
        val session = FFprobeSession.create(command)
        FFmpegKitConfig.ffprobeExecute(session)
        return session.allLogsAsString.isNotBlank()
    }

    companion object {
        private const val AUDIO_FILE_NAME = "audio.wav"
    }
}

package eu.kanade.tachiyomi.data.download.subtitle

import android.content.Context
import com.hippo.unifile.UniFile
import logcat.LogPriority
import tachiyomi.core.common.storage.nameWithoutExtension
import tachiyomi.core.common.util.system.logcat

/**
 * Generates a sidecar .srt subtitle file for an already-finalized, downloaded episode video.
 *
 * This is the only entry point the downloader talks to; it owns the extract -> transcribe ->
 * write pipeline end to end, including temporary-file cleanup. Deliberately not wired into the
 * download flow yet - see [generate] for why.
 *
 * Throwing from [generate] means subtitle generation genuinely failed and the caller should treat
 * the overall download as failed. Returning normally covers both a real success and legitimate
 * no-ops (e.g. the video has no audio track at all) - callers don't need to distinguish the two,
 * since "no audio to transcribe" isn't a failure.
 */
class SubtitleGenerator(
    private val context: Context,
    private val transcriber: SubtitleTranscriber,
    private val audioExtractor: AudioExtractor = FfmpegAudioExtractor(context),
    private val srtWriter: SrtWriter = SrtWriter(),
) {

    /**
     * @param videoFile the finalized video file already on disk (not a temp/in-progress file).
     * @param episodeDir the finalized episode directory [videoFile] lives in; the .srt is written
     * here, named after [videoFile] so the existing mpv-based player auto-loads it.
     */
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
            // Temporary audio (and anything else dropped in workDir) is always disposable,
            // regardless of whether generation succeeded, failed, or was cancelled.
            workDir.listFiles().orEmpty().forEach { it.delete() }
        }
    }

    companion object {
        private const val WORK_DIR_NAME = "subtitle_tmp"
    }
}

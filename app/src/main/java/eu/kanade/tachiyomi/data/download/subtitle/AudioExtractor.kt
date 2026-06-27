package eu.kanade.tachiyomi.data.download.subtitle

import com.hippo.unifile.UniFile

/**
 * Extracts a speech-recognition-ready audio track out of a finalized episode video file, so it
 * can be handed to a [SubtitleTranscriber].
 *
 * This is a seam so the extraction backend (currently ffmpeg-kit) can be swapped later without
 * touching [SubtitleGenerator] or anything upstream of it.
 */
interface AudioExtractor {

    /**
     * Extracts the first audio track of [videoFile] into a new file inside [workDir].
     *
     * @return the extracted audio file, or `null` if [videoFile] has no audio stream at all.
     * A `null` result is not a failure - some episodes are legitimately video-only, and callers
     * should treat that as "nothing to transcribe" rather than an error.
     * @throws Exception if [videoFile] does have an audio stream but extraction fails.
     */
    suspend fun extract(videoFile: UniFile, workDir: UniFile): UniFile?
}

package eu.kanade.tachiyomi.data.download.subtitle

/**
 * A single timed line of transcribed speech, normalized to plain milliseconds so that nothing
 * downstream (currently just [SrtWriter]) needs to know anything about the transcription backend
 * that produced it.
 *
 * @param startMs offset from the start of the audio, in milliseconds.
 * @param endMs offset from the start of the audio, in milliseconds. Expected to be > [startMs].
 * @param text the transcribed text for this segment.
 */
data class TranscriptSegment(
    val startMs: Long,
    val endMs: Long,
    val text: String,
)

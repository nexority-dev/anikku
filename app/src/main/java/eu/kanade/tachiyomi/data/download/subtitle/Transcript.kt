package eu.kanade.tachiyomi.data.download.subtitle

/**
 * A full, backend-agnostic transcription result for one episode's audio.
 */
data class Transcript(
    val segments: List<TranscriptSegment>,
)

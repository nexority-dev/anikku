package eu.kanade.tachiyomi.data.download.subtitle

data class TranscriptSegment(
    val startMs: Long,
    val endMs: Long,
    val text: String,
)

package eu.kanade.tachiyomi.data.download.subtitle

import com.hippo.unifile.UniFile
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import kotlin.math.max

/**
 * Renders a [Transcript] into a standard .srt file.
 *
 * Writes to a temporary "<baseFileName>.srt.tmp" file first and only replaces the real
 * "<baseFileName>.srt" once the new content has been fully written and flushed, so a crash or
 * cancellation mid-write never leaves a corrupt or partial subtitle file next to the video. Any
 * existing subtitle file with the same name is overwritten.
 *
 * Pure file I/O with no Android/ffmpeg/network dependency, so it's the easiest piece of this
 * pipeline to unit test in isolation.
 */
class SrtWriter {

    suspend fun write(transcript: Transcript, targetDir: UniFile, baseFileName: String): UniFile {
        val tempName = "$baseFileName.srt.tmp"
        val finalName = "$baseFileName.srt"

        targetDir.findFile(tempName)?.delete()
        val tempFile = targetDir.createFile(tempName)
            ?: throw Exception("Unable to create temporary subtitle file")

        try {
            writeEntries(transcript, tempFile)

            // Atomically replace any previous subtitle file with the new one.
            targetDir.findFile(finalName)?.delete()
            if (!tempFile.renameTo(finalName)) {
                throw Exception("Unable to finalize subtitle file")
            }
        } catch (e: Exception) {
            targetDir.findFile(tempName)?.delete()
            throw e
        }

        // Re-resolve by name rather than trusting renameTo() to have mutated tempFile in place.
        return targetDir.findFile(finalName)
            ?: throw Exception("Subtitle file missing after finalization")
    }

    private fun writeEntries(transcript: Transcript, tempFile: UniFile) {
        BufferedWriter(OutputStreamWriter(tempFile.openOutputStream(), Charsets.UTF_8)).use { writer ->
            transcript.segments.forEachIndexed { index, segment ->
                writer.write((index + 1).toString())
                writer.write(CRLF)
                writer.write(formatTimestamp(segment.startMs))
                writer.write(" --> ")
                writer.write(formatTimestamp(segment.endMs))
                writer.write(CRLF)
                writer.write(segment.text)
                writer.write(CRLF)
                writer.write(CRLF)
            }
            writer.flush()
        }
    }

    /**
     * Formats milliseconds as the SRT-standard HH:MM:SS,mmm timestamp.
     */
    private fun formatTimestamp(ms: Long): String {
        val clamped = max(0L, ms)
        val hours = clamped / 3_600_000
        val minutes = (clamped % 3_600_000) / 60_000
        val seconds = (clamped % 60_000) / 1000
        val millis = clamped % 1000
        return "%02d:%02d:%02d,%03d".format(hours, minutes, seconds, millis)
    }

    companion object {
        // SRT files conventionally use CRLF line endings for broad player compatibility.
        private const val CRLF = "\r\n"
    }
}

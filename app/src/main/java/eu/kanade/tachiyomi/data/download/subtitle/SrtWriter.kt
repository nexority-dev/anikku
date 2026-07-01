package eu.kanade.tachiyomi.data.download.subtitle

import com.hippo.unifile.UniFile
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import kotlin.math.max

class SrtWriter {

    suspend fun write(transcript: Transcript, targetDir: UniFile, baseFileName: String): UniFile {
        val tempName = "$baseFileName.srt.tmp"
        val finalName = "$baseFileName.srt"

        targetDir.findFile(tempName)?.delete()
        val tempFile = targetDir.createFile(tempName)
            ?: throw Exception("Unable to create temporary subtitle file")

        try {
            writeEntries(transcript, tempFile)
            targetDir.findFile(finalName)?.delete()
            if (!tempFile.renameTo(finalName)) {
                throw Exception("Unable to finalize subtitle file")
            }
        } catch (e: Exception) {
            targetDir.findFile(tempName)?.delete()
            throw e
        }

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

    private fun formatTimestamp(ms: Long): String {
        val clamped = max(0L, ms)
        val hours = clamped / 3_600_000
        val minutes = (clamped % 3_600_000) / 60_000
        val seconds = (clamped % 60_000) / 1000
        val millis = clamped % 1000
        return "%02d:%02d:%02d,%03d".format(hours, minutes, seconds, millis)
    }

    companion object {
        private const val CRLF = "\r\n"
    }
}

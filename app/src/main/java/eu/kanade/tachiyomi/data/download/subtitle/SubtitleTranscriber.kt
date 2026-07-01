package eu.kanade.tachiyomi.data.download.subtitle

import com.hippo.unifile.UniFile

interface SubtitleTranscriber {
    suspend fun transcribe(audioFile: UniFile): Transcript
}

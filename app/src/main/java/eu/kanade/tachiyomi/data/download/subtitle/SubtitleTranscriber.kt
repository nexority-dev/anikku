package eu.kanade.tachiyomi.data.download.subtitle

import com.hippo.unifile.UniFile

/**
 * Converts extracted speech audio into a normalized [Transcript].
 *
 * This is the seam for the actual transcription backend (on-device, cloud API, or otherwise).
 * No implementation exists yet on purpose - the backend hasn't been decided. Everything else in
 * the subtitle pipeline ([SubtitleGenerator], [AudioExtractor], [SrtWriter]) only depends on this
 * interface, so picking a backend later only means adding one new class and wiring it in, with no
 * changes required anywhere else in this package.
 */
interface SubtitleTranscriber {

    /**
     * @param audioFile mono, 16kHz, 16-bit PCM WAV, as produced by [AudioExtractor].
     * @throws Exception if transcription fails.
     */
    suspend fun transcribe(audioFile: UniFile): Transcript
}

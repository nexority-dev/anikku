package eu.kanade.tachiyomi.data.download.subtitle

import com.hippo.unifile.UniFile

interface AudioExtractor {

    suspend fun extract(videoFile: UniFile, workDir: UniFile): UniFile?
}

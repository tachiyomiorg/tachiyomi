package eu.kanade.tachiyomi.data.encoder

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import java.util.concurrent.TimeUnit
import timber.log.Timber
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class Encoder(
        private val preferences: PreferencesHelper = Injekt.get()
) {

    companion object {
        // Conversion options
        const val DISABLED = -1
        const val LOSSLESS_WEBP = 0
        const val LOSSY_WEBP = 1
    }

    private fun enumStr(value: Int): String {
        return when (value) {
            DISABLED -> "DISABLED"
            LOSSLESS_WEBP -> "LOSSLESS_WEBP"
            LOSSY_WEBP -> "LOSSY_WEBP"
            else -> "Unknown value $value"
        }
    }

    fun logTime(label: String, time: Long) {
        var min = TimeUnit.NANOSECONDS.toMinutes(time)
        var sec = TimeUnit.NANOSECONDS.toSeconds(time) - TimeUnit.MINUTES.toSeconds(TimeUnit.NANOSECONDS.toMinutes(time))
        var mill = TimeUnit.NANOSECONDS.toMillis(time) - TimeUnit.SECONDS.toMillis(TimeUnit.NANOSECONDS.toSeconds(time))
        Timber.i("WEBP_ENC: $label took $min:$sec:$mill ($time ns)")
    }

    private fun encode(bitmap: Bitmap, outDir: UniFile, filename: String): Boolean {
        var lossless = false;
        var extension = "";

        val encodeSetting = preferences.convertLosslessDownloads()
        when (encodeSetting) {
            DISABLED -> return false
            LOSSLESS_WEBP -> {
                lossless = true
                extension = "webp"
            }
            LOSSY_WEBP -> {
                lossless = false
                extension = "webp"
            }
            else -> {
                Timber.e("Unknown encode setting");
                return false;
            }
        }

        val outFile = outDir.createFile("$filename.out")

        // encode
        val encodeStart = System.nanoTime()
        var stream = outFile.openOutputStream()
        val success = when (encodeSetting) {
            LOSSLESS_WEBP, LOSSY_WEBP -> WebpEncoder.encode(bitmap, lossless, stream)
            else -> false
        }
        stream.close()
        val encodeEnd = System.nanoTime()
        logTime("encode", encodeEnd - encodeStart)

        if (success) {
            outFile.renameTo("$filename.$extension")
        } else {
            Timber.e("Encoding failed for ${outDir.filePath}/$filename")
            outFile.delete()
        }

        return success
    }

    fun encodeChapter(mangaDir: UniFile, chapterDir: UniFile, chapterDirname: String) {
        // Todo: Do this async to the download on the computation scheduler

        if (preferences.convertLosslessDownloads() == DISABLED) {
            return
        }

        // Only pngs are supported for now
        val images = chapterDir.listFiles().orEmpty().filter { it.name!!.endsWith("png")}
        images.forEach {
            val origExtension = it.name!!.substringAfterLast('.')
            val filename = it.name!!.substringBeforeLast('.')

            // TODO: do not use bitmap at all, use embedded libwebp png decoder
            // decodeFile
            val decodeStart = System.nanoTime()
            var bitmap = BitmapFactory.decodeFile(it.filePath)
            val decodeEnd = System.nanoTime()
            logTime("decodeFile", decodeEnd - decodeStart)

            if (bitmap != null && encode(bitmap, chapterDir, filename)) {
                it.delete()
            }
        }
    }
}
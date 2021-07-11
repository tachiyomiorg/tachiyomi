package eu.kanade.tachiyomi.data.coil

import android.content.res.Resources
import android.os.Build
import androidx.core.graphics.drawable.toDrawable
import coil.bitmap.BitmapPool
import coil.decode.DecodeResult
import coil.decode.Decoder
import coil.decode.Options
import coil.size.Size
import eu.kanade.tachiyomi.util.system.ImageUtil
import okio.BufferedSource
import tachiyomi.decoder.ImageDecoder

/**
 * A [Decoder] that uses built-in [ImageDecoder] to decode images that is not supported by the system.
 */
class TachiyomiImageDecoder(private val resources: Resources) : Decoder {

    override fun handles(source: BufferedSource, mimeType: String?): Boolean {
        val type = source.peek().inputStream().use {
            ImageUtil.findImageType(it)
        }
        return when (type) {
            ImageUtil.ImageType.AVIF, ImageUtil.ImageType.JXL -> true
            ImageUtil.ImageType.HEIF -> Build.VERSION.SDK_INT < Build.VERSION_CODES.O
            else -> false
        }
    }

    override suspend fun decode(
        pool: BitmapPool,
        source: BufferedSource,
        size: Size,
        options: Options
    ): DecodeResult {
        val decoder = source.use {
            ImageDecoder.newInstance(it.inputStream())
        }

        check(decoder != null && decoder.width > 0 && decoder.height > 0) { "Failed to initialize decoder." }

        val bitmap = decoder.decode(rgb565 = options.allowRgb565)
        decoder.recycle()

        check(bitmap != null) { "Failed to decode image." }

        return DecodeResult(
            drawable = bitmap.toDrawable(resources),
            isSampled = false
        )
    }
}

package eu.kanade.tachiyomi.util

import java.io.InputStream
import java.net.URLConnection

object ImageUtil {

    fun isImage(name: String, openStream: (() -> InputStream)? = null): Boolean {
        try {
            val guessedMime = URLConnection.guessContentTypeFromName(name)
            if (guessedMime.startsWith("image/")) {
                return true
            }
        } catch (e: Exception) {
            /* Ignore error */
        }

        return openStream?.let { findImageType(it) } != null
    }

    fun findImageType(openStream: () -> InputStream): ImageType? {
        try {
            openStream().buffered().use {
                val bytes = ByteArray(8)
                it.mark(bytes.size)
                val length = it.read(bytes, 0, bytes.size)
                it.reset()
                if (length == -1)
                    return null

                if (bytes.compareWith(charByteArrayOf(0xFF, 0xD8, 0xFF))) {
                    return ImageType.JPG
                }
                if (bytes.compareWith(charByteArrayOf(0x89, 0x50, 0x4E, 0x47))) {
                    return ImageType.PNG
                }
                if (bytes.compareWith("GIF8".toByteArray())) {
                    return ImageType.GIF
                }
                if (bytes.compareWith("RIFF".toByteArray()) || bytes.compareWith("WEBP".toByteArray(), 4)) {
                    return ImageType.WEBP
                }
            }
        } catch(e: Exception) {
        }
        return null
    }

    private fun ByteArray.compareWith(magic: ByteArray, offset: Int = 0): Boolean {
        for (i in offset until magic.size) {
            if (this[i] != magic[i]) return false
        }
        return true
    }

    private fun charByteArrayOf(vararg bytes: Int): ByteArray {
        return ByteArray(bytes.size).apply {
            for (i in 0 until bytes.size) {
                set(i, bytes[i].toByte())
            }
        }
    }

    fun isAnimatedImage(openStream: (() -> InputStream)): Boolean {
        return findImageType(openStream) == ImageType.GIF
    }

    enum class ImageType(val mime: String, val extension: String) {
        JPG("image/jpeg", "jpg"),
        PNG("image/png", "png"),
        GIF("image/gif", "gif"),
        WEBP("image/webp", "webp")
    }

}

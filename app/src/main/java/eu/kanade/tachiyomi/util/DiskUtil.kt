package eu.kanade.tachiyomi.util

import android.content.Context
import android.os.Environment
import android.support.v4.content.ContextCompat
import android.support.v4.os.EnvironmentCompat
import java.io.File
import java.io.InputStream
import java.net.URLConnection
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

object DiskUtil {

    fun isImage(name: String, openStream: (() -> InputStream)? = null): Boolean {
        val contentType = URLConnection.guessContentTypeFromName(name)
        if (contentType != null)
            return contentType.startsWith("image/")

        if (openStream != null) try {
            openStream.invoke().buffered().use {
                var bytes = ByteArray(11)
                it.mark(bytes.size)
                var length = it.read(bytes, 0, bytes.size)
                it.reset()
                if (length == -1)
                    return false
                if (bytes[0] == 'G'.toByte() && bytes[1] == 'I'.toByte() && bytes[2] == 'F'.toByte() && bytes[3] == '8'.toByte()) {
                    return true // image/gif
                } else if (bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() && bytes[2] == 0x4E.toByte()
                        && bytes[3] == 0x47.toByte() && bytes[4] == 0x0D.toByte() && bytes[5] == 0x0A.toByte()
                        && bytes[6] == 0x1A.toByte() && bytes[7] == 0x0A.toByte()) {
                    return true // image/png
                } else if (bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()) {
                    if (bytes[3] == 0xE0.toByte() || bytes[3] == 0xE1.toByte() && bytes[6] == 'E'.toByte()
                            && bytes[7] == 'x'.toByte() && bytes[8] == 'i'.toByte()
                            && bytes[9] == 'f'.toByte() && bytes[10] == 0.toByte()) {
                        return true // image/jpeg
                    } else if (bytes[3] == 0xEE.toByte()) {
                        return true // image/jpg
                    }
                } else if (bytes[0] == 'W'.toByte() && bytes[1] == 'E'.toByte() && bytes[2] == 'B'.toByte() && bytes[3] == 'P'.toByte()) {
                    return true // image/webp
                }
            }
        } catch(e: Exception) {
        }
        return false
    }

    fun hashKeyForDisk(key: String): String {
        return try {
            val bytes = MessageDigest.getInstance("MD5").digest(key.toByteArray())
            val sb = StringBuilder()
            bytes.forEach { byte ->
                sb.append(Integer.toHexString(byte.toInt() and 0xFF or 0x100).substring(1, 3))
            }
            sb.toString()
        } catch (e: NoSuchAlgorithmException) {
            key.hashCode().toString()
        }
    }

    fun getDirectorySize(f: File): Long {
        var size: Long = 0
        if (f.isDirectory) {
            for (file in f.listFiles()) {
                size += getDirectorySize(file)
            }
        } else {
            size = f.length()
        }
        return size
    }

    /**
     * Returns the root folders of all the available external storages.
     */
    fun getExternalStorages(context: Context): List<File> {
        return ContextCompat.getExternalFilesDirs(context, null)
                .filterNotNull()
                .mapNotNull {
                    val file = File(it.absolutePath.substringBefore("/Android/"))
                    val state = EnvironmentCompat.getStorageState(file)
                    if (state == Environment.MEDIA_MOUNTED || state == Environment.MEDIA_MOUNTED_READ_ONLY) {
                        file
                    } else {
                        null
                    }
                }
    }

    /**
     * Mutate the given filename to make it valid for a FAT filesystem,
     * replacing any invalid characters with "_". This method doesn't allow hidden files (starting
     * with a dot), but you can manually add it later.
     */
    fun buildValidFilename(origName: String): String {
        val name = origName.trim('.', ' ')
        if (name.isNullOrEmpty()) {
            return "(invalid)"
        }
        val sb = StringBuilder(name.length)
        name.forEach { c ->
            if (isValidFatFilenameChar(c)) {
                sb.append(c)
            } else {
                sb.append('_')
            }
        }
        // Even though vfat allows 255 UCS-2 chars, we might eventually write to
        // ext4 through a FUSE layer, so use that limit minus 15 reserved characters.
        return sb.toString().take(240)
    }

    /**
     * Returns true if the given character is a valid filename character, false otherwise.
     */
    private fun isValidFatFilenameChar(c: Char): Boolean {
        if (0x00.toChar() <= c && c <= 0x1f.toChar()) {
            return false
        }
        return when (c) {
            '"', '*', '/', ':', '<', '>', '?', '\\', '|', 0x7f.toChar() -> false
            else -> true
        }
    }
}


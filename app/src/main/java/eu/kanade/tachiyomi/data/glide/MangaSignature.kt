package eu.kanade.tachiyomi.data.glide

import com.bumptech.glide.load.Key
import eu.kanade.tachiyomi.data.database.models.Manga
import java.io.File
import java.security.MessageDigest

class MangaSignature(manga: Manga, file: File) : Key {

    private val key = manga.thumbnail_url + file.lastModified()

    override fun equals(other: Any?): Boolean {
        return if (other is MangaSignature) {
            key == other.key
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        return key.hashCode()
    }

    override fun updateDiskCacheKey(md: MessageDigest) {
        md.update(key.toByteArray(Key.CHARSET))
    }
}

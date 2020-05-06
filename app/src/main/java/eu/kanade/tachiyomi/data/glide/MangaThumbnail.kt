package eu.kanade.tachiyomi.data.glide

import com.bumptech.glide.load.Key
import eu.kanade.tachiyomi.data.database.models.Manga
import java.security.MessageDigest

data class MangaThumbnail(val manga: Manga, val metadataDate: Long) : Key {
    val key = manga.url + metadataDate

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(key.toByteArray(Key.CHARSET))
    }
}

fun Manga.toMangaThumbnail() = MangaThumbnail(this, metadata_date)

package eu.kanade.tachiyomi.data.glide

import eu.kanade.tachiyomi.data.database.models.Manga

class MangaThumbnail(val manga: Manga) {

    val url = manga.thumbnail_url

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MangaThumbnail

        if (manga != other.manga) return false
        if (url != other.url) return false

        return true
    }

    override fun hashCode(): Int {
        var result = manga.hashCode()
        result = 31 * result + (url?.hashCode() ?: 0)
        return result
    }
}

fun Manga.toMangaThumbnail() = MangaThumbnail(this)

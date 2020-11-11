package eu.kanade.tachiyomi.source.model

import tachiyomi.source.model.MangaInfo
import java.io.Serializable

interface SManga : Serializable {

    var url: String

    var title: String

    var artist: String?

    var author: String?

    var description: String?

    var genre: String?

    var status: Int

    var thumbnail_url: String?

    var initialized: Boolean

    fun copyFrom(other: SManga) {
        if (other.author != null) {
            author = other.author
        }

        if (other.artist != null) {
            artist = other.artist
        }

        if (other.description != null) {
            description = other.description
        }

        if (other.genre != null) {
            genre = other.genre
        }

        if (other.thumbnail_url != null) {
            thumbnail_url = other.thumbnail_url
        }

        status = other.status

        if (!initialized) {
            initialized = other.initialized
        }
    }

    companion object {
        const val UNKNOWN = 0
        const val ONGOING = 1
        const val COMPLETED = 2
        const val LICENSED = 3

        fun create(): SManga {
            return SMangaImpl()
        }
    }
}

fun SManga.toMangaInfo(): MangaInfo {
    return MangaInfo(
        key = this.url,
        title = this.title,
        artist = this.artist ?: "",
        author = this.author ?: "",
        description = this.description ?: "",
        genres = this.genre?.split(", ") ?: emptyList(),
        status = this.status,
        cover = this.thumbnail_url ?: ""
    )
}

fun MangaInfo.toSManga(): SManga {
    val mangaInfo = this
    return SManga.create().apply {
        url = mangaInfo.key
        title = mangaInfo.title
        artist = mangaInfo.artist
        author = mangaInfo.author
        description = mangaInfo.description
        genre = mangaInfo.genres.joinToString(", ")
        status = mangaInfo.status
        thumbnail_url = mangaInfo.cover
    }
}

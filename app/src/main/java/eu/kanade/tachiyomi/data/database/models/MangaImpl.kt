package eu.kanade.tachiyomi.data.database.models

class MangaImpl : Manga {

    override var id: Long? = null

    override var source: Long = 0

    override lateinit var url: String

    override lateinit var title: String

    override var artist: String? = null

    override var author: String? = null

    override var description: String? = null

    override var genre: String? = null

    override var status: Int = 0

    override var thumbnail_url: String? = null

    override var favorite: Boolean = false

    override var last_update: Long = 0

    override var initialized: Boolean = false

    override var viewer: Int = 0

    override var chapter_flags: Int = 0

    @Transient override var unread: Int = 0

    @Transient override var category: Int = 0

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false

        val manga = other as Manga

        return url == manga.url

    }

    override fun hashCode(): Int {
        return url.hashCode()
    }

}

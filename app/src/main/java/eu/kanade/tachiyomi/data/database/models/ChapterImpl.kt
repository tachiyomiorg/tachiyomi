package eu.kanade.tachiyomi.data.database.models

class ChapterImpl : Chapter {

    override var id: Long? = null

    override var manga_id: Long? = null

    override lateinit var url: String

    override lateinit var name: String

    override var scanlator: String? = null

    override var read: Boolean = false

    override var bookmark: Boolean = false

    override var last_page_read: Int = 0

    override var date_fetch: Long = 0

    override var date_upload: Long = 0

    override var chapter_number: Float = 0f

    override var source_order: Int = 0

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false

        val chapter = other as Chapter
        //forces updates on manga if scanlator changes.  This will allow existing manga in library with scanlator to update
        return url == chapter.url && scanlator == chapter.scanlator

    }

    override fun hashCode(): Int {
        return url.hashCode()
    }

}
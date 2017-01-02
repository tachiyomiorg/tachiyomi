package eu.kanade.tachiyomi.data.source.model

class SChapterImpl : SChapter {

    override lateinit var url: String

    override lateinit var name: String

    override var date_fetch: Long = 0

    override var date_upload: Long = 0

    override var chapter_number: Float = 0f

    override var source_order: Int = 0

}
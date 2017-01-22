package eu.kanade.tachiyomi.ui.reader

import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.source.model.Page

class ReaderChapter(c: Chapter) : Chapter by c {

    @Transient var pages: List<Page>? = null

    var isDownloaded: Boolean = false

    var requestedPage: Int = 0
}
package eu.kanade.tachiyomi.data.database.models

import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.ui.reader.ReadingMode
import eu.kanade.tachiyomi.ui.reader.RotationType

interface Manga : SManga {

    var id: Long?

    var source: Long

    var favorite: Boolean

    var last_update: Long

    var date_added: Long

    var chapter_flags: Int

    var viewer_flags: Int

    var cover_last_modified: Long

    fun setChapterOrder(order: Int) {
        setChapterFlags(order, CHAPTER_SORT_MASK)
    }

    fun sortDescending(): Boolean {
        return chapter_flags and CHAPTER_SORT_MASK == CHAPTER_SORT_DESC
    }

    fun getGenres(): List<String>? {
        return genre?.split(", ")?.map { it.trim() }
    }

    private fun setChapterFlags(flag: Int, mask: Int) {
        chapter_flags = chapter_flags and mask.inv() or (flag and mask)
    }

    private fun setViewerFlags(flag: Int, mask: Int) {
        viewer_flags = viewer_flags and mask.inv() or (flag and mask)
    }

    // Used to display the chapter's title one way or another
    var displayMode: Int
        get() = chapter_flags and CHAPTER_DISPLAY_MASK
        set(mode) = setChapterFlags(mode, CHAPTER_DISPLAY_MASK)

    var readFilter: Int
        get() = chapter_flags and CHAPTER_READ_MASK
        set(filter) = setChapterFlags(filter, CHAPTER_READ_MASK)

    var downloadedFilter: Int
        get() = chapter_flags and CHAPTER_DOWNLOADED_MASK
        set(filter) = setChapterFlags(filter, CHAPTER_DOWNLOADED_MASK)

    var bookmarkedFilter: Int
        get() = chapter_flags and CHAPTER_BOOKMARKED_MASK
        set(filter) = setChapterFlags(filter, CHAPTER_BOOKMARKED_MASK)

    var sorting: Int
        get() = chapter_flags and CHAPTER_SORTING_MASK
        set(sort) = setChapterFlags(sort, CHAPTER_SORTING_MASK)

    var readingMode: Int
        get() = viewer_flags and ReadingMode.MASK
        set(readingMode) = setViewerFlags(readingMode, ReadingMode.MASK)

    var rotationType: Int
        get() = viewer_flags and RotationType.MASK
        set(rotationType) = setViewerFlags(rotationType, RotationType.MASK)

    companion object {

        /**
         * Generic filter that does not filter anything
         */
        const val SHOW_ALL = 0x00000000

        /* Chapter settings flags */
        const val CHAPTER_SORT_DESC = 0x00000000
        const val CHAPTER_SORT_ASC = 0x00000001
        const val CHAPTER_SORT_MASK = 0x00000001

        const val CHAPTER_SHOW_UNREAD = 0x00000002
        const val CHAPTER_SHOW_READ = 0x00000004
        const val CHAPTER_READ_MASK = 0x00000006

        const val CHAPTER_SHOW_DOWNLOADED = 0x00000008
        const val CHAPTER_SHOW_NOT_DOWNLOADED = 0x00000010
        const val CHAPTER_DOWNLOADED_MASK = 0x00000018

        const val CHAPTER_SHOW_BOOKMARKED = 0x00000020
        const val CHAPTER_SHOW_NOT_BOOKMARKED = 0x00000040
        const val CHAPTER_BOOKMARKED_MASK = 0x00000060

        const val CHAPTER_SORTING_SOURCE = 0x00000000
        const val CHAPTER_SORTING_NUMBER = 0x00000100
        const val SORTING_UPLOAD_DATE = 0x00000200
        const val CHAPTER_SORTING_MASK = 0x00000300

        const val CHAPTER_DISPLAY_NAME = 0x00000000
        const val CHAPTER_DISPLAY_NUMBER = 0x00100000
        const val CHAPTER_DISPLAY_MASK = 0x00100000

        fun create(source: Long): Manga = MangaImpl().apply {
            this.source = source
        }

        fun create(pathUrl: String, title: String, source: Long = 0): Manga = MangaImpl().apply {
            url = pathUrl
            this.title = title
            this.source = source
        }
    }
}

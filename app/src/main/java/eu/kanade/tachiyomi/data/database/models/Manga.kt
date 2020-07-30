package eu.kanade.tachiyomi.data.database.models

import eu.kanade.tachiyomi.source.model.SManga

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
        setChapterFlags(order, SORT_MASK)
    }

    fun sortDescending(): Boolean {
        return chapter_flags and SORT_MASK == SORT_DESC
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
        get() = chapter_flags and DISPLAY_MASK
        set(mode) = setChapterFlags(mode, DISPLAY_MASK)

    var readFilter: Int
        get() = chapter_flags and READ_MASK
        set(filter) = setChapterFlags(filter, READ_MASK)

    var downloadedFilter: Int
        get() = chapter_flags and DOWNLOADED_MASK
        set(filter) = setChapterFlags(filter, DOWNLOADED_MASK)

    var bookmarkedFilter: Int
        get() = chapter_flags and BOOKMARKED_MASK
        set(filter) = setChapterFlags(filter, BOOKMARKED_MASK)

    var sorting: Int
        get() = chapter_flags and SORTING_MASK
        set(sort) = setChapterFlags(sort, SORTING_MASK)

    var readingMode: Int
        get() = viewer_flags and READING_MASK
        set(readingMode) = setViewerFlags(readingMode, ROTATION_MASK)

    var rotationType: Int
        get() = viewer_flags and ROTATION_MASK
        set(rotationType) = setViewerFlags(rotationType, ROTATION_MASK)

    companion object {

        const val SORT_DESC = 0x00000000
        const val SORT_ASC = 0x00000001
        const val SORT_MASK = 0x00000001

        // Generic filter that does not filter anything
        const val SHOW_ALL = 0x00000000

        const val SHOW_UNREAD = 0x00000002
        const val SHOW_READ = 0x00000004
        const val READ_MASK = 0x00000006

        const val SHOW_DOWNLOADED = 0x00000008
        const val SHOW_NOT_DOWNLOADED = 0x00000010
        const val DOWNLOADED_MASK = 0x00000018

        const val SHOW_BOOKMARKED = 0x00000020
        const val SHOW_NOT_BOOKMARKED = 0x00000040
        const val BOOKMARKED_MASK = 0x00000060

        const val SORTING_SOURCE = 0x00000000
        const val SORTING_NUMBER = 0x00000100
        const val SORTING_UPLOAD_DATE = 0x00000200
        const val SORTING_MASK = 0x00000300

        const val DISPLAY_NAME = 0x00000000
        const val DISPLAY_NUMBER = 0x00100000
        const val DISPLAY_MASK = 0x00100000

        const val READING_DEFAULT = 0x00000000
        const val READING_L2R = 0x00000001
        const val READING_R2L = 0x00000002
        const val READING_VERTICAL = 0x00000003
        const val READING_WEBTOON = 0x000000004
        const val READING_CONT_VERTICAL = 0x00000005
        const val READING_MASK = 0x00000007

        const val ROTATION_DEFAULT = 0x00000000
        const val ROTATION_FREE = 0x00000008
        const val ROTATION_LOCK = 0x00000010
        const val ROTATION_FORCE_PORTRAIT = 0x00000018
        const val ROTATION_FORCE_LANDSCAPE = 0x00000020
        const val ROTATION_MASK = 0x00000038

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

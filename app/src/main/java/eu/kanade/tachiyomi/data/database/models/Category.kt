package eu.kanade.tachiyomi.data.database.models

import java.io.Serializable

interface Category : Serializable {

    var id: Int?

    var name: String

    var order: Int

    var flags: Int

    private fun setFlags(flag: Int, mask: Int) {
        flags = flags and mask.inv() or (flag and mask)
    }

    var displayMode: Int
        get() = flags and MASK
        set(mode) = setFlags(mode, MASK)

    var sortMode: Int
        get() = flags and SORT_MASK
        set(mode) = setFlags(mode, SORT_MASK)

    var sortDirection: Int
        get() = flags and SORT_DIRECTION_MASK
        set(mode) = setFlags(mode, SORT_DIRECTION_MASK)

    companion object {

        const val COMPACT_GRID = 0b00000000
        const val COMFORTABLE_GRID = 0b00000001
        const val LIST = 0b00000010
        const val MASK = 0b00000011

        const val ALPHABETICAL = 0b00000000
        const val LAST_READ = 0b00000100
        const val LAST_CHECKED = 0b00001000
        const val UNREAD = 0b00001100
        const val TOTAL_CHAPTERS = 0b00010000
        const val LATEST_CHAPTER = 0b00010100
        const val DATE_FETCHED = 0b00011000
        const val DATE_ADDED = 0b00011100
        const val SORT_MASK = 0b00111100 // Mask supports for more sorting flags

        const val ASCENDING = 0b00000000
        const val DESCENDING = 0b01000000
        const val SORT_DIRECTION_MASK = 0b01000000


        fun create(name: String): Category = CategoryImpl().apply {
            this.name = name
        }

        fun createDefault(): Category = create("Default").apply { id = 0 }
    }
}

package eu.kanade.tachiyomi.ui.browse.migration

import eu.kanade.tachiyomi.R

object MigrationFlags {

    private const val CHAPTERS = 0b001
    private const val CATEGORIES = 0b010
    private const val TRACK = 0b100

    private const val CHAPTERS2 = 0x1
    private const val CATEGORIES2 = 0x2
    private const val TRACK2 = 0x4

    val titles get() = arrayOf(R.string.chapters, R.string.categories, R.string.track)

    val flags get() = arrayOf(CHAPTERS, CATEGORIES, TRACK)

    fun hasChapters(value: Int): Boolean {
        return value and CHAPTERS != 0
    }

    fun hasCategories(value: Int): Boolean {
        return value and CATEGORIES != 0
    }

    fun hasTracks(value: Int): Boolean {
        return value and TRACK != 0
    }

    fun getEnabledFlagsPositions(value: Int): List<Int> {
        return flags.mapIndexedNotNull { index, flag -> if (value and flag != 0) index else null }
    }

    fun getFlagsFromPositions(positions: Array<Int>): Int {
        return positions.fold(0, { accumulated, position -> accumulated or (1 shl position) })
    }
}

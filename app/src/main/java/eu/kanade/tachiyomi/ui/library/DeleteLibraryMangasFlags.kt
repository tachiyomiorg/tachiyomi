package eu.kanade.tachiyomi.ui.library

import eu.kanade.tachiyomi.R

object DeleteLibraryMangasFlags {

    private const val FROM_LIBRARY = 0b001
    private const val CHAPTERS = 0b010

    private const val FROM_LIBRARY2 = 0x1
    private const val CHAPTERS2 = 0x2

    val titles get() = arrayOf(R.string.it_from_library, R.string.downloaded_chapters)

    val flags get() = arrayOf(FROM_LIBRARY, CHAPTERS)

    fun hasFromLibrary(value: Int): Boolean {
        return value and FROM_LIBRARY != 0
    }

    fun hasChapters(value: Int): Boolean {
        return value and CHAPTERS != 0
    }

    fun getEnabledFlagsPositions(value: Int): List<Int> {
        return flags.mapIndexedNotNull { index, flag -> if (value and flag != 0) index else null }
    }

    fun getFlagsFromPositions(positions: Array<Int>): Int {
        return positions.fold(0, { accumulated, position -> accumulated or (1 shl position) })
    }
}

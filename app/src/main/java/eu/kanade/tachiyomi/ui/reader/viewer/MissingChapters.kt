package eu.kanade.tachiyomi.ui.reader.viewer

import eu.kanade.tachiyomi.data.database.models.Chapter
import kotlin.math.floor

object MissingChapters {

    fun hasMissingChapters(highChapter: Chapter, lowerChapter: Chapter): Boolean {
        return hasMissingChapters(highChapter.chapter_number, lowerChapter.chapter_number)
    }

    fun hasMissingChapters(highChapterNumber: Float, lowerChapterNumber: Float): Boolean {
        return floor(highChapterNumber) - floor(lowerChapterNumber) - 1f > 0f
    }
}

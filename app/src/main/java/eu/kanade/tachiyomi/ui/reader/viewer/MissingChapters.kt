package eu.kanade.tachiyomi.ui.reader.viewer

import eu.kanade.tachiyomi.data.database.models.Chapter
import kotlin.math.floor

object MissingChapters {

    fun hasMissingChapters(higher: Chapter, lower: Chapter): Boolean {
        return hasMissingChapters(higher.chapter_number, lower.chapter_number)
    }

    fun hasMissingChapters(higherChapterNumber: Float, lowerChapterNumber: Float): Boolean {
        return floor(higherChapterNumber) - floor(lowerChapterNumber) - 1f > 0f
    }
}

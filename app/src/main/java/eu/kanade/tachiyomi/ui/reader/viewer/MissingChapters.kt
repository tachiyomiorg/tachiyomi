package eu.kanade.tachiyomi.ui.reader.viewer

import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import kotlin.math.floor

fun hasMissingChapters(higher: ReaderChapter?, lower: ReaderChapter?): Boolean {
    if (higher == null || lower == null) return false
    return hasMissingChapters(higher.chapter, lower.chapter)
}

fun hasMissingChapters(higher: Chapter?, lower: Chapter?): Boolean {
    if (higher == null || lower == null) return false
    if (!higher.isRecognizedNumber || !lower.isRecognizedNumber) return false
    return hasMissingChapters(higher.chapter_number, lower.chapter_number)
}

fun hasMissingChapters(higherChapterNumber: Float, lowerChapterNumber: Float): Boolean {
    return floor(higherChapterNumber) - floor(lowerChapterNumber) - 1f > 0f
}

fun calculateChapterDifference(higher: ReaderChapter?, lower: ReaderChapter?): Float {
    if (higher == null || lower == null) return 0f
    return calculateChapterDifference(higher.chapter, lower.chapter)
}

fun calculateChapterDifference(higher: Chapter?, lower: Chapter?): Float {
    if (higher == null || lower == null) return 0f
    if (!higher.isRecognizedNumber || !lower.isRecognizedNumber) return 0f
    return calculateChapterDifference(higher.chapter_number, lower.chapter_number)
}

fun calculateChapterDifference(higherChapterNumber: Float, lowerChapterNumber: Float): Float {
    return floor(higherChapterNumber) - floor(lowerChapterNumber) - 1f
}

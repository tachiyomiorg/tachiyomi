package eu.kanade.tachiyomi.ui.reader.viewer

import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import kotlin.math.floor

private val pattern = Regex("""\d+""")

fun hasMissingChapters(higherReaderChapter: ReaderChapter?, lowerReaderChapter: ReaderChapter?): Boolean {
    if (higherReaderChapter == null || lowerReaderChapter == null) return false
    return hasMissingChapters(higherReaderChapter.chapter, lowerReaderChapter.chapter)
}

fun hasMissingChapters(higherChapter: Chapter?, lowerChapter: Chapter?): Boolean {
    if (higherChapter == null || lowerChapter == null) return false
    // Check if name contains a number that is potential chapter number
    if (!pattern.containsMatchIn(higherChapter.name) || !pattern.containsMatchIn(lowerChapter.name)) return false
    // Check if potential chapter number was recognized as chapter number
    if (!higherChapter.isRecognizedNumber || !lowerChapter.isRecognizedNumber) return false
    return hasMissingChapters(higherChapter.chapter_number, lowerChapter.chapter_number)
}

fun hasMissingChapters(higherChapterNumber: Float, lowerChapterNumber: Float): Boolean {
    if (higherChapterNumber < 0f || lowerChapterNumber < 0f) return false
    return calculateChapterDifference(higherChapterNumber, lowerChapterNumber) > 0f
}

fun calculateChapterDifference(higherReaderChapter: ReaderChapter?, lowerReaderChapter: ReaderChapter?): Float {
    if (higherReaderChapter == null || lowerReaderChapter == null) return 0f
    return calculateChapterDifference(higherReaderChapter.chapter, lowerReaderChapter.chapter)
}

fun calculateChapterDifference(higherChapter: Chapter?, lowerChapter: Chapter?): Float {
    if (higherChapter == null || lowerChapter == null) return 0f
    // Check if name contains a number that is potential chapter number
    if (!pattern.containsMatchIn(higherChapter.name) || !pattern.containsMatchIn(lowerChapter.name)) return 0f
    // Check if potential chapter number was recognized as chapter number
    if (!higherChapter.isRecognizedNumber || !lowerChapter.isRecognizedNumber) return 0f
    return calculateChapterDifference(higherChapter.chapter_number, lowerChapter.chapter_number)
}

fun calculateChapterDifference(higherChapterNumber: Float, lowerChapterNumber: Float): Float {
    if (higherChapterNumber < 0f || lowerChapterNumber < 0f) return 0f
    return floor(higherChapterNumber) - floor(lowerChapterNumber) - 1f
}

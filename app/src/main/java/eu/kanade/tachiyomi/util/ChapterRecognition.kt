package eu.kanade.tachiyomi.util

import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga

/**
 * -R> = regex conversion.
 */
object ChapterRecognition {
    /**
     * All cases with Ch.xx
     * Mokushiroku Alice Vol.1 Ch. 4: Misrepresentation -R> 4
     */
    private val basic = Regex("""(?<=ch\.)([0-9]+)(\.[0-9]+)?(\.?[a-z]+)?""")

    /**
     * Regex used when only one number occurrence
     * Example: Bleach 567: Down With Snowwhite -R> 567
     */
    private val occurrence = Regex("""([0-9]+)(\.[0-9]+)?(\.?[a-z]+)?""")

    /**
     * Regex used when manga title removed
     * Example: Solanin 028 Vol. 2 -> 028 Vol.2 -> 028Vol.2 -R> 028
     */
    private val withoutManga = Regex("""^([0-9]+)(\.[0-9]+)?(\.?[a-z]+)?""")

    /**
     * Regex used to remove unwanted tags
     * Example Prison School 12 v.1 vol004 version1243 volume64 -R> Prison School 12
     */
    private val unwanted = Regex("""(?:(v|ver|vol|version|volume|season|s).?[0-9]+)""")

    /**
     * Regex used to remove unwanted whitespace
     * Example One Piece 12 special -R> One Piece 12special
     */
    private val unwantedWhiteSpace = Regex("""(\s)(extra|special|omake)""")

    fun parseChapterNumber(chapter: Chapter, manga: Manga) {
        // If chapter number is known return.
        if (chapter.chapter_number == -2f || chapter.chapter_number > -1f)
            return

        // Get chapter title with lower case
        var name = chapter.name.toLowerCase()

        // Remove comma's from chapter.
        name = name.replace(',', '.')

        // Remove unwanted white spaces.
        unwantedWhiteSpace.findAll(name).let {
            it.forEach { occurrence -> name = name.replace(occurrence.value, occurrence.value.trim()) }
        }

        // Remove unwanted tags.
        unwanted.findAll(name).let {
            it.forEach { occurrence -> name = name.replace(occurrence.value, "") }
        }

        // Check base case ch.xx
        if (updateChapter(basic.find(name), chapter))
            return

        // Check one number occurrence.
        val occurrences: MutableList<MatchResult> = arrayListOf()
        occurrence.findAll(name).let {
            it.forEach { occurrence -> occurrences.add(occurrence) }
        }

        if (occurrences.size == 1) {
            if (updateChapter(occurrences[0], chapter))
                return
        }

        // Remove manga title from chapter title.
        val nameWithoutManga = name.replace(manga.title.toLowerCase(), "").trim()

        // Check if first value is number after title remove.
        if (updateChapter(withoutManga.find(nameWithoutManga), chapter))
            return

        // Take the first number encountered.
        if (updateChapter(occurrence.find(nameWithoutManga), chapter))
            return
    }

    /**
     * Check if volume is found and update chapter
     * @param match result of regex
     * @param chapter chapter object
     * @return true if volume is found
     */
    fun updateChapter(match: MatchResult?, chapter: Chapter): Boolean {
        match?.let {
            val initial = it.groups[1]?.value?.toFloat()!!
            val subChapterDecimal = it.groups[2]?.value
            val subChapterAlpha = it.groups[3]?.value
            val addition = checkForDecimal(subChapterDecimal, subChapterAlpha)
            chapter.chapter_number = initial.plus(addition)
            return true
        }
        return false
    }

    /**
     * Check for decimal in received strings
     * @param decimal decimal value of regex
     * @param alpha alpha value of regex
     * @return decimal/alpha float value
     */
    fun checkForDecimal(decimal: String?, alpha: String?): Float {
        if (!decimal.isNullOrEmpty())
            return decimal?.toFloat()!!

        if (!alpha.isNullOrEmpty()) {
            if (alpha!!.contains("extra"))
                return .99f

            if (alpha.contains("omake"))
                return .98f

            if (alpha.contains("special"))
                return .97f

            if (alpha[0] == '.') {
                // Take value after (.)
                return parseAlphaPostFix(alpha[1])
            } else {
                return parseAlphaPostFix(alpha[0])
            }
        }

        return .0f
    }

    /**
     * x.a -> x.1, x.b -> x.2, etc
     */
    private fun parseAlphaPostFix(alpha: Char): Float {
        return ("0." + Integer.toString(alpha.toInt() - 96)).toFloat()
    }

}

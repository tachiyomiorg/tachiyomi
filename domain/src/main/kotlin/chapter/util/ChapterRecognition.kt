/*
 * Copyright (C) 2018 The Tachiyomi Open Source Project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package tachiyomi.domain.chapter.util

import tachiyomi.domain.manga.model.Manga
import tachiyomi.source.CatalogSource
import tachiyomi.source.model.ChapterInfo
import tachiyomi.source.Source

/**
 * -R> = regex conversion.
 */
object ChapterRecognition {

  /**
   * All cases with Ch.xx
   * Mokushiroku Alice Vol.1 Ch. 4: Misrepresentation -R> 4
   */
  private val basic = Regex("""(?<=ch\.) *([0-9]+)(\.[0-9]+)?(\.?[a-z]+)?""")

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
  private val unwanted = Regex("""(?<![a-z])(v|ver|vol|version|volume|season|s).?[0-9]+""")

  /**
   * Regex used to remove unwanted whitespace
   * Example One Piece 12 special -R> One Piece 12special
   */
  private val unwantedWhiteSpace = Regex("""(\s)(extra|special|omake)""")

  /**
   * Parse using source regex if available otherwise use default parser (less acurate),
   * @param chapter object containing all information of chapter being parsed.
   * @param manga object containing the information of the manga being parsed.
   * @param source source object used to get regex if available.
   * @return chapter number TODO implement (vol|title? create general rules how to format Regex)
   */
  fun parse(chapter: ChapterInfo, manga: Manga, source: Source): Float {
    return if (!source.getRegex().pattern.isEmpty()) {
      return findMatch(source.getRegex().find(chapter.name))
    } else {
      parseDefault(chapter, manga)
    }
  }

  /**
   * Todo Rewrite to include title / volume, is this even needed with source providing regex?
   */
  private fun parseDefault(chapter: ChapterInfo, manga: Manga): Float {

//    // If chapter number is known return.
//    if (chapter.number == -2f || chapter.number > -1f)
//      return

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
    var number: Float?

    number = findMatch(basic.find(name))
    if (number != -1f) return number

    // Check one number occurrence.
    val occurrences: MutableList<MatchResult> = arrayListOf()
    occurrence.findAll(name).let {
      it.forEach { occurrence -> occurrences.add(occurrence) }
    }

    if (occurrences.size == 1) {
      number = findMatch(occurrences[0])
      if (number != -1f) return number
    }

    // Remove manga title from chapter title.
    val nameWithoutManga = name.replace(manga.title.toLowerCase(), "").trim()

    // Check if first value is number after title remove.
    number = findMatch(withoutManga.find(nameWithoutManga))
    if (number != -1f) return number

    // Take the first number encountered.
    number = findMatch(occurrence.find(nameWithoutManga))
    if (number != -1f) return number

    return -1f
  }

  /**
   * Check if volume is found and update chapter
   * @param match result of regex
   * @return true if volume is found
   */
  private fun findMatch(match: MatchResult?): Float {
    match?.let {
      val initial = it.groups[1]?.value?.toFloat()!!
      val subChapterDecimal = it.groups[2]?.value
      val subChapterAlpha = it.groups[3]?.value
      val addition = checkForDecimal(subChapterDecimal, subChapterAlpha)
      return initial.plus(addition)
    }
    return -1f
  }

  /**
   * Check for decimal in received strings
   * @param decimal decimal value of regex
   * @param alpha alpha value of regex
   * @return decimal/alpha float value
   */
  private fun checkForDecimal(decimal: String?, alpha: String?): Float {
    if (!decimal.isNullOrEmpty())
      return decimal.toFloat()

    if (!alpha.isNullOrEmpty()) {
      if (alpha.contains("extra"))
        return .99f

      if (alpha.contains("omake"))
        return .98f

      if (alpha.contains("special"))
        return .97f

      return if (alpha[0] == '.') {
        // Take value after (.)
        parseAlphaPostFix(alpha[1])
      } else {
        parseAlphaPostFix(alpha[0])
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

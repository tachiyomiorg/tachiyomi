package eu.kanade.tachiyomi.util

import java.lang.Math.floor

/**
 * Replaces the given string to have at most [count] characters using [replacement] at its end.
 * If [replacement] is longer than [count] an exception will be thrown when `length > count`.
 */
fun String.chop(count: Int, replacement: String = "..."): String {
    return if (length > count)
        take(count - replacement.length) + replacement
    else
        this

}

/**
 * Replaces the given string to have at most [count] characters using [replacement] near the center.
 * If [replacement] is longer than [count] an exception will be thrown when `length > count`.
 */
fun String.truncateCenter(count: Int, replacement: String = "..."): String{
    if(length <= count)
        return this

    val pieceLength:Int = floor((count - replacement.length).div(2.0)).toInt()

    return "${ take(pieceLength) }$replacement${ takeLast(pieceLength) }"
}
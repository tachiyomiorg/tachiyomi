package eu.kanade.tachiyomi.ui.library

object LibraryTriStateFilter {

    const val IGNORED = 0
    const val INCLUDED = 1
    const val EXCLUDED = 2

    fun isIgnored(state: Int) = state == IGNORED

    fun isIncluded(state: Int) = state == INCLUDED
}
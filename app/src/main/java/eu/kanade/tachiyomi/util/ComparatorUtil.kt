package eu.kanade.tachiyomi.util

object ComparatorUtil {
    val CaseInsensitiveNaturalComparator = compareBy<String, String>(String.CASE_INSENSITIVE_ORDER) { it }.then(naturalOrder())
}

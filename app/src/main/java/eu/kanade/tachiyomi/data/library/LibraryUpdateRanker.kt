package eu.kanade.tachiyomi.data.library

import eu.kanade.tachiyomi.data.database.models.Manga

/**
 * This class will provide various functions to rank manga to efficiently schedule manga to update.
 */
object LibraryUpdateRanker {

    val rankingScheme = listOf(
        (this::lexicographicRanking)(),
        (this::latestFirstRanking)()
    )

    /**
     * Provides a total ordering over all the [Manga]s.
     *
     * Assumption: An active [Manga] mActive is expected to have been last updated after an
     * inactive [Manga] mInactive.
     *
     * Using this insight, function returns a Comparator for which mActive appears before mInactive.
     * @return a Comparator that ranks manga based on relevance.
     */
    private fun latestFirstRanking(): Comparator<Manga> =
        Comparator { first: Manga, second: Manga ->
            compareValues(second.last_update, first.last_update)
        }

    /**
     * Provides a total ordering over all the [Manga]s.
     *
     * Order the manga lexicographically.
     * @return a Comparator that ranks manga lexicographically based on the title.
     */
    private fun lexicographicRanking(): Comparator<Manga> =
        Comparator { first: Manga, second: Manga ->
            compareValues(first.title, second.title)
        }
}

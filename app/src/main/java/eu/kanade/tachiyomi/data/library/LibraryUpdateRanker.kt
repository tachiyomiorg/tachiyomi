package eu.kanade.tachiyomi.data.library

import eu.kanade.tachiyomi.data.database.models.Manga

/**
 * This class will provide various functions to Rank mangas to efficiently schedule mangas to update.
 */
object LibraryUpdateRanker {

    val rankingScheme = listOf(
            (this::lexicographicRanking)(),
            (this::latestFirstRanking)())

    /**
     * Provides a total ordering over all the Mangas.
     *
     * Assumption: An active [Manga] mActive is expected to have been last updated after an
     * inactive [Manga] mInactive.
     *
     * Using this insight, function returns a Comparator for which mActive appears before mInactive.
     * @return a Comparator that ranks manga based on relevance.
     */
    fun latestFirstRanking(): Comparator<Manga> {
        return Comparator { mangaFirst: Manga,
                            mangaSecond: Manga ->
            compareValues(mangaSecond.last_update, mangaFirst.last_update)
        }
    }

    /**
     * Provides a total ordering over all the Mangas.
     *
     * Order the manga lexicographically.
     * @return a Comparator that ranks manga lexicographically based on the title.
     */
    fun lexicographicRanking(): Comparator<Manga> {
        return Comparator { mangaFirst: Manga,
                                   mangaSecond: Manga ->
            compareValues(mangaFirst.title, mangaSecond.title)
        }
    }

}
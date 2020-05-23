package eu.kanade.tachiyomi.ui.more

import android.content.Context
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.ui.base.controller.withFadeTransaction
import eu.kanade.tachiyomi.ui.setting.SettingsController
import eu.kanade.tachiyomi.util.preference.onClick
import eu.kanade.tachiyomi.util.preference.preference
import eu.kanade.tachiyomi.util.preference.preferenceCategory
import eu.kanade.tachiyomi.util.preference.titleRes
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class StatsMainController : SettingsController() {

    private val db: DatabaseHelper = Injekt.get()

    // Needed to get names from sources' longs
    private val sourceManager: SourceManager by injectLazy()

    // Favorited manga
    private var faves: List<Manga>

    // Map of sources' longs to their actual name 
    private var longNameMap: Map<Long, String>

    // Map of sources to their StatusCounts
    private val sourceStatusMap = mutableMapOf<String, StatusCounts>()

    // Map of categories and the mangas they contain
    private val categoryMangasMap: MutableMap<String?, MutableSet<Manga>>

    // Map of categories and the genres they contain
    private val categoryGenresMap: MutableMap<String?, MutableMap<String, Int>>

    // Map of genres with appearance count, using a separate map in case manga appear in multiple categories
    private val overallGenresMap = mutableMapOf<String, Int>()

    init {
        val categories = db.getCategories().executeAsBlocking()
        categoryMangasMap = categories.associate { Pair(it.name, mutableSetOf<Manga>()) }.toMutableMap()
        categoryGenresMap = categories.associate { Pair(it.name, mutableMapOf<String, Int>()) }.toMutableMap()
        faves = db.getFavoriteMangas().executeAsBlocking()
        longNameMap = faves.map { Pair(it.source, sourceManager.get(it.source)?.name ?: it.source.toString()) }.toMap()

        faves.forEach { manga ->
            buildSourceStatusMap(manga, sourceStatusMap, longNameMap)
            buildCategoryMaps(manga)
        }
    }

    @Suppress("MapGetWithNotNullAssertionOperator")
    private fun buildCategoryMaps(manga: Manga) {
        fun MutableMap<String, Int>.addGenres(genreList: List<String>?) {
            genreList?.let { genres ->
                genres.forEach { genre ->
                    if (genre !in this.keys) this[genre] = 1 else this.plusAssign(Pair(genre, this[genre]!! + 1))
                }
            }
        }

        db.getCategoriesForManga(manga).executeAsBlocking().let { catList ->
            catList.forEach { cat ->
                categoryMangasMap[cat.name]!! += manga
                categoryGenresMap[cat.name]!!.addGenres(manga.getGenres())
                overallGenresMap.addGenres(manga.getGenres())
            }
            if (catList.isEmpty()) {
                // add default category as a null key
                if (categoryMangasMap[null].isNullOrEmpty()) {
                    categoryMangasMap[null] = mutableSetOf()
                    categoryGenresMap[null] = mutableMapOf()
                }
                categoryMangasMap[null]!! += manga
                categoryGenresMap[null]!!.addGenres(manga.getGenres())
            }
        }
    }

    override fun setupPreferenceScreen(screen: PreferenceScreen) = with(screen) {
        titleRes = R.string.label_stats

        preference {
            titleRes = R.string.stats_status_totals

            summary = sourceStatusSummary(totalManga(sourceStatusMap), context)
        }

        preferenceCategory {
            val totalCats = categoryMangasMap.count()

            title = context.resources.getQuantityString(R.plurals.num_categories, totalCats, totalCats)

            preference {
                titleRes = R.string.stats_manga_per_category

                summary = categoryMangasMap.entries.joinToString { "${it.key ?: context.getString(R.string.default_category)} : ${it.value.count()}" }
                onClick {
                    router.pushController(StatsCategoryController(categoryMangasMap, longNameMap, categoryGenresMap, overallGenresMap).withFadeTransaction())
                }
            }

            preference {
                titleRes = R.string.stats_genres_overview

                summary = overallGenresMap.topGenres(5)

                onClick {
                    router.pushController(StatsCategoryController(categoryMangasMap, longNameMap, categoryGenresMap, overallGenresMap).withFadeTransaction())
                }
            }
        }

        preferenceCategory {
            titleRes = R.string.stats_source_status_totals
            summary = "${context.getString(R.string.stats_sources_with_faves)}: ${sourceStatusMap.count()}"

            sourceStatusMap.entries
                .sortedByDescending { it.value.total }
                .forEach { mapEntry ->
                    preference {
                        title = mapEntry.key

                        summary = sourceStatusSummary(mapEntry.value, context)
                    }
                }
        }
    }

    companion object {
        @Suppress("MapGetWithNotNullAssertionOperator")
        fun buildSourceStatusMap(manga: Manga, sourceStatusMap: MutableMap<String, StatusCounts>, longNameMap: Map<Long, String>) {
            val name = longNameMap[manga.source]!!
            if (name !in sourceStatusMap.keys) sourceStatusMap[name] = StatusCounts()
            when (manga.status) {
                1 -> sourceStatusMap[name]!!.ongoing++
                2 -> sourceStatusMap[name]!!.completed++
                3 -> sourceStatusMap[name]!!.licensed++
                else -> sourceStatusMap[name]!!.unknown++
            }
            sourceStatusMap[name]!!.total++
        }

        @Suppress("MapGetWithNotNullAssertionOperator")
        fun totalManga(sourceStatusMap: Map<String, StatusCounts>): StatusCounts {
            val totals = StatusCounts()
            sourceStatusMap.keys.forEach { key ->
                totals.ongoing += sourceStatusMap[key]!!.ongoing
                totals.completed += sourceStatusMap[key]!!.completed
                totals.unknown += sourceStatusMap[key]!!.unknown
                totals.licensed += sourceStatusMap[key]!!.licensed
                totals.total += sourceStatusMap[key]!!.total
            }
            return totals
        }

        fun sourceStatusSummary(totals: StatusCounts, context: Context): String {
            return "${totals.total} ${context.getString(R.string.stats_total)} | ${totals.ongoing} ${context.getString(R.string.ongoing)}" +
                " | ${totals.unknown} ${context.getString(R.string.unknown)} | ${totals.completed} ${context.getString(R.string.completed)}" +
                " | ${totals.licensed} ${context.getString(R.string.licensed)}"
        }

        fun MutableMap<String, Int>.topGenres(take: Int): String {
            return this.entries.sortedByDescending { it.value }.take(take).joinToString { "${it.key} : ${it.value}" }
        }

        data class StatusCounts(var ongoing: Int = 0, var completed: Int = 0, var unknown: Int = 0, var licensed: Int = 0, var total: Int = 0)
    }
}

package eu.kanade.tachiyomi.ui.more

import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.ui.more.StatsMainController.Companion.StatusCounts
import eu.kanade.tachiyomi.ui.more.StatsMainController.Companion.buildSourceStatusMap
import eu.kanade.tachiyomi.ui.more.StatsMainController.Companion.sourceStatusSummary
import eu.kanade.tachiyomi.ui.more.StatsMainController.Companion.topGenres
import eu.kanade.tachiyomi.ui.more.StatsMainController.Companion.totalManga
import eu.kanade.tachiyomi.ui.setting.SettingsController
import eu.kanade.tachiyomi.util.preference.preference
import eu.kanade.tachiyomi.util.preference.preferenceCategory
import eu.kanade.tachiyomi.util.preference.titleRes

class StatsCategoryController(
    private val categoryMangasMap: Map<String?, Set<Manga>> = emptyMap(),
    private val longNameMap: Map<Long, String> = emptyMap(),
    private val categoryGenresMap: MutableMap<String?, MutableMap<String, Int>> = mutableMapOf(),
    private val overallGenresMap: MutableMap<String, Int> = mutableMapOf()
) : SettingsController() {

    override fun setupPreferenceScreen(screen: PreferenceScreen) = with(screen) {
        titleRes = R.string.stats_category_breakdown

        preference {
            titleRes = R.string.stats_overall_top_genres

            summary = overallGenresMap.topGenres(20)
        }

        categoryMangasMap.forEach { catMangasEntry ->
            val sourceStatusMap = mutableMapOf<String, StatusCounts>()
            catMangasEntry.value.forEach { manga -> buildSourceStatusMap(manga, sourceStatusMap, longNameMap) }

            preferenceCategory {
                title = "${catMangasEntry.key ?: context.getString(R.string.default_category)} : ${sourceStatusSummary(totalManga(sourceStatusMap), context)}"

                preference {
                    titleRes = R.string.stats_genres_overview

                    summary = categoryGenresMap[catMangasEntry.key]!!.topGenres(5)
                }

                sourceStatusMap.entries
                    .sortedByDescending { it.value.total }
                    .forEach { sourceStatusEntry ->
                        preference {
                            title = sourceStatusEntry.key

                            summary = sourceStatusSummary(sourceStatusEntry.value, context)
                        }
                    }
            }
        }
    }
}

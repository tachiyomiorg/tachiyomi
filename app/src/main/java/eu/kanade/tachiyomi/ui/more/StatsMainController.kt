package eu.kanade.tachiyomi.ui.more

import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.ui.setting.SettingsController
import eu.kanade.tachiyomi.util.preference.preference
import eu.kanade.tachiyomi.util.preference.preferenceCategory
import eu.kanade.tachiyomi.util.preference.titleRes
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class StatsMainController : SettingsController() {
    private val db: DatabaseHelper = Injekt.get()
    // Needed to get names for source IDs
    internal val sourceManager: SourceManager by injectLazy()
    // Favorited manga
    private val faves = db.getFavoriteMangas().executeAsBlocking()

    private data class StatusCounts(var ongoing: Int = 0, var completed: Int = 0, var unknown: Int = 0, var licensed: Int = 0, var total: Int = 0)
    @Suppress("MapGetWithNotNullAssertionOperator")
    private val sourceStatusMap by lazy {
        val ssMap = mutableMapOf<Long, StatusCounts>()
        faves.forEach { manga ->
            if (manga.source !in ssMap.keys) ssMap[manga.source] = StatusCounts()
            when (manga.status) {
                1 -> ssMap[manga.source]!!.ongoing++
                2 -> ssMap[manga.source]!!.completed++
                3 -> ssMap[manga.source]!!.licensed++
                else -> ssMap[manga.source]!!.unknown++
            }
            ssMap[manga.source]!!.total++
        }
        val idNameMap = ssMap.keys
            .map { Pair(it, sourceManager.get(it)?.name ?: it.toString()) }
            .toMap()
        ssMap.keys
            .map { Pair(it, ssMap[it]!!.total) } // id, total manga
            .sortedByDescending { it.second } // sort by totals
            .map { it.first } // sorted keys
            .map { Pair(idNameMap[it]!!, ssMap[it]!!) } // build a sorted collection with source names
            .toMap()
    }

    @Suppress("MapGetWithNotNullAssertionOperator")
    private fun totalManga(): StatusCounts {
        val totals = StatusCounts()
        sourceStatusMap.keys.forEach { key -> totals.ongoing += sourceStatusMap[key]!!.ongoing }
        sourceStatusMap.keys.forEach { key -> totals.completed += sourceStatusMap[key]!!.completed }
        sourceStatusMap.keys.forEach { key -> totals.unknown += sourceStatusMap[key]!!.unknown }
        sourceStatusMap.keys.forEach { key -> totals.licensed += sourceStatusMap[key]!!.licensed }
        sourceStatusMap.keys.forEach { key -> totals.total += sourceStatusMap[key]!!.total }
        return totals
    }

    override fun setupPreferenceScreen(screen: PreferenceScreen) = with(screen) {
        titleRes = R.string.label_stats

        preference {
            titleRes = R.string.stats_status_totals

            val totals = totalManga()
            summary = "${totals.total} ${context.getString(R.string.stats_total)} | ${totals.ongoing} ${context.getString(R.string.ongoing)}" +
                " | ${totals.unknown} ${context.getString(R.string.unknown)} | ${totals.completed} ${context.getString(R.string.completed)}" +
                " | ${totals.licensed} ${context.getString(R.string.licensed)}"
        }

        preferenceCategory {
            titleRes = R.string.stats_source_status_totals
            summary = "${context.getString(R.string.stats_sources_with_faves)}: ${sourceStatusMap.count()}"

            sourceStatusMap.forEach { mapEntry ->
                preference {
                    title = mapEntry.key

                    val totals = mapEntry.value
                    summary = "${totals.total} ${context.getString(R.string.stats_total)} | ${totals.ongoing} ${context.getString(R.string.ongoing)}" +
                        " | ${totals.unknown} ${context.getString(R.string.unknown)} | ${totals.completed} ${context.getString(R.string.completed)}" +
                        " | ${totals.licensed} ${context.getString(R.string.licensed)}"
                }
            }
        }
    }
}

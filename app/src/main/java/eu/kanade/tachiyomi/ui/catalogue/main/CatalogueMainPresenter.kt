package eu.kanade.tachiyomi.ui.catalogue.main

import android.os.Bundle
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.LocalSource
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Presenter of [CatalogueMainController]
 * Function calls should be done from here. UI calls should be done from the controller.
 *
 * @param sourceManager manages the different sources.
 * @param preferences application preferences.
 */
class CatalogueMainPresenter(
        val sourceManager: SourceManager = Injekt.get(),
        private val preferences: PreferencesHelper = Injekt.get()
) : BasePresenter<CatalogueMainController>() {

    /**
     * Enabled sources.
     */
    var sources = getEnabledSources()

    /**
     * Subscription for retrieving enabled sources.
     */
    private var sourceSubscription: Subscription? = null

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        // Load enabled and last used sources
        loadSources()
        loadLastUsedSource()
    }

    /**
     * Unsubscribe and create a new subscription to fetch enabled sources.
     */
    fun loadSources() {
        sourceSubscription?.unsubscribe()

        val map = TreeMap<String, MutableList<CatalogueSource>> { d1, d2 -> d1.compareTo(d2) }
        val byLang = sources.groupByTo(map, { it.lang })
        val sourceItems = byLang.flatMap {
            val langItem = LangItem(it.key)
            it.value.map { source -> SourceItem(source, langItem) }
        }

        sourceSubscription = Observable.just(sourceItems)
                .subscribeLatestCache(CatalogueMainController::setSources)
    }

    private fun loadLastUsedSource() {
        val sharedObs = preferences.lastUsedCatalogueSource().asObservable().share()

        // Emit the first item immediately but delay subsequent emissions by 500ms.
        Observable.merge(
                sharedObs.take(1),
                sharedObs.skip(1).delay(500, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread()))
                .distinctUntilChanged()
                .map { (sourceManager.get(it) as? CatalogueSource)?.let { SourceItem(it) } }
                .subscribeLatestCache(CatalogueMainController::setLastUsedSource)
    }

    fun updateSources() {
        sources = getEnabledSources()
        loadSources()
    }

    /**
     * Returns a list of enabled sources ordered by language and name.
     *
     * @return list containing enabled sources.
     */
    private fun getEnabledSources(): List<CatalogueSource> {
        val languages = preferences.enabledLanguages().getOrDefault()
        val hiddenCatalogues = preferences.hiddenCatalogues().getOrDefault()

        return sourceManager.getCatalogueSources()
                .filter { it.lang in languages }
                .filterNot { it.id.toString() in hiddenCatalogues }
                .sortedBy { "(${it.lang}) ${it.name}" } +
                sourceManager.get(LocalSource.ID) as LocalSource
    }
}

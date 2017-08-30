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

/**
 * Presenter of [CatalogueMainController]
 * Function calls should be done from here. UI calls should be done from the controller.
 *
 * @param sourceManager manages the different sources.
 * @param preferencesHelper manages the database calls.
 */
class CatalogueMainPresenter(
        val sourceManager: SourceManager = Injekt.get(),
        private val preferencesHelper: PreferencesHelper = Injekt.get()
) : BasePresenter<CatalogueMainController>() {

    /**
     * Enabled sources.
     */
    var sources = getEnabledSources()

    /**
     * Subscription for retrieving enabled sources.
     */
    private var sourceSubscription: Subscription? = null

    /**
     * Subscription for retrieving most recent used source.
     */
    private var recentSourceSubscription: Subscription? = null

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        // Load enabled sources
        loadSources()

        // Load most recently used source.
        loadRecentSources()
    }

    override fun onDestroy() {
        // Unsubscribe subscriptions.
        recentSourceSubscription?.unsubscribe()
        sourceSubscription?.unsubscribe()
        super.onDestroy()
    }

    /**
     * Unsubscribe and create a new subscription to fetch enabled sources.
     */
    fun loadSources() {
        sourceSubscription?.unsubscribe()
        sourceSubscription = Observable.from(sources)
                .groupBy { it.lang }.flatMap{ group -> group.toList().map { group.key to it } } //Group by language.
                .map(::CatalogueMainItem).toList() // Map to CatalogueMainItem.
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeLatestCache(CatalogueMainController::setSources)
    }

    /**
     * Unsubscribe and create a new subscription to fetch most recent catalogue.
     */
    fun loadRecentSources(){
        recentSourceSubscription?.unsubscribe()
        recentSourceSubscription = preferencesHelper.lastUsedCatalogueSource().asObservable()
                .map { sourceManager.get(it) as CatalogueSource } // Retrieve catalogue.
                .map { Pair("recent", listOf(it)) } // Create recent item pair.
                .map (::CatalogueMainItem) // Map to CatalogueMainItem.
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeLatestCache(CatalogueMainController::setLastUsedSource)
    }

    /**
     * Update the last used source
     *
     * @param id id of source.
     */
    fun setLastUsedSource(id: Long){
        preferencesHelper.lastUsedCatalogueSource().set(id)
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
        val languages = preferencesHelper.enabledLanguages().getOrDefault()
        val hiddenCatalogues = preferencesHelper.hiddenCatalogues().getOrDefault()

        return sourceManager.getCatalogueSources()
                .filter { it.lang in languages }
                .filterNot { it.id.toString() in hiddenCatalogues }
                .sortedBy { "(${it.lang}) ${it.name}" } +
                sourceManager.get(LocalSource.ID) as LocalSource
    }
}

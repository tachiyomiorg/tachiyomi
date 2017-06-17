package eu.kanade.tachiyomi.ui.catalogue.main

import android.os.Bundle
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get


class CatalogueMainPresenter(
        val sourceManager: SourceManager = Injekt.get(),
        val db: DatabaseHelper = Injekt.get(),
        val prefs: PreferencesHelper = Injekt.get()
) : BasePresenter<CatalogueMainController>() {

    /**
     * Enabled sources.
     */
    val sources by lazy { getEnabledSources() }

    private var sourceSubscription: Subscription? = null
    private var recentSourceSubscription: Subscription? = null

    /**
     * {@inheritDoc}
     */
    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        loadSources()

        loadRecentSources()
    }

    fun loadSources() {
        sourceSubscription?.unsubscribe()
        sourceSubscription = Observable.from(sources)
                .groupBy { it.lang }.flatMap { group -> group.toList().map { group.key to it } }
                .map(::CatalogueMainItem).toList()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeLatestCache(CatalogueMainController::setSources)
    }

    fun loadRecentSources(){
        recentSourceSubscription?.unsubscribe()
        recentSourceSubscription = prefs.lastUsedCatalogueSource().asObservable()
                .map { sourceManager.get(it) as CatalogueSource }
                .map { Pair("recent", listOf(it)) }
                .map (::CatalogueMainItem)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeLatestCache(CatalogueMainController::setLastUsedSource)
    }

    /**
     * {@inheritDoc}
     */
    override fun onDestroy() {
        recentSourceSubscription?.unsubscribe()
        sourceSubscription?.unsubscribe()
        super.onDestroy()
    }

    /**
     * Returns a list of enabled sources ordered by language and name.
     */
    fun getEnabledSources(): List<CatalogueSource> {
        val languages = prefs.enabledLanguages().getOrDefault()
        val hiddenCatalogues = prefs.hiddenCatalogues().getOrDefault()

        // Ensure at least one language
        if (languages.isEmpty()) {
            languages.add("en")
        }

        return sourceManager.getCatalogueSources()
                .filter { it.lang in languages }
                .filterNot { it.id.toString() in hiddenCatalogues }
                .sortedBy { "(${it.lang}) ${it.name}" }
    }
}

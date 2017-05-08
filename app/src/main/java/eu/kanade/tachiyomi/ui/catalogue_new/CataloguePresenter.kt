package eu.kanade.tachiyomi.ui.catalogue_new

import android.os.Bundle
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.LocalSource
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class CataloguePresenter(
        val sourceManager: SourceManager = Injekt.get(),
        val db: DatabaseHelper = Injekt.get(),
        val prefs: PreferencesHelper = Injekt.get()
) : BasePresenter<CatalogueController>() {

    /**
     * Enabled sources.
     */
    val sources by lazy { getEnabledSources() }

    /**
     * Favorite sources.
     */
    val sourcesFavorite by lazy { getEnabledSources(true) }

    /**
     * Called when presenter is created
     */
    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        Observable.just(sources)
                .map { it.map(::CatalogueItem) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeLatestCache(CatalogueController::setSources)

        Observable.just(sourcesFavorite)
                .map { it.map(::CatalogueItem) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeLatestCache(CatalogueController::setSourcesFavorite)

        prefs.lastUsedCatalogueSource().asObservable()
                .map { sourceManager.get(it) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeLatestCache(CatalogueController::setLastUsedSource)
    }

    fun setLastUsedSource(key: Long) {
        prefs.lastUsedCatalogueSource().set(key)
    }

    /**
     * Returns a list of enabled sources ordered by language and name.
     */
    fun getEnabledSources(favorite: Boolean = false): List<CatalogueSource> {
        val languages = prefs.enabledLanguages().getOrDefault()
        val hiddenCatalogues = prefs.hiddenCatalogues().getOrDefault()
        val favoriteCatalogues = prefs.favoriteCatalogues().getOrDefault()

        // Ensure at least one language
        if (languages.isEmpty()) {
            languages.add("en")
        }

        return sourceManager.getCatalogueSources()
                .filter { it.lang in languages }
                .filterNot { it.id.toString() in hiddenCatalogues }
                .filter { if (favorite) it.id.toString() in favoriteCatalogues else it.id.toString() !in favoriteCatalogues }
                .filterNot { it is LocalSource }
                .sortedBy { "(${it.lang}) ${it.name}" }
    }
}
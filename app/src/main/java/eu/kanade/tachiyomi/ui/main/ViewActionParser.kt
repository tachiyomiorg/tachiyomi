package eu.kanade.tachiyomi.ui.main

import android.content.Intent
import android.util.Log
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.ui.catalogue.browse.BrowseCatalogueController
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Parse data uris from an [Intent.ACTION_VIEW], in particular ones that have a host of `search`
 */
class ViewActionParser(val intent: Intent) {
    private val sourceManager: SourceManager = Injekt.get()
    private val sourceName: String?
    private val searchQuery: String?

    init {
        val pathSegments = intent.data?.pathSegments
        if (pathSegments != null && pathSegments.size == 2) {
            sourceName = pathSegments[0]
            searchQuery = pathSegments[1]
        } else {
            sourceName = null
            searchQuery = null
        }
    }

    fun handleLink(router: Router): Boolean {
        val source = sourceName?.toSource()
        if (source != null && searchQuery != null) {
            router.setRoot(RouterTransaction.with(BrowseCatalogueController(source, searchQuery)))
            return true
        } else {
            Log.e("...", "could not open intent $intent")
            return false
        }
    }

    private fun String.toSource(): CatalogueSource? = sourceManager.getCatalogueSources()
            .firstOrNull { it.name == sourceName }
}
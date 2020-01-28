package eu.kanade.tachiyomi.ui.catalogue.latest

import android.os.Bundle
import androidx.drawerlayout.widget.DrawerLayout
import android.view.Menu
import android.view.ViewGroup
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.ui.catalogue.browse.BrowseCatalogueController
import eu.kanade.tachiyomi.ui.catalogue.browse.BrowseCataloguePresenter

/**
 * Controller that shows the latest manga from the catalogue. Inherit [BrowseCatalogueController].
 */
class LatestUpdatesController(bundle: Bundle) : BrowseCatalogueController(bundle) {

    constructor(source: CatalogueSource) : this(Bundle().apply {
        putLong(SOURCE_ID_KEY, source.id)
    })

    override fun createPresenter(): BrowseCataloguePresenter {
        return LatestUpdatesPresenter(args.getLong(SOURCE_ID_KEY))
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.action_search).isVisible = false
        menu.findItem(R.id.action_set_filter).isVisible = false
    }

    override fun createSecondaryDrawer(drawer: DrawerLayout): ViewGroup? {
        return null
    }

    override fun cleanupSecondaryDrawer(drawer: DrawerLayout) {

    }

}
